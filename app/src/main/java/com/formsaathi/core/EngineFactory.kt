package com.formsaathi.core

import android.content.Context
import com.formsaathi.contracts.AnswerProcessor
import com.formsaathi.contracts.CompletedPdfGenerator
import com.formsaathi.contracts.FakeAnswerProcessor
import com.formsaathi.contracts.FakeCompletedPdfGenerator
import com.formsaathi.contracts.FakeFormParser
import com.formsaathi.contracts.FakeQuestionProvider
import com.formsaathi.contracts.FakeVoiceService
import com.formsaathi.contracts.FormParser
import com.formsaathi.contracts.QuestionProvider
import com.formsaathi.contracts.VoiceService
import com.formsaathi.pdf.AndroidCompletedPdfGenerator

/**
 * Factory for creating FormSaathiCoordinator instances with either mock or real engines.
 * Enables zero-coupling parallel development across all 4 hackathon roles.
 */
object EngineFactory {

    /**
     * Creates a coordinator backed entirely by deterministic mocks.
     * Used by Role 1 (UI) and integration tests to run the full app flow without native/OCR dependencies.
     */
    fun createMockCoordinator(
        context: Context? = null,
        useRealPdfGenerator: Boolean = false
    ): FormSaathiCoordinator {
        val pdfGen: CompletedPdfGenerator = if (useRealPdfGenerator && context != null) {
            AndroidCompletedPdfGenerator(context)
        } else {
            FakeCompletedPdfGenerator()
        }

        return FormSaathiCoordinator(
            formParser = FakeFormParser(),
            questionProvider = FakeQuestionProvider(),
            voiceService = FakeVoiceService(),
            answerProcessor = FakeAnswerProcessor(),
            pdfGenerator = pdfGen,
            conversationEngine = ConversationEngine()
        )
    }

    /**
     * Creates a production coordinator with real engine implementations from all roles.
     * When Role 2 and Role 3 merge their implementations, they plug in here seamlessly.
     */
    fun createRealCoordinator(
        context: Context,
        formParser: FormParser,
        questionProvider: QuestionProvider,
        voiceService: VoiceService,
        answerProcessor: AnswerProcessor,
        pdfGenerator: CompletedPdfGenerator = AndroidCompletedPdfGenerator(context),
        conversationEngine: ConversationEngine = ConversationEngine()
    ): FormSaathiCoordinator {
        return FormSaathiCoordinator(
            formParser = formParser,
            questionProvider = questionProvider,
            voiceService = voiceService,
            answerProcessor = answerProcessor,
            pdfGenerator = pdfGenerator,
            conversationEngine = conversationEngine
        )
    }
}
