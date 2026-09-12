package com.formsaathi.answer

import com.formsaathi.model.FieldType
import com.formsaathi.model.ValidationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FieldValidator {

    fun validate(fieldType: FieldType, value: String): ValidationResult {
        val trimmed = value.trim()
        
        return when (fieldType) {
            FieldType.MOBILE -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 10) ValidationResult.Valid
                else ValidationResult.Invalid("Mobile number must be exactly 10 digits")
            }
            FieldType.PINCODE -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 6) ValidationResult.Valid
                else ValidationResult.Invalid("PIN code must be exactly 6 digits")
            }
            FieldType.AADHAAR -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 12) ValidationResult.Valid
                else ValidationResult.Invalid("Aadhaar number must be exactly 12 digits")
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
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                    val parsedDate = sdf.parse(trimmed)
                    if (parsedDate != null && parsedDate.before(Date())) ValidationResult.Valid
                    else ValidationResult.Invalid("Date of birth must be in YYYY-MM-DD format and cannot be in the future")
                } catch (e: Exception) {
                    ValidationResult.Invalid("Invalid date. Use YYYY-MM-DD format")
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
