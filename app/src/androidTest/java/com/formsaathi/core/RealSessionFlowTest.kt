package com.formsaathi.core

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.formsaathi.answer.RuleBasedAnswerProcessor
import com.formsaathi.contracts.FakeVoiceService
import com.formsaathi.formengine.AndroidPdfPageRenderer
import com.formsaathi.formengine.AnswerBoxEstimator
import com.formsaathi.formengine.FieldMapper
import com.formsaathi.formengine.LabelDetector
import com.formsaathi.formengine.MlKitOcrEngine
import com.formsaathi.formengine.RealFormParser
import com.formsaathi.formengine.RequirementExtractor
import com.formsaathi.language.JsonQuestionProvider
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.pdf.AndroidCompletedPdfGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Exercises the real session pipeline the app uses for an imported PDF:
 * launcher, coordinator, parser, conversation engine, generator.
 *
 * The failures this guards against are the ones that stranded the UI: a
 * coordinator that starts parsing while the UI observes a different instance, a
 * Processing screen with no terminal state, and a cancel that leaves work running.
 */
@RunWith(AndroidJUnit4::class)
class RealSessionFlowTest {

    private val appContext get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext get() = InstrumentationRegistry.getInstrumentation().context

    private val answersByType = mapOf(
        FieldType.FULL_NAME to "Aarav Sharma",
        FieldType.FATHER_NAME to "Ramesh Sharma",
        FieldType.MOTHER_NAME to "Sunita Sharma",
        FieldType.DATE_OF_BIRTH to "12/02/2006",
        FieldType.GENDER to "Male",
        FieldType.MOBILE to "9876543210",
        FieldType.EMAIL to "aarav@example.com",
        FieldType.AADHAAR to "123456789012",
        FieldType.PERMANENT_ADDRESS to "Flat 402, Lotus Heights, Pune 411041",
        FieldType.CURRENT_ADDRESS to "Flat 402, Lotus Heights, Pune 411041",
        FieldType.STATE to "Maharashtra",
        FieldType.DISTRICT to "Pune",
        FieldType.PINCODE to "411041",
        FieldType.CATEGORY to "General",
        FieldType.ANNUAL_INCOME to "250000",
        FieldType.SIGNATURE to "skip",
        FieldType.PHOTO to "skip"
    )

    private fun stageForm(assetName: String): Uri {
        val file = File(appContext.cacheDir, "flow_$assetName")
        testContext.assets.open("debugforms/$assetName").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return Uri.fromFile(file)
    }

    private fun newCoordinator(): FormSaathiCoordinator = EngineFactory.createRealCoordinator(
        context = appContext,
        formParser = RealFormParser(
            pageRenderer = AndroidPdfPageRenderer(appContext),
            ocrEngine = MlKitOcrEngine(),
            labelDetector = LabelDetector(),
            fieldMapper = FieldMapper(),
            answerBoxEstimator = AnswerBoxEstimator(),
            requirementExtractor = RequirementExtractor()
        ),
        questionProvider = JsonQuestionProvider(appContext),
        // The device may silence the microphone; voice is not what this test covers.
        voiceService = FakeVoiceService(),
        answerProcessor = RuleBasedAnswerProcessor(),
        pdfGenerator = AndroidCompletedPdfGenerator(appContext)
    )

    @Test
    fun importedFormRunsFromPickerToGeneratedPdf() = runBlocking {
        val uri = stageForm("FormSaathi_Test_Form_1_Simple.pdf")
        val launcher = FormSessionLauncher { newCoordinator() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        try {
            val started = launcher.startRealSession(uri, SupportedLanguage.ENGLISH, scope)
            assertSame(
                "The UI must observe the same coordinator that started parsing",
                started,
                launcher.activeCoordinator
            )

            val questioning = withTimeoutOrNull(PARSE_BUDGET_MS) {
                started.uiState.first { it is FormUiState.Questioning || it is FormUiState.Error }
            }
            assertNotNull("Parsing never reached a terminal state", questioning)
            assertTrue(
                "Parsing must produce questions, got $questioning",
                questioning is FormUiState.Questioning
            )

            // Every question must be answerable and the flow must end in review.
            var guard = 0
            while (started.uiState.value is FormUiState.Questioning && guard++ < MAX_QUESTIONS) {
                val state = started.uiState.value as FormUiState.Questioning
                assertTrue(
                    "Question text must never be empty for '${state.currentField.sourceLabel}'",
                    state.questionText.isNotBlank()
                )
                assertTrue(
                    "A question must never be a bare placeholder: '${state.questionText}'",
                    !state.questionText.contains("provide the requested detail", ignoreCase = true)
                )
                val answer = answersByType[state.currentField.type]
                if (answer == null || answer == "skip") {
                    started.skipCurrentField()
                } else {
                    started.submitAnswer(answer, AnswerSource.TYPED)
                }
            }

            val review = started.uiState.value
            assertTrue("Flow must end in review, got $review", review is FormUiState.Reviewing)
            review as FormUiState.Reviewing
            assertTrue("Review must list the detected fields", review.fields.size >= 8)
            assertTrue("Review must carry the typed answers", review.answers.isNotEmpty())

            val output = File(appContext.cacheDir, "flow-completed.pdf")
            started.generatePdf(Uri.fromFile(output))

            val completed = started.uiState.value
            assertTrue("Generation must complete, got $completed", completed is FormUiState.Completed)
            assertTrue("Generated PDF must not be empty", output.length() > 1_000L)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun cancellingWhileParsingLeavesProcessingImmediately() = runBlocking {
        val uri = stageForm("FormSaathi_Test_Form_2_Boxed.pdf")
        val launcher = FormSessionLauncher { newCoordinator() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        try {
            val coordinator = launcher.startRealSession(uri, SupportedLanguage.ENGLISH, scope)
            withTimeoutOrNull(5_000) {
                coordinator.uiState.first { it is FormUiState.Parsing }
            }

            launcher.cancelActiveSession()

            val settled = withTimeoutOrNull(5_000) {
                coordinator.uiState.first { it !is FormUiState.Parsing }
            }
            assertNotNull("Cancelling must leave the Processing state", settled)
            assertEquals(FormUiState.Idle, settled)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aDocumentThatIsNotAPdfFailsRecoverablyInsteadOfHanging() = runBlocking {
        val broken = File(appContext.cacheDir, "not-a-pdf.pdf")
        broken.writeText("This is not a PDF at all.")

        val coordinator = newCoordinator()
        coordinator.startSession(Uri.fromFile(broken), SupportedLanguage.ENGLISH)

        val state = coordinator.uiState.value
        assertTrue("A broken file must surface an error, got $state", state is FormUiState.Error)
        state as FormUiState.Error
        assertTrue("The error must be recoverable", state.recoverable)
        assertTrue(
            "The error must tell the user what to do: ${state.message}",
            state.message.contains("Try again", ignoreCase = true)
        )
    }

    private companion object {
        /** Generous: real OCR on a cold ML Kit recogniser is slow on the first page. */
        const val PARSE_BUDGET_MS = 120_000L
        const val MAX_QUESTIONS = 40
    }
}
