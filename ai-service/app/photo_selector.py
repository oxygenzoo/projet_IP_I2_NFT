import base64
import io
import json
import os
from datetime import datetime
from pathlib import Path

import cv2
import imagehash
import numpy as np
import requests
from PIL import ExifTags, Image


CATEGORIES = [
    "natural landscape",
    "mountain or sky",
    "portrait or people",
    "monument or architecture",
    "food or restaurant",
    "beach or sea",
    "animal",
    "indoor or night",
    "transport or vehicle",
    "party or event",
]

SHORT_LABELS = {
    "natural landscape": "paysage",
    "mountain or sky": "ciel_montagne",
    "portrait or people": "portrait",
    "monument or architecture": "monument",
    "food or restaurant": "nourriture",
    "beach or sea": "plage",
    "animal": "animal",
    "indoor or night": "nuit_interieur",
    "transport or vehicle": "transport",
    "party or event": "evenement",
}


def sharpness_score(image_path: str) -> float:
    image = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
    if image is None:
        return 0.0
    return float(cv2.Laplacian(image, cv2.CV_64F).var())


def brightness_score(image_path: str) -> float:
    image = cv2.imread(image_path)
    if image is None:
        return 0.0
    hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)
    return float(np.mean(hsv[:, :, 2]))


def usability(image_path: str, sharpness_threshold: float = 80, min_brightness: float = 40,
              max_brightness: float = 220) -> dict:
    sharpness = sharpness_score(image_path)
    brightness = brightness_score(image_path)
    ok_sharpness = sharpness >= sharpness_threshold
    ok_brightness = min_brightness <= brightness <= max_brightness
    return {
        "usable": ok_sharpness and ok_brightness,
        "sharpness": round(sharpness, 1),
        "brightness": round(brightness, 1),
        "reject_reason": (
            "floue" if not ok_sharpness else
            "trop sombre" if brightness < min_brightness else
            "surexposee" if brightness > max_brightness else None
        ),
    }


def visual_hash(image_path: str) -> imagehash.ImageHash:
    return imagehash.phash(Image.open(image_path))


def group_duplicates(photos: list[dict], similarity_threshold: int = 12) -> list[list[dict]]:
    groups: list[list[dict]] = []
    assigned = set()

    for index, photo in enumerate(photos):
        if index in assigned:
            continue

        group = [photo]
        assigned.add(index)
        changed = True

        while changed:
            changed = False
            for other_index, other in enumerate(photos):
                if other_index in assigned:
                    continue
                if any(member["hash"] - other["hash"] <= similarity_threshold for member in group):
                    group.append(other)
                    assigned.add(other_index)
                    changed = True

        groups.append(group)

    return groups


def best_of_group(group: list[dict]) -> dict:
    return max(group, key=lambda photo: photo["sharpness"])


def extract_exif(image_path: str) -> dict:
    try:
        image = Image.open(image_path)
        raw_exif = image._getexif()
        if not raw_exif:
            return {}

        exif = {ExifTags.TAGS.get(key, key): value for key, value in raw_exif.items()}
        metadata = {}

        if "DateTimeOriginal" in exif:
            metadata["date"] = exif["DateTimeOriginal"]
        elif "DateTime" in exif:
            metadata["date"] = exif["DateTime"]

        if "GPSInfo" in exif:
            gps = {ExifTags.GPSTAGS.get(key, key): value for key, value in exif["GPSInfo"].items()}

            def to_decimal(dms, ref):
                degrees, minutes, seconds = dms
                decimal = float(degrees) + float(minutes) / 60 + float(seconds) / 3600
                return -decimal if ref in ["S", "W"] else decimal

            if "GPSLatitude" in gps and "GPSLongitude" in gps:
                metadata["latitude"] = to_decimal(gps["GPSLatitude"], gps.get("GPSLatitudeRef", "N"))
                metadata["longitude"] = to_decimal(gps["GPSLongitude"], gps.get("GPSLongitudeRef", "E"))

        if "Model" in exif:
            metadata["device_model"] = str(exif["Model"])

        return metadata
    except Exception:
        return {}


def classify_with_groq(image_path: str, api_key: str) -> str:
    image = Image.open(image_path).convert("RGB")
    image.thumbnail((512, 512))

    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", quality=85)
    encoded = base64.b64encode(buffer.getvalue()).decode()

    categories = "\n".join(f"- {category}" for category in CATEGORIES)
    prompt = (
        "Look at this travel photo and choose exactly one category from this list:\n"
        f"{categories}\n\n"
        "Answer only with the exact category text."
    )

    response = requests.post(
        "https://api.groq.com/openai/v1/chat/completions",
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        json={
            "model": "meta-llama/llama-4-scout-17b-16e-instruct",
            "messages": [{
                "role": "user",
                "content": [
                    {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{encoded}"}},
                    {"type": "text", "text": prompt},
                ],
            }],
            "max_tokens": 20,
            "temperature": 0,
        },
        timeout=20,
    )
    response.raise_for_status()
    answer = response.json()["choices"][0]["message"]["content"].strip().lower()

    for category in CATEGORIES:
        if category in answer or answer in category:
            return SHORT_LABELS[category]

    return "scene_mixte"


def classify_with_google_vision(image_path: str, api_key: str) -> str:
    with open(image_path, "rb") as file:
        encoded = base64.b64encode(file.read()).decode()

    response = requests.post(
        f"https://vision.googleapis.com/v1/images:annotate?key={api_key}",
        json={"requests": [{
            "image": {"content": encoded},
            "features": [
                {"type": "LABEL_DETECTION", "maxResults": 10},
                {"type": "LANDMARK_DETECTION", "maxResults": 3},
                {"type": "FACE_DETECTION", "maxResults": 1},
            ],
        }]},
        timeout=15,
    )
    response.raise_for_status()
    result = response.json()["responses"][0]
    labels = [label["description"].lower() for label in result.get("labelAnnotations", [])]

    if result.get("faceAnnotations"):
        return "portrait"
    if result.get("landmarkAnnotations"):
        return "monument"
    if any(word in labels for word in ["mountain", "sky", "cloud", "hill"]):
        return "ciel_montagne"
    if any(word in labels for word in ["beach", "sea", "ocean", "coast"]):
        return "plage"
    if any(word in labels for word in ["nature", "tree", "forest", "field", "grass"]):
        return "paysage"
    if any(word in labels for word in ["food", "dish", "meal", "restaurant", "cuisine"]):
        return "nourriture"
    if any(word in labels for word in ["animal", "dog", "cat", "bird", "wildlife"]):
        return "animal"
    if any(word in labels for word in ["vehicle", "car", "train", "airplane", "boat"]):
        return "transport"
    if any(word in labels for word in ["night", "dark", "interior", "room", "indoor"]):
        return "nuit_interieur"
    if any(word in labels for word in ["party", "festival", "crowd", "celebration"]):
        return "evenement"

    return "scene_mixte"


def classify_heuristic(image_path: str) -> str:
    image = cv2.imread(image_path)
    if image is None:
        return "inconnu"

    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    height, _width = rgb.shape[:2]
    top_mean = np.mean(rgb[:height // 3, :, :], axis=(0, 1))
    bottom_mean = np.mean(rgb[2 * height // 3:, :, :], axis=(0, 1))
    blue_top = float(top_mean[2]) > float(top_mean[0]) + 20
    green_bottom = float(bottom_mean[1]) > float(bottom_mean[0]) + 15
    brightness = float(np.mean(rgb))

    if blue_top and green_bottom:
        return "paysage"
    if blue_top:
        return "ciel_montagne"
    if brightness < 80:
        return "nuit_interieur"
    if brightness > 180:
        return "exterieur_jour"
    return "scene_mixte"


def classify_scene(image_path: str) -> str:
    groq_key = os.getenv("GROQ_API_KEY") or os.getenv("AI_VISION_API_KEY")
    google_key = os.getenv("GOOGLE_VISION_KEY")

    if groq_key:
        try:
            return classify_with_groq(image_path, groq_key)
        except Exception:
            pass

    if google_key:
        try:
            return classify_with_google_vision(image_path, google_key)
        except Exception:
            pass

    return classify_heuristic(image_path)


def final_score(photo: dict) -> float:
    sharpness = min(photo["sharpness"] / 500 * 50, 50)
    brightness = photo["brightness"]
    ideal_distance = abs(brightness - 120) / 120
    brightness_part = max(0, 30 * (1 - ideal_distance))
    gps_bonus = 10 if "latitude" in photo.get("metadata", {}) else 0
    date_bonus = 10 if "date" in photo.get("metadata", {}) else 0
    scene_bonus = {
        "monument": 5,
        "paysage": 3,
        "plage": 3,
        "portrait": 2,
        "evenement": 4,
        "animal": 3,
        "nourriture": 1,
    }.get(photo.get("scene", ""), 0)
    return round(sharpness + brightness_part + gps_bonus + date_bonus + scene_bonus, 1)


def select_with_diversity(photos: list[dict], top_n: int = 50) -> list[dict]:
    sorted_photos = sorted(photos, key=lambda photo: photo["score"], reverse=True)
    quota = max(1, int(top_n * 0.4))
    counts: dict[str, int] = {}
    selected = []

    for photo in sorted_photos:
        if len(selected) >= top_n:
            break
        scene = photo.get("scene", "inconnu")
        if counts.get(scene, 0) < quota:
            selected.append(photo)
            counts[scene] = counts.get(scene, 0) + 1

    for photo in sorted_photos:
        if len(selected) >= top_n:
            break
        if photo not in selected:
            selected.append(photo)

    return selected


def group_by_day(photos: list[dict]) -> dict:
    groups: dict[str, list[dict]] = {}
    for photo in photos:
        date_text = photo.get("metadata", {}).get("date", "")
        if date_text:
            try:
                key = datetime.strptime(date_text, "%Y:%m:%d %H:%M:%S").strftime("%Y-%m-%d")
            except ValueError:
                key = "sans_date"
        else:
            key = "sans_date"
        groups.setdefault(key, []).append(photo)

    for key in groups:
        groups[key].sort(key=lambda photo: photo.get("metadata", {}).get("date", ""))

    return groups


def is_supported(path: str) -> bool:
    return Path(path).suffix.lower() in {".jpg", ".jpeg", ".png", ".heic", ".webp"}


def run_pipeline(input_dir: str, output_dir: str, top_n: int = 50,
                 sharpness_threshold: float = 80, verbose: bool = False) -> dict:
    all_photos = [str(path) for path in Path(input_dir).rglob("*") if is_supported(str(path))]
    if not all_photos:
        raise ValueError("Aucune photo trouvee pour la generation IA.")

    usable_photos = []
    rejects = {"floue": 0, "trop sombre": 0, "surexposee": 0}

    for path in all_photos:
        result = usability(path, sharpness_threshold)
        if result["usable"]:
            usable_photos.append({
                "path": path,
                "filename": Path(path).name,
                "nom": Path(path).name,
                "sharpness": result["sharpness"],
                "nettete": result["sharpness"],
                "brightness": result["brightness"],
                "luminosite": result["brightness"],
            })
        elif result["reject_reason"] in rejects:
            rejects[result["reject_reason"]] += 1

    if not usable_photos:
        usable_photos = [{
            "path": path,
            "filename": Path(path).name,
            "nom": Path(path).name,
            "sharpness": sharpness_score(path),
            "nettete": sharpness_score(path),
            "brightness": brightness_score(path),
            "luminosite": brightness_score(path),
        } for path in all_photos]

    for photo in usable_photos:
        try:
            photo["hash"] = visual_hash(photo["path"])
        except Exception:
            photo["hash"] = imagehash.phash(Image.new("RGB", (8, 8)))

    unique_photos = [best_of_group(group) for group in group_duplicates(usable_photos)]

    for photo in unique_photos:
        photo["metadata"] = extract_exif(photo["path"])
        photo["scene"] = classify_scene(photo["path"])
        photo["score"] = final_score(photo)
        photo["final_score"] = photo["score"]

    selected = select_with_diversity(unique_photos, top_n)
    by_day = group_by_day(selected)

    Path(output_dir).mkdir(parents=True, exist_ok=True)
    exported_photos = [{key: value for key, value in photo.items() if key != "hash"} for photo in selected]
    report = {
        "total_initial": len(all_photos),
        "apres_qualite": len(usable_photos),
        "apres_doublons": len(unique_photos),
        "selection_finale": len(selected),
        "rejets": rejects,
        "par_journee": {
            day: [{"nom": photo["nom"], "score": photo["score"], "scene": photo["scene"]} for photo in photos]
            for day, photos in by_day.items()
        },
        "photos": exported_photos,
    }

    report_path = Path(output_dir) / "selection_rapport.json"
    with open(report_path, "w", encoding="utf-8") as file:
        json.dump(report, file, ensure_ascii=False, indent=2)

    if verbose:
        print(f"Photo selection report written to {report_path}")

    return report
