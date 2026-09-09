"""Triangular Learning Rate Scheduler."""

import math
from torch.optim.lr_scheduler import _LRScheduler


class TriangularLRScheduler(_LRScheduler):
    """
    Triangular learning rate schedule with warmup and decay.
    
    Used in BhashaSetu for stable NMT training.
    """
    
    def __init__(
        self,
        optimizer,
        warmup_steps: int,
        decay_steps: int,
        min_lr: float = 1e-6,
        max_lr: float = 1e-3,
        last_epoch: int = -1,
    ):
        self.warmup_steps = warmup_steps
        self.decay_steps = decay_steps
        self.min_lr = min_lr
        self.max_lr = max_lr
        super().__init__(optimizer, last_epoch)
    
    def get_lr(self):
        step = self.last_epoch
        
        if step < self.warmup_steps:
            # Linear warmup
            scale = step / self.warmup_steps
        elif step < self.warmup_steps + self.decay_steps:
            # Linear decay
            progress = (step - self.warmup_steps) / self.decay_steps
            scale = 1.0 - progress
        else:
            # Constant minimum
            scale = 0.0
        
        return [self.min_lr + (self.max_lr - self.min_lr) * scale for _ in self.base_lrs]
