package com.formsaathi.core

import android.net.Uri
import com.formsaathi.contracts.FakeAnswerProcessor
import com.formsaathi.contracts.FakeCompletedPdfGenerator
import com.formsaathi.contracts.FakeFormParser
import com.formsaathi.contracts.FakeQuestionProvider
import com.formsaathi.contracts.FakeVoiceService
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class FormSessionLauncherTest {

    private val mockUri = mock(Uri::class.java)

    private fun createTestCoordinator(): FormSaathiCoordinator {
        return FormSaathiCoordinator(
            formParser = FakeFormParser(),
            questionProvider = FakeQuestionProvider(),
            voiceService = FakeVoiceService(),
            answerProcessor = FakeAnswerProcessor(),
            pdfGenerator = FakeCompletedPdfGenerator(),
            conversationEngine = ConversationEngine()
        )
    }

    @Test
    fun testStartRealSession_createsRealCoordinatorAndAssignsActive() = runTest {
        var requestedParserType: Boolean? = null
        val createdCoordinators = mutableListOf<FormSaathiCoordinator>()

        val launcher = FormSessionLauncher { useRealParser ->
            requestedParserType = useRealParser
            val coord = createTestCoordinator()
            createdCoordinators.add(coord)
            coord
        }

        val initialCoordinator = launcher.activeCoordinator
        assertFalse("Initial coordinator should not be real parser", launcher.isRealParserActive)

        val returnedCoordinator = launcher.startRealSession(
            uri = mockUri,
            language = SupportedLanguage.ENGLISH,
            scope = this
        )

        assertEquals(true, requestedParserType)
        assertTrue(launcher.isRealParserActive)
        assertSame("Returned coordinator must match activeCoordinator", returnedCoordinator, launcher.activeCoordinator)
        assertTrue("activeCoordinator must be the newly created coordinator", createdCoordinators.contains(launcher.activeCoordinator))
        assertTrue("Coordinator must not be initial idle one", returnedCoordinator !== initialCoordinator)
    }

    @Test
    fun testStartMockSession_createsMockCoordinatorAndAssignsActive() = runTest {
        var requestedParserType: Boolean? = null

        val launcher = FormSessionLauncher { useRealParser ->
            requestedParserType = useRealParser
            createTestCoordinator()
        }

        val returnedCoordinator = launcher.startMockSession(
            uri = mockUri,
            language = SupportedLanguage.HINDI,
            scope = this
        )

        assertEquals(false, requestedParserType)
        assertFalse(launcher.isRealParserActive)
        assertSame("Returned coordinator must match activeCoordinator", returnedCoordinator, launcher.activeCoordinator)
    }

    @Test
    fun testRetrySession_retainsParserType() = runTest {
        val parserTypes = mutableListOf<Boolean>()

        val launcher = FormSessionLauncher { useRealParser ->
            parserTypes.add(useRealParser)
            createTestCoordinator()
        }

        // 1. Start real session
        launcher.startRealSession(mockUri, SupportedLanguage.ENGLISH, this)
        assertEquals(listOf(false, true), parserTypes) // false on init, true on startRealSession

        // 2. Retry session -> should use real parser (true)
        val retriedCoordinator = launcher.retrySession(mockUri, SupportedLanguage.ENGLISH, this)
        assertEquals(listOf(false, true, true), parserTypes)
        assertTrue(launcher.isRealParserActive)
        assertSame(retriedCoordinator, launcher.activeCoordinator)

        // 3. Start sample session
        launcher.startMockSession(mockUri, SupportedLanguage.ENGLISH, this)
        assertEquals(listOf(false, true, true, false), parserTypes)
        assertFalse(launcher.isRealParserActive)

        // 4. Retry sample session -> should use mock parser (false)
        val retriedSampleCoord = launcher.retrySession(mockUri, SupportedLanguage.ENGLISH, this)
        assertEquals(listOf(false, true, true, false, false), parserTypes)
        assertFalse(launcher.isRealParserActive)
        assertSame(retriedSampleCoord, launcher.activeCoordinator)
    }

    @Test
    fun testCoordinatorStability_instanceDoesNotChangeSpontaneously() {
        var createCount = 0
        val launcher = FormSessionLauncher {
            createCount++
            createTestCoordinator()
        }

        val firstInstance = launcher.activeCoordinator
        val secondCheck = launcher.activeCoordinator
        assertSame("Active coordinator must remain identical without session launch", firstInstance, secondCheck)
        assertEquals(1, createCount)
    }
}
