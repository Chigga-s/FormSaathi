package com.formsaathi.core

import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.ParsedForm
import com.formsaathi.model.SupportedLanguage

/**
 * Manages the in-memory session data for an active form filling session.
 * Tracks answers keyed by fieldId, current position, navigation history, and language.
 */
class FormSession(
    val parsedForm: ParsedForm,
    var language: SupportedLanguage
) {
    private val _answers = mutableMapOf<String, FormAnswer>()
    val answers: Map<String, FormAnswer> get() = _answers.toMap()

    private val history = mutableListOf<Int>()

    var currentFieldIndex: Int = 0
        private set

    /**
     * True while the user is correcting one answer reached from the review screen.
     * Answering it returns them to review instead of marching them through every
     * remaining question again.
     */
    var editingFromReview: Boolean = false
        private set

    init {
        if (parsedForm.fields.isNotEmpty()) {
            history.add(0)
        }
    }

    /**
     * Retrieves the current field being presented to the user.
     */
    fun getCurrentField(): FormField? {
        val fields = parsedForm.fields
        return if (currentFieldIndex in fields.indices) fields[currentFieldIndex] else null
    }

    /**
     * Retrieves the current user answer for the active field if one exists.
     */
    fun getCurrentAnswer(): FormAnswer? {
        val current = getCurrentField() ?: return null
        return _answers[current.id]
    }

    /**
     * Stores or updates an answer for a specific field.
     */
    fun setAnswer(answer: FormAnswer) {
        _answers[answer.fieldId] = answer
    }

    /**
     * Removes an answer if user clears or undoes it.
     */
    fun removeAnswer(fieldId: String) {
        _answers.remove(fieldId)
    }

    /**
     * Determines whether the user can navigate back in the current question sequence.
     */
    fun canGoBack(): Boolean {
        return history.size > 1
    }

    /**
     * Navigates back to the previously visited field index.
     * Returns true if navigated back, or false if already at the first step.
     */
    fun goBack(): Boolean {
        if (history.size <= 1) return false
        history.removeAt(history.lastIndex) // Pop current
        currentFieldIndex = history.last()
        return true
    }

    /**
     * Advances to the next eligible question index according to conversation engine rules.
     * Returns true if another question is available, or false if the question flow is completed.
     */
    fun advance(conversationEngine: ConversationEngine): Boolean {
        val nextIndex = conversationEngine.getNextFieldIndex(
            fromIndex = currentFieldIndex,
            fields = parsedForm.fields,
            currentAnswers = _answers
        )

        return if (nextIndex != -1) {
            currentFieldIndex = nextIndex
            history.add(nextIndex)
            true
        } else {
            false
        }
    }

    /**
     * Skips the current question if allowed and advances to the next eligible question.
     */
    fun skip(conversationEngine: ConversationEngine): Boolean {
        // If there was an existing answer, remove it because user chose to skip
        getCurrentField()?.let { _answers.remove(it.id) }
        return advance(conversationEngine)
    }

    /**
     * Jumps directly to a field (used when user edits an answer from the Review screen).
     */
    fun jumpToField(index: Int, fromReview: Boolean = false) {
        if (index in parsedForm.fields.indices) {
            currentFieldIndex = index
            history.add(index)
            editingFromReview = fromReview
        }
    }

    /** Clears the review-edit flag once the user has returned to review. */
    fun clearReviewEdit() {
        editingFromReview = false
    }

    /**
     * Returns total count of eligible questions considering current skips.
     */
    fun getActiveQuestionsCount(conversationEngine: ConversationEngine): Int {
        return parsedForm.fields.count { !conversationEngine.shouldSkipField(it, parsedForm.fields, _answers) }
    }

    /**
     * Returns the 0-based position of the current field among active (non-skipped) fields.
     * Used for accurate "Question X of Y" display when conditional rules skip fields.
     */
    fun getActiveQuestionPosition(conversationEngine: ConversationEngine): Int {
        var position = 0
        for (i in parsedForm.fields.indices) {
            if (i == currentFieldIndex) return position
            if (!conversationEngine.shouldSkipField(parsedForm.fields[i], parsedForm.fields, _answers)) {
                position++
            }
        }
        return position
    }
}

