package com.formsaathi.answer

import org.junit.Assert.assertEquals
import org.junit.Test

class DateNormalizerTest {

    @Test
    fun testIsoFormat() {
        assertEquals("2026-09-12", DateNormalizer.normalize("2026-09-12"))
    }

    @Test
    fun testCommonFormats() {
        assertEquals("2026-09-12", DateNormalizer.normalize("12/09/2026"))
        assertEquals("2026-09-12", DateNormalizer.normalize("12-09-2026"))
    }

    @Test
    fun testTextFormats() {
        assertEquals("2026-09-12", DateNormalizer.normalize("12 September 2026"))
        assertEquals("2026-09-12", DateNormalizer.normalize("12 Sept 2026"))
        assertEquals("2026-09-12", DateNormalizer.normalize("12th Sept 2026"))
        assertEquals("2026-09-12", DateNormalizer.normalize("September 12 2026"))
    }
}
