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
import com.formsaathi.answer.RuleBasedAnswerProcessor
import com.formsaathi.language.JsonQuestionProvider
import com.formsaathi.pdf.AndroidCompletedPdfGenerator
import com.formsaathi.voice.RealVoiceService

/**
 * Builds coordinators from either the real engines or the fakes.
 *
 * The fakes exist for unit tests and the debug harness only. Anything reachable
 * from the normal user flow must come from [createRealCoordinator]; a fake parser
 * silently powering an imported PDF would draw answers at invented coordinates.
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
     * Coordinator backed by fakes, for unit tests and the debug harness.
     * Never reachable from the normal user flow.
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
     * Production coordinator. Every default here is a real implementation so a
     * missing argument can never downgrade the live flow to a fake engine.
     */
    fun createRealCoordinator(
        context: Context,
        formParser: FormParser = createRealFormParser(context),
        questionProvider: QuestionProvider = JsonQuestionProvider(context),
        voiceService: VoiceService = RealVoiceService(context),
        answerProcessor: AnswerProcessor = RuleBasedAnswerProcessor(),
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
