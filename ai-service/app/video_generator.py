from __future__ import annotations

import json
import os
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont


def font(size: int):
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/Library/Fonts/Arial.ttf",
    ]
    for candidate in candidates:
        try:
            return ImageFont.truetype(candidate, size)
        except Exception:
            pass
    return ImageFont.load_default()


def wrap_text(text: str, max_chars: int = 70) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        if len(current) + len(word) + 1 <= max_chars:
            current = f"{current} {word}".strip()
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


VIDEO_WIDTH = int(os.getenv("AI_VIDEO_WIDTH", "854"))
VIDEO_HEIGHT = int(os.getenv("AI_VIDEO_HEIGHT", "480"))
VIDEO_FPS = int(os.getenv("AI_VIDEO_FPS", "12"))
VIDEO_MAX_SCENES = int(os.getenv("AI_VIDEO_MAX_SCENES", "6"))


def load_image(path: str, width: int = VIDEO_WIDTH, height: int = VIDEO_HEIGHT) -> np.ndarray:
    image = Image.open(path).convert("RGB")
    source_w, source_h = image.size
    target_ratio = width / height
    source_ratio = source_w / source_h

    if source_ratio > target_ratio:
        new_w = int(source_h * target_ratio)
        offset = (source_w - new_w) // 2
        image = image.crop((offset, 0, offset + new_w, source_h))
    else:
        new_h = int(source_w / target_ratio)
        offset = (source_h - new_h) // 2
        image = image.crop((0, offset, source_w, offset + new_h))

    return np.array(image.resize((width, height), Image.LANCZOS))


def text_card(lines: list[tuple[str, tuple[int, int, int], int]],
              width: int = VIDEO_WIDTH, height: int = VIDEO_HEIGHT) -> np.ndarray:
    image = Image.new("RGB", (width, height), color=(10, 10, 15))
    draw = ImageDraw.Draw(image)
    draw.rectangle([40, 40, width - 40, 42], fill=(80, 70, 120))
    draw.rectangle([40, height - 42, width - 40, height - 40], fill=(80, 70, 120))

    total_height = len(lines) * 54
    y = (height - total_height) // 2
    for text, color, size in lines:
        draw.text((width // 2, y), text, fill=color, font=font(size), anchor="mm")
        y += 54

    return np.array(image)


def add_caption(frame: np.ndarray, voiceover: str, screen_text: str = "") -> np.ndarray:
    image = Image.fromarray(frame.astype(np.uint8))
    draw = ImageDraw.Draw(image, "RGBA")
    width, height = image.size

    if screen_text:
        draw.rectangle([0, 0, width, 42], fill=(0, 0, 0, 150))
        draw.text((20, 21), screen_text, fill=(230, 225, 245, 255), font=font(20), anchor="lm")

    if voiceover:
        lines = wrap_text(voiceover, 58)
        band_height = 34 + len(lines) * 34
        draw.rectangle([0, height - band_height, width, height], fill=(0, 0, 0, 170))
        y = height - band_height + 22
        for line in lines:
            draw.text((width // 2, y), line, fill=(245, 240, 255, 255), font=font(24), anchor="mm")
            y += 34

    return np.array(image.convert("RGB"))


def find_photo(filename: str, photos_dir: str) -> str | None:
    expected = filename.lower()
    for path in Path(photos_dir).rglob("*"):
        if path.is_file() and path.name.lower() == expected:
            return str(path)
    return None


def generate_episode(episode: dict, photos_dir: str, output_dir: str) -> str | None:
    from moviepy import ImageClip, concatenate_videoclips

    Path(output_dir).mkdir(parents=True, exist_ok=True)
    number = episode.get("episode_numero", 1)
    title = episode.get("episode_titre", f"Episode {number}")
    place = episode.get("lieu", "")
    date = episode.get("date", "")
    clips = []

    clips.append(ImageClip(text_card([
        (f"Episode {number}", (140, 120, 200), 24),
        (title, (255, 255, 255), 42),
        (place, (190, 185, 210), 26),
        (date, (140, 135, 160), 22),
    ]), duration=2.5))

    for line in wrap_text(episode.get("intro", ""), 58):
        clips.append(ImageClip(text_card([(line, (220, 215, 240), 26)]), duration=1.8))

    for scene in episode.get("scenes", [])[:VIDEO_MAX_SCENES]:
        photo_path = find_photo(scene.get("photo_fichier", ""), photos_dir)
        duration = min(float(scene.get("duree_secondes", 4)), 4.0)
        if not photo_path:
            frame = text_card([
                ("Photo introuvable", (220, 90, 90), 40),
                (scene.get("photo_fichier", ""), (180, 180, 190), 26),
            ])
        else:
            frame = add_caption(
                load_image(photo_path),
                scene.get("voix_off", ""),
                scene.get("texte_ecran", ""),
            )
        clips.append(ImageClip(frame, duration=duration))

    for line in wrap_text(episode.get("outro", ""), 58):
        clips.append(ImageClip(text_card([(line, (220, 215, 240), 26)]), duration=1.8))

    if not clips:
        return None

    video = concatenate_videoclips(clips, method="compose")
    try:
        safe_title = "".join(char if char.isalnum() or char in "-_" else "_" for char in title[:35])
        output_path = str(Path(output_dir) / f"episode_{number:02d}_{safe_title}.mp4")
        video.write_videofile(output_path, fps=VIDEO_FPS, codec="libx264", audio=False, logger=None)
        return output_path
    finally:
        video.close()
        for clip in clips:
            clip.close()


def run_pipeline(scripts_path: str, photos_dir: str, output_dir: str,
                 max_episodes: int | None = None) -> list[str]:
    with open(scripts_path, "r", encoding="utf-8") as file:
        data = json.load(file)

    episodes = data.get("episodes", [])
    if max_episodes:
        episodes = episodes[:max_episodes]

    generated = []
    for episode in episodes:
        path = generate_episode(episode, photos_dir, output_dir)
        if path:
            generated.append(path)

    return generated
