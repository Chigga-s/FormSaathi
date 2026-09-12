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
}
