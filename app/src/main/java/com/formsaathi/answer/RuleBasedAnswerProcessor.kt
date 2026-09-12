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
                NumberNormalizer.normalizeDigits(trimmed)
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
                when (lower) {
                    "yes", "y", "true", "haan", "ha", "ho", "hoy", "हाँ", "हां", "हो", "होय" -> "yes"
                    "no", "n", "false", "नहीं", "नही", "ना", "नाही", "nahi" -> "no"
                    else -> trimmed
                }
            }
            FieldType.GENDER -> {
                val lower = trimmed.lowercase()
                when (lower) {
                    "m", "male", "पुरुष" -> "Male"
                    "f", "female", "महिला", "स्त्री" -> "Female"
                    "other", "अन्य", "इतर" -> "Other"
                    else -> trimmed
                }
            }
            FieldType.CATEGORY -> when (trimmed.lowercase()) {
                "general", "open", "सामान्य", "जनरल", "खुला" -> "General"
                "obc", "ओबीसी" -> "OBC"
                "sc", "एससी" -> "SC"
                "st", "एसटी" -> "ST"
                else -> trimmed
            }
            else -> trimmed
        }
    }

    override fun validate(fieldType: FieldType, value: String): ValidationResult {
        return fieldValidator.validate(fieldType, value)
    }
}
