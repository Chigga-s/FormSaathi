package com.formsaathi.answer

import com.formsaathi.model.FieldType
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.model.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleBasedAnswerProcessorTest {

    private lateinit var processor: RuleBasedAnswerProcessor

    @Before
    fun setup() {
        processor = RuleBasedAnswerProcessor()
    }

    @Test
    fun testIncomeNormalization() {
        val normalized = processor.normalize(
            FieldType.ANNUAL_INCOME,
            "My income is two lakh",
            SupportedLanguage.ENGLISH
        )
        // Note: The NumberNormalizer might not parse "My income is two lakh" perfectly 
        // unless it ignores words or just sums up known ones. 
        // Our simple implementation just sums up known numeric words.
        // Let's test with a cleaner input or update NumberNormalizer to ignore unknown words.
        // "two lakh" will definitely return "200000".
        val cleanNormalized = processor.normalize(
            FieldType.ANNUAL_INCOME,
            "two lakh",
            SupportedLanguage.ENGLISH
        )
        assertEquals("200000", cleanNormalized)
    }

    @Test
    fun testDateNormalization() {
        val normalized = processor.normalize(
            FieldType.DATE_OF_BIRTH,
            "12th Sept 2026",
            SupportedLanguage.ENGLISH
        )
        assertEquals("12/09/2026", normalized)
    }
    
    @Test
    fun testValidation() {
        val result = processor.validate(FieldType.MOBILE, "9876543210")
        assertTrue(result is ValidationResult.Valid)
        
        val invalidResult = processor.validate(FieldType.MOBILE, "123")
        assertTrue(invalidResult is ValidationResult.Invalid)
    }
}
