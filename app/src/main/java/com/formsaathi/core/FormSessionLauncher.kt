package com.formsaathi.core

import android.net.Uri
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns the coordinator instance for the current session.
 *
 * The instance that starts parsing is the instance stored in [activeCoordinator],
 * so the UI observes the same object across recomposition and navigation. A
 * previous session's parse is cancelled before a new one starts, so an abandoned
 * OCR run cannot keep emitting into a screen the user has moved past.
 */
class FormSessionLauncher(
    private val coordinatorFactory: (useRealParser: Boolean) -> FormSaathiCoordinator
) {
    var activeCoordinator: FormSaathiCoordinator = coordinatorFactory(false)
        private set

    /** True when the active session was launched with the real OCR parser. */
    var isRealParserActive: Boolean = false
        private set

    private var activeJob: Job? = null

    /**
     * Starts a session for a real PDF on a freshly created real-parser coordinator.
     */
    fun startRealSession(
        uri: Uri,
        language: SupportedLanguage,
        scope: CoroutineScope
    ): FormSaathiCoordinator = start(useRealParser = true, uri = uri, language = language, scope = scope)

    /**
     * Starts a session on a fake-engine coordinator. Debug harness and tests only.
     */
    fun startMockSession(
        uri: Uri,
        language: SupportedLanguage,
        scope: CoroutineScope
    ): FormSaathiCoordinator = start(useRealParser = false, uri = uri, language = language, scope = scope)

    /**
     * Re-runs the current session with the same kind of parser.
     */
    fun retrySession(
        uri: Uri,
        language: SupportedLanguage,
        scope: CoroutineScope
    ): FormSaathiCoordinator = start(isRealParserActive, uri, language, scope)

    /** Stops any parse in flight without starting a new session. */
    fun cancelActiveSession() {
        activeCoordinator.cancelParsing()
        activeJob?.cancel()
        activeJob = null
    }

    private fun start(
        useRealParser: Boolean,
        uri: Uri,
        language: SupportedLanguage,
        scope: CoroutineScope
    ): FormSaathiCoordinator {
        activeCoordinator.cancelParsing()
        activeJob?.cancel()

        val coordinator = coordinatorFactory(useRealParser)
        activeCoordinator = coordinator
        isRealParserActive = useRealParser
        activeJob = scope.launch {
            coordinator.startSession(uri, language)
        }
        return coordinator
    }
}
