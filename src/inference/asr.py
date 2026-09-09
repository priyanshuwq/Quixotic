"""ASR (Automatic Speech Recognition) pipeline using WHISPER."""

import time
from typing import Dict, Optional
import numpy as np


class ASRPipeline:
    """
    Speech-to-Text pipeline using WHISPER-tiny.
    
    Supports:
        - Real-time voice transcription
        - Hindi and tribal language recognition
        - Low-latency processing
    """
    
    def __init__(
        self,
        model_name: str = "openai/whisper-tiny",
        language: str = "hi",
        device: str = "cpu",
    ):
        self.model_name = model_name
        self.language = language
        self.device = device
        self.model = None
        
    def load_model(self):
        """Load WHISPER model."""
        try:
            import whisper
            self.model = whisper.load_model(self.model_name.split("/")[-1])
        except ImportError:
            raise ImportError("Please install whisper: pip install openai-whisper")
    
    def transcribe(
        self,
        audio: np.ndarray,
        sample_rate: int = 16000,
    ) -> Dict[str, any]:
        """
        Transcribe audio to text.
        
        Args:
            audio: Audio array (float32, mono)
            sample_rate: Audio sample rate
            
        Returns:
            Dictionary with transcribed text and metadata
        """
        if self.model is None:
            self.load_model()
        
        start_time = time.time()
        
        # Ensure correct sample rate
        if sample_rate != 16000:
            import librosa
            audio = librosa.resample(audio, orig_sr=sample_rate, target_sr=16000)
        
        # Transcribe
        result = self.model.transcribe(
            audio,
            language=self.language,
            task="transcribe",
            fp16=False,
        )
        
        latency = time.time() - start_time
        
        return {
            "text": result["text"],
            "language": result.get("language", self.language),
            "segments": result.get("segments", []),
            "latency_ms": latency * 1000,
        }
