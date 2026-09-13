package com.formsaathi.core

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private fun createRealParser() = RealFormParser(
        AndroidPdfPageRenderer(getApplication()),
        ocr,
        LabelDetector(),
        FieldMapper(),
        AnswerBoxEstimator(),
        RequirementExtractor()
    )

    private fun createRealCoordinator(): FormSaathiCoordinator {
        // A fresh parser per session: the coordinator registers a progress
        // listener on it for the duration of the parse, and sharing one parser
        // across overlapping sessions would cross those listeners.
        return EngineFactory.createRealCoordinator(
            context = getApplication(),
            formParser = createRealParser(),
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
    private val _transcribing = MutableStateFlow(false)
    val transcribing = _transcribing.asStateFlow()
    private val _source = MutableStateFlow(AnswerSource.TYPED)
    val source = _source.asStateFlow()
    private var draftField: String? = null
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
                    _source.value = current.currentAnswer?.source ?: AnswerSource.TYPED
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

    /**
     * Opens one of the demo forms shipped in assets through the ordinary real
     * pipeline. The demo path runs the same renderer, OCR and parser as an
     * imported PDF, so what it shows is what the app actually does.
     */
    fun startBundledDemoSession(assetName: String, displayName: String) {
        if (_busy.value) return
        viewModelScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) { copyDemoFormToCache(assetName) }
                startRealSession(uri, displayName)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _voiceError.value = null
                sessionLauncher.cancelActiveSession()
                _state.value = FormUiState.Error(
                    message = "The bundled demo form could not be opened: ${error.message ?: "unknown error"}",
                    recoverable = false
                )
            }
        }
    }

    private fun copyDemoFormToCache(assetName: String): Uri {
        val context = getApplication<Application>()
        val demoDir = File(context.cacheDir, "demo-forms").apply { mkdirs() }
        val target = File(demoDir, assetName)
        if (!target.isFile || target.length() == 0L) {
            context.assets.open("samples/$assetName").use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
    }

    fun retrySession() {
        val uri = selectedUri ?: return
        draftField = null
        val newCoordinator = sessionLauncher.retrySession(uri, language, viewModelScope)
        observeCoordinatorState(newCoordinator)
    }

    /** Stops an in-flight parse when the user backs out of the processing screen. */
    fun cancelSession() {
        sessionLauncher.cancelActiveSession()
        draftField = null
    }

    fun clearSelection() {
        sessionLauncher.cancelActiveSession()
        selectedUri = null
        selectedFileName = null
        draftField = null
    }

    fun edit(value: String) {
        _draft.value = value
        _source.value = AnswerSource.TYPED
    }

    fun submit() {
        if (_busy.value || recording.value || _transcribing.value) return
        viewModelScope.launch {
            coordinator.submitAnswer(_draft.value, _source.value)
        }
    }

    fun attachPhoto(uri: Uri) {
        if (_busy.value || recording.value || _transcribing.value) return
        val field = draftField ?: return
        viewModelScope.launch {
            _busy.value = true
            try {
                val target = withContext(Dispatchers.IO) {
                    val photosDir = File(getApplication<Application>().filesDir, "photos")
                        .apply { mkdirs() }
                    val file = File(photosDir, "photo_${field}_${System.currentTimeMillis()}.jpg")
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { input.copyTo(it) }
                    } ?: throw IllegalStateException("Unable to read the selected image")
                    file
                }
                if (field == draftField) {
                    _draft.value = target.absolutePath
                    _source.value = AnswerSource.PHOTO
                    _voiceError.value = null
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _voiceError.value = error.message ?: "Unable to attach photo"
            } finally {
                _busy.value = false
            }
        }
    }

    fun previousField() {
        coordinator.previousField()
    }

    fun skipCurrentField() {
        coordinator.skipCurrentField()
    }

    fun jumpToField(fieldId: String, fromReview: Boolean = false) {
        coordinator.jumpToField(fieldId, fromReview)
    }

    fun updateAnswerInReview(fieldId: String, newRawText: String) {
        coordinator.updateAnswerInReview(fieldId, newRawText)
    }

    fun startVoice() {
        if (_busy.value || recording.value || _transcribing.value) return
        try {
            _voiceError.value = null
            voice.startListening()
            recordingTimeout = viewModelScope.launch {
                delay(MAX_RECORDING_MS)
                recordingTimeout = null
                stopVoice()
            }
        } catch (error: Exception) {
            _voiceError.value = error.message ?: "Unable to start recording. You can type your answer."
        }
    }

    fun stopVoice() {
        if (_busy.value || _transcribing.value) return
        if (!recording.value) return
        recordingTimeout?.cancel()
        recordingTimeout = null
        val field = draftField
        val selectedLanguage = language
        viewModelScope.launch {
            _busy.value = true
            _transcribing.value = true
            try {
                val transcript = voice.stopListening(selectedLanguage).trim()
                if (transcript.isEmpty()) {
                    // Defensive: the voice service already rejects empty and
                    // [BLANK_AUDIO] results. A blank transcript must never be
                    // written into the form as if it were an answer.
                    _voiceError.value =
                        "No speech detected — try again or type your answer."
                } else if (field == draftField) {
                    _draft.value = transcript
                    _source.value = AnswerSource.VOICE
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _voiceError.value = error.message
                    ?: "Voice input is unavailable. You can type your answer."
            } finally {
                _transcribing.value = false
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
        sessionLauncher.cancelActiveSession()
        voice.close()
        ocr.close()
    }

    private companion object {
        const val MAX_RECORDING_MS = 10_000L
    }
}
