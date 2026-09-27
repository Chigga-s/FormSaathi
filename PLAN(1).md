# FormSaathi Implementation Plan

## 1. Project summary

FormSaathi is an offline-first Android application that converts difficult government PDF forms into a simple multilingual question flow. The user selects a PDF, the app detects form fields, asks understandable questions in English, Hindi, or Marathi, accepts typed or spoken answers, validates them, and generates a flattened completed PDF on the device.

**Hackathon track:** Jan Jeevan

**Build window:** 48 hours

**Team size:** 4 developers

**Primary constraint:** All four roles must be able to develop simultaneously after the shared contract is frozen.

## 2. Problem statement

Many Indian government forms use formal language, dense layouts, and unclear instructions. This creates barriers for people with limited digital literacy, limited English proficiency, or difficulty interpreting bureaucratic wording. FormSaathi reduces that burden through a conversational, local-language workflow that runs without sending sensitive form data to a server.

## 3. Final MVP

The finished prototype must demonstrate one uninterrupted offline flow.

1. Enable airplane mode.
2. Open FormSaathi.
3. Select English, Hindi, or Marathi.
4. Import one supported government PDF.
5. Render every PDF page locally.
6. Run on-device OCR and identify supported fields.
7. Show one simplified question at a time.
8. Accept a typed answer or a short offline voice answer.
9. Normalize and validate the answer.
10. Show detected document requirements.
11. Let the user review and edit all answers.
12. Overlay answers onto the original page images.
13. Export and preview `Completed_Form.pdf`.

The team will optimize two or three selected forms for the demo. Universal government-form support is not an MVP requirement.

## 4. Scope

### Must have

- Native Android application in Kotlin
- Jetpack Compose user interface
- PDF import through Android's document picker
- PDF rendering through `PdfRenderer`
- Offline OCR through the bundled Google ML Kit text-recognition model
- Rule-based detection of 8 to 15 common fields
- English, Hindi, and Marathi question resources stored locally
- Typed answers
- Offline voice input through `whisper.cpp`
- Normalization for common answer types
- Basic answer validation
- Review and edit flow
- Required-document checklist when found in the form
- Flattened completed PDF generated on-device
- No required network connection during the demonstrated workflow
- Public GitHub repository with setup and run instructions
- Installable release APK

### Stretch goals

- Android text-to-speech for reading questions aloud
- CameraX scanning of paper forms
- Room database for saved form sessions
- More conditional question rules
- Additional supported forms and aliases
- ONNX sentence embeddings for semantic field matching
- Additional Indian languages

### Explicitly out of scope

- Guaranteed support for every government form
- Government portal submission
- Aadhaar or identity verification
- Cloud OCR, cloud speech, or cloud LLM calls
- Training a new speech or OCR model
- Handwriting recognition
- Editable AcroForm output
- Automatic legal-name transliteration
- Perfect reconstruction of arbitrary PDF layouts

## 5. Frozen technology stack

| Layer | Technology | Purpose |
| --- | --- | --- |
| Application | Kotlin | Native Android implementation |
| UI | Jetpack Compose and Navigation Compose | Screens, state-driven UI, and navigation |
| App pattern | MVVM with repository-style interfaces | Separate screens from engines |
| File selection | Android Storage Access Framework | Import a PDF without storage-path assumptions |
| PDF input | Android `PdfRenderer` | Render pages as bitmaps |
| OCR | Bundled Google ML Kit Latin and Devanagari Text Recognition | On-device text and bounding-box extraction |
| Form understanding | Kotlin rules, aliases, keywords, and edit distance | Map labels to canonical fields |
| Speech input | `whisper.cpp` with a quantized multilingual Tiny model | Offline multilingual transcription |
| Native bridge | Android NDK, JNI, and CMake | Connect Kotlin to `whisper.cpp` |
| Audio | Android `AudioRecord` | Capture short PCM recordings |
| Language resources | Local JSON files | Store simplified questions in three languages |
| Answer processing | Kotlin regex and deterministic rules | Normalize and validate answers |
| Session state | In-memory `StateFlow` and optional JSON save | Avoid Room during the core build |
| PDF output | `PdfDocument`, `Canvas`, and rendered page bitmaps | Create a flattened completed PDF |
| Settings | DataStore only if time permits | Persist chosen language |
| Build | Gradle and Android Studio | Build and dependency management |
| Source control | Git and GitHub | Parallel branches and public submission |

Do not add a backend, Firebase, React, or a cloud AI API to the core flow. Those additions weaken the offline privacy claim and create unnecessary integration risk.

## 6. Architecture

```text
Selected PDF
    |
    v
PdfRenderer -> PageBitmap[]
    |
    v
ML Kit OCR -> OcrBlock[]
    |
    v
Form Parser -> ParsedForm
    |
    v
Conversation Engine <-> Question Provider
    |                         |
    |                         +-> English, Hindi, Marathi JSON
    |
    +-> Typed answer
    |
    +-> AudioRecord -> whisper.cpp -> Raw transcript
    |
    v
Normalizer -> Validator -> FormSession
    |
    v
Review screen
    |
    v
PDF Generator -> Completed_Form.pdf
```

The UI depends only on interfaces. Each engine must have a mock implementation so Role 1 and Role 4 can build without waiting for OCR or speech integration.

## 7. Repository structure

```text
FormSaathi/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── assets/
│   │   │   ├── questions_en.json
│   │   │   ├── questions_hi.json
│   │   │   ├── questions_mr.json
│   │   │   └── models/
│   │   │       └── whisper-tiny-multilingual-q5.bin
│   │   ├── cpp/
│   │   │   ├── CMakeLists.txt
│   │   │   └── whisper_jni.cpp
│   │   └── java/com/formsaathi/
│   │       ├── MainActivity.kt
│   │       ├── model/
│   │       ├── contracts/
│   │       ├── ui/
│   │       ├── formengine/
│   │       ├── voice/
│   │       ├── language/
│   │       ├── answer/
│   │       ├── core/
│   │       └── pdf/
│   └── src/test/
├── samples/
│   ├── forms/
│   └── expected/
├── docs/
│   ├── architecture.md
│   ├── demo-script.md
│   └── screenshots/
├── README.md
├── PLAN.md
└── .gitignore
```

Do not commit a large Whisper model until the team checks repository limits. If it is too large, document a one-command or one-step download and keep a local demo copy on the presentation phone.

## 8. Shared setup for the first 45 minutes

All four members complete this together before branching.

1. Create a single Android Studio project with Kotlin and Jetpack Compose.
2. Set the application ID and minimum SDK agreed by the team.
3. Confirm the starter app builds on at least one physical Android phone.
4. Create the public GitHub repository.
5. Add a proper Android `.gitignore` before the first push.
6. Add the shared models and service interfaces from this plan.
7. Add mock implementations that return deterministic sample data.
8. Add one small sample PDF that is legal to redistribute.
9. Create the four feature branches.
10. Confirm every member can clone, build, run, commit, push, and open a pull request.

Branches must be named as follows.

```text
main
feature/ui
feature/form-engine
feature/voice-language
feature/core-pdf
```

Nobody develops directly on `main`.

## 9. Shared data contracts

These models are created on `main` before parallel work begins. Changes after the split require agreement from all roles.

```kotlin
enum class SupportedLanguage(val code: String) {
    ENGLISH("en"),
    HINDI("hi"),
    MARATHI("mr")
}

enum class FieldType {
    FULL_NAME,
    FATHER_NAME,
    MOTHER_NAME,
    DATE_OF_BIRTH,
    GENDER,
    MOBILE,
    EMAIL,
    AADHAAR,
    PERMANENT_ADDRESS,
    CURRENT_ADDRESS,
    SAME_AS_PERMANENT_ADDRESS,
    STATE,
    DISTRICT,
    PINCODE,
    CATEGORY,
    ANNUAL_INCOME,
    PHOTO,
    SIGNATURE,
    UNKNOWN
}

data class PageInfo(
    val pageIndex: Int,
    val pdfWidthPoints: Float,
    val pdfHeightPoints: Float,
    val renderedWidthPx: Int,
    val renderedHeightPx: Int
)

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class FormField(
    val id: String,
    val sourceLabel: String,
    val type: FieldType,
    val pageIndex: Int,
    val labelBox: NormalizedRect,
    val answerBox: NormalizedRect,
    val required: Boolean = false,
    val confidence: Float = 0f
)

data class FormAnswer(
    val fieldId: String,
    val rawValue: String,
    val normalizedValue: String,
    val source: AnswerSource
)

enum class AnswerSource {
    TYPED,
    VOICE,
    COPIED_BY_RULE
}

data class RequiredDocument(
    val name: String,
    val requirement: String? = null
)

data class ParsedForm(
    val pages: List<PageInfo>,
    val fields: List<FormField>,
    val documents: List<RequiredDocument>,
    val warnings: List<String> = emptyList()
)

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}
```

### Coordinate rule

All OCR and answer rectangles must be normalized to a 0.0 to 1.0 coordinate system relative to the rendered page.

```text
normalizedX = pixelX / renderedPageWidth
normalizedY = pixelY / renderedPageHeight

outputX = normalizedX * outputPageWidth
outputY = normalizedY * outputPageHeight
```

This rule prevents Role 2's OCR resolution from being coupled to Role 4's PDF output resolution. Android bitmap coordinates start at the top-left. Android Canvas also uses a top-left origin, which keeps the overlay calculation simple.

`answerBox` must identify where the answer should be drawn. It must not be the same as `labelBox` unless the form layout requires that behavior.

## 10. Frozen service interfaces

```kotlin
interface FormParser {
    suspend fun parse(uri: Uri): ParsedForm
}

interface QuestionProvider {
    fun questionFor(
        fieldType: FieldType,
        language: SupportedLanguage
    ): String
}

interface VoiceService {
    suspend fun transcribe(
        audioFile: File,
        language: SupportedLanguage
    ): String
}

interface AnswerProcessor {
    fun normalize(
        fieldType: FieldType,
        rawText: String,
        language: SupportedLanguage
    ): String

    fun validate(
        fieldType: FieldType,
        value: String
    ): ValidationResult
}

interface CompletedPdfGenerator {
    suspend fun generate(
        sourceUri: Uri,
        parsedForm: ParsedForm,
        answers: Map<String, FormAnswer>,
        outputUri: Uri
    )
}
```

Every real implementation must have a matching fake implementation.

```text
FakeFormParser
FakeQuestionProvider
FakeVoiceService
FakeAnswerProcessor
FakeCompletedPdfGenerator
```

The fakes allow every branch to compile and demonstrate its own subsystem independently.

## 11. Role 1 workstream

### Android UI and user experience

**Branch:** `feature/ui`

**Owns:** Everything visible to the user

**Does not own:** OCR internals, Whisper integration, or PDF drawing

### Files

```text
ui/
├── navigation/AppNavigation.kt
├── screens/LanguageScreen.kt
├── screens/HomeScreen.kt
├── screens/ProcessingScreen.kt
├── screens/QuestionScreen.kt
├── screens/ReviewScreen.kt
├── screens/DocumentsScreen.kt
├── screens/ResultScreen.kt
├── components/PrimaryButton.kt
├── components/QuestionCard.kt
├── components/VoiceButton.kt
├── components/ProgressHeader.kt
└── theme/
```

### Tasks

#### UI-1. Navigation shell

- Build the complete screen graph.
- Use a single activity.
- Pass IDs and lightweight state between destinations.
- Keep files and bitmaps out of navigation arguments.

#### UI-2. Language screen

- Display English, Hindi, and Marathi.
- Store the selected language in app state.
- Use large tap targets and high contrast.

#### UI-3. Home and PDF picker

- Add `Select a form` using `ACTION_OPEN_DOCUMENT` or Compose's document launcher.
- Accept `application/pdf` only for the MVP.
- Show the selected filename.
- Forward the returned `Uri` without attempting to convert it to an absolute filesystem path.

#### UI-4. Processing screen

- Show clear stages such as rendering, reading text, and preparing questions.
- Use indeterminate progress unless an engine reports real progress.
- Display a recoverable error if parsing fails.

#### UI-5. Question flow

- Show one question per screen.
- Display `Question X of Y`.
- Provide text input, microphone button, previous, next, and skip.
- Show validation feedback without clearing the user's answer.
- Disable next only for required fields with invalid values.

#### UI-6. Review and documents

- List every detected field and normalized answer.
- Allow each answer to be edited.
- Show required documents and extracted constraints.
- Clearly label low-confidence or unknown fields for manual review.

#### UI-7. Result screen

- Show generation success or a useful failure message.
- Provide Open PDF and Share PDF actions.
- Display the saved filename.

#### UI-8. Accessibility and visual polish

- Use readable type sizes and strong contrast.
- Use 48 dp or larger interactive targets.
- Add content descriptions to icons.
- Keep Hindi and Marathi text from clipping.
- Avoid dense screens and technical vocabulary.

### Mock data

Role 1 builds against a `FakeFormParser` containing 10 to 12 fields and two document requirements. `FakeVoiceService` can return a fixed transcript. `FakeCompletedPdfGenerator` can return a success state without generating a file.

### Role 1 completion gate

The branch must demonstrate the complete user journey with fake engines. A user must be able to select a mock form, answer questions, review answers, view documents, press Generate, and reach the result screen.

## 12. Role 2 workstream

### OCR and form understanding

**Branch:** `feature/form-engine`

**Owns:** PDF rendering for OCR, text recognition, label detection, canonical field mapping, answer-box estimation, and requirement extraction

**Does not own:** UI, voice processing, session navigation, or PDF output

### Files

```text
formengine/
├── AndroidPdfPageRenderer.kt
├── MlKitOcrEngine.kt
├── OcrBlock.kt
├── LabelDetector.kt
├── FieldAliases.kt
├── FieldMapper.kt
├── AnswerBoxEstimator.kt
├── RequirementExtractor.kt
└── RealFormParser.kt
```

### Tasks

#### OCR-1. Render PDF pages

- Open the selected content `Uri` with a `ParcelFileDescriptor`.
- Use `PdfRenderer` to create consistent page bitmaps.
- Record PDF page size and rendered bitmap size in `PageInfo`.
- Close pages, renderer, and descriptors reliably.
- Limit bitmap size to avoid out-of-memory crashes.

#### OCR-2. Recognize text locally

- Configure bundled ML Kit Latin and Devanagari recognizers so first use does not require a model download.
- Select the recognizer from the detected or chosen form script. Do not assume the Latin recognizer can read Hindi or Marathi text.
- Return text, bounding boxes, page index, and OCR confidence when available.
- Preserve line and element grouping because label detection depends on layout.

```kotlin
data class OcrBlock(
    val text: String,
    val pageIndex: Int,
    val box: NormalizedRect,
    val confidence: Float?
)
```

#### OCR-3. Detect likely field labels

- Normalize case, punctuation, whitespace, and repeated separators.
- Reject headers, instructions, and paragraphs that are too long to be field labels.
- Detect visual cues such as a label followed by a blank line, box, colon, or nearby empty region.
- Keep unknown labels instead of silently dropping them when an answer area is clear.

#### OCR-4. Map labels to canonical fields

Use this fixed priority.

1. Exact normalized alias match
2. Strong keyword pattern
3. Token overlap
4. Bounded edit-distance match
5. `UNKNOWN`

Do not use a fuzzy threshold alone. A weak fuzzy match can incorrectly turn one legal field into another.

Initial aliases must cover at least these fields.

```text
Full name
Father's name
Mother's name
Date of birth
Gender
Mobile number
Email
Aadhaar number
Permanent address
Current address
State
District
PIN code
Category
Annual income
Photograph
Signature
```

#### OCR-5. Estimate answer rectangles

- Prefer a printed box or underline immediately right of or below the label.
- Fall back to a conservative blank rectangle after the label.
- Normalize the rectangle before returning it.
- Clamp all coordinates to the 0.0 to 1.0 page range.
- Add a warning when the answer location cannot be estimated safely.

#### OCR-6. Extract document requirements

- Search complete OCR text for known document names.
- Capture nearby constraints such as file size, format, count, original, copy, or self-attested.
- Deduplicate repeated requirements.
- Support Aadhaar card, income certificate, photograph, residence certificate, PAN card, and signature in the MVP.

#### OCR-7. Optimize selected demo forms

- Choose two or three legal sample forms.
- Save expected parsed output in `samples/expected/`.
- Add aliases and layout rules only when they improve a real selected form.
- Record unsupported fields in warnings so the UI can ask for manual review.

### Role 2 completion gate

Calling `RealFormParser.parse(uri)` on each selected form must return page metadata, at least 8 useful fields, normalized answer rectangles, canonical types, and extracted document requirements where present. The role must provide a small debug screen or instrumentation entry point that draws detected boxes over page images.

## 13. Role 3 workstream

### Offline voice, multilingual questions, normalization, and validation

**Branch:** `feature/voice-language`

**Owns:** Audio capture, `whisper.cpp`, local question text, deterministic answer normalization, and validation

**Does not own:** Form parsing, UI navigation, session rules, or PDF generation

### Files

```text
voice/
├── AudioRecorder.kt
├── WhisperBridge.kt
├── WhisperManager.kt
├── RealVoiceService.kt
└── native/
    └── whisper_jni.cpp
language/
├── JsonQuestionProvider.kt
├── questions_en.json
├── questions_hi.json
└── questions_mr.json
answer/
├── RuleBasedAnswerProcessor.kt
├── NumberNormalizer.kt
├── DateNormalizer.kt
└── FieldValidator.kt
```

### Tasks

#### VOICE-1. Prove native integration first

- Add NDK and CMake configuration.
- Build `whisper.cpp` for the phone's target ABI.
- Load the quantized multilingual Tiny model from app-managed storage.
- Prove one bundled or recorded WAV file can be transcribed with airplane mode enabled.
- Measure load time, transcription time, memory use, and APK/model size.

This is the highest-risk technical task. If it does not work by the first major checkpoint, use the fallback defined in Section 18.

#### VOICE-2. Record compatible audio

- Request microphone permission at the moment of use.
- Record mono 16 kHz PCM audio in a format accepted by the native layer.
- Limit answers to roughly 3 to 10 seconds.
- Expose start, stop, cancel, and current recording state.
- Delete temporary audio after transcription unless debugging is enabled.

#### VOICE-3. Implement the service boundary

- Keep JNI details inside `WhisperBridge`.
- Return a plain transcript from `VoiceService`.
- Support explicit `en`, `hi`, and `mr` language hints.
- Return a typed error for missing model, recording failure, or native inference failure.

#### LANG-1. Write local question resources

- Provide one natural, simple question per supported canonical field.
- Avoid literal bureaucratic translations.
- Keep JSON keys identical across all three languages.
- Add a fallback to English for a missing translation.
- Have a fluent speaker review Hindi and Marathi text before the demo.

#### ANSWER-1. Normalize common values

Support these deterministic transformations.

| Field | Examples | Stored value |
| --- | --- | --- |
| Income | `two lakh fifty thousand`, `दो लाख पचास हजार` | `250000` |
| Date | `12 May 2005` | `12/05/2005` |
| Phone | Spoken digits with spaces | Ten digits |
| PIN code | Spoken digits with spaces | Six digits |
| Yes or no | Common English, Hindi, and Marathi forms | `yes` or `no` |
| Category | Common variants | Canonical category label |

Preserve the raw transcript with the normalized value. Never discard the original text.

#### ANSWER-2. Validate values

- Mobile number must contain ten digits for the prototype.
- PIN code must contain six digits.
- Email must pass a conservative format check.
- Date of birth must be a real date and cannot be in the future.
- Annual income must be a non-negative number.
- Aadhaar input must contain twelve digits. Do not claim that this verifies Aadhaar authenticity.
- Free-text names and addresses must not be over-restricted.

#### VOICE-4. Optional text-to-speech

Add Android `TextToSpeech` only after offline speech input, questions, normalization, and validation are stable. Detect whether the selected language voice is installed. If it is missing, keep the visible text flow functional and explain that read-aloud is unavailable.

### Role 3 completion gate

On the demo phone with airplane mode enabled, the branch must record and transcribe short English, Hindi, and Marathi answers, return simple localized questions, normalize the agreed common value types, and validate them through the frozen interfaces.

## 14. Role 4 workstream

### Core session, conditional logic, PDF generation, and integration preparation

**Branch:** `feature/core-pdf`

**Owns:** Form session state, progress, conditional rules, output generation, export, preview integration, and dependency wiring

**Does not own:** Compose screen design, OCR heuristics, or Whisper internals

### Files

```text
core/
├── FormSession.kt
├── ConversationEngine.kt
├── FormSaathiCoordinator.kt
├── FormUiState.kt
└── EngineFactory.kt
pdf/
├── AndroidCompletedPdfGenerator.kt
├── PdfPageComposer.kt
├── AnswerTextFitter.kt
└── OutputFileManager.kt
```

### Tasks

#### CORE-1. Form session

- Hold the parsed form, current visible field, answers, language, and generation state.
- Expose immutable `StateFlow` to the UI.
- Implement set, edit, remove, next, previous, skip, and progress operations.
- Key answers by `fieldId`, not by `FieldType`, because a form can contain repeated types.
- Keep raw and normalized values.

#### CORE-2. Conditional behavior

Implement only clear MVP rules.

- If current address is the same as permanent address, copy the answer and skip the duplicate address question.
- Skip fields marked not applicable by an explicit yes or no question.
- Do not invent answers from unrelated fields.
- Show a copied answer on the review screen so the user can correct it.

#### CORE-3. PDF generation proof

- Open the source PDF through a `ParcelFileDescriptor`.
- Render one source page to a bitmap.
- Create a matching `PdfDocument.PageInfo` page.
- Draw the rendered source page as the background.
- Convert each normalized answer box into output canvas coordinates.
- Draw the normalized answer with a readable font and padding.
- Save to a caller-provided output `Uri`.
- Reopen and preview the created PDF.

#### CORE-4. Multi-page output

- Repeat rendering and composition for every source page.
- Draw only answers assigned to the current page.
- Preserve page order and aspect ratio.
- Close every stream, page, renderer, and document in `finally` or `use` blocks.

#### CORE-5. Text fitting

- Fit font size within safe minimum and maximum sizes.
- Wrap address fields over multiple lines.
- Clip text to the answer rectangle.
- Add a warning if text cannot fit rather than silently drawing outside the form.
- Keep answer ink visually distinct but professional.

#### CORE-6. Export and preview

- Use `ACTION_CREATE_DOCUMENT` for the output destination.
- Suggest `Completed_Form.pdf` as the filename.
- Use content URIs and `FileProvider` where needed.
- Do not request broad storage permission.
- Ensure Open PDF and Share PDF work on the demo phone.

#### CORE-7. Dependency wiring

- Create one coordinator that receives all interfaces through constructors.
- Provide a mock factory and a real factory.
- Do not instantiate OCR, Whisper, or PDF engines directly inside Compose screens.

```kotlin
class FormSaathiCoordinator(
    private val formParser: FormParser,
    private val questionProvider: QuestionProvider,
    private val voiceService: VoiceService,
    private val answerProcessor: AnswerProcessor,
    private val pdfGenerator: CompletedPdfGenerator
)
```

### Role 4 completion gate

Using a mock `ParsedForm` and mock answers, the branch must manage a full session and generate a readable, correctly aligned, multi-page flattened PDF that opens on the demo phone.

## 15. Parallel execution schedule

The dedicated system-testing phase occurs near the end, but each role must keep its own branch building throughout development. Deferring all verification would make integration failure likely.

| Time | Role 1 | Role 2 | Role 3 | Role 4 |
| --- | --- | --- | --- | --- |
| 0 to 0.75 h | Shared project, contracts, mocks, branches | Shared project, contracts, mocks, branches | Shared project, contracts, mocks, branches | Shared project, contracts, mocks, branches |
| 0.75 to 8 h | Navigation and main screens | PDF rendering and OCR | NDK, model, and first transcription | Session and one-page PDF proof |
| 8 to 16 h | Full mock user flow | Field mapping and coordinates | Audio recording and service | Multi-page output and text fitting |
| 16 to 24 h | Review, documents, errors, polish | Requirements and selected-form tuning | Questions and normalization | Conditional rules and export |
| 24 to 30 h | Finish UI and clean branch | Finish parser and debug overlay | Validation and offline proof | Coordinator and real/mock factories |
| 30 to 34 h | Merge and wire real engines | Merge and resolve contracts | Merge and resolve native build | Lead integration and output wiring |
| 34 to 40 h | End-to-end testing and bug fixing by all four members | End-to-end testing and bug fixing by all four members | End-to-end testing and bug fixing by all four members | End-to-end testing and bug fixing by all four members |
| 40 to 44 h | Screenshots and UI polish | Parser evidence | Offline voice evidence | Release APK and final PDF proof |
| 44 to 48 h | PPT, README, demo video, repository cleanup, and submission | PPT, README, demo video, repository cleanup, and submission | PPT, README, demo video, repository cleanup, and submission | PPT, README, demo video, repository cleanup, and submission |

## 16. Merge plan

### Before hour 30

Each branch must satisfy these conditions.

- Builds from a clean clone
- Contains no secrets, local absolute paths, or machine-specific configuration
- Uses the frozen contracts
- Includes a short branch-specific README note
- Has no unrelated formatting changes
- Has at least one repeatable way to demonstrate the subsystem

### Merge order

1. Merge `feature/core-pdf` first because it contains coordinator wiring and shared session state.
2. Merge `feature/form-engine` and replace `FakeFormParser` through the factory.
3. Merge `feature/voice-language` and replace question, voice, and processor fakes.
4. Merge `feature/ui` and connect UI events to the coordinator.
5. Keep every fake implementation available through a debug build flag until the real end-to-end flow is stable.

### Conflict rule

The owner of a file resolves conflicts in that file. Role 4 owns coordinator and wiring conflicts. Contract changes require all four members to review the diff before merging.

### Commit style

Use small commits with clear messages.

```text
feat(ui): add review screen
feat(ocr): normalize detected field coordinates
feat(voice): add Hindi transcription path
fix(pdf): scale overlay to output page
docs(readme): add offline setup steps
```

## 17. End-to-end testing from hour 34 to hour 40

No new features enter the MVP after hour 34 unless the complete flow is already stable.

### Functional test matrix

| Test | Expected result |
| --- | --- |
| Launch with airplane mode | App opens without a network error |
| Import supported PDF | Pages render and OCR begins |
| Parse selected forms | At least 8 useful fields appear |
| Switch each language | Questions update without restarting the session |
| Type valid answer | Answer is normalized and stored |
| Enter invalid phone or PIN | Clear correction message appears |
| Record short voice answer | Transcript appears without network access |
| Same-address rule | Current address is copied and skipped |
| Review and edit | Updated value persists |
| Show requirements | Detected documents are listed without duplicates |
| Generate PDF | Output contains the original page and answers |
| Multi-page input | Page order and overlays remain correct |
| Open exported PDF | Android PDF viewer opens it |
| Share exported PDF | Android share sheet receives the PDF |
| Rotate or background app | Session does not immediately disappear |
| Deny microphone permission | Typed input remains fully usable |

### Visual PDF checks

- No answer is drawn over the wrong label.
- No text is clipped outside the answer box.
- Long addresses wrap legibly.
- Hindi and Marathi glyphs render correctly in the UI.
- Generated pages preserve the source aspect ratio.
- Every page from the source PDF exists in the output.
- Output opens in at least two PDF viewers when possible.

### Device checks

- Run on the exact phone used for the video.
- Test a fresh install of the release APK.
- Verify the Whisper model is present after installation or setup.
- Record cold-start and transcription time.
- Confirm sufficient storage space before the demo.
- Keep a second tested phone or screen recording as backup if available.

### Privacy checks

- Disable Wi-Fi and mobile data for the final proof.
- Confirm the app has no required Internet permission unless a justified optional feature needs it.
- Inspect the public repository for keys, credentials, private forms, recorded voices, and personal identifiers.
- Use fictional data in screenshots, sample forms, PDFs, and the video.

## 18. Risk register and fallbacks

| Risk | Trigger | Fallback |
| --- | --- | --- |
| `whisper.cpp` does not build | No offline transcription proof by hour 12 | Keep typed input as fully functional and use a prebuilt compatible integration only if licensing and ABI support are verified |
| Tiny model is too inaccurate | Demo phrases fail repeatedly | Use shorter prompted answers, explicit language selection, and editable transcripts. Test a larger quantized model only if the phone handles it |
| Marathi transcription is weak | Marathi demo is unreliable | Keep Marathi typed questions and typed answers. Demonstrate Hindi voice and state Marathi voice as limited in the prototype |
| OCR misses arbitrary layouts | Fewer than 8 fields on selected form | Tune aliases and geometry for two selected demo forms and disclose limited prototype coverage |
| Wrong answer coordinates | Overlay is visibly misaligned | Store normalized boxes, add a debug overlay, and allow per-form coordinate overrides for demo forms |
| PDF generation causes memory errors | Large page bitmap crashes | Render one page at a time at a lower safe resolution and recycle page bitmaps immediately |
| Native model makes APK too large | GitHub or build distribution becomes impractical | Exclude the model from Git and document a setup download. Preload it on the demo phone |
| Hindi or Marathi glyphs fail in PDF | Output shows missing-glyph boxes | Embed or bundle a compatible Devanagari font for canvas drawing and verify licensing |
| Team merge blocks progress | Conflicts persist beyond 30 minutes | Restore frozen interfaces, keep engines behind mocks, and cut nonessential features |
| No time for optional features | Core flow is not stable by hour 30 | Drop CameraX, Room, text-to-speech, persistence, and extra forms immediately |

Typed input is part of the MVP and remains the recovery path when speech fails. A fake voice result must never be presented as real transcription in the final demo.

## 19. Definition of done

### Prototype

- The release build installs on the demo phone.
- The demonstrated workflow runs with airplane mode enabled.
- A real selected PDF is parsed locally.
- At least 8 useful fields are shown as simple questions.
- English, Hindi, and Marathi question text is available.
- Typed input works for every question.
- At least one non-English language works with offline voice input if the final demo claims multilingual voice.
- Validation catches the agreed invalid formats.
- Review and edit works.
- Required documents are shown when detected.
- `Completed_Form.pdf` is generated, readable, and correctly aligned.

### Repository

- Repository is public.
- Default branch builds from documented steps.
- README explains the problem, solution, architecture, technology stack, setup, run process, supported forms, limitations, and team roles.
- No keys, credentials, private user data, or unnecessary binaries are committed.
- License notices for ML Kit, `whisper.cpp`, the Whisper model, fonts, and sample forms are reviewed and included where required.
- Release APK or a clear prototype link is available.

### Submission

- PPT is complete.
- Public GitHub link works in a signed-out browser.
- Working prototype link or APK access works.
- Three-minute video is complete and understandable without live explanation.
- All Unstop fields are filled before the deadline.

## 20. Six-slide PPT plan

If the organizers do not provide a template, use this exact structure.

| Slide | Content |
| --- | --- |
| 1. Title | FormSaathi, team name, member names, Jan Jeevan track, and one-line summary |
| 2. Problem | Difficult government forms, affected users, consequences, and grounded evidence or observations |
| 3. Solution | Conversational form filling, three languages, offline voice, validation, requirements, and completed PDF |
| 4. Technology | Architecture diagram, Kotlin and Compose, ML Kit OCR, `whisper.cpp`, rules, and on-device PDF output |
| 5. Feasibility and impact | Offline privacy, low-connectivity use, current form coverage, scaling path, and expected user benefit |
| 6. Prototype and future scope | Screenshots, demo evidence, GitHub and prototype links, supported forms, honest limitations, and next steps |

Keep every slide self-explanatory because judging is online and there is no live question session.

## 21. Three-minute video plan

| Time | Show |
| --- | --- |
| 0:00 to 0:15 | The problem and the FormSaathi promise |
| 0:15 to 0:25 | Enable airplane mode and show there is no Wi-Fi or mobile data |
| 0:25 to 0:45 | Open the app, select Hindi or Marathi, and import a supported form |
| 0:45 to 1:10 | Show local processing and detected questions |
| 1:10 to 1:45 | Answer by voice, show transcript, normalization, validation, and a conditional skip |
| 1:45 to 2:05 | Show required documents and review answers |
| 2:05 to 2:30 | Generate and open `Completed_Form.pdf` |
| 2:30 to 2:50 | Show architecture and privacy claim |
| 2:50 to 3:00 | Show GitHub link, prototype link, and closing impact statement |

Record the demo only after completing a rehearsal with the exact PDF, answers, phone, screen recorder, and APK version.

## 22. README checklist

The README must contain these sections.

1. Project overview
2. Problem statement
3. Target users
4. Key features
5. Offline architecture
6. Technology stack
7. Supported forms and languages
8. Setup prerequisites
9. Model setup
10. Build and run instructions
11. How to use the app
12. Repository structure
13. Known limitations
14. Privacy and data handling
15. Team roles and contributions
16. Demo video and prototype links
17. Future scope
18. Third-party licenses and acknowledgements

## 23. Team knowledge handoff

At the end, every member must understand the complete pipeline rather than only their own branch.

| Member | Must explain another subsystem |
| --- | --- |
| Role 1 owner | OCR, field mapping, and normalized coordinates |
| Role 2 owner | Offline voice and answer normalization |
| Role 3 owner | PDF rendering, overlay scaling, and export |
| Role 4 owner | Compose navigation, UI state, and the user flow |

Run a final 20-minute internal walkthrough. Each member explains their assigned external subsystem and answers one failure-scenario question about it.

## 24. Final submission checklist

- [ ] Project is clearly submitted under Jan Jeevan
- [ ] Prototype works from a fresh release installation
- [ ] Airplane-mode demo succeeds
- [ ] Supported sample PDFs contain fictional data
- [ ] Completed PDF is readable and correctly aligned
- [ ] Public GitHub repository opens without authentication
- [ ] README contains setup and run instructions
- [ ] Repository contains no secrets or personal data
- [ ] Six-slide PPT is complete or organizer template is used
- [ ] Three-minute video stays within the limit
- [ ] Prototype link or APK access works
- [ ] GitHub link is entered in the designated Unstop field
- [ ] All links are tested from a different device or signed-out browser
- [ ] Submission is completed before the deadline

## 25. Immediate first actions

1. Select two or three demo government forms with redistribution permission.
2. Choose the physical Android phone and target ABI for the demo.
3. Create the Android project and public repository.
4. Commit shared contracts and mocks to `main`.
5. Create all four feature branches.
6. Start the four independent workstreams immediately.
