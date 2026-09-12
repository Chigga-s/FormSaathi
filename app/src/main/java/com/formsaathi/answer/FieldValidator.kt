package com.formsaathi.answer

import com.formsaathi.model.FieldType
import com.formsaathi.model.ValidationResult
import java.time.LocalDate
import java.time.format.DateTimeParseException

class FieldValidator {

    fun validate(fieldType: FieldType, value: String): ValidationResult {
        val trimmed = value.trim()
        
        return when (fieldType) {
            FieldType.MOBILE -> {
                if (trimmed.matches(Regex("[0-9]{10}"))) ValidationResult.Valid
                else ValidationResult.Invalid("Mobile number must be exactly 10 digits")
            }
            FieldType.PINCODE -> {
                if (trimmed.matches(Regex("[0-9]{6}"))) ValidationResult.Valid
                else ValidationResult.Invalid("PIN code must be exactly 6 digits")
            }
            FieldType.AADHAAR -> {
                if (trimmed.matches(Regex("[0-9]{12}"))) ValidationResult.Valid
                else ValidationResult.Invalid("Aadhaar number must be exactly 12 digits")
            }
            FieldType.PHOTO, FieldType.SIGNATURE -> {
                if (trimmed.isNotEmpty()) ValidationResult.Valid
                else ValidationResult.Invalid("Please attach a photo to continue")
            }
            FieldType.SAME_AS_PERMANENT_ADDRESS -> {
                if (trimmed == "yes" || trimmed == "no") ValidationResult.Valid
                else ValidationResult.Invalid("Please answer yes or no")
            }
            FieldType.EMAIL -> {
                if (trimmed.isEmpty()) ValidationResult.Valid
                else if (android.util.Patterns.EMAIL_ADDRESS?.matcher(trimmed)?.matches() == true ||
                    trimmed.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"))
                ) ValidationResult.Valid
                else ValidationResult.Invalid("Please enter a valid email address")
            }
            FieldType.ANNUAL_INCOME -> {
                val income = trimmed.toLongOrNull()
                if (income != null && income >= 0) ValidationResult.Valid
                else ValidationResult.Invalid("Annual income must be a valid non-negative number")
            }
            FieldType.DATE_OF_BIRTH -> {
                try {
                    val date = LocalDate.parse(trimmed, DateNormalizer.canonicalFormat)
                    if (!date.isAfter(LocalDate.now())) ValidationResult.Valid
                    else ValidationResult.Invalid("Date of birth cannot be in the future")
                } catch (_: DateTimeParseException) {
                    ValidationResult.Invalid("Invalid date. Use DD/MM/YYYY format")
                }
            }
            FieldType.FULL_NAME, FieldType.FATHER_NAME -> {
                if (trimmed.isNotBlank()) ValidationResult.Valid
                else ValidationResult.Invalid("This field cannot be blank")
            }
            else -> ValidationResult.Valid
        }
    }
}
