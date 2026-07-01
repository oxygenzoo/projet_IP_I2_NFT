from __future__ import annotations

import json
import os
import shutil
from pathlib import Path
from typing import Annotated, Optional
from uuid import uuid4

from fastapi import FastAPI, File, Form, HTTPException, Request, UploadFile
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from app import narrative_generator, photo_selector, video_generator


app = FastAPI(title="NFT AI Service", version="0.1.0")
BASE_WORKDIR = Path(os.getenv("AI_WORKDIR", "workdir")).resolve()
BASE_WORKDIR.mkdir(parents=True, exist_ok=True)
app.mount("/media", StaticFiles(directory=str(BASE_WORKDIR)), name="media")


class GenerationResponse(BaseModel):
    job_id: str
    status: str
    message: str
    selection_report: dict
    script: dict
    videos: list[str]
    workdir: str


def safe_filename(filename: str) -> str:
    cleaned = "".join(char if char.isalnum() or char in "._-" else "_" for char in filename)
    return cleaned or "photo.jpg"


def parse_preferences(raw_preferences: Optional[str]) -> dict:
    if not raw_preferences:
        return {}
    try:
        parsed = json.loads(raw_preferences)
        return parsed if isinstance(parsed, dict) else {}
    except json.JSONDecodeError:
        return {}


def should_render_video() -> bool:
    return os.getenv("AI_RENDER_VIDEO", "false").lower() in {"1", "true", "yes", "on"}


@app.get("/health")
def health() -> dict:
    return {"status": "UP"}


@app.post("/ai/episodes", response_model=GenerationResponse)
async def generate_episode(
    request: Request,
    images: Annotated[list[UploadFile], File(description="Travel photos")],
    title: Annotated[str, Form()] = "Mon voyage",
    destination: Annotated[str, Form()] = "",
    preferences: Annotated[Optional[str], Form()] = None,
) -> GenerationResponse:
    if not images:
        raise HTTPException(status_code=400, detail="Ajoutez au moins une photo.")

    job_id = str(uuid4())
    base_dir = BASE_WORKDIR
    job_dir = base_dir / "jobs" / job_id
    photos_dir = job_dir / "photos"
    selection_dir = job_dir / "selection"
    scripts_dir = job_dir / "scripts"
    videos_dir = job_dir / "videos"

    try:
        photos_dir.mkdir(parents=True, exist_ok=True)

        for upload in images:
            if not upload.content_type or not upload.content_type.startswith("image/"):
                raise HTTPException(status_code=400, detail=f"{upload.filename} n'est pas une image.")

            target = photos_dir / safe_filename(upload.filename or "photo.jpg")
            with target.open("wb") as file:
                shutil.copyfileobj(upload.file, file)

        parsed_preferences = parse_preferences(preferences)
        travel_name = title if not destination else f"{title} - {destination}"
        top_n = int(os.getenv("AI_TOP_PHOTOS", "50"))
        max_episodes = int(os.getenv("AI_MAX_EPISODES", "6"))
        llm_key = os.getenv("AI_LLM_API_KEY") or os.getenv("GROQ_API_KEY") or os.getenv("GEMINI_API_KEY")

        selection_report = photo_selector.run_pipeline(
            input_dir=str(photos_dir),
            output_dir=str(selection_dir),
            top_n=top_n,
        )
        script = narrative_generator.run_pipeline(
            report_path=str(selection_dir / "selection_rapport.json"),
            output_dir=str(scripts_dir),
            travel_name=travel_name,
            preferences=parsed_preferences,
            api_key=llm_key,
            max_episodes=max_episodes,
        )

        videos: list[str] = []
        if should_render_video():
            videos = video_generator.run_pipeline(
                scripts_path=str(scripts_dir / "scripts_episodes.json"),
                photos_dir=str(photos_dir),
                output_dir=str(videos_dir),
                max_episodes=max_episodes,
            )

        video_urls = [
            str(request.url_for("media", path=Path(video).resolve().relative_to(base_dir).as_posix()))
            for video in videos
        ]

        return GenerationResponse(
            job_id=job_id,
            status="completed",
            message="Episode genere par le service IA.",
            selection_report=selection_report,
            script=script,
            videos=video_urls,
            workdir=str(job_dir),
        )
    except HTTPException:
        raise
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error
