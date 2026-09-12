package com.formsaathi.answer

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberNormalizerTest {

    @Test
    fun testIndianNumberWords() {
        assertEquals("200000", NumberNormalizer.normalize("two lakh"))
        assertEquals("150000", NumberNormalizer.normalize("1.5 lakh"))
        assertEquals("50000", NumberNormalizer.normalize("fifty thousand"))
        assertEquals("10000000", NumberNormalizer.normalize("one crore"))
    }

    @Test
    fun testCurrencySymbols() {
        assertEquals("50000", NumberNormalizer.normalize("₹50000"))
        assertEquals("50000", NumberNormalizer.normalize("Rs. 50000"))
        assertEquals("50000", NumberNormalizer.normalize("50000 rupees"))
        assertEquals("50000", NumberNormalizer.normalize("50,000"))
    }

    @Test
    fun testMixedInput() {
        assertEquals("250000", NumberNormalizer.normalize("two lakh fifty thousand"))
        assertEquals("1200", NumberNormalizer.normalize("twelve hundred"))
    }
    
    @Test
    fun testPlainDigits() {
        assertEquals("200000", NumberNormalizer.normalize("200000"))
    }
}
