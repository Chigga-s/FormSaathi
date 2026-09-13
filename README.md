# FormSaathi

Offline-first Android prototype that turns a difficult government PDF form into a
simple question flow in English, Hindi or Marathi, then writes the answers back
onto the original pages and exports a flattened `Completed_Form.pdf`.

Everything — rendering, OCR, field detection, answer normalization, speech
recognition and PDF generation — runs on the device. The installed APK holds one
permission, `RECORD_AUDIO`, and no network permission at all. See `PLAN(1).md`
for the original scope.

## Supported forms

FormSaathi is a prototype tuned for two layout families, both shipped in
`samples/forms/` and bundled in the app under `assets/samples/`:

| Demo form | Layout | What it exercises |
| --- | --- | --- |
| `FormSaathi_Test_Form_1_Simple.pdf` — Citizen Services Application Form | Label on the left, printed underline on the right | Underline snapping, a documents-to-attach checklist, 15 detected fields |
| `FormSaathi_Test_Form_2_Boxed.pdf` — General Application Form | Label on the left, printed rectangular answer box on the right | Printed-box detection, a tall multi-line address box, a signature line in the footer, 9 detected fields |

Both are synthetic and carry fictional data; neither is an official government
document. `scripts/make_test_form_2.py` regenerates the second one.

Layout handling is geometric, not per-file: the parser detects printed underlines
and printed boxes from the rendered page, so other forms built from the same two
idioms have a reasonable chance of working. There are no filename or page-size
special cases. Arbitrary government forms are explicitly **not** supported — a
form whose fields FormSaathi cannot place safely yields a manual-review warning
instead of ink in the wrong place.

## How it works

```text
PDF (SAF picker or bundled demo)
  -> PdfRenderer            one page at a time, longest side capped at 1800 px
  -> printed geometry       underlines and answer boxes from a luminance scan
  -> ML Kit OCR             bundled Latin recognizer, line boxes normalized to 0..1
  -> field mapping          alias -> keyword -> token overlap -> bounded edit distance
  -> answer-area resolution printed box > underline > blank space, never over a label
  -> question flow          one question at a time, typed or spoken
  -> normalize + validate    deterministic rules, invalid input is kept for correction
  -> review and edit
  -> PdfDocument            original page as background, answers fitted and clipped
  -> Completed_Form.pdf     saved through ACTION_CREATE_DOCUMENT, opened or shared
```

An answer rectangle is only accepted if it sits inside real printed geometry and
collides with no label, no neighbouring text and no other field's answer area.
Anything else becomes a warning on the review screen rather than a guess.

## Build and run

Requirements: JDK 17, Android SDK 34. The NDK is optional — see **Offline voice**.

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. **This is a debug
APK.** No release signing config exists in this repository, so there is no signed
release build to hand out; `assembleRelease` would produce an unsigned artifact.

To use the app: choose a language, then either pick a PDF with **Select a form**
or tap one of the two bundled demo forms. Both paths run the identical real
pipeline. Answer each question by typing or by holding a short voice answer,
check the review screen, then choose where to save the completed PDF.

## Offline voice

Voice input needs two things that are deliberately **not** committed:

1. `whisper.cpp` checked out at `app/src/main/cpp/whisper.cpp`
2. the quantized multilingual Tiny model at
   `app/src/main/assets/models/whisper-tiny-multilingual-q5.bin`

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup-voice.ps1 -DownloadModel
.\gradlew.bat :app:assembleDebug
```

The script pins whisper.cpp v1.7.6 at commit
`a8d002cfd879315632a579e73f0148d06959de36` and verifies the model against SHA-256
`818710568DA3CA15689E31A743197B520007872FF9576237BDA97BD1B469C3D7`. Without the
NDK installed, the Gradle build skips the native block entirely and produces a
working APK with no voice: the app then reports "Offline voice input is not
included in this build. Please type your answer." Typed input is a complete path
through every screen and is never gated on voice.

Recording format is mono 16 kHz PCM16 little-endian, capped at 10 seconds, written
to the cache and deleted after transcription or cancellation.

### Known voice limitations

- **A blank transcript is never written into the form.** An empty result or
  whisper's `[BLANK_AUDIO]` token becomes "No speech detected — try again or type
  your answer."
- **A silenced microphone is detected and reported.** Android hands a blocked app
  correctly sized buffers of pure silence with no error, which is the usual cause
  of "it recorded but the transcript was empty". FormSaathi checks
  `AudioRecordingConfiguration.isClientSilenced` and the captured peak amplitude,
  and tells the user whether a call or another app holds the microphone, or
  whether the phone is blocking microphone access for this app.
- Marathi transcription with the Tiny model is weak. Treat Marathi voice as
  best-effort and demonstrate Hindi voice; Marathi typed input is fully supported.
- Transcription quality has not been measured on a device in this repository's
  test runs (see **Verification** for exactly what was and was not proven).

## Verification

```powershell
.\gradlew.bat :app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

The instrumented suite needs a connected device. Of note:

- `SupportedTemplatesAcceptanceTest` parses both demo forms with the real OCR
  pipeline, asserts the placement invariants on the real detected geometry,
  generates both completed PDFs, and writes a PNG of every page plus a field
  report to `Downloads/FormSaathiAcceptance` for visual review.
- `RealSessionFlowTest` drives the real coordinator from import to generated PDF,
  and checks that cancelling and a corrupt file both leave Processing.
- `AudioCapturePathTest` exercises the real microphone path. Gradle's install
  cannot grant `RECORD_AUDIO` on every OEM build; when the grant fails these
  tests **skip** rather than pass. To run them for real:

  ```powershell
  .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
  adb install -r -g app\build\outputs\apk\debug\app-debug.apk
  adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
  adb shell am instrument -w -e class com.formsaathi.voice.AudioCapturePathTest com.formsaathi.test/androidx.test.runner.AndroidJUnitRunner
  ```

- `MicrophoneDiagnosticsTest` prints the captured peak level for every audio
  source. Run it when voice comes back empty; a peak of 0 everywhere means the
  platform is silencing the app, not that the model failed.
- Tests that need whisper.cpp skip themselves when the native library is absent
  and say so. A synthetic WAV passing through the bridge is not evidence that
  real spoken input works.

### Verifying the privacy claim

```powershell
adb shell dumpsys package com.formsaathi | findstr permission
```

`RECORD_AUDIO` should be the only entry. ML Kit's transitive dependency on
Google's datatransport library merges `INTERNET` and `ACCESS_NETWORK_STATE` into
the manifest; both are stripped in `AndroidManifest.xml` with `tools:node="remove"`,
so the installed APK cannot open a socket. The only thing that removal disables is
Google's usage telemetry upload.

## Repository layout

```text
app/src/main/java/com/formsaathi/
├── UI/            Compose screens and navigation
├── core/          session, conversation rules, coordinator, launcher, view model
├── formengine/    PDF rendering, printed-geometry detection, OCR, field mapping
├── language/      JSON-backed multilingual questions
├── answer/        normalization and validation
├── voice/         AudioRecord capture, JNI bridge, Whisper manager
├── pdf/           page composition, text fitting, export
└── contracts/     service interfaces and the fakes used by tests
samples/forms/     the two demo PDFs
samples/expected/  reference renders for the first demo form
scripts/           voice setup, demo-form generator
```

Fakes live in `contracts/FakeEngines.kt`. They back unit tests and the debug-only
developer harness; nothing reachable from the normal user flow uses them.

## Known limitations

- Two demo layout families only. Other forms may parse partially or not at all.
- OCR uses the Latin recognizer. A Devanagari recognizer is wired up but not
  selected automatically, so Hindi/Marathi **labels** on a source form are not yet
  read. Questions, answers and the UI are fully multilingual.
- Answers are drawn with a Latin sans-serif face. Devanagari answer text in the
  generated PDF has not been verified and may show missing glyphs.
- Session state lives in memory. It survives rotation and navigation but not
  process death.
- Aadhaar validation checks for twelve digits. It does not verify an Aadhaar
  number against anything.
- No release signing configuration, so no signed release APK.

## Third-party notices

whisper.cpp and ggml licenses ship inside the source checkout that
`scripts/setup-voice.ps1` creates. The Tiny model is distributed through
`ggerganov/whisper.cpp` on Hugging Face and derives from OpenAI Whisper; review
its licensing before redistributing the model with an APK. ML Kit is subject to
Google's SDK terms. The sample forms in this repository are synthetic and written
for this project.
