package com.formsaathi.core

import android.net.Uri
import com.formsaathi.contracts.FakeAnswerProcessor
import com.formsaathi.contracts.FakeCompletedPdfGenerator
import com.formsaathi.contracts.FakeFormParser
import com.formsaathi.contracts.FakeQuestionProvider
import com.formsaathi.contracts.FakeVoiceService
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.runBlocking
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
}
