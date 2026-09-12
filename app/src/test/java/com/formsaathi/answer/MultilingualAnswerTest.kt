package com.formsaathi.answer

import com.formsaathi.model.FieldType
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.model.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultilingualAnswerTest {
    private val processor = RuleBasedAnswerProcessor()

    @Test fun femaleIsNotMale() {
        assertEquals("Female", processor.normalize(FieldType.GENDER, "female", SupportedLanguage.ENGLISH))
    }

    @Test fun nativeScriptAffirmativesAreRecognized() {
        assertEquals("yes", processor.normalize(FieldType.SAME_AS_PERMANENT_ADDRESS, "हाँ", SupportedLanguage.HINDI))
        assertEquals("yes", processor.normalize(FieldType.SAME_AS_PERMANENT_ADDRESS, "होय", SupportedLanguage.MARATHI))
    }

    @Test fun unknownBooleanIsNotInvented() {
        val value = processor.normalize(FieldType.SAME_AS_PERMANENT_ADDRESS, "maybe", SupportedLanguage.ENGLISH)
        assertEquals("maybe", value)
        assertTrue(processor.validate(FieldType.SAME_AS_PERMANENT_ADDRESS, value) is ValidationResult.Invalid)
    }

    @Test fun devanagariDigitsArePreserved() {
        assertEquals("411001", processor.normalize(FieldType.PINCODE, "४११००१", SupportedLanguage.MARATHI))
    }

    @Test fun spokenDigitsRetainOrderAndLeadingZero() {
        assertEquals("041001", processor.normalize(FieldType.PINCODE, "zero four one zero zero one", SupportedLanguage.ENGLISH))
    }

    @Test fun negativeIncomeCannotBecomePositive() {
        val value = processor.normalize(FieldType.ANNUAL_INCOME, "-500", SupportedLanguage.ENGLISH)
        assertEquals("-500", value)
        assertTrue(processor.validate(FieldType.ANNUAL_INCOME, value) is ValidationResult.Invalid)
    }

    @Test fun unknownIncomeWordsAreNotDiscarded() {
        assertEquals("two cats", NumberNormalizer.normalize("two cats"))
    }

    @Test fun multilingualIncomeUsesIndianScales() {
        assertEquals("250000", NumberNormalizer.normalize("दो लाख पचास हजार"))
        assertEquals("250000", NumberNormalizer.normalize("दोन लाख पन्नास हजार"))
        assertEquals("0", NumberNormalizer.normalize("शून्य"))
    }

    @Test fun photoRequiresAnAttachment() {
        assertEquals("/photos/a.jpg", processor.normalize(FieldType.PHOTO, "/photos/a.jpg", SupportedLanguage.ENGLISH))
        assertTrue(processor.validate(FieldType.PHOTO, "/photos/a.jpg") is ValidationResult.Valid)
        assertTrue(processor.validate(FieldType.PHOTO, "  ") is ValidationResult.Invalid)
    }

    @Test fun identifiersRejectEmbeddedLetters() {
        assertTrue(processor.validate(FieldType.MOBILE, "abc9876543210") is ValidationResult.Invalid)
    }

    @Test fun datesUseThePlannedDayFirstFormat() {
        assertEquals("12/05/2005", DateNormalizer.normalize("12 May 2005"))
        assertEquals("12/05/2005", DateNormalizer.normalize("2005-05-12"))
        assertTrue(processor.validate(FieldType.DATE_OF_BIRTH, "29/02/2004") is ValidationResult.Valid)
    }

    @Test fun invalidAndPartialDatesAreRejected() {
        listOf("29/02/2005", "12/05/2005 garbage", "01/01/2999").forEach {
            assertTrue(processor.validate(FieldType.DATE_OF_BIRTH, it) is ValidationResult.Invalid)
        }
        assertEquals("12 May 2005 garbage", DateNormalizer.normalize("12 May 2005 garbage"))
    }

    @Test fun categoriesUseCanonicalLabels() {
        assertEquals("General", processor.normalize(FieldType.CATEGORY, "खुला", SupportedLanguage.MARATHI))
        assertEquals("OBC", processor.normalize(FieldType.CATEGORY, "ओबीसी", SupportedLanguage.HINDI))
    }
}
