package com.formsaathi.core

import android.net.Uri
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages coordinator instance ownership and active form session launching.
 * Guarantees that the exact coordinator instance used to start parsing is retained
 * and observed across all screen navigation transitions and recompositions.
 */
class FormSessionLauncher(
    private val coordinatorFactory: (useRealParser: Boolean) -> FormSaathiCoordinator
) {
    /**
     * Currently active coordinator for the ongoing or most recent session.
     */
    var activeCoordinator: FormSaathiCoordinator = coordinatorFactory(false)
        private set

    /**
     * Flag indicating whether the active session was launched with the real OCR parser.
     */
    var isRealParserActive: Boolean = false
        private set

    /**
     * Starts a session for a real PDF using a newly created real-parser coordinator.
     * Ensures parsing begins on the exact coordinator instance that will be observed by the UI.
     */
    fun startRealSession(
        uri: Uri,
        language: SupportedLanguage,
        scope: CoroutineScope
    ): FormSaathiCoordinator {
        val coordinator = coordinatorFactory(true)
        activeCoordinator = coordinator
        isRealParserActive = true
        scope.launch {
            coordinator.startSession(uri, language)
        }
        return coordinator
    }

    /**
     * Starts a session for the sample mock form using a newly created mock-parser coordinator.
     * Ensures parsing begins on the exact coordinator instance that will be observed by the UI.
     */
    fun startSampleSession(
        uri: Uri,
        language: SupportedLanguage,
        scope: CoroutineScope
    ): FormSaathiCoordinator {
        val coordinator = coordinatorFactory(false)
        activeCoordinator = coordinator
        isRealParserActive = false
        scope.launch {
            coordinator.startSession(uri, language)
        }
        return coordinator
    }

    /**
     * Retries the current session with the appropriate coordinator type.
     */
    fun retrySession(
        uri: Uri,
        language: SupportedLanguage,
        scope: CoroutineScope
    ): FormSaathiCoordinator {
        return if (isRealParserActive) {
            startRealSession(uri, language, scope)
        } else {
            startSampleSession(uri, language, scope)
        }
    }
}
