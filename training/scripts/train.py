#!/usr/bin/env python
"""Main training script for BhashaSetu NMT model."""

import argparse
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from src.utils.helpers import load_config, set_seed
from src.models.nmt import NMTModel
from src.training.trainer import NMTTrainer


def main():
    parser = argparse.ArgumentParser(description="Train BhashaSetu NMT Model")
    parser.add_argument(
        "--config",
        type=str,
        default="training/configs/nmt_config.yaml",
        help="Path to config file",
    )
    parser.add_argument(
        "--resume",
        type=str,
        default=None,
        help="Path to checkpoint to resume from",
    )
    args = parser.parse_args()
    
    # Load config
    config = load_config(args.config)
    
    # Set seed
    set_seed(config.get("training", {}).get("seed", 42))
    
    print("=" * 60)
    print("BhashaSetu NMT Training")
    print("=" * 60)
    
    # Initialize model
    print("\nInitializing model...")
    model = NMTModel(
        model_name=config["model"]["name"],
        max_length=config["model"]["max_length"],
        dropout=config["model"]["dropout"],
        label_smoothing=config["model"]["label_smoothing"],
    )
    
    # TODO: Load data loaders
    # train_loader = ...
    # val_loader = ...
    
    # Initialize trainer
    # trainer = NMTTrainer(
    #     model=model,
    #     train_loader=train_loader,
    #     val_loader=val_loader,
    #     config=config["training"],
    #     output_dir=config["training"]["output_dir"],
    # )
    
    # Resume from checkpoint if specified
    if args.resume:
        print(f"Resuming from checkpoint: {args.resume}")
        # trainer.load_checkpoint(args.resume)
    
    # Start training
    # trainer.train(epochs=config["training"]["epochs"])
    
    print("\nTraining complete!")
    print("TODO: Implement data loading and run training")


if __name__ == "__main__":
    main()
