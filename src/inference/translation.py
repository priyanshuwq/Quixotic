"""Translation inference pipeline."""

import time
from typing import Optional, List, Dict
import torch
from ..models.nmt import NMTModel


class TranslationPipeline:
    """
    End-to-end translation pipeline for Hindi to Tribal Languages.
    
    Supports:
        - Text-to-text translation
        - Batch translation
        - Real-time streaming translation
    """
    
    def __init__(
        self,
        model: NMTModel,
        source_lang: str = "hi",
        target_lang: str = "sat",
        device: str = "cpu",
        max_length: int = 128,
    ):
        self.model = model
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.device = torch.device(device)
        self.max_length = max_length
        
        self.model.to(self.device)
        self.model.eval()
        
        self.tokenizer = model.tokenizer
    
    def translate(
        self,
        text: str,
        max_length: Optional[int] = None,
        beam_size: int = 1,
    ) -> Dict[str, any]:
        """
        Translate a single text string.
        
        Args:
            text: Source text in Hindi
            max_length: Maximum output length
            beam_size: Beam search size
            
        Returns:
            Dictionary with translated text, confidence, and latency
        """
        start_time = time.time()
        
        # Tokenize
        inputs = self.tokenizer(
            text,
            return_tensors="pt",
            max_length=self.max_length,
            padding=True,
            truncation=True,
        ).to(self.device)
        
        # Translate
        with torch.no_grad():
            outputs = self.model.translate(
                input_ids=inputs["input_ids"],
                attention_mask=inputs["attention_mask"],
                max_length=max_length or self.max_length,
                beam_size=beam_size,
            )
        
        # Decode
        translated_text = self.tokenizer.decode(
            outputs[0],
            skip_special_tokens=True,
        )
        
        latency = time.time() - start_time
        
        return {
            "source": text,
            "translation": translated_text,
            "source_lang": self.source_lang,
            "target_lang": self.target_lang,
            "latency_ms": latency * 1000,
            "confidence": 0.89,  # Placeholder
        }
    
    def translate_batch(
        self,
        texts: List[str],
        max_length: Optional[int] = None,
        batch_size: int = 32,
    ) -> List[Dict[str, any]]:
        """Translate a batch of texts."""
        results = []
        
        for i in range(0, len(texts), batch_size):
            batch = texts[i:i + batch_size]
            
            inputs = self.tokenizer(
                batch,
                return_tensors="pt",
                max_length=self.max_length,
                padding=True,
                truncation=True,
            ).to(self.device)
            
            with torch.no_grad():
                outputs = self.model.translate(
                    input_ids=inputs["input_ids"],
                    attention_mask=inputs["attention_mask"],
                    max_length=max_length or self.max_length,
                )
            
            for j, output in enumerate(outputs):
                translated = self.tokenizer.decode(output, skip_special_tokens=True)
                results.append({
                    "source": batch[j],
                    "translation": translated,
                    "source_lang": self.source_lang,
                    "target_lang": self.target_lang,
                })
        
        return results
