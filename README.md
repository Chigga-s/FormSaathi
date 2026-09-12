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

1. Select a question language and import a PDF through the document picker, or start a mock session with the built-in sample form.
2. Answer detected questions by typing or recording up to ten seconds of audio.
3. Inspect/edit the transcript before submitting it. Invalid answers remain available for correction.
4. Review answers and document requirements; edit any field that needs correction.
5. Choose where to save `Completed_Form.pdf`, then open or share it.

Question language can change within a session. Session state is held across navigation and rotation. Backgrounding cancels an active recording. Raw and normalized answers are retained in memory. Internal recordings use mono 16 kHz PCM16 little-endian `.pcm` files, deleted after transcription or cancellation.

## Architecture and Integration

- `UI/`: Role 1 screens (Splash, Language, Home, Processing, Questions, Review, Result) connected to live state by `AppNavigation.kt`.
- `core/`: Role 4 session/coordinator/factories, `FormSessionLauncher` for stable coordinator lifecycle, and `FormViewModel` for live voice/recording integration.
- `formengine/`: Role 2 ML Kit OCR form engine, PdfRenderer, bundled Latin/Devanagari recognizers, and rule-based field/rectangle detection.
- `voice/` and `cpp/`: Role 3 recording, file decoding, serialized native Whisper model ownership and explicit `en`/`hi`/`mr` inference hints.
- `language/`, `assets/questions_*.json`, `answer/`: local multilingual questions and deterministic normalization/validation (gender, dates, numbers, categories).
- `pdf/`: Role 4 multi-page flattened output, dynamic memory scaling, text fitting, wrapping/clipping warnings, same-address conditional copying, and content-URI export.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin --offline --console=plain
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.formsaathi.pdf.AndroidCompletedPdfGeneratorTest" --offline --console=plain
```

The connected command requires a connected device/emulator.

## Third-party notices

Whisper and ggml license files are included in the downloaded source checkout. The model is distributed through `ggerganov/whisper.cpp` on Hugging Face and derives from OpenAI Whisper. Review upstream model licensing and redistribute the required notices with the APK. ML Kit is subject to Google's SDK terms. Include licenses/redistribution permission for selected sample forms and any added fonts before public release.
