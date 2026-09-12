package com.formsaathi.model

/**
 * Supported languages in FormSaathi for multilingual conversational flow.
 */
enum class SupportedLanguage(val code: String) {
    ENGLISH("en"),
    HINDI("hi"),
    MARATHI("mr")
}

/**
 * Standard canonical field types detected on Indian government forms.
 */
enum class FieldType {
    FULL_NAME,
    FATHER_NAME,
    MOTHER_NAME,
    DATE_OF_BIRTH,
    GENDER,
    MOBILE,
    EMAIL,
    AADHAAR,
    PERMANENT_ADDRESS,
    CURRENT_ADDRESS,
    SAME_AS_PERMANENT_ADDRESS,
    STATE,
    DISTRICT,
    PINCODE,
    CATEGORY,
    ANNUAL_INCOME,
    PHOTO,
    SIGNATURE,
    UNKNOWN
}

/**
 * Metadata for a rendered page of the PDF.
 */
data class PageInfo(
    val pageIndex: Int,
    val pdfWidthPoints: Float,
    val pdfHeightPoints: Float,
    val renderedWidthPx: Int,
    val renderedHeightPx: Int
)

/**
 * Coordinate rectangle normalized to a 0.0 to 1.0 coordinate system relative to the rendered page.
 * Formula:
 *   normalizedX = pixelX / renderedPageWidth
 *   normalizedY = pixelY / renderedPageHeight
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(left <= right) { "left ($left) must be <= right ($right)" }
        require(top <= bottom) { "top ($top) must be <= bottom ($bottom)" }
    }

    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Represents a single detected form field on a document page.
 */
data class FormField(
    val id: String,
    val sourceLabel: String,
    val type: FieldType,
    val pageIndex: Int,
    val labelBox: NormalizedRect,
    val answerBox: NormalizedRect,
    val required: Boolean = false,
    val confidence: Float = 0f
)

/**
 * Represents the user's answer to a field, including the raw and normalized values.
 */
data class FormAnswer(
    val fieldId: String,
    val rawValue: String,
    val normalizedValue: String,
    val source: AnswerSource
)

/**
 * Source from which the answer was obtained.
 */
enum class AnswerSource {
    TYPED,
    VOICE,
    COPIED_BY_RULE
}

/**
 * Document requirement extracted from the form (e.g. Aadhaar Card copy, Income Certificate).
 */
data class RequiredDocument(
    val name: String,
    val requirement: String? = null
)

/**
 * Complete parsed structure of a government PDF form.
 */
data class ParsedForm(
    val pages: List<PageInfo>,
    val fields: List<FormField>,
    val documents: List<RequiredDocument>,
    val warnings: List<String> = emptyList()
)

/**
 * Validation result for an answer value.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}
