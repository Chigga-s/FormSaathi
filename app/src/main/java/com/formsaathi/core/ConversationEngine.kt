package com.formsaathi.core

import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField

/**
 * Handles field progression, question sequencing, and conditional business logic rules.
 * Keeps conditional branching isolated and deterministic.
 */
class ConversationEngine {

    /**
     * Evaluates whether answering a field automatically produces side-effect answers for other fields.
     * Rule: If SAME_AS_PERMANENT_ADDRESS is answered affirmatively, copy PERMANENT_ADDRESS into CURRENT_ADDRESS.
     * If PERMANENT_ADDRESS is answered/updated and SAME_AS_PERMANENT_ADDRESS is already affirmative, sync CURRENT_ADDRESS.
     */
    fun evaluateAutomatedAnswers(
        fieldJustAnswered: FormField,
        answer: FormAnswer,
        fields: List<FormField>,
        currentAnswers: Map<String, FormAnswer>
    ): List<FormAnswer> {
        val automatedAnswers = mutableListOf<FormAnswer>()

        when (fieldJustAnswered.type) {
            FieldType.SAME_AS_PERMANENT_ADDRESS -> {
                val isAffirmative = answer.normalizedValue.equals("yes", ignoreCase = true)
                if (isAffirmative) {
                    // Find permanent address answer
                    val permAnswer = currentAnswers.values.firstOrNull { ans ->
                        fields.any { f -> f.id == ans.fieldId && f.type == FieldType.PERMANENT_ADDRESS }
                    }
                    val currentAddressField = fields.firstOrNull { it.type == FieldType.CURRENT_ADDRESS }

                    if (permAnswer != null && permAnswer.rawValue.isNotBlank() && currentAddressField != null) {
                        automatedAnswers.add(
                            FormAnswer(
                                fieldId = currentAddressField.id,
                                rawValue = permAnswer.rawValue,
                                normalizedValue = permAnswer.normalizedValue,
                                source = AnswerSource.COPIED_BY_RULE
                            )
                        )
                    }
                }
            }
            FieldType.PERMANENT_ADDRESS -> {
                val sameAsPermAnswer = currentAnswers.values.firstOrNull { ans ->
                    fields.any { f -> f.id == ans.fieldId && f.type == FieldType.SAME_AS_PERMANENT_ADDRESS }
                }
                if (sameAsPermAnswer?.normalizedValue.equals("yes", ignoreCase = true)) {
                    val currentAddressField = fields.firstOrNull { it.type == FieldType.CURRENT_ADDRESS }
                    if (answer.rawValue.isNotBlank() && currentAddressField != null) {
                        automatedAnswers.add(
                            FormAnswer(
                                fieldId = currentAddressField.id,
                                rawValue = answer.rawValue,
                                normalizedValue = answer.normalizedValue,
                                source = AnswerSource.COPIED_BY_RULE
                            )
                        )
                    }
                }
            }
            else -> Unit
        }

        return automatedAnswers
    }

    /**
     * Evaluates answers that must be removed when a condition changes.
     * Rule: If SAME_AS_PERMANENT_ADDRESS is answered negatively or cleared, any CURRENT_ADDRESS answer
     * that was created by rule (COPIED_BY_RULE) is removed so user can answer it cleanly.
     */
    fun evaluateAnswersToRemove(
        fieldJustAnswered: FormField,
        answer: FormAnswer,
        fields: List<FormField>,
        currentAnswers: Map<String, FormAnswer>
    ): List<String> {
        val toRemove = mutableListOf<String>()

        if (fieldJustAnswered.type == FieldType.SAME_AS_PERMANENT_ADDRESS) {
            val isAffirmative = answer.normalizedValue.equals("yes", ignoreCase = true)
            if (!isAffirmative) {
                val currentAddressField = fields.firstOrNull { it.type == FieldType.CURRENT_ADDRESS }
                if (currentAddressField != null) {
                    val existing = currentAnswers[currentAddressField.id]
                    if (existing?.source == AnswerSource.COPIED_BY_RULE) {
                        toRemove.add(currentAddressField.id)
                    }
                }
            }
        }

        return toRemove
    }

    /**
     * Determines if a field should be skipped in the conversational flow based on current answers.
     * Does NOT skip CURRENT_ADDRESS if a valid copied answer could not be created
     * (e.g. permanent address is missing or blank).
     */
    fun shouldSkipField(
        field: FormField,
        allFields: List<FormField>,
        currentAnswers: Map<String, FormAnswer>
    ): Boolean {
        // Skip CURRENT_ADDRESS only if SAME_AS_PERMANENT_ADDRESS is answered affirmative,
        // permanent address is available, and copied answer exists.
        if (field.type == FieldType.CURRENT_ADDRESS) {
            val sameAsPermAnswer = currentAnswers.values.firstOrNull { ans ->
                allFields.any { f -> f.id == ans.fieldId && f.type == FieldType.SAME_AS_PERMANENT_ADDRESS }
            }
            if (sameAsPermAnswer?.normalizedValue.equals("yes", ignoreCase = true)) {
                val permAnswer = currentAnswers.values.firstOrNull { ans ->
                    allFields.any { f -> f.id == ans.fieldId && f.type == FieldType.PERMANENT_ADDRESS }
                }
                val currentAnswer = currentAnswers[field.id]
                if (permAnswer != null && permAnswer.rawValue.isNotBlank() && currentAnswer?.source == AnswerSource.COPIED_BY_RULE) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Computes the next active field index in the question sequence.
     * Returns -1 if all fields have been answered or skipped (i.e. ready for review).
     */
    fun getNextFieldIndex(
        fromIndex: Int,
        fields: List<FormField>,
        currentAnswers: Map<String, FormAnswer>
    ): Int {
        var next = fromIndex + 1
        while (next < fields.size) {
            val candidate = fields[next]
            if (!shouldSkipField(candidate, fields, currentAnswers)) {
                return next
            }
            next++
        }
        return -1
    }

    /**
     * Computes the previous active field index in the question sequence.
     * Returns -1 if already at the first valid field.
     */
    fun getPreviousFieldIndex(
        fromIndex: Int,
        fields: List<FormField>,
        currentAnswers: Map<String, FormAnswer>
    ): Int {
        var prev = fromIndex - 1
        while (prev >= 0) {
            val candidate = fields[prev]
            if (!shouldSkipField(candidate, fields, currentAnswers)) {
                return prev
            }
            prev--
        }
        return -1
    }

    /**
     * Finds the first eligible question index when starting a session.
     */
    fun getFirstFieldIndex(
        fields: List<FormField>,
        currentAnswers: Map<String, FormAnswer>
    ): Int {
        return fields.indexOfFirst { !shouldSkipField(it, fields, currentAnswers) }
    }
}
