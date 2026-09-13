package com.formsaathi.core

import android.net.Uri
import com.formsaathi.contracts.ParseStage
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.RequiredDocument
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.model.TextFitWarning

/**
 * Immutable UI state emitted to the Jetpack Compose layer by FormSaathiCoordinator.
 */
sealed interface FormUiState {
    /**
     * Initial idle state before a document is selected.
     */
    data object Idle : FormUiState

    /**
     * Processing state during PDF rendering and OCR extraction. Carries the real
     * stage and page position reported by the parser so the screen never shows a
     * stage the engine has not actually reached.
     */
    data class Parsing(
        val stage: ParseStage = ParseStage.RENDERING,
        val pageIndex: Int = 0,
        val pageCount: Int = 0
    ) : FormUiState

    /**
     * Active state while the user is answering conversational questions.
     */
    data class Questioning(
        val currentField: FormField,
        val questionText: String,
        val currentAnswer: FormAnswer?,
        val questionIndex: Int,
        val totalQuestions: Int,
        val canGoBack: Boolean,
        val canSkip: Boolean,
        val validationError: String? = null,
        val isTranscribing: Boolean = false,
        val language: SupportedLanguage
    ) : FormUiState {
        val progressPercent: Float
            get() = if (totalQuestions > 0) (questionIndex + 1).toFloat() / totalQuestions.toFloat() else 0f
    }

    /**
     * Review screen state displaying all detected fields, user answers, and required documents.
     */
    data class Reviewing(
        val answers: Map<String, FormAnswer>,
        val fields: List<FormField>,
        val documents: List<RequiredDocument>,
        val language: SupportedLanguage,
        val isGenerating: Boolean = false,
        /** Fields the parser could not place confidently; shown as manual-review notes. */
        val warnings: List<String> = emptyList()
    ) : FormUiState

    /**
     * Completed state after the flattened PDF is generated on device.
     */
    data class Completed(
        val outputUri: Uri,
        val filename: String,
        val warnings: List<TextFitWarning> = emptyList()
    ) : FormUiState

    /**
     * Error state for recoverable or fatal failures.
     */
    data class Error(
        val message: String,
        val recoverable: Boolean = true
    ) : FormUiState
}
