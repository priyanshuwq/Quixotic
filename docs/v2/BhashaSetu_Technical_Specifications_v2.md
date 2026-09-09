# BhashaSetu --- Technical Specifications (Evidence-Backed Revision)

> SIH 2026 Problem Statement #26042 \| Team Quixotic\
> Version 2.0 --- September 2026

## 0. Revision Philosophy

This revision keeps the original product direction but removes or
changes claims that were not sufficiently supported by the supplied
documents and external evidence.

The design principle is:

-   **Prototype what the SIH problem explicitly requires.**
-   **Use proven components where credible evidence exists.**
-   **Do not claim model support, accuracy, latency, memory, or "first"
    status without reproducible evidence.**
-   **Treat Ho and Mundari as expansion targets until language-specific
    data and validation are available.**
-   **Make Santali the first fully demonstrated tribal-language
    prototype because multiple established resources explicitly support
    Santali.**

------------------------------------------------------------------------

# 1. Problem Alignment

## 1.1 Target Problem

BhashaSetu addresses SIH 2026 Problem Statement #26042:

**AI-Powered Vernacular Pedagogy and Real-Time Translation Tool for
Mother Tongue-Based Primary Education**

The system is designed to help Hindi-medium primary teachers deliver
mother-tongue-based instruction to tribal-language-speaking students in
low-connectivity environments.

The problem requires:

1.  Hindi foundational literacy and numeracy content translated into
    tribal-language text.
2.  Synthesized tribal-language audio.
3.  Real-time voice-to-voice translation with latency below three
    seconds.
4.  Bilingual worksheets.
5.  Visual flashcards.
6.  NIPUN Bharat learning-outcome alignment.
7.  Offline operation on low-cost Android tablets with approximately 2
    GB RAM.
8.  A working prototype demonstrating at least one tribal language.

## 1.2 BhashaSetu Response

BhashaSetu implements these requirements as one offline classroom system
rather than as an isolated translator.

### Core loop

**Hindi teacher speech → Hindi ASR → curriculum/context-aware
translation → tribal-language text → tribal-language TTS → student**

### Classroom response loop

**Student tribal-language speech → tribal-language ASR → Hindi
translation → Hindi text/audio for teacher**

The second loop is an enhancement for interactive classroom dialogue. It
should only be enabled for a target language after sufficient ASR,
translation and native-speaker validation exists.

------------------------------------------------------------------------

# 2. Product Definition

## 2.1 Product Statement

> **BhashaSetu is an offline, curriculum-aware AI classroom bridge that
> converts Hindi-medium teaching into mother-tongue learning experiences
> on low-cost Android devices.**

It combines:

-   speech recognition,
-   low-resource machine translation,
-   speech synthesis,
-   curriculum mapping,
-   worksheet generation,
-   visual flashcards,
-   local assessment,
-   offline synchronization.

The differentiator is not "translation alone". The system translates the
**teaching interaction and learning content**.

------------------------------------------------------------------------

# 3. Evidence-Backed Language Strategy

## 3.1 Prototype Language: Santali

Santali is the first production-grade prototype language.

This is a deliberate engineering decision, not a reduction of the
product vision.

Evidence supporting this choice includes:

1.  AI4Bharat's IndicTrans2 explicitly supports Santali (`sat_Olck`)
    among its 22 scheduled Indic languages.
2.  IndicTrans2-M2M explicitly supports direct Indic-to-Indic
    translation and includes Santali.
3.  NLLB-200 explicitly lists Santali among its supported languages.
4.  Public Hugging Face resources contain dedicated Santali ASR models
    and multiple Santali translation models.
5.  The SIH problem statement itself requires only one tribal language
    at prototype stage.

Therefore:

**Santali = prototype-ready target.**

## 3.2 Ho and Mundari

Ho and Mundari remain first-class roadmap targets.

However, BhashaSetu will **not** claim that a generic Hindi-to-Ho or
Hindi-to-Mundari MarianMT checkpoint already exists or is
production-ready.

Instead:

### Phase 1

-   Hindi ↔ Santali
-   Full offline classroom demo
-   Complete ASR → MT → TTS pipeline
-   Worksheets
-   Flashcards
-   NIPUN mapping

### Phase 2

-   Hindi ↔ Ho
-   Native-speaker corpus creation
-   Language-specific fine-tuning/adapters
-   Native-speaker evaluation
-   TTS/ASR validation

### Phase 3

-   Hindi ↔ Mundari
-   Same language-specific adaptation pipeline

This avoids unsupported model claims while preserving the full intended
product scope.

------------------------------------------------------------------------

# 4. AI/ML Architecture

## 4.1 High-Level Architecture

``` text
                    ┌─────────────────────────┐
                    │       BhashaSetu        │
                    │  Offline AI Classroom   │
                    └────────────┬────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
          ▼                      ▼                      ▼
   LIVE SPEECH LAYER       PEDAGOGY LAYER        CONTENT LAYER
          │                      │                      │
      ┌───┴───┐             ┌────┴────┐          ┌─────┼─────┐
      │       │             │         │          │     │     │
     ASR     TTS         NIPUN DB  Context    Sheets Cards Assessments
      │       │             │         │
      └───┬───┘             └────┬────┘
          │                      │
          ▼                      ▼
      Translation          Lesson Mapping
          │
     ┌────┴────┐
     │         │
 Santali    Future:
            Ho/Mundari
```

------------------------------------------------------------------------

# 5. Speech Recognition

## 5.1 Primary ASR Strategy

The prototype uses a compact multilingual/Indic ASR model suitable for
mobile inference.

Candidate families:

-   Whisper Tiny / optimized Whisper
-   AI4Bharat IndicConformer where language coverage and
    licensing/testing permit
-   language-specific ASR models when available

The final model is selected using **measured WER, latency, memory and
battery performance on the target device**.

### Important correction

The previous specification treated Whisper Tiny as if its stated latency
and accuracy were universal.

They are not.

The revised specification therefore defines them as **measurement
targets**, not guaranteed values.

------------------------------------------------------------------------

# 6. Translation Engine

## 6.1 Santali Translation

For Santali, the preferred starting point is:

**IndicTrans2 / IndicTrans2-M2M family**

because the model family explicitly supports Santali and direct
Indic-Indic translation.

The translation layer is:

``` text
Hindi
  ↓
Hindi tokenizer
  ↓
IndicTrans2-compatible encoder/decoder
  ↓
Santali (Ol Chiki)
```

Where deployment constraints require a smaller model, use a
distilled/optimized checkpoint and benchmark it on the actual device.

## 6.2 Domain Adaptation

Generic MT is not treated as sufficient for classroom deployment.

A curriculum adaptation layer is added:

``` text
Generic translation
        ↓
Terminology dictionary
        ↓
Grade/subject context
        ↓
NIPUN learning outcome
        ↓
Educational post-processing
        ↓
Validated classroom output
```

Examples of controlled terminology:

-   number names,
-   mathematical operations,
-   shapes,
-   classroom commands,
-   body parts,
-   colours,
-   local examples,
-   assessment vocabulary.

## 6.3 Ho/Mundari Translation

No unsupported off-the-shelf model is assumed.

For each language:

``` text
Native corpus
    ↓
Cleaning + normalization
    ↓
Parallel Hindi ↔ tribal pairs
    ↓
Base multilingual model
    ↓
Parameter-efficient fine-tuning
    ↓
Native-speaker validation
    ↓
Quantization
    ↓
Android deployment
```

The model cannot be declared deployment-ready until it passes the
BhashaSetu language validation gate.

------------------------------------------------------------------------

# 7. Translation Safety and Confidence

Every generated translation receives a confidence/validation status.

``` text
HIGH CONFIDENCE
    ↓
Auto-speak

MEDIUM CONFIDENCE
    ↓
Show text + optional confirmation

LOW CONFIDENCE
    ↓
Ask teacher to confirm / use approved phrase
```

The system maintains a controlled educational phrasebook for high-risk
classroom commands and terminology.

This reduces the risk of silently generating a fluent but incorrect
educational instruction.

------------------------------------------------------------------------

# 8. Text-to-Speech

## 8.1 TTS Strategy

The TTS layer is language-specific.

For Santali, select a verified Santali voice/model where quality,
licensing and offline deployment are acceptable.

Piper may be used where a suitable voice/model is available and
validated, but **Piper is not assumed to provide a ready-made voice for
every target tribal language**.

For Ho/Mundari, the project will require:

-   recorded native-speaker corpus,
-   pronunciation inventory,
-   phoneme/grapheme normalization,
-   voice model training/fine-tuning,
-   native-speaker MOS evaluation.

------------------------------------------------------------------------

# 9. Curriculum Intelligence

## 9.1 NIPUN Mapping

NIPUN Bharat is the pedagogical anchor.

The content database stores:

``` text
Grade
 ↓
Subject
 ↓
Learning Outcome
 ↓
Concept
 ↓
Hindi canonical lesson
 ↓
Validated tribal-language content
 ↓
Activity
 ↓
Worksheet
 ↓
Assessment
```

NIPUN is not merely a label attached to generated content.

Every generated learning object should contain an explicit
learning-outcome ID.

## 9.2 Curriculum Database

Room/SQLite stores:

-   learning outcomes,
-   lesson metadata,
-   approved terminology,
-   bilingual sentence pairs,
-   activities,
-   worksheets,
-   flashcard templates,
-   assessment questions,
-   audio references,
-   version metadata.

------------------------------------------------------------------------

# 10. Worksheet Generation

The prototype must generate a bilingual worksheet locally.

Input:

``` text
Grade: 2
Subject: Mathematics
Topic: Addition
Language: Santali
Learning Outcome: NIPUN outcome ID
```

Output:

-   Hindi instruction
-   Santali instruction
-   examples
-   child-level exercises
-   answer key
-   learning-outcome identifier

The generator should use templates and validated educational content
rather than unrestricted free-form generation.

------------------------------------------------------------------------

# 11. Visual Flashcard Generation

Flashcards are generated from structured templates.

Each card can contain:

-   image/icon,
-   Hindi label,
-   Santali label,
-   optional pronunciation/audio,
-   concept category,
-   learning-outcome ID.

The first release should use a curated asset library rather than a
generative-image model. This keeps the application:

-   offline,
-   deterministic,
-   small,
-   culturally controllable,
-   safe for children.

------------------------------------------------------------------------

# 12. Offline Architecture

``` text
┌────────────────────────────────────┐
│             Android Tablet         │
│                                    │
│  UI                                │
│   │                                │
│  Classroom Controller              │
│   │                                │
│  ├── ASR                           │
│  ├── Translation                   │
│  ├── TTS                           │
│  ├── Curriculum DB                 │
│  ├── Worksheet Generator           │
│  └── Flashcard Engine              │
│                                    │
│          NO INTERNET REQUIRED      │
└────────────────────────────────────┘
             │
             │ optional
             ▼
       Wi-Fi Direct / Local
       synchronization
```

Initial synchronization may transfer:

-   curriculum updates,
-   model updates,
-   approved terminology,
-   content packs,
-   application updates.

Classroom inference remains local.

------------------------------------------------------------------------

# 13. Android/Inference Stack

  -----------------------------------------------------------------------
  Layer                   Technology              Revised role
  ----------------------- ----------------------- -----------------------
  Platform                Android 9+              Target

  Language                Kotlin                  Application

  UI                      Material 3              Accessible classroom UI

  Architecture            MVVM                    Maintainability

  Database                Room/SQLite             Offline content

  Async                   Kotlin Coroutines       Background inference

  Runtime                 ONNX Runtime Mobile /   Local inference
                          ORT format              

  Acceleration            CPU/XNNPACK first;      Device-dependent
                          NNAPI optional          optimization

  ASR                     Compact validated ASR   Hindi/Santali prototype

  MT                      IndicTrans2-derived     Prototype
                          Santali model           

  TTS                     Validated offline       Prototype
                          Santali voice           

  Quantization            INT8 where validated    Mobile optimization

  Sync                    Wi-Fi Direct / local    Offline updates
                          transfer                
  -----------------------------------------------------------------------

ONNX Runtime officially supports Android deployment, CPU, XNNPACK and
NNAPI execution providers, and recommends measuring model/application
performance on the target platform. Quantization is a documented
model-size optimization. Hardware-accelerator performance is
device/model dependent.

------------------------------------------------------------------------

# 14. Quantization Strategy

INT8 is the default optimization target.

However:

**INT8 is not automatically assumed to preserve accuracy.**

For each model:

1.  establish FP32 baseline;
2.  quantize;
3.  evaluate accuracy;
4.  evaluate latency;
5.  evaluate memory;
6.  compare on representative classroom samples;
7.  retain INT8 only if the quality loss is acceptable.

For transformer models, dynamic quantization is a practical starting
point; static quantization is evaluated when calibration data and model
architecture make it advantageous.

------------------------------------------------------------------------

# 15. Resource Budget

The previous document contained exact memory numbers that were not
sufficiently evidenced.

The revised project uses **acceptance limits**, not invented "actual"
numbers.

### Prototype acceptance targets

  Metric                                                   Target
  ------------------------------- -------------------------------
  Device RAM                                                ≤2 GB
  Peak app memory                           ≤60% of available RAM
  Model package                               Minimized/quantized
  Classroom inference                                      No OOM
  Crash rate during stress test                              \<1%
  Data loss                         0 in controlled failure tests
  Offline functionality               100% of core classroom path

Actual measured values will be populated after hardware testing.

------------------------------------------------------------------------

# 16. Latency Budget

The SIH requirement is:

**\<3 seconds for real-time voice translation.**

BhashaSetu sets a more ambitious engineering target, but does not claim
a fixed number until measurement.

### Measurement

Report:

-   P50 latency
-   P95 latency
-   P99 latency
-   worst observed latency

for:

``` text
Audio capture
+
ASR
+
translation
+
TTS
+
playback start
```

### Acceptance

**P95 end-to-end latency \<3 seconds**

on the specified low-end Android test device.

A stretch target of:

**P95 ≤1.5 seconds**

may be pursued after the required functionality is stable.

------------------------------------------------------------------------

# 17. Performance Test Protocol

## 17.1 Hardware

At least one actual low-cost device meeting:

-   Android 9+
-   approximately 2 GB RAM
-   representative ARM processor

must be tested.

A 4 GB tablet may be used for development but must not be presented as
proof of 2 GB deployment.

## 17.2 Test Set

Minimum prototype evaluation:

-   500 Hindi classroom utterances
-   500 Hindi→Santali sentence pairs
-   100 classroom commands
-   100 mathematical instructions
-   100 assessment prompts
-   100 noisy/hesitant speech samples

The exact dataset size can be expanded.

## 17.3 Metrics

### ASR

-   WER
-   CER
-   P50/P95 latency

### MT

-   chrF++
-   BLEU
-   COMET where applicable
-   human adequacy
-   human fluency
-   terminology accuracy

### TTS

-   MOS
-   intelligibility
-   pronunciation correctness
-   latency

### End-to-end

-   P50/P95/P99 latency
-   memory
-   battery
-   thermal behaviour
-   crash rate

------------------------------------------------------------------------

# 18. Translation Evaluation

BLEU alone is not sufficient for this project.

The evaluation stack is:

``` text
Automatic metrics
    +
Native-speaker evaluation
    +
Teacher evaluation
    +
Educational correctness
```

### Required human checks

1.  Meaning preserved?
2.  Instruction preserved?
3.  Child-appropriate?
4.  Natural in target language?
5.  Terminology correct?
6.  Culturally inappropriate wording?
7.  Any dangerous ambiguity?

------------------------------------------------------------------------

# 19. Native-Speaker Validation

Before a language is marked "production-ready":

### Language Gate

``` text
Translation benchmark
       ↓
Native speaker review
       ↓
Teacher review
       ↓
Educational review
       ↓
Error analysis
       ↓
Release candidate
```

The release should maintain a rejected-output/error set so recurring
mistakes can be addressed.

------------------------------------------------------------------------

# 20. Privacy and Security

Core classroom inference requires:

-   no cloud API,
-   no student account,
-   no mandatory internet,
-   local processing,
-   minimal permissions.

Optional synchronization should be authenticated.

Local data should be protected using Android storage controls; database
encryption can be added where the threat model requires it.

------------------------------------------------------------------------

# 21. Accessibility

Teacher UI:

-   Hindi-first
-   English fallback
-   large controls
-   clear offline status
-   minimal interaction steps

Student UI:

-   target-language-first
-   large text
-   audio-first interaction
-   visual feedback
-   simple play/replay controls

------------------------------------------------------------------------

# 22. Scalability

The architecture supports language packs.

``` text
BhashaSetu Core
      │
      ├── Santali Pack
      ├── Ho Pack
      ├── Mundari Pack
      └── Future Language Packs
```

Each language pack contains:

-   tokenizer/model,
-   language metadata,
-   terminology,
-   TTS,
-   ASR,
-   validated phrasebook,
-   curriculum translations.

This means adding a language does not require rewriting the Android
application.

------------------------------------------------------------------------

# 23. Real-World Deployment Path

## Stage 1 --- Prototype

-   Santali
-   offline Android
-   live Hindi→Santali
-   worksheet
-   flashcards
-   NIPUN mapping

## Stage 2 --- Pilot

-   2--5 schools
-   teacher feedback
-   native-speaker review
-   classroom latency
-   error collection

## Stage 3 --- Language Expansion

-   Ho
-   Mundari

## Stage 4 --- State Deployment

-   SCERT/DIET content governance
-   controlled curriculum updates
-   language expert network
-   device fleet management
-   monitoring and support

------------------------------------------------------------------------

# 24. Core USP

> **BhashaSetu does not merely translate languages; it translates the
> classroom.**

A Hindi-medium teacher can:

1.  speak naturally in Hindi;
2.  receive a target-language classroom rendering;
3.  hear the target-language instruction;
4.  receive student responses in Hindi;
5.  generate bilingual learning material;
6.  operate without internet.

The strongest differentiator is the combination of:

**offline + low-resource language support + curriculum awareness +
speech-to-speech + learning-content generation.**

------------------------------------------------------------------------

# 25. What BhashaSetu Must Demonstrate

The final demonstration should prove, not merely state:

### Demo 1

Airplane mode ON.

### Demo 2

Teacher speaks Hindi.

### Demo 3

System produces Santali text/audio.

### Demo 4

Student responds.

### Demo 5

Teacher receives Hindi interpretation.

### Demo 6

Teacher selects lesson.

### Demo 7

Bilingual worksheet is generated.

### Demo 8

Flashcards are generated.

### Demo 9

Show NIPUN learning-outcome mapping.

### Demo 10

Show latency and device resource measurements.

------------------------------------------------------------------------

# 26. Claims Policy

The following claims are prohibited unless backed by reproducible
experiments:

-   "524 ms latency"
-   "387 MB peak memory"
-   "12.3% WER"
-   "28.4 BLEU"
-   "3.2% battery/hour"
-   "first offline tribal translator"
-   "production-ready Ho/Mundari"
-   "native-quality translation"

The final presentation should show:

**Target → Measured result → Device → Dataset → Test procedure**

------------------------------------------------------------------------

# 27. Final Technical Position

### Strongly supported

-   Offline Android inference
-   ONNX Runtime mobile deployment
-   INT8 quantization as an optimization path
-   Santali translation as a viable prototype direction
-   NIPUN-aligned content architecture
-   Speech-to-speech composition from ASR + MT + TTS
-   Low-resource language adaptation as a research/development path

### Requires project-generated evidence

-   exact latency
-   exact memory
-   exact battery drain
-   exact WER
-   exact BLEU/chrF++
-   actual TTS MOS
-   classroom robustness

### Not claimed yet

-   production-ready Ho
-   production-ready Mundari
-   universal three-language offline performance

------------------------------------------------------------------------

# 28. Success Criteria

BhashaSetu is considered a successful SIH prototype when it can
demonstrate:

1.  **One complete tribal-language classroom pipeline --- Santali.**
2.  **Offline operation on a representative low-cost Android device.**
3.  **End-to-end voice translation below the SIH's three-second
    requirement at P95.**
4.  **Bilingual worksheet generation.**
5.  **Visual flashcard generation.**
6.  **NIPUN learning-outcome mapping.**
7.  **Native-speaker validation evidence.**
8.  **Reproducible performance measurements.**

The project then has a credible engineering path to Ho and Mundari.

------------------------------------------------------------------------

# 29. References

1.  Government of India --- NIPUN Bharat:
    https://nipunbharat.education.gov.in/
2.  AI4Bharat --- IndicTrans2: https://github.com/AI4Bharat/IndicTrans2
3.  AI4Bharat --- IndicTrans2-M2M:
    https://ai4bharat.iitm.ac.in/blog/indictrans2/
4.  AI4Bharat Models: https://models.ai4bharat.org/
5.  Meta --- NLLB-200 model card:
    https://huggingface.co/facebook/nllb-200-distilled-600M
6.  OpenAI --- Whisper: https://openai.com/index/whisper/
7.  ONNX Runtime --- Mobile:
    https://onnxruntime.ai/docs/tutorials/mobile/
8.  ONNX Runtime --- Android:
    https://onnxruntime.ai/docs/build/android.html
9.  ONNX Runtime --- Quantization:
    https://onnxruntime.ai/docs/how-to/quantization.html
