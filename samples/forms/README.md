# Demo forms

The two PDFs FormSaathi is tuned for. Both are synthetic, carry fictional data,
and are not official government documents.

| File | Layout family |
| --- | --- |
| `FormSaathi_Test_Form_1_Simple.pdf` | Labels on the left, printed underlines on the right |
| `FormSaathi_Test_Form_2_Boxed.pdf` | Labels on the left, printed rectangular answer boxes on the right |

The same two files are bundled in `app/src/main/assets/samples/` so the app can
offer them without a file picker, and in `app/src/androidTest/assets/debugforms/`
for the instrumented acceptance tests. Regenerate the boxed form with
`python scripts/make_test_form_2.py`; keep all three copies in step.
