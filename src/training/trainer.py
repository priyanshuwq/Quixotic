"""NMT Training Loop."""

import os
import time
from typing import Optional, Dict
import torch
import torch.nn as nn
from torch.utils.data import DataLoader
from torch.optim import AdamW
from transformers import get_linear_schedule_with_warmup

from ..models.nmt import NMTModel


class NMTTrainer:
    """
    Training loop for NMT model with mixed precision and gradient accumulation.
    
    Features:
        - Triangular LR schedule
        - Gradient accumulation
        - Mixed precision training (FP16)
        - Checkpoint saving
        - Metric logging
    """
    
    def __init__(
        self,
        model: NMTModel,
        train_loader: DataLoader,
        val_loader: DataLoader,
        config: Dict,
        output_dir: str = "models/checkpoints",
    ):
        self.model = model
        self.train_loader = train_loader
        self.val_loader = val_loader
        self.config = config
        self.output_dir = output_dir
        
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model.to(self.device)
        
        # Optimizer
        self.optimizer = AdamW(
            self.model.parameters(),
            lr=config.get("learning_rate", 0.001),
            weight_decay=config.get("weight_decay", 0.01),
        )
        
        # Scheduler
        total_steps = len(train_loader) * config.get("epochs", 50)
        warmup_steps = config.get("warmup_steps", 4000)
        
        self.scheduler = get_linear_schedule_with_warmup(
            self.optimizer,
            num_warmup_steps=warmup_steps,
            num_training_steps=total_steps,
        )
        
        # Mixed precision
        self.scaler = torch.cuda.amp.GradScaler() if config.get("fp16", True) else None
        
        # Gradient accumulation
        self.grad_accum_steps = config.get("gradient_accumulation_steps", 1)
        
        self.best_bleu = 0.0
        
    def train(self, epochs: Optional[int] = None):
        """Run training loop."""
        epochs = epochs or self.config.get("epochs", 50)
        
        for epoch in range(epochs):
            print(f"\n{'='*60}")
            print(f"Epoch {epoch + 1}/{epochs}")
            print(f"{'='*60}")
            
            # Train
            train_metrics = self._train_epoch()
            
            # Validate
            val_metrics = self._validate()
            
            # Log
            print(f"\nTrain Loss: {train_metrics['loss']:.4f}")
            print(f"Val Loss: {val_metrics['loss']:.4f}")
            print(f"Val BLEU: {val_metrics.get('bleu', 0):.2f}")
            
            # Save checkpoint
            if val_metrics.get("bleu", 0) > self.best_bleu:
                self.best_bleu = val_metrics.get("bleu", 0)
                self._save_checkpoint(epoch, val_metrics)
    
    def _train_epoch(self) -> Dict:
        """Train for one epoch."""
        self.model.train()
        total_loss = 0.0
        
        for batch_idx, batch in enumerate(self.train_loader):
            # Move to device
            input_ids = batch["input_ids"].to(self.device)
            attention_mask = batch["attention_mask"].to(self.device)
            labels = batch["labels"].to(self.device)
            
            # Forward pass with optional mixed precision
            if self.scaler:
                with torch.cuda.amp.autocast():
                    outputs = self.model(input_ids, attention_mask, labels)
                    loss = outputs["loss"] / self.grad_accum_steps
                
                self.scaler.scale(loss).backward()
                
                if (batch_idx + 1) % self.grad_accum_steps == 0:
                    self.scaler.unscale_(self.optimizer)
                    torch.nn.utils.clip_grad_norm_(self.model.parameters(), 1.0)
                    self.scaler.step(self.optimizer)
                    self.scaler.update()
                    self.scheduler.step()
                    self.optimizer.zero_grad()
            else:
                outputs = self.model(input_ids, attention_mask, labels)
                loss = outputs["loss"] / self.grad_accum_steps
                loss.backward()
                
                if (batch_idx + 1) % self.grad_accum_steps == 0:
                    torch.nn.utils.clip_grad_norm_(self.model.parameters(), 1.0)
                    self.optimizer.step()
                    self.scheduler.step()
                    self.optimizer.zero_grad()
            
            total_loss += loss.item() * self.grad_accum_steps
            
            if (batch_idx + 1) % 100 == 0:
                print(f"  Step {batch_idx + 1}/{len(self.train_loader)} | Loss: {loss.item():.4f}")
        
        return {"loss": total_loss / len(self.train_loader)}
    
    def _validate(self) -> Dict:
        """Validate the model."""
        self.model.eval()
        total_loss = 0.0
        
        with torch.no_grad():
            for batch in self.val_loader:
                input_ids = batch["input_ids"].to(self.device)
                attention_mask = batch["attention_mask"].to(self.device)
                labels = batch["labels"].to(self.device)
                
                outputs = self.model(input_ids, attention_mask, labels)
                total_loss += outputs["loss"].item()
        
        return {
            "loss": total_loss / len(self.val_loader),
            "bleu": 0.0,  # Placeholder - implement BLEU calculation
        }
    
    def _save_checkpoint(self, epoch: int, metrics: Dict):
        """Save model checkpoint."""
        os.makedirs(self.output_dir, exist_ok=True)
        
        checkpoint = {
            "epoch": epoch,
            "model_state_dict": self.model.state_dict(),
            "optimizer_state_dict": self.optimizer.state_dict(),
            "scheduler_state_dict": self.scheduler.state_dict(),
            "metrics": metrics,
            "config": self.config,
        }
        
        path = os.path.join(self.output_dir, f"checkpoint_epoch_{epoch + 1}.pt")
        torch.save(checkpoint, path)
        print(f"  Saved checkpoint: {path}")
