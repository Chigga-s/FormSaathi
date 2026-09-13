package com.formsaathi.core

import android.net.Uri
import com.formsaathi.contracts.FakeAnswerProcessor
import com.formsaathi.contracts.FakeCompletedPdfGenerator
import com.formsaathi.contracts.FakeFormParser
import com.formsaathi.contracts.FakeQuestionProvider
import com.formsaathi.contracts.FakeVoiceService
import com.formsaathi.contracts.FormParser
import com.formsaathi.contracts.ParseStage
import com.formsaathi.contracts.ProgressReportingFormParser
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.ParsedForm
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class FormSaathiCoordinatorTest {

    private lateinit var coordinator: FormSaathiCoordinator
    private lateinit var fakePdfGen: FakeCompletedPdfGenerator
    private val mockUri = mock(Uri::class.java)

    @Before
    fun setUp() {
        fakePdfGen = FakeCompletedPdfGenerator()
        coordinator = FormSaathiCoordinator(
            formParser = FakeFormParser(),
            questionProvider = FakeQuestionProvider(),
            voiceService = FakeVoiceService(),
            answerProcessor = FakeAnswerProcessor(),
            pdfGenerator = fakePdfGen,
            conversationEngine = ConversationEngine()
        )
    }

    @Test
    fun testStartSessionTransitionsToFirstQuestion() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)

        val state = coordinator.uiState.value
        assertTrue("Expected Questioning state, but was: $state", state is FormUiState.Questioning)
        val questioning = state as FormUiState.Questioning
        assertEquals("field_full_name", questioning.currentField.id)
        assertEquals("What is your full legal name?", questioning.questionText)
        assertNull(questioning.validationError)
        assertEquals(0, questioning.questionIndex)
    }

    @Test
    fun testSwitchLanguageUpdatesQuestionText() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)

        coordinator.switchLanguage(SupportedLanguage.HINDI)
        val state = coordinator.uiState.value as FormUiState.Questioning
        assertEquals("आपका पूरा नाम क्या है?", state.questionText)
        assertEquals(SupportedLanguage.HINDI, state.language)
    }

    @Test
    fun testValidationFailureKeepsUserOnQuestion() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)

        // Empty name is invalid
        val accepted = coordinator.submitAnswer("", AnswerSource.TYPED)
        assertEquals(false, accepted)

        val state = coordinator.uiState.value as FormUiState.Questioning
        assertEquals("field_full_name", state.currentField.id)
        assertNotNull(state.validationError)
    }

    @Test
    fun testValidAnswerAdvancesQuestion() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)

        val accepted = coordinator.submitAnswer("Rahul Sharma", AnswerSource.TYPED)
        assertTrue(accepted)

        val state = coordinator.uiState.value as FormUiState.Questioning
        assertEquals("field_father_name", state.currentField.id)
        assertNull(state.validationError)
    }

    @Test
    fun testGeneratePdfCallsGeneratorAndCompletes() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)
        coordinator.goToReview()

        val outputUri = mock(Uri::class.java)
        coordinator.generatePdf(outputUri)

        val state = coordinator.uiState.value
        assertTrue("Expected Completed state, got $state", state is FormUiState.Completed)
        assertTrue(fakePdfGen.wasGenerateCalled)
    }

    @Test
    fun testSameAddressSequenceCopiesSkipsAndAppearsInReviewAndPdf() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)

        // Jump directly to permanent address
        coordinator.jumpToField("field_permanent_address")
        var state = coordinator.uiState.value as FormUiState.Questioning
        assertEquals("field_permanent_address", state.currentField.id)

        // 1. Enter permanent address
        val permAccepted = coordinator.submitAnswer("Flat 402, Lotus Heights, Bengaluru", AnswerSource.TYPED)
        assertTrue("Permanent address answer should be accepted", permAccepted)

        // Flow lands on same-as-permanent-address
        state = coordinator.uiState.value as FormUiState.Questioning
        assertEquals("field_same_as_permanent", state.currentField.id)

        // 2. Answer 'yes' to same-address
        val sameAccepted = coordinator.submitAnswer("yes", AnswerSource.TYPED)
        assertTrue("Same-as answer should be accepted", sameAccepted)

        // 3. Flow must skip current-address and advance directly to category
        state = coordinator.uiState.value as FormUiState.Questioning
        assertEquals("field_category", state.currentField.id)

        // 4. Transition to Review
        coordinator.goToReview()
        val reviewState = coordinator.uiState.value as FormUiState.Reviewing

        // 5. Review contains copied current address answer marked COPIED_BY_RULE
        val currAnswer = reviewState.answers["field_current_address"]
        assertNotNull("Current address must exist in review answers", currAnswer)
        assertEquals("Flat 402, Lotus Heights, Bengaluru", currAnswer!!.rawValue)
        assertEquals("Flat 402, Lotus Heights, Bengaluru", currAnswer.normalizedValue)
        assertEquals(AnswerSource.COPIED_BY_RULE, currAnswer.source)

        // 6. Generate PDF and verify copied answer is passed to generator
        val outputUri = mock(Uri::class.java)
        coordinator.generatePdf(outputUri)
        assertTrue(fakePdfGen.wasGenerateCalled)
        val generatedCurrAnswer = fakePdfGen.lastAnswers["field_current_address"]
        assertNotNull("Current address answer must be delivered to PDF generator", generatedCurrAnswer)
        assertEquals("Flat 402, Lotus Heights, Bengaluru", generatedCurrAnswer!!.rawValue)
        assertEquals(AnswerSource.COPIED_BY_RULE, generatedCurrAnswer.source)
    }

    @Test
    fun testSameAddressYesWhenPermanentAddressMissingDoesNotSkipCurrentAddress() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)

        // Jump directly to same-as-permanent without entering permanent address
        coordinator.jumpToField("field_same_as_permanent")
        var state = coordinator.uiState.value as FormUiState.Questioning
        assertEquals("field_same_as_permanent", state.currentField.id)

        // Answer 'yes'
        val sameAccepted = coordinator.submitAnswer("yes", AnswerSource.TYPED)
        assertTrue(sameAccepted)

        // Because permanent address is missing, current address CANNOT be copied and MUST NOT be skipped
        state = coordinator.uiState.value as FormUiState.Questioning
        assertEquals("field_current_address", state.currentField.id)
        assertNotNull("Expected recoverable error/notice for missing permanent address", state.validationError)

        // User can recover by entering current address manually
        val currAccepted = coordinator.submitAnswer("789 MG Road, Pune", AnswerSource.TYPED)
        assertTrue(currAccepted)

        coordinator.goToReview()
        val reviewState = coordinator.uiState.value as FormUiState.Reviewing
        val currAnswer = reviewState.answers["field_current_address"]
        assertNotNull(currAnswer)
        assertEquals("789 MG Road, Pune", currAnswer!!.rawValue)
        assertEquals(AnswerSource.TYPED, currAnswer.source)
    }

    @Test
    fun testReviewUpdatingPermanentAddressSyncsCurrentAddressWhenSameAddressIsYes() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)
        coordinator.jumpToField("field_permanent_address")
        coordinator.submitAnswer("Initial Address 1", AnswerSource.TYPED)
        coordinator.submitAnswer("yes", AnswerSource.TYPED)
        coordinator.goToReview()

        var reviewState = coordinator.uiState.value as FormUiState.Reviewing
        assertEquals("Initial Address 1", reviewState.answers["field_current_address"]?.rawValue)

        // Update permanent address in review
        coordinator.updateAnswerInReview("field_permanent_address", "Updated Address 2")

        reviewState = coordinator.uiState.value as FormUiState.Reviewing
        assertEquals("Updated Address 2", reviewState.answers["field_permanent_address"]?.rawValue)
        assertEquals("Updated Address 2", reviewState.answers["field_current_address"]?.rawValue)
        assertEquals(AnswerSource.COPIED_BY_RULE, reviewState.answers["field_current_address"]?.source)
    }

    @Test
    fun testReviewTogglingSameAddressToNoRemovesCopiedCurrentAddress() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)
        coordinator.jumpToField("field_permanent_address")
        coordinator.submitAnswer("Initial Address 1", AnswerSource.TYPED)
        coordinator.submitAnswer("yes", AnswerSource.TYPED)
        coordinator.goToReview()

        var reviewState = coordinator.uiState.value as FormUiState.Reviewing
        assertNotNull(reviewState.answers["field_current_address"])

        // Toggle same-as to 'no'
        coordinator.updateAnswerInReview("field_same_as_permanent", "no")

        reviewState = coordinator.uiState.value as FormUiState.Reviewing
        assertNull("Copied current address should be removed when same-as becomes no", reviewState.answers["field_current_address"])
    }

    @Test
    fun testStalledParseTransitionsToRecoverableError() = runTest {
        val hangingParser = object : FormParser {
            override suspend fun parse(uri: Uri): ParsedForm {
                // Never reports progress and never finishes.
                kotlinx.coroutines.awaitCancellation()
            }
        }

        val stallCoordinator = FormSaathiCoordinator(
            formParser = hangingParser,
            questionProvider = FakeQuestionProvider(),
            voiceService = FakeVoiceService(),
            answerProcessor = FakeAnswerProcessor(),
            pdfGenerator = fakePdfGen,
            conversationEngine = ConversationEngine(),
            stallTimeoutMs = 10_000L,
            nowMs = { testScheduler.currentTime }
        )

        stallCoordinator.startSession(mockUri, SupportedLanguage.ENGLISH)

        val state = stallCoordinator.uiState.value
        assertTrue("Expected Error state on a stalled parse, got $state", state is FormUiState.Error)
        val errorState = state as FormUiState.Error
        assertTrue("Error should be recoverable", errorState.recoverable)
        assertTrue(
            "Error should explain the form stopped responding: ${errorState.message}",
            errorState.message.contains("stopped responding", ignoreCase = true)
        )
    }

    @Test
    fun testSlowButProgressingParseIsNotInterrupted() = runTest {
        // A large real form can spend minutes in on-device OCR. As long as the
        // parser keeps reporting progress it must be left alone, even when the
        // total run far exceeds the stall window.
        val slowParser = object : ProgressReportingFormParser {
            private var listener: ((ParseStage, Int, Int) -> Unit)? = null
            override fun setProgressListener(l: ((ParseStage, Int, Int) -> Unit)?) {
                listener = l
            }

            override suspend fun parse(uri: Uri): ParsedForm {
                repeat(12) { page ->
                    listener?.invoke(ParseStage.RENDERING, page, 12)
                    kotlinx.coroutines.delay(8_000L)
                    listener?.invoke(ParseStage.READING_TEXT, page, 12)
                    kotlinx.coroutines.delay(8_000L)
                }
                return FakeFormParser().parse(uri)
            }
        }

        val slowCoordinator = FormSaathiCoordinator(
            formParser = slowParser,
            questionProvider = FakeQuestionProvider(),
            voiceService = FakeVoiceService(),
            answerProcessor = FakeAnswerProcessor(),
            pdfGenerator = fakePdfGen,
            conversationEngine = ConversationEngine(),
            stallTimeoutMs = 10_000L,
            nowMs = { testScheduler.currentTime }
        )

        slowCoordinator.startSession(mockUri, SupportedLanguage.ENGLISH)

        val state = slowCoordinator.uiState.value
        assertTrue(
            "A parser that keeps reporting progress must finish, got $state",
            state is FormUiState.Questioning
        )
    }

    @Test
    fun testCancelParsingLeavesProcessingImmediately() = runTest {
        val hangingParser = object : FormParser {
            override suspend fun parse(uri: Uri): ParsedForm = kotlinx.coroutines.awaitCancellation()
        }

        val cancelCoordinator = FormSaathiCoordinator(
            formParser = hangingParser,
            questionProvider = FakeQuestionProvider(),
            voiceService = FakeVoiceService(),
            answerProcessor = FakeAnswerProcessor(),
            pdfGenerator = fakePdfGen,
            conversationEngine = ConversationEngine(),
            stallTimeoutMs = 10_000_000L,
            nowMs = { testScheduler.currentTime }
        )

        val job = launch { cancelCoordinator.startSession(mockUri, SupportedLanguage.ENGLISH) }
        runCurrent()
        assertTrue(
            "Expected Parsing while the job runs",
            cancelCoordinator.uiState.value is FormUiState.Parsing
        )

        cancelCoordinator.cancelParsing()
        job.join()

        assertTrue(
            "Cancelling must leave Processing, got ${cancelCoordinator.uiState.value}",
            cancelCoordinator.uiState.value is FormUiState.Idle
        )
    }

    @Test
    fun testParseFailureTransitionsToRecoverableError() = runBlocking {
        val failingParser = object : FormParser {
            override suspend fun parse(uri: Uri): ParsedForm {
                throw IllegalStateException("Corrupted PDF document")
            }
        }

        val failingCoordinator = FormSaathiCoordinator(
            formParser = failingParser,
            questionProvider = FakeQuestionProvider(),
            voiceService = FakeVoiceService(),
            answerProcessor = FakeAnswerProcessor(),
            pdfGenerator = fakePdfGen,
            conversationEngine = ConversationEngine()
        )

        failingCoordinator.startSession(mockUri, SupportedLanguage.ENGLISH)

        val state = failingCoordinator.uiState.value
        assertTrue("Expected Error state on parsing failure, got $state", state is FormUiState.Error)
        val errorState = state as FormUiState.Error
        assertTrue("Error should be recoverable", errorState.recoverable)
        assertTrue(errorState.message.contains("Corrupted PDF document"))
    }

    @Test
    fun testEditingOneAnswerFromReviewReturnsToReview() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)
        coordinator.goToReview()

        // Correct one field reached from the review list.
        coordinator.jumpToField("field_mobile", fromReview = true)
        val questioning = coordinator.uiState.value as FormUiState.Questioning
        assertEquals("field_mobile", questioning.currentField.id)

        assertTrue(coordinator.submitAnswer("9876543210", AnswerSource.TYPED))

        val after = coordinator.uiState.value
        assertTrue(
            "Answering a field opened from review must return to review, got $after",
            after is FormUiState.Reviewing
        )
        assertEquals(
            "9876543210",
            (after as FormUiState.Reviewing).answers["field_mobile"]?.rawValue
        )
    }

    @Test
    fun testEditingFromReviewPreservesTheExistingAnswer() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)
        coordinator.submitAnswer("Rahul Sharma", AnswerSource.TYPED)
        coordinator.goToReview()

        coordinator.jumpToField("field_full_name", fromReview = true)

        val questioning = coordinator.uiState.value as FormUiState.Questioning
        assertEquals(
            "Reopening a field must show what the user already entered",
            "Rahul Sharma",
            questioning.currentAnswer?.rawValue
        )
    }

    @Test
    fun testAnsweringNormallyStillWalksForward() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)
        coordinator.jumpToField("field_full_name")

        assertTrue(coordinator.submitAnswer("Rahul Sharma", AnswerSource.TYPED))

        val after = coordinator.uiState.value
        assertTrue("A normal answer must advance, got $after", after is FormUiState.Questioning)
        assertEquals("field_father_name", (after as FormUiState.Questioning).currentField.id)
    }

    @Test
    fun testInvalidAnswerDoesNotDiscardWhatTheUserTyped() = runBlocking {
        coordinator.startSession(mockUri, SupportedLanguage.ENGLISH)
        coordinator.jumpToField("field_mobile")

        val accepted = coordinator.submitAnswer("12345", AnswerSource.TYPED)
        assertEquals(false, accepted)

        val state = coordinator.uiState.value as FormUiState.Questioning
        assertEquals(
            "The user must stay on the field they were correcting",
            "field_mobile",
            state.currentField.id
        )
        assertNotNull("A correction message must be shown", state.validationError)
    }

    @Test
    fun testParsedWarningsReachTheReviewScreen() = runBlocking {
        val warningParser = object : FormParser {
            override suspend fun parse(uri: Uri): ParsedForm {
                val base = FakeFormParser().parse(uri)
                return base.copy(warnings = listOf("Could not place 'Ward Number' on page 1."))
            }
        }
        val warningCoordinator = FormSaathiCoordinator(
            formParser = warningParser,
            questionProvider = FakeQuestionProvider(),
            voiceService = FakeVoiceService(),
            answerProcessor = FakeAnswerProcessor(),
            pdfGenerator = fakePdfGen,
            conversationEngine = ConversationEngine()
        )

        warningCoordinator.startSession(mockUri, SupportedLanguage.ENGLISH)
        warningCoordinator.goToReview()

        val review = warningCoordinator.uiState.value as FormUiState.Reviewing
        assertTrue(
            "Fields the parser could not place must be surfaced for manual review",
            review.warnings.any { it.contains("Ward Number") }
        )
    }
}
