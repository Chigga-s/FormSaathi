package com.formsaathi.answer

import com.formsaathi.contracts.AnswerProcessor
import com.formsaathi.model.FieldType
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.model.ValidationResult

class RuleBasedAnswerProcessor : AnswerProcessor {

    private val fieldValidator = FieldValidator()

    override fun normalize(
        fieldType: FieldType,
        rawText: String,
        language: SupportedLanguage
    ): String {
        val trimmed = rawText.trim()
        
        return when (fieldType) {
            FieldType.MOBILE, FieldType.PINCODE, FieldType.AADHAAR -> {
                // Keep only numeric digits
                trimmed.replace(Regex("[^0-9]"), "")
            }
            FieldType.ANNUAL_INCOME -> {
                NumberNormalizer.normalize(trimmed)
            }
            FieldType.DATE_OF_BIRTH -> {
                DateNormalizer.normalize(trimmed)
            }
            FieldType.SAME_AS_PERMANENT_ADDRESS -> {
                val lower = trimmed.lowercase()
                // Simple yes/no matching for common English/Hindi/Marathi words
                if (lower in listOf("yes", "y", "true", "haan", "ha", "ho", "hoy")) "yes" else "no"
            }
            FieldType.GENDER -> {
                val lower = trimmed.lowercase()
                when {
                    lower.startsWith("m") || lower.contains("male") -> "Male"
                    lower.startsWith("f") || lower.contains("female") -> "Female"
                    else -> "Other"
                }
            }
            else -> trimmed
        }
    }

    override fun validate(fieldType: FieldType, value: String): ValidationResult {
        return fieldValidator.validate(fieldType, value)
    }
}
