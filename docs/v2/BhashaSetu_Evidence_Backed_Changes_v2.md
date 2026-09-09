# BhashaSetu --- Evidence-Backed Change Log

> Version 2.0 --- September 2026

This document records the changes made to the original BhashaSetu
technical specification and developer guide.

## Decision Rule

A material architectural correction was made only where multiple
independent sources converged on the same conclusion.

Where evidence was insufficient, the specification deliberately uses a
target, roadmap, or validation gate instead of making a stronger claim.

------------------------------------------------------------------------

# 1. Replace Helsinki-NLP/opus-mt-hi as the claimed tribal-language model

### Decision

**Changed.**

The revised specification no longer claims that a generic Helsinki
MarianMT/OPUS Hindi checkpoint directly provides production-ready
Hindi→Santhali/Ho/Mundari translation.

### Evidence

1.  AI4Bharat IndicTrans2 explicitly lists Santali (`sat_Olck`) and
    Hindi among supported languages.
2.  AI4Bharat IndicTrans2-M2M explicitly provides direct Indic-to-Indic
    translation and includes Santali.
3.  NLLB-200 explicitly lists Santali as a supported language.
4.  Public model resources contain Santali translation checkpoints.
5.  The supplied SIH problem statement requires only one tribal language
    at prototype stage.

### Result

**Santali is the prototype language.**

Ho and Mundari require language-specific data/model validation before
being called production-ready.

------------------------------------------------------------------------

# 2. Use IndicTrans2/IndicTrans2-M2M as the Santali baseline

### Decision

**Changed.**

### Evidence

1.  AI4Bharat's official IndicTrans2 repository explicitly lists
    Santali.
2.  The official AI4Bharat M2M release provides direct Indic-Indic
    translation.
3.  The IndicTrans2 research paper reports broad Indic coverage and
    public training/evaluation resources.
4.  The official model ecosystem includes distilled variants for
    deployment efficiency.
5.  The project specifically needs Indic-to-Indic translation rather
    than a Hindi→English→tribal pivot.

### Result

IndicTrans2 becomes the preferred **baseline**, not an automatic final
model.

------------------------------------------------------------------------

# 3. Do not claim exact latency/memory/accuracy numbers until reproduced

### Decision

**Changed.**

The old values such as 524 ms, 387 MB, 12.3% WER and 28.4 BLEU are now
treated as unverified until reproduced.

### Evidence

1.  ONNX Runtime's mobile guidance explicitly says application
    performance must be measured against target-platform requirements.
2.  ONNX Runtime states accelerator performance is device/model
    specific.
3.  The original project specification tested on a 4 GB tablet while
    targeting a 2 GB deployment.
4.  The original document's latency table contains a raw pipeline total
    and a separate optimized target rather than a reproducible benchmark
    trace.
5.  The SIH requirement only requires sub-three-second real-time voice
    translation, so fabricated sub-second precision is unnecessary.

### Result

Final submission must report:

**device + model version + dataset + P50 + P95 + P99 + worst case.**

------------------------------------------------------------------------

# 4. Keep ONNX Runtime Mobile

### Decision

**Retained.**

### Evidence

1.  ONNX Runtime officially supports Android.
2.  ONNX Runtime Mobile supports Android inference.
3.  Android supports CPU, XNNPACK and NNAPI execution providers.
4.  ONNX Runtime documents quantization as a model-size optimization.
5.  ONNX Runtime recommends measuring application latency, binary size
    and power on the target device.

### Result

ONNX Runtime remains the deployment runtime.

------------------------------------------------------------------------

# 5. Keep INT8, but make it validation-gated

### Decision

**Retained with correction.**

### Evidence

1.  ONNX Runtime documents 8-bit quantization.
2.  Quantization reduces model weight precision and can reduce size.
3.  ONNX Runtime provides dynamic and static quantization methods.
4.  The documentation distinguishes transformer-oriented dynamic
    quantization from CNN-oriented static quantization.
5.  Quantization can introduce accuracy changes, so quality must be
    evaluated.

### Result

INT8 is the optimization path, not a guaranteed free performance gain.

------------------------------------------------------------------------

# 6. Keep Whisper as a candidate, not a guaranteed benchmark

### Decision

**Retained with correction.**

### Evidence

1.  OpenAI reports Whisper was trained on 680,000 hours of
    multilingual/multitask data.
2.  Whisper is designed for robust multilingual speech recognition.
3.  The project requires Hindi speech input.
4.  The supplied architecture already uses Whisper Tiny.
5.  The final application must benchmark the actual Android deployment.

### Result

Whisper Tiny remains a candidate baseline. Exact WER and latency are
project measurements.

------------------------------------------------------------------------

# 7. Keep NIPUN as the curriculum anchor

### Decision

**Retained.**

### Evidence

1.  The SIH problem explicitly requires NIPUN-aligned content.
2.  The Government of India's NIPUN Bharat program is explicitly focused
    on foundational literacy and numeracy.
3.  NIPUN includes learning-outcome/benchmark-oriented educational
    planning.
4.  The official FLS framework measures foundational literacy and
    numeracy skills.
5.  The original project already structures curriculum data around NIPUN
    milestones.

### Result

NIPUN IDs become first-class data fields rather than presentation-only
metadata.

------------------------------------------------------------------------

# 8. Make worksheets and flashcards deterministic/template-based

### Decision

**Changed.**

### Evidence

1.  The problem requires autogenerated worksheets and visual flashcards.
2.  The system must work offline.
3.  The project targets low-cost devices.
4.  Curriculum material needs predictable educational correctness.
5.  The project does not need a generative-image model to satisfy the
    requirement.

### Result

Use local structured templates and curated visual assets for the
prototype.

------------------------------------------------------------------------

# 9. Treat Ho and Mundari as expansion language packs

### Decision

**Changed.**

### Evidence

1.  The SIH problem explicitly says minimum one tribal language at
    prototype stage.
2.  IndicTrans2 explicitly supports Santali but its published
    22-language list does not include Ho or Mundari.
3.  NLLB-200 supports many low-resource languages and explicitly
    includes Santali, but this does not establish production-ready
    Ho/Mundari support.
4.  Public model ecosystems show a much stronger established resource
    base for Santali than for the requested Ho/Mundari classroom stack.
5.  Low-resource translation quality depends heavily on suitable
    parallel data and evaluation.

### Result

Do not pretend Ho/Mundari are solved by simply selecting a generic
multilingual checkpoint.

Build them through language-specific data and validation.

------------------------------------------------------------------------

# 10. What was deliberately NOT changed

The following remain because the supplied documents and problem
statement already support them:

-   Android 9+ target
-   offline-first design
-   Room/SQLite curriculum database
-   Kotlin/Android native architecture
-   ONNX Runtime family
-   NIPUN mapping
-   worksheet generation
-   flashcards
-   local synchronization
-   privacy-first processing
-   language-pack architecture

------------------------------------------------------------------------

# 11. What Must Be Proven by the Team

External evidence supports architecture selection.

It does **not** prove your final application performance.

Your own experiments must establish:

-   ASR WER/CER
-   translation BLEU/chrF++
-   native-speaker quality
-   TTS MOS
-   end-to-end P95 latency
-   peak memory
-   battery drain
-   thermal stability
-   crash rate
-   actual 2 GB-device operation

------------------------------------------------------------------------

# 12. Final Recommendation

The safest high-performance strategy is:

``` text
                    SIH PROTOTYPE
                         │
                         ▼
                     SANTALI
                         │
             ┌───────────┼───────────┐
             ▼           ▼           ▼
             ASR         MT          TTS
             │           │           │
             └───────────┼───────────┘
                         ▼
                  LIVE CLASSROOM
                         │
                ┌────────┼────────┐
                ▼        ▼        ▼
             NIPUN    WORKSHEET  FLASHCARD
                         │
                         ▼
                    OFFLINE TABLET
```

Then:

``` text
Santali validated
      ↓
Native-speaker corrections
      ↓
Performance optimization
      ↓
Ho language pack
      ↓
Mundari language pack
```

This is the strongest evidence-backed path identified in the review.
