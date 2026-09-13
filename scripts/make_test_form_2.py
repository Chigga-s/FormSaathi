"""Regenerates samples/forms/FormSaathi_Test_Form_2_Boxed.pdf.

Test Form 2 is the second supported demo layout family: labels on the left with
printed rectangular answer boxes on the right. It exists so the boxed-layout
placement path has a real, redistributable source PDF to be tested against.
Geometry mirrors the layout FormSaathi was reported to mis-fill.

Usage:  python scripts/make_test_form_2.py
"""
from reportlab.lib.pagesizes import A4
from reportlab.lib.colors import Color, HexColor
from reportlab.pdfgen import canvas
import os

W, H = A4  # 595.28 x 841.89 pt

NAVY = HexColor("#1F4E79")
DARK = HexColor("#333333")
BAND = HexColor("#F1F5F9")
BOX_FILL = HexColor("#F8FAFC")
BOX_EDGE = HexColor("#A0AEC0")
GREY = HexColor("#555555")

L = 0.0717 * W          # left margin
R = 0.9300 * W          # right edge
BOX_L = 0.3717 * W      # answer box left edge


def y(frac):
    """Top-origin fraction -> PDF bottom-origin points."""
    return H - frac * H


def band(c, top, bottom, title):
    c.setFillColor(BAND)
    c.rect(L, y(bottom), R - L, (bottom - top) * H, stroke=0, fill=1)
    c.setFillColor(NAVY)
    c.rect(L, y(bottom), 4, (bottom - top) * H, stroke=0, fill=1)
    c.setFont("Helvetica-Bold", 13)
    c.drawString(L + 14, y(bottom) + (bottom - top) * H / 2 - 4.5, title)


def box(c, top, bottom):
    c.setFillColor(BOX_FILL)
    c.setStrokeColor(BOX_EDGE)
    c.setLineWidth(1)
    c.roundRect(BOX_L, y(bottom), R - BOX_L, (bottom - top) * H, 4, stroke=1, fill=1)


def label(c, top, bottom, text):
    c.setFillColor(DARK)
    c.setFont("Helvetica-Bold", 11)
    c.drawString(L, y(bottom) + (bottom - top) * H / 2 - 4, text)


ROWS = [
    ("Full Name", 0.2286, 0.2610),
    ("Father's Name", 0.2708, 0.3026),
    ("Date of Birth (DD/MM/YYYY)", 0.3123, 0.3447),
    ("Mobile Number", 0.3545, 0.3863),
    ("Category", 0.3961, 0.4285),
    ("Annual Income", 0.4383, 0.4700),
    ("Residential Address", 0.4798, 0.5520),
]

DECLARATION = (
    "I hereby declare that all the information provided above is true and correct to the best of my "
    "knowledge and belief. I understand that in the event of any information being found false or "
    "incorrect, my application is liable to be rejected."
)


def build(path):
    c = canvas.Canvas(path, pagesize=A4)
    c.setTitle("General Application Form")

    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 24)
    c.drawCentredString(W / 2, y(0.082), "GENERAL APPLICATION FORM")
    c.setFillColor(GREY)
    c.setFont("Helvetica-Oblique", 11)
    c.drawCentredString(W / 2, y(0.115), "Please fill out all the fields in BLOCK letters")

    c.setStrokeColor(DARK)
    c.setLineWidth(3)
    c.line(L, y(0.1458), R, y(0.1458))

    band(c, 0.1693, 0.2048, "Personal Details")
    for text, top, bottom in ROWS:
        label(c, top, bottom, text)
        box(c, top, bottom)

    band(c, 0.5892, 0.6253, "Identity & Verification")
    label(c, 0.6491, 0.6803, "Aadhaar Number")
    box(c, 0.6491, 0.6803)

    c.setFillColor(DARK)
    c.setFont("Helvetica-Bold", 10)
    c.drawString(L, y(0.7350), "Declaration:")
    c.setFont("Helvetica", 10)
    words, line, lines = DECLARATION.split(), "", []
    for word in words:
        trial = (line + " " + word).strip()
        if c.stringWidth(trial, "Helvetica", 10) > (R - L - 62):
            lines.append(line)
            line = word
        else:
            line = trial
    lines.append(line)
    c.drawString(L + 62, y(0.7350), lines[0])
    for index, text in enumerate(lines[1:], start=1):
        c.drawString(L, y(0.7350 + index * 0.0185), text)

    c.setLineWidth(1)
    c.setStrokeColor(DARK)
    c.line(0.1357 * W, y(0.8897), 0.4365 * W, y(0.8897))
    c.line(0.5635 * W, y(0.8897), 0.8643 * W, y(0.8897))
    c.setFont("Helvetica-Bold", 11)
    c.drawCentredString(0.2861 * W, y(0.9110), "Date & Place")
    c.drawCentredString(0.7139 * W, y(0.9110), "Applicant's Signature")

    c.setFillColor(GREY)
    c.setFont("Helvetica-Oblique", 8)
    c.drawString(L, y(0.9650), "Synthetic test form for FormSaathi OCR/parser validation. Not an official government document.")
    c.drawRightString(R, y(0.9650), "Page 1")

    c.showPage()
    c.save()


if __name__ == "__main__":
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out = os.path.join(root, "samples", "forms", "FormSaathi_Test_Form_2_Boxed.pdf")
    build(out)
    print("wrote", out, os.path.getsize(out), "bytes")
