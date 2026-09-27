package com.formsaathi.pdf

import com.formsaathi.model.FieldType
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect
import com.formsaathi.model.TextFitWarning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PdfPageComposerTest {

    private lateinit var composer: PdfPageComposer

    @Before
    fun setUp() {
        composer = PdfPageComposer()
    }

    @Test
    fun testToCanvasCoordinatesMath() {
        val normBox = NormalizedRect(
            left = 0.10f,
            top = 0.20f,
            right = 0.60f,
            bottom = 0.40f
        )
        val pageWidth = 600f
        val pageHeight = 800f

        val canvasRect = composer.toCanvasCoordinates(normBox, pageWidth, pageHeight)

        assertEquals(60f, canvasRect.left, 0.001f)
        assertEquals(160f, canvasRect.top, 0.001f)
        assertEquals(360f, canvasRect.right, 0.001f)
        assertEquals(320f, canvasRect.bottom, 0.001f)
        assertEquals(300f, canvasRect.width, 0.001f)
        assertEquals(160f, canvasRect.height, 0.001f)
    }

    @Test
    fun testIsMultiLineField() {
        val permAddrField = FormField(
            id = "f1",
            sourceLabel = "Permanent Address",
            type = FieldType.PERMANENT_ADDRESS,
            pageIndex = 0,
            labelBox = NormalizedRect(0f, 0f, 0.5f, 0.1f),
            answerBox = NormalizedRect(0f, 0.1f, 0.8f, 0.25f)
        )
        assertTrue(composer.isMultiLineField(permAddrField))

        val singleLineField = FormField(
            id = "f2",
            sourceLabel = "Mobile",
            type = FieldType.MOBILE,
            pageIndex = 0,
            labelBox = NormalizedRect(0f, 0f, 0.2f, 0.05f),
            answerBox = NormalizedRect(0.25f, 0f, 0.7f, 0.05f) // height=0.05, width=0.45, ratio=0.11
        )
        assertFalse(composer.isMultiLineField(singleLineField))
    }

    @Test
    fun testCanvasRectDimensions() {
        // Pure Kotlin test — no Android RectF dependency
        val canvasRect = CanvasRect(10f, 20f, 300f, 100f)

        assertEquals(10f, canvasRect.left, 0.001f)
        assertEquals(20f, canvasRect.top, 0.001f)
        assertEquals(300f, canvasRect.right, 0.001f)
        assertEquals(100f, canvasRect.bottom, 0.001f)
        assertEquals(290f, canvasRect.width, 0.001f)
        assertEquals(80f, canvasRect.height, 0.001f)
    }

    @Test
    fun testTextFitWarningStructure() {
        // Verify TextFitWarning data class works correctly (pure Kotlin, JVM-safe)
        val warning = TextFitWarning(
            fieldId = "field_address",
            fieldLabel = "Permanent Address / स्थायी पता",
            reason = "Answer text was clipped to fit within the answer box at minimum font size"
        )
        assertEquals("field_address", warning.fieldId)
        assertEquals("Permanent Address / स्थायी पता", warning.fieldLabel)
        assertTrue(warning.reason.contains("clipped"))
    }
}

