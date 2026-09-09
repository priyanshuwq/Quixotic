# Data README

## Dataset Structure

This directory contains training data for the BhashaSetu NMT model.

### Expected Data Format

- **Parallel Corpora**: Hindi-Tribal language sentence pairs
- **File Format**: Parquet, CSV, or TSV
- **Columns**: `source` (Hindi), `target` (Tribal language), `domain` (optional)

### Data Sources

1. **IIT Bombay Parallel Corpus**: Hindi-English parallel sentences
2. **PMO India Translations**: Government document translations
3. **AI4Bharat Datasets**: Indic language parallel corpora
4. **Custom Datasets**: Domain-specific educational content

### Data Processing

```bash
python scripts/process_data.py --input data/raw --output data/processed
```

### Language Codes

| Language | Code | Family |
|----------|------|--------|
| Hindi | hi | Indo-Aryan |
| Ho | ho | Munda |
| Mundari | mnj | Munda |
| Santhali | sat | Munda |

### Adding New Data

1. Place raw files in `data/raw/`
2. Run preprocessing: `python scripts/preprocess.py`
3. Verify output in `data/processed/`
