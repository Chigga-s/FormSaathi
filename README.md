# FormSaathi

Native Android prototype for answering PDF-form questions in English, Hindi and Marathi, then generating a flattened PDF on-device. See `PLAN(1).md` for the original scope.

## Build

Requirements: JDK 17 or 21, Android SDK 34, NDK `26.1.10909125`, CMake `3.22.1`, and Git. The build targets Android 8/API 26 or newer, with ARM64 phone and x86-64 emulator libraries. Set `JAVA_HOME` and `ANDROID_HOME` for your machine, or configure these through Android Studio. Do not commit machine-specific Java/SDK paths.

From the repository root on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup-voice.ps1 -DownloadModel
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The setup script retrieves Whisper v1.7.6 at commit `a8d002cfd879315632a579e73f0148d06959de36` and downloads the multilingual Tiny Q5_1 model. It verifies the model SHA-256 `818710568DA3CA15689E31A743197B520007872FF9576237BDA97BD1B469C3D7` and fails on mismatch. The upstream model URL currently follows `main`; keep the pinned hash for repeatable releases. Existing source checkouts and models are not overwritten.

The source checkout and model are intentionally ignored by Git. Model asset: `app/src/main/assets/models/whisper-tiny-multilingual-q5.bin`. Omitting `-DownloadModel` prepares native sources only. The app can launch and accept typed input without a model; voice then reports that the model is unavailable. Rebuild and reinstall after adding a model.

APK: `app/build/outputs/apk/debug/app-debug.apk`. This is a debug APK, not a signed production release.

## Workflow

1. Select a question language and import a PDF through the document picker.
2. Answer detected questions by typing or recording up to ten seconds of audio.
3. Inspect/edit the transcript before submitting it. Invalid answers remain available for correction.
4. Review answers and document requirements; edit any field that needs correction.
5. Choose where to save `Completed_Form.pdf`, then open or share it.

Question language can change within a session. Session state is held in a ViewModel across rotation; process-death persistence is not implemented. Backgrounding cancels an active recording. Raw and normalized answers are retained in memory. Internal recordings use mono 16 kHz PCM16 little-endian `.pcm` files, deleted after transcription or cancellation.

## Architecture and integration

- `UI/`: existing Role 1 screens connected to live state by `AppNavigation.kt`.
- `core/`: Role 4 session/coordinator/factories, with `FormViewModel` owning live dependencies and lifecycle.
- `formengine/`: PdfRenderer, bundled Latin/Devanagari ML Kit recognizers and rule-based field/rectangle detection.
- `voice/` and `cpp/`: recording, file decoding, serialized native model ownership and explicit `en`/`hi`/`mr` inference hints.
- `language/`, `assets/questions_*.json`, `answer/`: local questions and deterministic normalization/validation.
- `pdf/`: multi-page flattened output, wrapping/clipping warnings and content-URI export.

Integration adopts Role 4's `CompletedPdfGenerator.generate(...): GenerationResult` refinement so overflow warnings reach the result screen. VoiceService remains `transcribe(File, SupportedLanguage): String`; failures use `VoiceException`. Real dates use the original plan's `DD/MM/YYYY` format. Existing mock factories remain available for subsystem development.

## Verification

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.formsaathi.AppLaunchTest,com.formsaathi.pdf.AndroidCompletedPdfGeneratorTest,com.formsaathi.voice.WhisperBridgeInstrumentedTest
```

The last command requires a connected device/emulator. Native linking tests do not prove speech accuracy. The existing live-microphone voice test requires a person to speak and a provisioned model. Do not treat random-noise inference or printed transcripts as successful speech acceptance tests.

Before a demo, test English, Hindi and Marathi recordings in airplane mode, microphone denial, ten-second timeout, repeated record/cancel, rotation, long addresses, PDF glyph rendering, multi-page alignment, and open/share. Record cold load time, inference time and memory on the actual phone.

## Current limitations

- Two or three redistributable government forms and their expected parser fixtures still need selecting. Generic OCR geometry has not been accepted against those forms.
- Spoken-number vocabulary is bounded; unsupported phrases are retained for correction rather than silently converted. Hindi/Marathi question wording and speech accuracy need fluent-speaker/device review.
- Photo/signature image insertion is not implemented. These need manual completion; the prototype does not verify identity or submit to government portals.
- Device acceptance, signed release configuration, presentation and final demo recording remain release gates.
- No Internet permission is declared by the application. Initial developer dependency/model provisioning requires Internet access; runtime inference is local.

## Third-party notices

Whisper and ggml license files are included in the downloaded source checkout. The model is distributed through `ggerganov/whisper.cpp` on Hugging Face and derives from OpenAI Whisper. Review upstream model licensing and redistribute the required notices with the APK. ML Kit is subject to Google's SDK terms. Include licenses/redistribution permission for selected sample forms and any added fonts before public release.
