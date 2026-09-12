package com.formsaathi.core

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.formsaathi.answer.RuleBasedAnswerProcessor
import com.formsaathi.formengine.AndroidPdfPageRenderer
import com.formsaathi.formengine.AnswerBoxEstimator
import com.formsaathi.formengine.FieldMapper
import com.formsaathi.formengine.LabelDetector
import com.formsaathi.formengine.MlKitOcrEngine
import com.formsaathi.formengine.RealFormParser
import com.formsaathi.formengine.RequirementExtractor
import com.formsaathi.language.JsonQuestionProvider
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.voice.RealVoiceService
import com.formsaathi.voice.VoiceController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FormViewModel(application: Application) : AndroidViewModel(application) {
    private val speech = RealVoiceService(application)
    val voice = VoiceController(application, speech)
    private val ocr = MlKitOcrEngine()
    var language = SupportedLanguage.ENGLISH
        private set
    var selectedFileName: String? = null
        private set
    var selectedUri: Uri? = null
        private set

    private val parser = RealFormParser(
        AndroidPdfPageRenderer(application),
        ocr,
        LabelDetector(),
        FieldMapper(),
        AnswerBoxEstimator(),
        RequirementExtractor()
    )

    private fun createRealCoordinator(): FormSaathiCoordinator {
        return EngineFactory.createRealCoordinator(
            context = getApplication(),
            formParser = parser,
            questionProvider = JsonQuestionProvider(getApplication()),
            voiceService = speech,
            answerProcessor = RuleBasedAnswerProcessor()
        )
    }

    private fun createMockCoordinator(): FormSaathiCoordinator {
        return EngineFactory.createMockCoordinator(
            context = getApplication(),
            useRealPdfGenerator = true
        )
    }

    val sessionLauncher = FormSessionLauncher { useRealParser ->
        if (useRealParser) createRealCoordinator() else createMockCoordinator()
    }

    val coordinator: FormSaathiCoordinator
        get() = sessionLauncher.activeCoordinator

    private val _state = MutableStateFlow<FormUiState>(FormUiState.Idle)
    val state: StateFlow<FormUiState> = _state.asStateFlow()

    val recording = voice.isRecording
    private val _draft = MutableStateFlow("")
    val draft = _draft.asStateFlow()
    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError = _voiceError.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private var draftField: String? = null
    private var draftSource = AnswerSource.TYPED
    private var recordingTimeout: Job? = null
    private var stateObserverJob: Job? = null

    init {
        observeCoordinatorState(sessionLauncher.activeCoordinator)
    }

    private fun observeCoordinatorState(targetCoordinator: FormSaathiCoordinator) {
        stateObserverJob?.cancel()
        stateObserverJob = viewModelScope.launch {
            targetCoordinator.uiState.collect { current ->
                _state.value = current
                if (current is FormUiState.Questioning && current.currentField.id != draftField) {
                    draftField = current.currentField.id
                    _draft.value = current.currentAnswer?.rawValue.orEmpty()
                    draftSource = current.currentAnswer?.source ?: AnswerSource.TYPED
                    _voiceError.value = null
                }
            }
        }
    }

    fun selectLanguage(value: SupportedLanguage) {
        language = value
        coordinator.switchLanguage(value)
    }

    fun startRealSession(uri: Uri, fileName: String? = null) {
        if (_busy.value) return
        selectedUri = uri
        selectedFileName = fileName ?: uri.lastPathSegment?.substringAfterLast("/") ?: "Selected_Form.pdf"
        draftField = null
        val newCoordinator = sessionLauncher.startRealSession(uri, language, viewModelScope)
        observeCoordinatorState(newCoordinator)
    }

    fun startSampleSession(sampleUri: Uri, fileName: String? = "Sample_Form.pdf") {
        if (_busy.value) return
        selectedUri = sampleUri
        selectedFileName = fileName
        draftField = null
        val newCoordinator = sessionLauncher.startSampleSession(sampleUri, language, viewModelScope)
        observeCoordinatorState(newCoordinator)
    }

    fun retrySession() {
        val uri = selectedUri ?: return
        draftField = null
        val newCoordinator = sessionLauncher.retrySession(uri, language, viewModelScope)
        observeCoordinatorState(newCoordinator)
    }

    /**
     * Backward-compatible entry point for starting a real PDF session.
     */
    fun start(uri: Uri) {
        startRealSession(uri)
    }

    fun edit(value: String) {
        _draft.value = value
        draftSource = AnswerSource.TYPED
    }

    fun submit() {
        if (_busy.value || recording.value) return
        viewModelScope.launch {
            coordinator.submitAnswer(_draft.value, draftSource)
        }
    }

    fun previousField() {
        coordinator.previousField()
    }

    fun skipCurrentField() {
        coordinator.skipCurrentField()
    }

    fun jumpToField(fieldId: String) {
        coordinator.jumpToField(fieldId)
    }

    fun updateAnswerInReview(fieldId: String, newRawText: String) {
        coordinator.updateAnswerInReview(fieldId, newRawText)
    }

    fun startVoice() {
        if (_busy.value) return
        try {
            _voiceError.value = null
            voice.startListening()
            recordingTimeout = viewModelScope.launch {
                delay(10000)
                recordingTimeout = null
                stopVoice()
            }
        } catch (error: Exception) {
            _voiceError.value = error.message
        }
    }

    fun stopVoice() {
        if (_busy.value) return
        recordingTimeout?.cancel()
        recordingTimeout = null
        val field = draftField
        val selectedLanguage = language
        viewModelScope.launch {
            _busy.value = true
            try {
                val transcript = voice.stopListening(selectedLanguage)
                if (field == draftField) {
                    _draft.value = transcript
                    draftSource = AnswerSource.VOICE
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _voiceError.value = error.message
            } finally {
                _busy.value = false
            }
        }
    }

    fun cancelVoice() {
        recordingTimeout?.cancel()
        recordingTimeout = null
        voice.cancel()
    }

    fun permissionDenied() {
        _voiceError.value = "Microphone permission was denied. You can type your answer."
    }

    fun generate(uri: Uri) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                coordinator.generatePdf(uri)
            } finally {
                _busy.value = false
            }
        }
    }

    override fun onCleared() {
        voice.close()
        ocr.close()
    }
}
