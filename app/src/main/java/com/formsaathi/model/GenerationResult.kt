package com.formsaathi.model

/**
 * Warning emitted when answer text could not be cleanly fitted within its designated answer box.
 * Collected during PDF generation and surfaced to the user on the Completed screen.
 */
data class TextFitWarning(
    val fieldId: String,
    val fieldLabel: String,
    val reason: String
)

/**
 * Result of PDF generation, carrying both success state and any non-fatal warnings.
 */
data class GenerationResult(
    val warnings: List<TextFitWarning> = emptyList()
) {
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}
