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
     */
    fun evaluateAutomatedAnswers(
        fieldJustAnswered: FormField,
        answer: FormAnswer,
        fields: List<FormField>,
        currentAnswers: Map<String, FormAnswer>
    ): List<FormAnswer> {
        val automatedAnswers = mutableListOf<FormAnswer>()

        if (fieldJustAnswered.type == FieldType.SAME_AS_PERMANENT_ADDRESS) {
            val isAffirmative = answer.normalizedValue.equals("yes", ignoreCase = true)
            if (isAffirmative) {
                // Find permanent address answer
                val permAnswer = currentAnswers.values.firstOrNull { ans ->
                    fields.any { f -> f.id == ans.fieldId && f.type == FieldType.PERMANENT_ADDRESS }
                }

                // Find current address field
                val currentAddressField = fields.firstOrNull { it.type == FieldType.CURRENT_ADDRESS }

                if (permAnswer != null && currentAddressField != null) {
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

        return automatedAnswers
    }

    /**
     * Determines if a field should be skipped in the conversational flow based on current answers.
     */
    fun shouldSkipField(
        field: FormField,
        allFields: List<FormField>,
        currentAnswers: Map<String, FormAnswer>
    ): Boolean {
        // Photo and signature fields are asked with an attach-photo UI
        // instead of a text input, so they are never skipped here.
        // Skip CURRENT_ADDRESS if SAME_AS_PERMANENT_ADDRESS answered affirmative
        if (field.type == FieldType.CURRENT_ADDRESS) {
            val sameAsPermAnswer = currentAnswers.values.firstOrNull { ans ->
                allFields.any { f -> f.id == ans.fieldId && f.type == FieldType.SAME_AS_PERMANENT_ADDRESS }
            }
            if (sameAsPermAnswer?.normalizedValue.equals("yes", ignoreCase = true)) {
                return true
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
