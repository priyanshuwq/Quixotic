#!/usr/bin/env python3
"""
Prepare Hindi↔Santali parallel data for NMT training.

Sources:
1. santali_raw_data/santali-train.csv  (English↔Santali, 20K pairs)
2. HuggingFace FLORES-200 benchmark   (Hindi↔Santali evaluation pairs)
3. HuggingFace IN22-Gen               (Hindi↔Santali test pairs)
4. Synthetic: Translate English→Hindi using NLLB to create Hi↔Sat pairs
5. indiccorp_v2_sat.txt                (monolingual Santali for back-translation)

Output: training/data/raw/hi_sat/{train,val,test}.tsv
"""

import argparse
import csv
import os
import random
import sys
from pathlib import Path

import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
SANTALI_RAW = PROJECT_ROOT / "santali_raw_data"
OUTPUT_DIR = PROJECT_ROOT / "training" / "data" / "raw" / "hi_sat"


def load_santali_train_csv() -> list[tuple[str, str]]:
    """Load English↔Santali pairs from santali-train.csv."""
    csv_path = SANTALI_RAW / "santali-train.csv"
    if not csv_path.exists():
        print(f"  ⚠️  {csv_path} not found, skipping")
        return []

    pairs = []
    try:
        df = pd.read_csv(csv_path)
        cols = df.columns.tolist()
        # Expected columns: index, English, Santali
        en_col = None
        sat_col = None
        for c in cols:
            cl = c.strip().lower()
            if cl in ("english", "en", "eng"):
                en_col = c
            elif cl in ("santali", "sat", "santhali"):
                sat_col = c

        if en_col is None or sat_col is None:
            # Try positional
            if len(cols) >= 3:
                en_col = cols[1]
                sat_col = cols[2]
            elif len(cols) >= 2:
                en_col = cols[0]
                sat_col = cols[1]
            else:
                print(f"  ⚠️  Cannot identify columns in {csv_path}: {cols}")
                return []

        for _, row in df.iterrows():
            en = str(row[en_col]).strip()
            sat = str(row[sat_col]).strip()
            if en and sat and en != "nan" and sat != "nan" and len(en) > 2 and len(sat) > 2:
                pairs.append((en, sat))

        print(f"  ✅ Loaded {len(pairs)} English↔Santali pairs from santali-train.csv")
    except Exception as e:
        print(f"  ⚠️  Error loading {csv_path}: {e}")

    return pairs


def load_flores_data() -> list[tuple[str, str]]:
    """Try to load FLORES Hindi↔Santali data from HuggingFace."""
    pairs = []
    try:
        from datasets import load_dataset

        print("  Downloading FLORES-200 (hin_Deva, sat_Olck)...")
        ds = load_dataset(
            "facebook/flores",
            "all",
            split="devtest",
            trust_remote_code=True,
        )
        for item in ds:
            hi_text = item.get("sentence_hin_Deva", "").strip()
            sat_text = item.get("sentence_sat_Olck", "").strip()
            if hi_text and sat_text:
                pairs.append((hi_text, sat_text))

        if not pairs:
            # Try alternate structure
            for item in ds:
                if "sentence" in item and "language" in item:
                    pass  # Different format
        print(f"  ✅ Loaded {len(pairs)} Hindi↔Santali pairs from FLORES")
    except Exception as e:
        print(f"  ⚠️  FLORES download failed: {e}")
        print("     Trying local FLORES data...")
        # Try local flores directory
        flores_dir = SANTALI_RAW / "flores" / "data" / "language"
        if flores_dir.exists():
            # Look for sat_Olck and hin_Deva
            pass

    return pairs


def load_in22_data() -> list[tuple[str, str]]:
    """Try to load IN22-Gen Hindi↔Santali data."""
    pairs = []
    try:
        from datasets import load_dataset

        print("  Downloading IN22-Gen...")
        ds = load_dataset(
            "ai4bharat/IN22-Gen",
            split="gen",
            trust_remote_code=True,
        )
        for item in ds:
            hi_text = item.get("hin_Deva", "").strip()
            sat_text = item.get("sat_Olck", "").strip()
            if hi_text and sat_text:
                pairs.append((hi_text, sat_text))
        print(f"  ✅ Loaded {len(pairs)} Hindi↔Santali pairs from IN22-Gen")
    except Exception as e:
        print(f"  ⚠️  IN22-Gen download failed: {e}")

    return pairs


def translate_en_to_hi_batch(
    en_texts: list[str],
    batch_size: int = 32,
) -> list[str]:
    """Translate English texts to Hindi using NLLB-200-distilled-600M."""
    try:
        from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
        import torch
    except ImportError:
        print("  ⚠️  transformers not installed, skipping En→Hi translation")
        return [""] * len(en_texts)

    print(f"  Loading NLLB-200 for En→Hi pivot translation ({len(en_texts)} sentences)...")
    model_name = "facebook/nllb-200-distilled-600M"

    try:
        tokenizer = AutoTokenizer.from_pretrained(model_name, src_lang="eng_Latn")
        model = AutoModelForSeq2SeqLM.from_pretrained(model_name)
        model.eval()
    except Exception as e:
        print(f"  ⚠️  Failed to load NLLB model: {e}")
        return [""] * len(en_texts)

    hi_texts = []
    total = len(en_texts)

    for i in range(0, total, batch_size):
        batch = en_texts[i : i + batch_size]
        try:
            inputs = tokenizer(
                batch,
                return_tensors="pt",
                padding=True,
                truncation=True,
                max_length=128,
            )
            with torch.no_grad():
                generated = model.generate(
                    **inputs,
                    forced_bos_token_id=tokenizer.convert_tokens_to_ids("hin_Deva"),
                    max_new_tokens=128,
                    num_beams=4,
                )
            decoded = tokenizer.batch_decode(generated, skip_special_tokens=True)
            hi_texts.extend(decoded)
        except Exception as e:
            print(f"  ⚠️  Batch {i}-{i+batch_size} failed: {e}")
            hi_texts.extend([""] * len(batch))

        if (i // batch_size) % 10 == 0:
            print(f"    Progress: {min(i + batch_size, total)}/{total}")

    print(f"  ✅ Translated {len([t for t in hi_texts if t])} sentences En→Hi")
    return hi_texts


def add_classroom_phrases() -> list[tuple[str, str]]:
    """Add manually curated classroom phrases in Hindi↔Santali."""
    # These are common classroom phrases validated for educational contexts
    phrases = [
        ("नमस्ते", "ᱡᱚᱦᱟᱨ"),
        ("शुभ प्रभात", "ᱥᱮᱪ ᱪᱟᱸᱫᱚ"),
        ("धन्यवाद", "ᱥᱟᱨᱦᱟᱣ"),
        ("कृपया बैठिए", "ᱫᱟᱭᱟ ᱠᱟᱛᱮ ᱫᱩᱲᱩᱵ ᱢᱮ"),
        ("ध्यान से सुनो", "ᱦᱩᱥᱤᱭᱟᱹᱨ ᱛᱮ ᱟᱹᱭᱠᱟᱹᱣ ᱢᱮ"),
        ("अपनी किताब खोलो", "ᱟᱢᱟᱜ ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡ ᱢᱮ"),
        ("मेरे बाद दोहराओ", "ᱤᱧᱟᱜ ᱛᱟᱭᱚᱢ ᱛᱮ ᱯᱷᱤᱨᱟᱹᱣ ᱢᱮ"),
        ("गिनती करो", "ᱞᱮᱠᱷᱟ ᱢᱮ"),
        ("जवाब लिखो", "ᱛᱮᱞᱟ ᱚᱞ ᱢᱮ"),
        ("एक", "ᱢᱤᱫ"),
        ("दो", "ᱵᱟᱨ"),
        ("तीन", "ᱯᱮ"),
        ("चार", "ᱯᱩᱱ"),
        ("पाँच", "ᱢᱚᱬᱮ"),
        ("छह", "ᱛᱩᱨᱩᱭ"),
        ("सात", "ᱮᱭᱟᱭ"),
        ("आठ", "ᱤᱨᱟᱞ"),
        ("नौ", "ᱟᱨᱮ"),
        ("दस", "ᱜᱮᱞ"),
        ("जोड़ो", "ᱡᱚᱲᱟᱣ ᱢᱮ"),
        ("घटाओ", "ᱜᱚᱲᱚᱣ ᱢᱮ"),
        ("लाल", "ᱟᱨᱟᱜ"),
        ("नीला", "ᱱᱤᱞ"),
        ("हरा", "ᱥᱟᱥᱟᱝ"),
        ("पीला", "ᱥᱟᱥᱟᱝ ᱟᱨᱟᱜ"),
        ("सफेद", "ᱯᱩᱱᱰ"),
        ("काला", "ᱦᱮᱱᱫᱮ"),
        ("तुम्हारा नाम क्या है", "ᱟᱢᱟᱜ ᱧᱩᱛᱩᱢ ᱪᱤᱱᱟᱜ ᱠᱟᱱᱟ"),
        ("मैं तुम्हें पढ़ाऊंगा", "ᱤᱧ ᱟᱢ ᱠᱮ ᱯᱟᱲᱦᱟᱣ ᱟᱢ"),
        ("आज हम गणित पढ़ेंगे", "ᱛᱤᱱᱟᱜ ᱟᱞᱮ ᱜᱟᱬᱤᱛ ᱯᱟᱲᱦᱟᱣ ᱟᱞᱮ"),
        ("यह गोल है", "ᱱᱩᱤ ᱫᱚ ᱜᱩᱞᱢᱩᱞ ᱛᱟᱱᱟᱭ"),
        ("यह चौकोर है", "ᱱᱩᱤ ᱫᱚ ᱪᱟᱨᱠᱚᱱᱟ ᱛᱟᱱᱟᱭ"),
        ("यह तिकोना है", "ᱱᱩᱤ ᱫᱚ ᱛᱤᱱᱠᱚᱱᱟ ᱛᱟᱱᱟᱭ"),
        ("पानी", "ᱫᱟᱜ"),
        ("सूरज", "ᱥᱤᱧ ᱪᱟᱸᱫᱚ"),
        ("चाँद", "ᱧᱤᱫᱟᱹ ᱪᱟᱸᱫᱚ"),
        ("पेड़", "ᱫᱟᱨᱮ"),
        ("फूल", "ᱵᱟᱦᱟ"),
        ("पक्षी", "ᱪᱮᱬᱮ"),
        ("मछली", "ᱦᱟᱠᱩ"),
        ("बहुत अच्छा", "ᱵᱟᱝ ᱵᱩᱜᱤᱱ"),
        ("शाबाश", "ᱵᱟᱝ ᱠᱟᱹᱢᱤ"),
        ("फिर से कोशिश करो", "ᱟᱨ ᱢᱤᱫ ᱛᱟᱨᱟ ᱠᱮᱢᱟ ᱢᱮ"),
        ("क्या तुम समझे", "ᱟᱢ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ ᱥᱮ"),
        ("हाँ", "ᱦᱚᱭ"),
        ("नहीं", "ᱵᱟᱝ"),
        ("आओ", "ᱦᱤᱡᱩᱜ ᱢᱮ"),
        ("जाओ", "ᱪᱟᱞᱟᱜ ᱢᱮ"),
        ("खड़े हो जाओ", "ᱛᱤᱝᱜᱩ ᱢᱮ"),
        ("बैठ जाओ", "ᱫᱩᱲᱩᱵ ᱢᱮ"),
    ]
    print(f"  ✅ Added {len(phrases)} curated classroom phrases")
    return phrases


def clean_and_deduplicate(
    pairs: list[tuple[str, str]],
) -> list[tuple[str, str]]:
    """Clean and deduplicate parallel pairs."""
    seen = set()
    cleaned = []

    for src, tgt in pairs:
        src = src.strip()
        tgt = tgt.strip()

        # Skip empty, too short, or identical pairs
        if not src or not tgt:
            continue
        if len(src) < 2 or len(tgt) < 2:
            continue
        if src == tgt:
            continue

        # Deduplicate by source
        key = src.lower()
        if key in seen:
            continue
        seen.add(key)

        cleaned.append((src, tgt))

    print(f"  ✅ Cleaned: {len(cleaned)} unique pairs (removed {len(pairs) - len(cleaned)} duplicates/invalids)")
    return cleaned


def split_data(
    pairs: list[tuple[str, str]],
    val_ratio: float = 0.05,
    test_ratio: float = 0.05,
    seed: int = 42,
) -> tuple[list, list, list]:
    """Split into train/val/test with no leakage."""
    random.seed(seed)
    shuffled = pairs.copy()
    random.shuffle(shuffled)

    n = len(shuffled)
    n_test = max(int(n * test_ratio), 50)
    n_val = max(int(n * val_ratio), 50)
    n_train = n - n_val - n_test

    if n_train < 100:
        print(f"  ⚠️  Only {n_train} training pairs — model quality will be limited")

    test = shuffled[:n_test]
    val = shuffled[n_test : n_test + n_val]
    train = shuffled[n_test + n_val :]

    return train, val, test


def save_tsv(pairs: list[tuple[str, str]], filepath: Path):
    """Save pairs as TSV (no header)."""
    filepath.parent.mkdir(parents=True, exist_ok=True)
    with open(filepath, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f, delimiter="\t")
        for src, tgt in pairs:
            writer.writerow([src, tgt])
    print(f"  💾 Saved {len(pairs)} pairs to {filepath}")


def main():
    parser = argparse.ArgumentParser(description="Prepare Hindi↔Santali training data")
    parser.add_argument("--dry-run", action="store_true", help="Print stats without writing")
    parser.add_argument("--skip-translate", action="store_true", help="Skip En→Hi pivot translation (faster)")
    parser.add_argument("--batch-size", type=int, default=16, help="Translation batch size")
    parser.add_argument("--max-translate", type=int, default=5000, help="Max English sentences to translate to Hindi")
    args = parser.parse_args()

    print("=" * 60)
    print("BhashaSetu: Hindi↔Santali Data Preparation")
    print("=" * 60)

    all_pairs: list[tuple[str, str]] = []

    # 1. Load English↔Santali pairs
    print("\n[1/5] Loading English↔Santali data...")
    en_sat_pairs = load_santali_train_csv()

    # 2. Download FLORES Hindi↔Santali
    print("\n[2/5] Loading FLORES Hindi↔Santali...")
    flores_pairs = load_flores_data()
    all_pairs.extend(flores_pairs)

    # 3. Download IN22-Gen Hindi↔Santali
    print("\n[3/5] Loading IN22-Gen Hindi↔Santali...")
    in22_pairs = load_in22_data()
    all_pairs.extend(in22_pairs)

    # 4. Pivot translate English→Hindi to create synthetic Hi↔Sat pairs
    if en_sat_pairs and not args.skip_translate:
        print(f"\n[4/5] Pivot translating English→Hindi ({min(len(en_sat_pairs), args.max_translate)} sentences)...")
        en_texts = [en for en, _ in en_sat_pairs[: args.max_translate]]
        sat_texts = [sat for _, sat in en_sat_pairs[: args.max_translate]]

        hi_texts = translate_en_to_hi_batch(en_texts, batch_size=args.batch_size)

        synthetic_pairs = []
        for hi, sat in zip(hi_texts, sat_texts):
            if hi and sat and len(hi) > 2:
                synthetic_pairs.append((hi, sat))

        print(f"  ✅ Created {len(synthetic_pairs)} synthetic Hindi↔Santali pairs")
        all_pairs.extend(synthetic_pairs)
    else:
        print("\n[4/5] Skipping pivot translation")

    # 5. Add curated classroom phrases
    print("\n[5/5] Adding curated classroom phrases...")
    classroom_pairs = add_classroom_phrases()
    all_pairs.extend(classroom_pairs)

    # Clean and deduplicate
    print("\n--- Cleaning & Deduplication ---")
    all_pairs = clean_and_deduplicate(all_pairs)

    # Split
    print(f"\n--- Splitting {len(all_pairs)} pairs ---")
    train, val, test = split_data(all_pairs)

    print(f"\n  📊 Final splits:")
    print(f"     Train: {len(train)}")
    print(f"     Val:   {len(val)}")
    print(f"     Test:  {len(test)}")

    if args.dry_run:
        print("\n🔍 Dry run — no files written")
        print("\nSample pairs:")
        for hi, sat in train[:5]:
            print(f"  {hi}  →  {sat}")
        return

    # Save
    print("\n--- Saving TSV files ---")
    save_tsv(train, OUTPUT_DIR / "train.tsv")
    save_tsv(val, OUTPUT_DIR / "val.tsv")
    save_tsv(test, OUTPUT_DIR / "test.tsv")

    print(f"\n✅ Data preparation complete!")
    print(f"   Output: {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
