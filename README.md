# FormSaathi - Role 4: Core Session, Conditional Logic, PDF Generation & Integration Wiring

**Branch:** `feature/core-pdf`  
**Target Track:** Jan Jeevan  
**Primary Responsibilities:** Session state management, question conversation flow & conditional rules, anti-OOM multi-page PDF generation with dynamic text fitting, and integration orchestration (`FormSaathiCoordinator`).

---

## 1. Subsystem Overview

Role 4 acts as the central spine and dependency wiring harness for the FormSaathi application. It implements:

1. **Shared Data Contracts (`com.formsaathi.model`)**:
   - `SupportedLanguage`, `FieldType`, `PageInfo`, `NormalizedRect`, `FormField`, `FormAnswer`, `AnswerSource`, `RequiredDocument`, `ParsedForm`, `ValidationResult`.
   - Normalization rule: All coordinates are normalized $0.0 \dots 1.0$ relative to rendered page width/height.

2. **Frozen Service Interfaces & Fakes (`com.formsaathi.contracts`)**:
   - `FormParser`, `QuestionProvider`, `VoiceService`, `AnswerProcessor`, `CompletedPdfGenerator`.
   - Complete deterministic fake implementations (`FakeFormParser`, `FakeQuestionProvider`, `FakeVoiceService`, `FakeAnswerProcessor`, `FakeCompletedPdfGenerator`) allowing Role 1 (UI) to run independently without waiting for native or ML dependencies.
   - **Contract Refinement Note**: `CompletedPdfGenerator.generate()` returns `GenerationResult` (wrapping `warnings: List<TextFitWarning>`) rather than `Unit`. This change is necessary for CORE-5 text-fitting truncation warnings to be surfaced to the review screen and completed session state. This modifies the frozen contract from `PLAN(1).md` and requires team agreement across all roles before branch integration into `main`.

3. **Core State & Conversation Logic (`com.formsaathi.core`)**:
   - `FormSession`: In-memory state container tracking answers (`fieldId -> FormAnswer`), visited history, and language. Designed for single-threaded UI coordination via `FormSaathiCoordinator`.
   - `ConversationEngine`: Implements the MVP conditional rule: answering affirmative ("yes") to `SAME_AS_PERMANENT_ADDRESS` automatically copies the permanent address answer to `CURRENT_ADDRESS` with `AnswerSource.COPIED_BY_RULE` and skips the duplicate question. Negative or alternative answers leave `CURRENT_ADDRESS` active for the user.
   - `FormUiState`: Immutable sealed hierarchy consumed by Jetpack Compose.
   - `FormSaathiCoordinator`: The central entry point for UI events (`startSession`, `switchLanguage`, `submitAnswer`, `skipCurrentField`, `previousField`, `updateAnswerInReview`, `generatePdf`).
   - `EngineFactory`: Factory for assembling coordinators using either deterministic mocks or production engines.

4. **PDF Generation & Export Engine (`com.formsaathi.pdf`)**:
   - `AnswerTextFitter`: High-precision text fitting using `TextPaint` and `StaticLayout`. Auto-scales fonts (13sp down to 6.5sp), wraps multi-line addresses, applies 4% padding, and enforces strict canvas clipping to prevent text spilling onto adjacent form lines.
   - `PdfPageComposer`: Scales background page bitmaps to PDF page points, converts normalized coordinates ($0.0 \dots 1.0$) to canvas coordinates, and overlays answer text with distinct pen-blue ink (`#0D2546`). Returns text-fit warnings when text is clipped.
   - `AndroidCompletedPdfGenerator`: Production `CompletedPdfGenerator` processing pages one-by-one with immediate `bitmap.recycle()` calls, dynamic scale capping to prevent OOM on large pages, and strict stream closure.
   - `SamplePdfFactory`: Generates a valid multi-page source PDF matching `FakeFormParser` layout for realistic offline testing and mock sessions without network access.
   - `OutputFileManager`: Manages Storage Access Framework (`ACTION_CREATE_DOCUMENT`), internal cache PDFs, `FileProvider` content URIs, and Android View/Share intents without broad storage permissions.

---

## 2. Directory Structure

```text
app/src/main/java/com/formsaathi/
├── model/
│   ├── DataContracts.kt
│   └── GenerationResult.kt
├── contracts/
│   ├── ServiceInterfaces.kt
│   └── FakeEngines.kt
├── core/
│   ├── FormUiState.kt
│   ├── ConversationEngine.kt
│   ├── FormSession.kt
│   ├── FormSaathiCoordinator.kt
│   └── EngineFactory.kt
└── pdf/
    ├── AnswerTextFitter.kt
    ├── PdfPageComposer.kt
    ├── AndroidCompletedPdfGenerator.kt
    ├── SamplePdfFactory.kt
    └── OutputFileManager.kt
```

---

## 3. How Other Roles Merge With Role 4

### Role 1 (UI)
Injects `FormSaathiCoordinator` into the screen/viewmodel hierarchy via `EngineFactory.createMockCoordinator()`:
```kotlin
val coordinator = EngineFactory.createMockCoordinator(context)
val uiState by coordinator.uiState.collectAsState()
```

### Role 2 (Form Engine / OCR)
Replaces `FakeFormParser` in `EngineFactory.createRealCoordinator(context, realFormParser, ...)` with their `RealFormParser`.

### Role 3 (Voice / Language / Normalization)
Replaces `FakeQuestionProvider`, `FakeVoiceService`, and `FakeAnswerProcessor` in `EngineFactory.createRealCoordinator(context, ..., realVoiceService, realQuestionProvider, realAnswerProcessor)`.

---

## 4. Verification & Testing

### Unit Tests (`app/src/test/java/com/formsaathi/`)
- `ConversationEngineTest`: Verifies automated same-address copying, non-address field preservation, skipping, and index calculations.
- `FormSessionTest`: Verifies state transitions, answer mutations, active question position counting, and navigation history.
- `PdfPageComposerTest`: Verifies coordinate transform formulas, CanvasRect dimensions, text-fit warning structures, and multi-line heuristics.
- `FormSaathiCoordinatorTest`: Verifies end-to-end lifecycle from document parse to question progression, validation errors, and completed PDF export.

### Instrumented Proof Tests (`app/src/androidTest/java/com/formsaathi/`)
- `AndroidCompletedPdfGeneratorTest`: Verifies on Android that `SamplePdfFactory` creates a valid 2-page source PDF, `AndroidCompletedPdfGenerator` produces a valid non-empty output PDF with answers for both page 1 and page 2 (specifically verifying `field_annual_income`), preserves page order, and reopens cleanly via Android's `PdfRenderer`.
