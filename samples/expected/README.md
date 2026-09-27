# Expected Form Parsed Data Directory

Store expected parsed JSON structures for your sample demo forms here.
Role 2 and Role 4 use these to verify that OCR bounding boxes and field aliases match expected layouts.

## Test Form 1 (`../forms/FormSaathi_Test_Form_1_Simple.pdf`)

Synthetic single-column form used for OCR/parser validation.

- `form1_expected.json` — expected field types in order, expected
  documents, exact field count (15) and warning count (0).
- `form1_render.png` — what the parser feeds ML Kit (opaque white
  base; transparent renders starve OCR).
- `form1_overlay.png` — debug overlay: red label boxes, blue answer
  boxes snapped onto the printed underlines, magenta type/confidence.
- `form1_completed_sample.png` — flattened output filled with fictional
  answers through the real engines (see
  `FormEndToEndPlacementTest`).

Regenerate the images on-device with `FormParseDiagnosticsTest`
(published to Downloads/FormSaathiDiag) and copy verified versions here.
