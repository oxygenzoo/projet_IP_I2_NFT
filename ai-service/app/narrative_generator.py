from __future__ import annotations

import json
import os
import re
import time
from datetime import datetime
from pathlib import Path

import requests


SYSTEM_PROMPT = (
    "Tu es un narrateur de voyages expert. Tu crees des scripts video immersifs, "
    "poetiques mais accessibles. Tu reponds uniquement en JSON valide."
)


def load_report(report_path: str) -> dict:
    with open(report_path, "r", encoding="utf-8") as file:
        return json.load(file)


def photos_by_day(report: dict) -> dict:
    grouped = {}
    full_photos = {photo["nom"]: photo for photo in report.get("photos", [])}

    for day, summaries in report.get("par_journee", {}).items():
        photos = [full_photos.get(summary["nom"], summary) for summary in summaries]
        coords = []
        for photo in photos:
            metadata = photo.get("metadata", {})
            if "latitude" in metadata and "longitude" in metadata:
                coords.append((metadata["latitude"], metadata["longitude"]))

        grouped[day] = {
            "photos": photos,
            "coords": coords,
            "scenes": sorted({photo.get("scene", "inconnu") for photo in photos}),
            "photo_count": len(photos),
        }

    return grouped


def reverse_geocode(lat: float, lon: float) -> str:
    try:
        response = requests.get(
            "https://nominatim.openstreetmap.org/reverse",
            params={"lat": lat, "lon": lon, "format": "json", "accept-language": "fr"},
            headers={"User-Agent": "NFT-Travel-App/1.0"},
            timeout=5,
        )
        response.raise_for_status()
        address = response.json().get("address", {})
        place = (
            address.get("city") or address.get("town") or address.get("village") or
            address.get("county") or address.get("state") or address.get("country") or
            "lieu inconnu"
        )
        country = address.get("country", "")
        return f"{place}, {country}" if country and country not in place else place
    except Exception:
        return "lieu inconnu"


def wikipedia_context(place_name: str) -> str:
    try:
        search_response = requests.get(
            "https://fr.wikipedia.org/w/api.php",
            params={"action": "query", "list": "search", "srsearch": place_name, "format": "json", "srlimit": 1},
            timeout=5,
        )
        results = search_response.json().get("query", {}).get("search", [])
        if not results:
            return ""

        extract_response = requests.get(
            "https://fr.wikipedia.org/w/api.php",
            params={
                "action": "query",
                "prop": "extracts",
                "exintro": True,
                "explaintext": True,
                "titles": results[0]["title"],
                "format": "json",
                "exsentences": 3,
            },
            timeout=5,
        )
        pages = extract_response.json().get("query", {}).get("pages", {})
        for page in pages.values():
            extract = page.get("extract", "")
            if extract:
                return extract[:300].strip()
    except Exception:
        pass

    return ""


def enrich_day(day_data: dict, travel_name: str, index: int) -> dict:
    coords = day_data.get("coords", [])
    if coords:
        lat = sum(coord[0] for coord in coords) / len(coords)
        lon = sum(coord[1] for coord in coords) / len(coords)
        place = reverse_geocode(lat, lon)
        time.sleep(1)
        context = wikipedia_context(place)
    else:
        place = travel_name or f"Souvenir {index + 1}"
        context = ""

    day_data["main_place"] = place
    day_data["wikipedia_context"] = context
    return day_data


def build_prompt(day: str, day_data: dict, travel_name: str, preferences: dict) -> str:
    photos = "\n".join(
        f"- {photo.get('nom', 'photo')} (scene: {photo.get('scene', 'inconnu')}, score: {photo.get('score', 0)}/100)"
        for photo in day_data["photos"]
    )
    preferences_text = json.dumps(preferences or {}, ensure_ascii=False)
    context = day_data.get("wikipedia_context", "")

    return f"""
VOYAGE: {travel_name}
DATE: {day}
LIEU PRINCIPAL: {day_data.get('main_place', 'inconnu')}
PREFERENCES UTILISATEUR: {preferences_text}
TYPES DE SCENES: {', '.join(day_data.get('scenes', []))}
CONTEXTE: {context}

PHOTOS DISPONIBLES:
{photos}

Genere un JSON avec exactement cette structure:
{{
  "episode_titre": "Titre court",
  "episode_numero": 1,
  "lieu": "{day_data.get('main_place', 'inconnu')}",
  "date": "{day}",
  "intro": "2 phrases d'introduction",
  "scenes": [
    {{
      "scene_numero": 1,
      "photo_fichier": "nom_exact.jpg",
      "voix_off": "1 ou 2 phrases",
      "texte_ecran": "Lieu · date",
      "duree_secondes": 6,
      "effet": "ken_burns_zoom_in"
    }}
  ],
  "outro": "1 phrase de conclusion",
  "musique_ambiance": "style musical"
}}

Regles: utilise les noms exacts des photos, un fichier par scene, duree entre 4 et 8 secondes.
"""


def generate_with_groq(prompt: str, api_key: str) -> dict:
    response = requests.post(
        "https://api.groq.com/openai/v1/chat/completions",
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        json={
            "model": os.getenv("GROQ_TEXT_MODEL", "llama-3.3-70b-versatile"),
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.7,
            "max_tokens": 2200,
            "response_format": {"type": "json_object"},
        },
        timeout=45,
    )
    response.raise_for_status()
    return json.loads(response.json()["choices"][0]["message"]["content"].strip())


def generate_with_gemini(prompt: str, api_key: str) -> dict:
    payload = {
        "contents": [{"parts": [{"text": f"{SYSTEM_PROMPT}\n\n{prompt}"}]}],
        "generationConfig": {
            "temperature": 0.7,
            "maxOutputTokens": 2200,
            "responseMimeType": "application/json",
        },
    }
    models = ["gemini-2.0-flash-lite", "gemini-2.0-flash", "gemini-2.5-flash", "gemini-flash-latest"]
    last_error = None

    for model in models:
        try:
            response = requests.post(
                f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}",
                json=payload,
                timeout=45,
            )
            if response.status_code == 404:
                continue
            response.raise_for_status()
            text = response.json()["candidates"][0]["content"]["parts"][0]["text"].strip()
            text = re.sub(r"^```(?:json)?|```$", "", text).strip()
            return json.loads(text)
        except Exception as error:
            last_error = error

    raise RuntimeError(f"Aucun modele Gemini disponible: {last_error}")


def fallback_episode(day: str, day_data: dict, travel_name: str, episode_number: int, preferences: dict) -> dict:
    style = (preferences or {}).get("style", "cinematographique")
    place = day_data.get("main_place") or travel_name or f"Souvenir {episode_number}"
    scenes = []

    for index, photo in enumerate(day_data.get("photos", [])[:12], start=1):
        scene = photo.get("scene", "souvenir")
        scene_label = human_scene_label(scene)
        scenes.append({
            "scene_numero": index,
            "photo_fichier": photo.get("nom", photo.get("filename", "")),
            "voix_off": fallback_voice_over(scene_label, place, index),
            "texte_ecran": f"{place} · {day}",
            "duree_secondes": 6,
            "effet": ["ken_burns_zoom_in", "ken_burns_zoom_out", "pan_right", "static"][index % 4],
        })

    return {
        "episode_titre": f"{place}",
        "episode_numero": episode_number,
        "lieu": place,
        "date": day,
        "intro": f"{travel_name} se reconstruit à travers les meilleurs souvenirs de cette journée.",
        "scenes": scenes,
        "outro": "Ces images composent une mémoire de voyage prête à revivre.",
        "musique_ambiance": "ambient cinématographique doux",
        "nb_photos_source": day_data.get("photo_count", len(scenes)),
    }


def human_scene_label(scene: str) -> str:
    labels = {
        "scene_mixte": "souvenir spontané",
        "souvenir": "souvenir",
        "paysage": "paysage",
        "portrait": "portrait",
        "groupe": "moment partagé",
        "repas": "pause gourmande",
        "monument": "découverte",
    }
    normalized = str(scene or "souvenir").strip().lower()
    return labels.get(normalized, normalized.replace("_", " "))


def fallback_voice_over(scene_label: str, place: str, index: int) -> str:
    templates = [
        "On garde ce moment comme une petite capsule de voyage, simple et lumineuse.",
        f"A {place}, ce {scene_label} raconte une partie de l'ambiance que les photos ne veulent pas laisser filer.",
        "La journée continue avec ces détails qui deviennent, sans prévenir, les vrais souvenirs.",
        f"Ce passage remet {place} au centre du récit, entre mouvement, regards et petits instants retrouvés.",
    ]
    return templates[(index - 1) % len(templates)]


def generate_episode(prompt: str, api_key: str | None) -> dict | None:
    if not api_key:
        return None

    provider = "groq" if api_key.startswith("gsk_") else "gemini"
    for attempt in range(3):
        try:
            if provider == "groq":
                return generate_with_groq(prompt, api_key)
            return generate_with_gemini(prompt, api_key)
        except Exception:
            if attempt == 2:
                return None
            time.sleep(3)

    return None


def run_pipeline(report_path: str, output_dir: str, travel_name: str,
                 preferences: dict | None = None, api_key: str | None = None,
                 max_episodes: int | None = None, verbose: bool = False) -> dict:
    report = load_report(report_path)
    grouped = photos_by_day(report)
    days = sorted(grouped.keys())
    if max_episodes:
        days = days[:max_episodes]

    scripts = []
    for index, day in enumerate(days):
        day_data = enrich_day(grouped[day], travel_name, index)
        prompt = build_prompt(day, day_data, travel_name, preferences or {})
        script = generate_episode(prompt, api_key)

        if not script:
            script = fallback_episode(day, day_data, travel_name, index + 1, preferences or {})

        script["episode_numero"] = index + 1
        script["date"] = day
        script["nb_photos_source"] = day_data.get("photo_count", len(script.get("scenes", [])))
        script["dossier_photos"] = str(Path(report_path).parent)
        scripts.append(script)

    output = {
        "voyage": travel_name,
        "preferences": preferences or {},
        "genere_le": datetime.now().isoformat(),
        "nb_episodes": len(scripts),
        "provider_llm": "api" if api_key else "fallback",
        "episodes": scripts,
    }

    Path(output_dir).mkdir(parents=True, exist_ok=True)
    output_path = Path(output_dir) / "scripts_episodes.json"
    with open(output_path, "w", encoding="utf-8") as file:
        json.dump(output, file, ensure_ascii=False, indent=2)

    if verbose:
        print(f"Narrative scripts written to {output_path}")

    return output
