"""FastAPI backend for BhashaSetu."""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="BhashaSetu API",
    description="AI-Powered Vernacular Pedagogy and Translation Tool",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root():
    return {
        "name": "BhashaSetu API",
        "version": "0.1.0",
        "description": "AI-Powered Vernacular Pedagogy and Translation Tool",
    }


@app.get("/health")
async def health():
    return {"status": "healthy"}


# TODO: Add translation, ASR, TTS endpoints
