"""NMT Model Architecture - DistilBERT + Madlad-400 Backbone."""

import torch
import torch.nn as nn
from transformers import AutoModel, AutoTokenizer


class NMTModel(nn.Module):
    """
    Neural Machine Translation model for Hindi to Tribal Languages.
    
    Architecture:
        - Encoder: DistilBERT/Madlad-400 backbone
        - Decoder: Transformer decoder with cross-attention
        - Output: Linear projection to target vocabulary
    """
    
    def __init__(
        self,
        model_name: str = "distilbert-base-multilingual-cased",
        max_length: int = 128,
        dropout: float = 0.1,
        label_smoothing: float = 0.1,
    ):
        super().__init__()
        self.encoder = AutoModel.from_pretrained(model_name)
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        
        hidden_size = self.encoder.config.hidden_size
        vocab_size = self.encoder.config.vocab_size
        
        # Decoder
        decoder_layer = nn.TransformerDecoderLayer(
            d_model=hidden_size,
            nhead=12,
            dim_feedforward=hidden_size * 4,
            dropout=dropout,
            batch_first=True,
        )
        self.decoder = nn.TransformerDecoder(decoder_layer, num_layers=6)
        
        # Output projection
        self.output_projection = nn.Linear(hidden_size, vocab_size)
        
        # Loss with label smoothing
        self.loss_fn = nn.CrossEntropyLoss(label_smoothing=label_smoothing)
        
        self.max_length = max_length
    
    def encode(self, input_ids: torch.Tensor, attention_mask: torch.Tensor) -> torch.Tensor:
        """Encode source sequence."""
        return self.encoder(input_ids=input_ids, attention_mask=attention_mask).last_hidden_state
    
    def decode(
        self,
        target_ids: torch.Tensor,
        memory: torch.Tensor,
        tgt_mask: torch.Tensor = None,
    ) -> torch.Tensor:
        """Decode target sequence with cross-attention."""
        tgt_emb = self.encoder.embeddings(target_ids)
        return self.decoder(tgt_emb, memory, tgt_mask=tgt_mask)
    
    def forward(
        self,
        input_ids: torch.Tensor,
        attention_mask: torch.Tensor,
        labels: torch.Tensor = None,
    ) -> dict:
        """Forward pass."""
        # Encode source
        memory = self.encode(input_ids, attention_mask)
        
        if labels is not None:
            # Teacher forcing
            shifted_labels = labels[:, :-1]
            outputs = self.decode(shifted_labels, memory)
            logits = self.output_projection(outputs)
            
            # Compute loss
            loss = self.loss_fn(logits.reshape(-1, logits.size(-1)), labels[:, 1:].reshape(-1))
            
            return {"loss": loss, "logits": logits}
        
        return {"memory": memory}
    
    @torch.no_grad()
    def translate(
        self,
        input_ids: torch.Tensor,
        attention_mask: torch.Tensor,
        max_length: int = None,
        beam_size: int = 5,
    ) -> torch.Tensor:
        """Translate source sequence using beam search."""
        if max_length is None:
            max_length = self.max_length
        
        memory = self.encode(input_ids, attention_mask)
        
        # Simple greedy decoding for now
        batch_size = input_ids.size(0)
        device = input_ids.device
        
        # Start with BOS token
        decoder_input = torch.full(
            (batch_size, 1),
            self.tokenizer.cls_token_id or self.tokenizer.bos_token_id,
            dtype=torch.long,
            device=device,
        )
        
        for _ in range(max_length):
            outputs = self.decode(decoder_input, memory)
            next_token_logits = self.output_projection(outputs[:, -1:, :])
            next_token = next_token_logits.argmax(dim=-1)
            decoder_input = torch.cat([decoder_input, next_token], dim=-1)
        
        return decoder_input
