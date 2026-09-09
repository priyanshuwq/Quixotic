# BhashaSetu --- Developer Guide (Evidence-Backed Revision)

> SIH 2026 Problem Statement #26042 \| Team Quixotic\
> Version 2.0 --- September 2026

## 1. Development Objective

Build an offline Android application that demonstrates:

**Hindi speech → Santali speech/text → classroom content generation**

with NIPUN-aligned learning material.

Ho and Mundari are implemented through the same language-pack
architecture after language-specific model/data validation.

------------------------------------------------------------------------

# 2. Repository Structure

``` text
BhashaSetu/
├── android/
│   ├── app/
│   │   └── src/main/
│   │       ├── java/
│   │       ├── res/
│   │       └── assets/
│   │           ├── models/
│   │           │   ├── asr/
│   │           │   ├── nmt/
│   │           │   └── tts/
│   │           ├── curriculum/
│   │           ├── flashcards/
│   │           └── audio/
│   └── build.gradle
│
├── training/
│   ├── data/
│   │   ├── raw/
│   │   ├── cleaned/
│   │   ├── parallel/
│   │   └── evaluation/
│   ├── scripts/
│   ├── notebooks/
│   └── requirements.txt
│
├── models/
│   ├── asr/
│   ├── nmt/
│   └── tts/
│
├── evaluation/
│   ├── asr/
│   ├── nmt/
│   ├── tts/
│   └── end_to_end/
│
└── docs/
```

------------------------------------------------------------------------

# 3. Android Setup

1.  Install Android Studio.
2.  Open `android/`.
3.  Sync Gradle.
4.  Build the debug APK.
5.  Connect an Android 9+ device.
6.  Install the APK.
7.  Confirm microphone permission.
8.  Confirm offline mode.
9.  Run the classroom pipeline.

The final performance test must use a representative low-cost device,
preferably an actual approximately 2 GB RAM device.

------------------------------------------------------------------------

# 4. Model Preparation

## 4.1 ASR

The first prototype uses a compact Hindi ASR model.

Candidate:

-   Whisper Tiny optimized for mobile, or
-   an Indic ASR model with better measured Hindi/Indic performance.

Workflow:

``` text
Base model
   ↓
Hindi validation set
   ↓
Optional domain adaptation
   ↓
Export
   ↓
ONNX
   ↓
Optimize
   ↓
INT8
   ↓
Benchmark
```

Do not record a latency number until the complete Android inference path
has been measured.

------------------------------------------------------------------------

# 5. Santali Translation Model

## 5.1 Starting Point

Use an IndicTrans2-compatible Santali model as the baseline.

IndicTrans2 explicitly supports:

``` text
Hindi: hin_Deva
Santali: sat_Olck
```

The model must be tested for the actual classroom domain.

## 5.2 Domain Adaptation

Create:

``` text
training/data/parallel/hin_sat/
```

with:

``` text
Hindi sentence || Santali sentence
```

Recommended categories:

-   greetings
-   classroom commands
-   numbers
-   shapes
-   colours
-   addition
-   subtraction
-   reading
-   writing
-   environmental studies
-   assessments

Every training/validation/test split must be separated to prevent
leakage.

------------------------------------------------------------------------

# 6. Ho and Mundari Model Pipeline

Do not simply rename the Santali model.

For each language:

``` text
Native speaker data
        ↓
Data cleaning
        ↓
Hindi ↔ target parallel corpus
        ↓
Train/dev/test split
        ↓
Multilingual base model
        ↓
Fine-tuning
        ↓
Human evaluation
        ↓
Quantization
        ↓
Android benchmark
```

A language pack becomes "supported" only after it passes the release
gate.

------------------------------------------------------------------------

# 7. Data Governance

Every parallel sentence should carry metadata:

``` json
{
  "id": "sat_math_000001",
  "source_language": "hin_Deva",
  "target_language": "sat_Olck",
  "grade": 2,
  "subject": "mathematics",
  "topic": "addition",
  "nipun_outcome": "OUTCOME_ID",
  "source": "native_validated",
  "review_status": "approved"
}
```

This allows the translation engine to distinguish:

-   general language,
-   classroom language,
-   curriculum language.

------------------------------------------------------------------------

# 8. Curriculum Database

Room entities should include:

``` text
LearningOutcome
Lesson
Concept
Phrase
Translation
Activity
WorksheetTemplate
Flashcard
Assessment
AudioAsset
LanguagePack
```

Every learning object should reference a learning-outcome identifier.

------------------------------------------------------------------------

# 9. Inference Pipeline

## 9.1 Teacher-to-Student

``` text
AudioRecord
   ↓
Voice Activity Detection
   ↓
ASR
   ↓
Hindi text
   ↓
Context Resolver
   ↓
Translation Engine
   ↓
Santali text
   ↓
TTS
   ↓
AudioTrack
```

## 9.2 Student-to-Teacher

``` text
Student speech
   ↓
Target-language ASR
   ↓
Target-language text
   ↓
Hindi translation
   ↓
Hindi text/audio
   ↓
Teacher
```

The second pipeline should be enabled only after the target-language
ASR/translation model passes validation.

------------------------------------------------------------------------

# 10. Context Resolver

The context resolver receives:

``` text
grade
subject
lesson
learning outcome
current activity
```

and sends structured context to the translation layer.

Example:

``` json
{
  "grade": 2,
  "subject": "mathematics",
  "topic": "addition",
  "learning_outcome": "NIPUN_OUTCOME_021",
  "text": "दो और तीन जोड़ो"
}
```

This enables terminology-aware translation.

------------------------------------------------------------------------

# 11. Approved Phrasebook

Store high-frequency classroom phrases locally.

Examples:

-   Listen carefully.
-   Repeat after me.
-   Open your book.
-   Count the objects.
-   Write the answer.
-   Work with your partner.
-   Show me the number.
-   Which one is bigger?

Phrasebook entries should be native-speaker validated.

If an incoming utterance is sufficiently similar to an approved phrase,
the system can bypass expensive generation and directly use the
validated target-language rendering.

This can improve both reliability and latency.

------------------------------------------------------------------------

# 12. Translation Confidence

Implement a confidence pipeline.

``` text
Translation
   ↓
Confidence/quality checks
   ↓
 ┌───────────────┬────────────────┬──────────────┐
 │ High          │ Medium         │ Low          │
 ↓               ↓                ↓
Speak           Show text        Confirm/
automatically   + optional       fallback
                confirmation
```

For low-confidence output, use:

-   approved phrasebook,
-   teacher correction,
-   alternative candidate,
-   text-only fallback.

------------------------------------------------------------------------

# 13. TTS Integration

TTS must be treated as a language-specific component.

Asset structure:

``` text
assets/models/tts/
├── santali/
│   ├── voice.onnx
│   └── metadata.json
├── ho/
│   └── ...
└── mundari/
    └── ...
```

Do not ship a language pack without a validated voice.

------------------------------------------------------------------------

# 14. ONNX Runtime Mobile

Use ONNX Runtime Mobile for supported models.

Preferred sequence:

1.  CPU baseline.
2.  Optimize model.
3.  Quantize.
4.  Convert to ORT format if beneficial.
5.  Benchmark.
6.  Test XNNPACK where useful.
7.  Test NNAPI only after CPU baseline is known.
8.  Keep the fastest stable configuration for the target device.

Performance is device/model specific.

------------------------------------------------------------------------

# 15. Android Memory Management

Do not load every language model simultaneously.

Use language-pack loading:

``` text
User selects Santali
       ↓
Load Santali models
       ↓
Unload unused target-language models
```

ASR/NMT/TTS sessions should be lifecycle-managed.

On memory pressure:

1.  release unused buffers;
2.  close inactive sessions;
3.  reduce audio buffer size;
4.  unload unused language packs;
5.  retry inference;
6.  fall back to text-only mode if necessary.

------------------------------------------------------------------------

# 16. Audio Configuration

Initial prototype:

``` text
Input:
16 kHz
Mono
16-bit PCM

Processing:
Float32 only where required by the model

Output:
Model-dependent sample rate
Mono
```

Do not force 22.05 kHz as a universal requirement. The TTS model's
native output rate should determine playback configuration.

------------------------------------------------------------------------

# 17. Worksheet Engine

The worksheet generator should be deterministic.

``` text
NIPUN outcome
     ↓
Lesson template
     ↓
Hindi content
     ↓
Validated translation
     ↓
Worksheet renderer
     ↓
PDF/printable output
```

Templates should prevent hallucinated facts and unsafe educational
instructions.

------------------------------------------------------------------------

# 18. Flashcard Engine

Flashcards should use a curated asset library.

Example:

``` text
Concept
 ↓
Approved image
 ↓
Hindi label
 ↓
Santali label
 ↓
Audio
 ↓
Flashcard
```

This is preferred over online generative image APIs because the
application must operate offline.

------------------------------------------------------------------------

# 19. Offline Synchronization

Use local Wi-Fi Direct/local HTTP only for synchronization.

Sync package:

``` text
manifest.json
models/
curriculum/
language/
audio/
images/
checksums/
```

Every package must contain:

-   version,
-   language,
-   curriculum version,
-   checksum,
-   compatibility version.

Installation:

``` text
Receive package
    ↓
Verify checksum
    ↓
Verify compatibility
    ↓
Stage files
    ↓
Update database
    ↓
Switch version atomically
```

------------------------------------------------------------------------

# 20. Testing

## 20.1 Unit Tests

``` bash
cd android
./gradlew test
```

Test:

-   tokenization,
-   DB operations,
-   curriculum lookup,
-   language-pack loading,
-   worksheet generation,
-   flashcard generation.

## 20.2 Instrumentation Tests

``` bash
./gradlew connectedAndroidTest
```

Test:

-   microphone,
-   audio playback,
-   inference lifecycle,
-   offline mode,
-   permissions,
-   low-memory behaviour.

------------------------------------------------------------------------

# 21. Model Tests

Each model must have:

``` text
model_test/
├── accuracy/
├── latency/
├── memory/
├── regression/
└── robustness/
```

Every model version gets a reproducible evaluation record.

------------------------------------------------------------------------

# 22. End-to-End Benchmark

Measure:

``` text
t0 = microphone capture start
t1 = speech segment complete
t2 = ASR complete
t3 = translation complete
t4 = TTS first audio available
t5 = playback begins
```

Report:

``` text
ASR latency = t2 - t1
MT latency  = t3 - t2
TTS latency = t4 - t3
E2E latency = t5 - t1
```

Run at least 100 repeated trials and report:

-   mean,
-   median/P50,
-   P95,
-   P99,
-   standard deviation,
-   worst case.

------------------------------------------------------------------------

# 23. Target Hardware Test

Final evidence must include:

``` text
Device
RAM
CPU
Android version
Battery state
Thermal state
Model versions
Application version
```

A development laptop or high-end tablet must not be used as the sole
proof of low-end deployment.

------------------------------------------------------------------------

# 24. Accuracy Evaluation

## ASR

Use:

-   WER
-   CER

Test separately:

-   quiet speech,
-   classroom noise,
-   different speakers,
-   slow speech,
-   fast speech,
-   hesitation.

## Translation

Use:

-   BLEU
-   chrF++
-   optional COMET
-   human adequacy
-   human fluency
-   terminology accuracy.

## TTS

Use:

-   MOS
-   intelligibility
-   pronunciation accuracy.

------------------------------------------------------------------------

# 25. Human Evaluation Protocol

For Santali:

-   native speakers review outputs;
-   teachers review classroom suitability;
-   reviewers score:
    -   meaning,
    -   naturalness,
    -   educational correctness,
    -   cultural appropriateness,
    -   terminology.

Store both score and error category.

Example:

``` text
ERROR_001
Type: Terminology
Hindi: जोड़
Output: incorrect/common-language equivalent
Correct: approved mathematical term
```

------------------------------------------------------------------------

# 26. Regression Testing

Every new model/content release must run:

``` text
Old benchmark
+
New benchmark
+
Critical phrase suite
+
NIPUN content suite
```

A release is blocked if:

-   latency worsens beyond the limit,
-   critical classroom phrase accuracy falls,
-   translation quality falls materially,
-   memory exceeds the target,
-   crashes increase.

------------------------------------------------------------------------

# 27. Deployment

Build release:

``` bash
cd android
./gradlew assembleRelease
```

Then:

1.  sign APK;
2.  verify package;
3.  install on target device;
4.  run offline smoke test;
5.  run benchmark;
6.  generate release manifest;
7.  distribute through USB/local sync.

------------------------------------------------------------------------

# 28. First Demo Build

The first demo should contain only the strongest vertical slice:

``` text
Santali
+
Hindi speech
+
offline
+
NIPUN lesson
+
voice output
+
worksheet
+
flashcards
```

Do not delay the demo by trying to finish all three languages.

------------------------------------------------------------------------

# 29. Demo Script

### Step 1

Enable airplane mode.

### Step 2

Open BhashaSetu.

### Step 3

Select:

``` text
Grade 2
Mathematics
Addition
Santali
```

### Step 4

Teacher speaks Hindi.

### Step 5

Display:

``` text
Hindi transcription
↓
Santali translation
↓
Santali audio
```

### Step 6

Student responds.

### Step 7

Display Hindi interpretation for the teacher.

### Step 8

Generate worksheet.

### Step 9

Generate flashcards.

### Step 10

Show NIPUN outcome mapping.

### Step 11

Show actual P95 latency and device memory.

------------------------------------------------------------------------

# 30. Troubleshooting

## OOM

-   unload unused language packs;
-   reduce concurrent model sessions;
-   reduce audio buffers;
-   use quantized models;
-   use ORT Mobile/custom runtime where justified.

## High latency

Check:

1.  audio segmentation;
2.  ASR;
3.  translation;
4.  TTS;
5.  thread contention;
6.  model loading;
7.  CPU frequency/thermal throttling.

Do not optimize based only on individual model inference time. Measure
the complete pipeline.

## Poor translation

Check:

-   domain mismatch,
-   tokenizer,
-   terminology,
-   language direction,
-   training leakage,
-   data quality,
-   native-speaker errors.

## Poor speech recognition

Check:

-   microphone level,
-   noise,
-   VAD,
-   language configuration,
-   accent variation,
-   audio preprocessing.

------------------------------------------------------------------------

# 31. Release Checklist

## Functional

-   [ ] Offline classroom path works
-   [ ] Hindi ASR works
-   [ ] Santali translation works
-   [ ] Santali TTS works
-   [ ] Worksheet generation works
-   [ ] Flashcard generation works
-   [ ] NIPUN mapping works

## Performance

-   [ ] Tested on \~2 GB device
-   [ ] P95 E2E \<3 seconds
-   [ ] Memory recorded
-   [ ] Thermal behaviour recorded
-   [ ] Battery behaviour recorded

## Quality

-   [ ] Native-speaker review
-   [ ] Teacher review
-   [ ] Educational review
-   [ ] Critical phrase regression suite
-   [ ] Translation benchmark

## Security

-   [ ] No internet dependency
-   [ ] No unnecessary permissions
-   [ ] Local data handling verified
-   [ ] Sync packages checksum verified

------------------------------------------------------------------------

# 32. Development Priorities

### Priority 1

Working offline Santali voice translation.

### Priority 2

Reliable classroom terminology.

### Priority 3

NIPUN-linked worksheet generation.

### Priority 4

Flashcards.

### Priority 5

Bidirectional interaction after target-language ASR validation.

### Priority 6

Ho and Mundari language packs.

------------------------------------------------------------------------

# 33. Engineering Principle

> **Never replace a measured result with a theoretical result in the
> final submission.**

The final repository should make it possible for a reviewer to
reproduce:

-   model conversion,
-   quantization,
-   inference,
-   benchmarking,
-   content generation,
-   APK build.

------------------------------------------------------------------------

# 34. Evidence Policy

The following are project-generated results and must not be copied from
the old specification without reproduction:

-   WER
-   BLEU
-   CER
-   MOS
-   latency
-   memory
-   CPU
-   battery
-   thermal rise

External sources justify the **architecture and model choices**.

Your experiments justify the **performance claims**.

------------------------------------------------------------------------

# 35. Final Architecture

``` text
                         BHASHASETU
                              │
               ┌──────────────┼──────────────┐
               │              │              │
               ▼              ▼              ▼
         LIVE SPEECH      PEDAGOGY        CONTENT
               │              │              │
          ┌────┴────┐     NIPUN DB      Worksheets
          │         │         │           Flashcards
         ASR       TTS     Context       Assessments
          │         │         │
          └────┬────┘         │
               │              │
               ▼              ▼
          TRANSLATION ← CONTEXT
               │
        ┌──────┴──────┐
        │             │
     Santali       Future packs
                    Ho/Mundari
        │
        ▼
   OFFLINE ANDROID
        │
        ▼
  LOW-COST DEVICE
```

------------------------------------------------------------------------

# 36. Final Developer Position

Build **one excellent language implementation first**.

Do not distribute engineering effort equally across three languages
before proving one.

The correct sequence is:

**Santali → validate → optimize → deploy → collect corrections → extend
to Ho → extend to Mundari.**

This is more technically defensible, more achievable within a hackathon,
and more aligned with the SIH prototype requirement than claiming three
production-ready languages before their data and models are validated.
