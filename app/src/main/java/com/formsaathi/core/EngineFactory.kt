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
import com.formsaathi.formengine.AndroidPdfPageRenderer
import com.formsaathi.formengine.AnswerBoxEstimator
import com.formsaathi.formengine.FieldMapper
import com.formsaathi.formengine.LabelDetector
import com.formsaathi.formengine.MlKitOcrEngine
import com.formsaathi.formengine.OcrScript
import com.formsaathi.formengine.RealFormParser
import com.formsaathi.formengine.RequirementExtractor
import com.formsaathi.pdf.AndroidCompletedPdfGenerator

/**
 * Factory for creating FormSaathiCoordinator instances with either mock or real engines.
 * Enables zero-coupling parallel development across all 4 hackathon roles.
 */
object EngineFactory {

    /**
     * Creates a RealFormParser instance using Role 2's OCR engine and heuristic extractors.
     */
    fun createRealFormParser(
        context: Context,
        ocrScript: OcrScript = OcrScript.LATIN
    ): RealFormParser {
        return RealFormParser(
            pageRenderer = AndroidPdfPageRenderer(context),
            ocrEngine = MlKitOcrEngine(),
            labelDetector = LabelDetector(),
            fieldMapper = FieldMapper(),
            answerBoxEstimator = AnswerBoxEstimator(),
            requirementExtractor = RequirementExtractor(),
            ocrScript = ocrScript
        )
    }

    /**
     * Creates a coordinator backed by mocks, with options to enable real PDF generator (Role 4)
     * and/or real FormParser (Role 2).
     */
    fun createMockCoordinator(
        context: Context? = null,
        useRealPdfGenerator: Boolean = false,
        useRealFormParser: Boolean = false
    ): FormSaathiCoordinator {
        val parser: FormParser = if (useRealFormParser && context != null) {
            createRealFormParser(context)
        } else {
            FakeFormParser()
        }

        val pdfGen: CompletedPdfGenerator = if (useRealPdfGenerator && context != null) {
            AndroidCompletedPdfGenerator(context)
        } else {
            FakeCompletedPdfGenerator()
        }

        return FormSaathiCoordinator(
            formParser = parser,
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
        formParser: FormParser = createRealFormParser(context),
        questionProvider: QuestionProvider = FakeQuestionProvider(),
        voiceService: VoiceService = FakeVoiceService(),
        answerProcessor: AnswerProcessor = FakeAnswerProcessor(),
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
