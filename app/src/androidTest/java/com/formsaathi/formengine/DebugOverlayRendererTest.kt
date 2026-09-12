package com.formsaathi.formengine

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugOverlayRendererTest {

    @Test
    fun draw_returnsNewBitmapWithSameDimensions() {
        val source = createWhiteBitmap()

        val renderer = DebugOverlayRenderer()

        val output = renderer.draw(
            source = source,
            fields = listOf(createField()),
            pageIndex = 0
        )

        assertNotSame(source, output)
        assertTrue(output.width == source.width)
        assertTrue(output.height == source.height)
    }

    @Test
    fun draw_placesLabelAndAnswerBoxesAtNormalizedCoordinates() {
        val source = createWhiteBitmap()

        val renderer = DebugOverlayRenderer()

        val output = renderer.draw(
            source = source,
            fields = listOf(createField()),
            pageIndex = 0
        )

        /*
         * Bitmap size:
         * 200 x 100
         *
         * labelBox:
         * left   = 0.10 -> 20 px
         * top    = 0.20 -> 20 px
         * right  = 0.30 -> 60 px
         * bottom = 0.40 -> 40 px
         *
         * answerBox:
         * left   = 0.50 -> 100 px
         * top    = 0.20 -> 20 px
         * right  = 0.90 -> 180 px
         * bottom = 0.40 -> 40 px
         */

        assertTrue(
            regionContainsNonWhitePixel(
                bitmap = output,
                left = 18,
                top = 38,
                right = 62,
                bottom = 42
            )
        )

        assertTrue(
            regionContainsNonWhitePixel(
                bitmap = output,
                left = 98,
                top = 38,
                right = 182,
                bottom = 42
            )
        )
    }

    @Test
    fun fieldFromDifferentPage_isNotDrawn() {
        val source = createWhiteBitmap()

        val renderer = DebugOverlayRenderer()

        val output = renderer.draw(
            source = source,
            fields = listOf(createField()),
            pageIndex = 1
        )

        assertTrue(source.sameAs(output))
    }

    @Test
    fun fieldOnRequestedPage_changesBitmap() {
        val source = createWhiteBitmap()

        val renderer = DebugOverlayRenderer()

        val output = renderer.draw(
            source = source,
            fields = listOf(createField()),
            pageIndex = 0
        )

        assertFalse(source.sameAs(output))
    }

    private fun createWhiteBitmap(): Bitmap {
        return Bitmap.createBitmap(
            200,
            100,
            Bitmap.Config.ARGB_8888
        ).apply {
            eraseColor(Color.WHITE)
        }
    }

    private fun createField(): FormField {
        return FormField(
            id = "p0_b0_l0",
            sourceLabel = "Full Name",
            type = FieldType.FULL_NAME,
            pageIndex = 0,
            labelBox = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.30f,
                bottom = 0.40f
            ),
            answerBox = NormalizedRect(
                left = 0.50f,
                top = 0.20f,
                right = 0.90f,
                bottom = 0.40f
            ),
            required = false,
            confidence = 1.0f
        )
    }

    private fun regionContainsNonWhitePixel(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Boolean {

        for (y in top..bottom) {
            for (x in left..right) {

                if (
                    x in 0 until bitmap.width &&
                    y in 0 until bitmap.height &&
                    bitmap.getPixel(x, y) != Color.WHITE
                ) {
                    return true
                }
            }
        }

        return false
    }
}