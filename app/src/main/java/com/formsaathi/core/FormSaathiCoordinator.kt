package com.formsaathi.core

import android.net.Uri
import com.formsaathi.contracts.AnswerProcessor
import com.formsaathi.contracts.CompletedPdfGenerator
import com.formsaathi.contracts.FormParser
import com.formsaathi.contracts.QuestionProvider
import com.formsaathi.contracts.VoiceService
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.model.ValidationResult
import java.io.File
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Central coordinator orchestrating FormSaathi's session lifecycle, conversation flow,
 * voice input, answer normalization/validation, and PDF export.
 */
class FormSaathiCoordinator(
    private val formParser: FormParser,
    private val questionProvider: QuestionProvider,
    private val voiceService: VoiceService,
    private val answerProcessor: AnswerProcessor,
    private val pdfGenerator: CompletedPdfGenerator,
    private val conversationEngine: ConversationEngine = ConversationEngine(),
    private val parseTimeoutMs: Long = 35_000L
) {
    private val _uiState = MutableStateFlow<FormUiState>(FormUiState.Idle)
    val uiState: StateFlow<FormUiState> = _uiState.asStateFlow()

    private val mutex = Mutex()
    private var activeSession: FormSession? = null
    private var sourcePdfUri: Uri? = null

    /**
     * Initializes a new session with a selected PDF and target language.
     */
    suspend fun startSession(uri: Uri, language: SupportedLanguage = SupportedLanguage.ENGLISH) {
        mutex.withLock {
            sourcePdfUri = uri
            _uiState.value = FormUiState.Parsing("Rendering PDF and identifying form fields...")
        }

        try {
            coroutineScope {
                // Background stage progress updater that accurately reflects parsing phases
                val stageJob = launch {
                    delay(750)
                    if (isActive && _uiState.value is FormUiState.Parsing) {
                        _uiState.value = FormUiState.Parsing("Reading text and performing OCR...")
                    }
                    delay(1750)
                    if (isActive && _uiState.value is FormUiState.Parsing) {
                        _uiState.value = FormUiState.Parsing("Detecting fields and preparing questions...")
                    }
                }

                try {
                    val parsedForm = withTimeout(parseTimeoutMs) {
                        formParser.parse(uri)
                    }

                    stageJob.cancel()

                    mutex.withLock {
                        val session = FormSession(parsedForm, language)
                        activeSession = session

                        if (parsedForm.fields.isEmpty()) {
                            _uiState.value = FormUiState.Reviewing(
                                answers = emptyMap(),
                                fields = emptyList(),
                                documents = parsedForm.documents,
                                language = language
                            )
                        } else {
                            emitCurrentQuestion(session, null)
                        }
                    }
                } finally {
                    stageJob.cancel()
                }
            }
        } catch (e: TimeoutCancellationException) {
            mutex.withLock {
                _uiState.value = FormUiState.Error(
                    message = "Form processing timed out after ${parseTimeoutMs / 1000} seconds. The document might be too complex or unreadable. Please try again or select another form.",
                    recoverable = true
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            mutex.withLock {
                _uiState.value = FormUiState.Error(
                    message = "Failed to parse document: ${e.localizedMessage ?: "Unknown error"}",
                    recoverable = true
                )
            }
        }
    }

    /**
     * Switches the application language on the fly without resetting user answers.
     */
    fun switchLanguage(newLanguage: SupportedLanguage) {
        val session = activeSession ?: return
        session.language = newLanguage

        when (val current = _uiState.value) {
            is FormUiState.Questioning -> {
                emitCurrentQuestion(session, current.validationError)
            }
            is FormUiState.Reviewing -> {
                _uiState.value = current.copy(language = newLanguage)
            }
            else -> Unit
        }
    }

    /**
     * Submits an answer (typed or from voice) for the currently active field.
     * Returns true if valid and accepted, false if invalid.
     */
    suspend fun submitAnswer(
        rawText: String,
        source: AnswerSource = AnswerSource.TYPED
    ): Boolean = mutex.withLock {
        val session = activeSession ?: return false
        val currentField = session.getCurrentField() ?: return false

        val normalized = answerProcessor.normalize(currentField.type, rawText, session.language)
        val validation = answerProcessor.validate(currentField.type, normalized)

        return when (validation) {
            is ValidationResult.Invalid -> {
                emitCurrentQuestion(session, validation.message)
                false
            }
            is ValidationResult.Valid -> {
                val answer = FormAnswer(
                    fieldId = currentField.id,
                    rawValue = rawText,
                    normalizedValue = normalized,
                    source = source
                )
                session.setAnswer(answer)

                // Check and apply automated answers from conditional rules
                val automated = conversationEngine.evaluateAutomatedAnswers(
                    fieldJustAnswered = currentField,
                    answer = answer,
                    fields = session.parsedForm.fields,
                    currentAnswers = session.answers
                )
                for (autoAnswer in automated) {
                    session.setAnswer(autoAnswer)
                }

                // Check and remove answers if a conditional rule was reversed (e.g. yes -> no)
                val toRemove = conversationEngine.evaluateAnswersToRemove(
                    fieldJustAnswered = currentField,
                    answer = answer,
                    fields = session.parsedForm.fields,
                    currentAnswers = session.answers
                )
                for (removeId in toRemove) {
                    session.removeAnswer(removeId)
                }

                // Advance to next field or transition to review
                val hasNext = session.advance(conversationEngine)
                if (hasNext) {
                    val nextField = session.getCurrentField()
                    val recoverableNotice = if (nextField?.type == FieldType.CURRENT_ADDRESS) {
                        val sameAsPerm = session.answers.values.firstOrNull { ans ->
                            session.parsedForm.fields.any { f -> f.id == ans.fieldId && f.type == FieldType.SAME_AS_PERMANENT_ADDRESS }
                        }
                        if (sameAsPerm?.normalizedValue.equals("yes", ignoreCase = true)) {
                            val permAns = session.answers.values.firstOrNull { ans ->
                                session.parsedForm.fields.any { f -> f.id == ans.fieldId && f.type == FieldType.PERMANENT_ADDRESS }
                            }
                            if (permAns == null || permAns.rawValue.isBlank()) {
                                "Permanent address was not provided to copy. Please enter your current address."
                            } else null
                        } else null
                    } else null

                    emitCurrentQuestion(session, recoverableNotice)
                } else {
                    emitReviewScreen(session)
                }
                true
            }
        }
    }

    /**
     * Skips the current field if permitted and moves to the next question.
     */
    fun skipCurrentField() {
        val session = activeSession ?: return
        val currentField = session.getCurrentField() ?: return

        // If field is marked required, disallow skipping
        if (currentField.required) {
            emitCurrentQuestion(session, "This field is required by the form.")
            return
        }

        val hasNext = session.skip(conversationEngine)
        if (hasNext) {
            emitCurrentQuestion(session, null)
        } else {
            emitReviewScreen(session)
        }
    }

    /**
     * Navigates back to the previous question.
     */
    fun previousField() {
        val session = activeSession ?: return
        if (session.goBack()) {
            emitCurrentQuestion(session, null)
        }
    }

    /**
     * Transcribes an audio recording using the offline VoiceService.
     */
    suspend fun transcribeVoice(audioFile: File): Result<String> {
        val session = activeSession ?: return Result.failure(IllegalStateException("No active session"))
        
        val currentState = _uiState.value
        if (currentState is FormUiState.Questioning) {
            _uiState.value = currentState.copy(isTranscribing = true)
        }

        return try {
            val transcript = voiceService.transcribe(audioFile, session.language)
            Result.success(transcript)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (_uiState.value is FormUiState.Questioning) {
                val state = _uiState.value as FormUiState.Questioning
                _uiState.value = state.copy(isTranscribing = false)
            }
        }
    }

    /**
     * Updates an answer directly from the review screen.
     */
    fun updateAnswerInReview(fieldId: String, newRawText: String): ValidationResult {
        val session = activeSession ?: return ValidationResult.Invalid("No active session")
        val field = session.parsedForm.fields.firstOrNull { it.id == fieldId }
            ?: return ValidationResult.Invalid("Field not found")

        val normalized = answerProcessor.normalize(field.type, newRawText, session.language)
        val validation = answerProcessor.validate(field.type, normalized)

        if (validation is ValidationResult.Valid) {
            val answer = FormAnswer(
                fieldId = fieldId,
                rawValue = newRawText,
                normalizedValue = normalized,
                source = AnswerSource.TYPED
            )
            session.setAnswer(answer)

            // Propagate automated side-effects or removals (e.g. updating permanent address or toggling same-as)
            val automated = conversationEngine.evaluateAutomatedAnswers(
                fieldJustAnswered = field,
                answer = answer,
                fields = session.parsedForm.fields,
                currentAnswers = session.answers
            )
            for (autoAnswer in automated) {
                session.setAnswer(autoAnswer)
            }

            val toRemove = conversationEngine.evaluateAnswersToRemove(
                fieldJustAnswered = field,
                answer = answer,
                fields = session.parsedForm.fields,
                currentAnswers = session.answers
            )
            for (removeId in toRemove) {
                session.removeAnswer(removeId)
            }

            emitReviewScreen(session)
        }

        return validation
    }

    /**
     * Navigates from Review screen directly to a specific field.
     */
    fun jumpToField(fieldId: String) {
        val session = activeSession ?: return
        val index = session.parsedForm.fields.indexOfFirst { it.id == fieldId }
        if (index != -1) {
            session.jumpToField(index)
            emitCurrentQuestion(session, null)
        }
    }

    /**
     * Transitions from Questioning directly to the Review screen.
     */
    fun goToReview() {
        val session = activeSession ?: return
        emitReviewScreen(session)
    }

    /**
     * Generates the final flattened completed PDF using CompletedPdfGenerator.
     */
    suspend fun generatePdf(outputUri: Uri) {
        val session = activeSession ?: run {
            _uiState.value = FormUiState.Error("Session expired or not initialized.")
            return
        }
        val sourceUri = sourcePdfUri ?: run {
            _uiState.value = FormUiState.Error("Source PDF document reference missing.")
            return
        }

        mutex.withLock {
            val current = _uiState.value
            if (current is FormUiState.Reviewing) {
                _uiState.value = current.copy(isGenerating = true)
            }
        }

        try {
            val result = pdfGenerator.generate(
                sourceUri = sourceUri,
                parsedForm = session.parsedForm,
                answers = session.answers,
                outputUri = outputUri
            )

            mutex.withLock {
                _uiState.value = FormUiState.Completed(
                    outputUri = outputUri,
                    filename = "Completed_Form.pdf",
                    warnings = result.warnings
                )
            }
        } catch (e: Exception) {
            mutex.withLock {
                _uiState.value = FormUiState.Error(
                    message = "Failed to generate completed PDF: ${e.localizedMessage ?: "Unknown error"}",
                    recoverable = true
                )
            }
        }
    }

    private fun emitCurrentQuestion(session: FormSession, validationError: String?) {
        val field = session.getCurrentField() ?: run {
            emitReviewScreen(session)
            return
        }

        val questionText = questionProvider.questionFor(field.type, session.language)
        val currentAnswer = session.getCurrentAnswer()
        val totalActive = session.getActiveQuestionsCount(conversationEngine)
        val activePosition = session.getActiveQuestionPosition(conversationEngine)

        _uiState.value = FormUiState.Questioning(
            currentField = field,
            questionText = questionText,
            currentAnswer = currentAnswer,
            questionIndex = activePosition,
            totalQuestions = totalActive,
            canGoBack = session.canGoBack(),
            canSkip = !field.required,
            validationError = validationError,
            isTranscribing = false,
            language = session.language
        )
    }

    private fun emitReviewScreen(session: FormSession) {
        _uiState.value = FormUiState.Reviewing(
            answers = session.answers,
            fields = session.parsedForm.fields,
            documents = session.parsedForm.documents,
            language = session.language,
            isGenerating = false
        )
    }
}
