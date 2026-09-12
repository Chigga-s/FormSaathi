package com.formsaathi.UI

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.formsaathi.core.FormUiState
import com.formsaathi.core.FormViewModel
import com.formsaathi.model.FieldType
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.pdf.OutputFileManager

@Composable
fun FormSaathiNavigation(model: FormViewModel) {
    val context = LocalContext.current
    val state by model.state.collectAsState()
    val draft by model.draft.collectAsState()
    val recording by model.recording.collectAsState()
    val busy by model.busy.collectAsState()
    val voiceError by model.voiceError.collectAsState()
    var languageSelected by rememberSaveable { mutableStateOf(false) }
    var home by rememberSaveable { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val output = remember { OutputFileManager(context) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            home = false
            model.start(uri)
        }
    }
    val creator = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {
        if (it != null) model.generate(it)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            model.startVoice()
        } else model.permissionDenied()
    }
    BackHandler(enabled = !home && !busy && !recording) {
        when (state) {
            is FormUiState.Questioning -> model.coordinator.previousField()
            else -> home = true
        }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().imePadding()) {
            if (!languageSelected) {
                LanguageScreen(onContinue = {
                    model.selectLanguage(it)
                    languageSelected = true
                }, onBack = {})
            } else if (home) {
                HomeScreen(model.language, null) { picker.launch(arrayOf("application/pdf")) }
            } else {
                if (state is FormUiState.Questioning || state is FormUiState.Reviewing) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SupportedLanguage.entries.forEach { language ->
                            TextButton(enabled = !busy && !recording, onClick = { model.selectLanguage(language) }) {
                                Text(when (language) {
                                    SupportedLanguage.ENGLISH -> "English"
                                    SupportedLanguage.HINDI -> "हिन्दी"
                                    SupportedLanguage.MARATHI -> "मराठी"
                                })
                            }
                        }
                    }
                }
                when (val current = state) {
                    FormUiState.Idle -> CircularProgressIndicator()
                    is FormUiState.Parsing -> {
                        CircularProgressIndicator(Modifier.padding(24.dp))
                        Text(current.stageMessage, Modifier.padding(24.dp))
                    }
                    is FormUiState.Questioning -> {
                        if (recording) Text("Recording… tap Stop, or wait up to 10 seconds", Modifier.padding(16.dp))
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        voiceError?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
                        if (recording) Button(onClick = {
                            model.stopVoice()
                        }) { Text("Stop recording") }
                        QuestionScreen(
                            QuestionItem(current.questionText, draft, current.currentField.required, current.validationError),
                            current.questionIndex + 1, current.totalQuestions,
                            model::edit,
                            { model.coordinator.previousField() }, model::submit,
                            { model.coordinator.skipCurrentField() },
                            { permission.launch(Manifest.permission.RECORD_AUDIO) },
                            enabled = !busy && !recording,
                            canSkip = current.canSkip
                        )
                    }
                    is FormUiState.Reviewing -> {
                        ReviewScreen(
                            current.fields.map { field -> ReviewField(field.sourceLabel,
                                current.answers[field.id]?.normalizedValue.orEmpty(),
                                field.confidence < 0.75f, field.type == FieldType.UNKNOWN, field.id) },
                            current.documents.map { ReviewDocument(it.name, it.requirement) },
                            { model.coordinator.jumpToField(it.fieldId) },
                            { creator.launch("Completed_Form.pdf") },
                            enabled = !busy
                        )
                    }
                    is FormUiState.Completed -> {
                        current.warnings.forEach { Text("${it.fieldLabel}: ${it.reason}", Modifier.padding(8.dp)) }
                        ResultScreen(true, current.filename, onOpenPdf = {
                            try { context.startActivity(output.createOpenPdfIntent(current.outputUri)) }
                            catch (_: ActivityNotFoundException) { errorMessage = "Install a PDF viewer to open this file." }
                        }, onSharePdf = {
                            try { context.startActivity(output.createSharePdfIntent(current.outputUri)) }
                            catch (_: ActivityNotFoundException) { errorMessage = "No sharing app is available." }
                        }, onBackHome = { home = true })
                    }
                    is FormUiState.Error -> {
                        Text(current.message, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { picker.launch(arrayOf("application/pdf")) }) { Text("Select another PDF") }
                    }
                }
            }
        }
    }
    errorMessage?.let { message ->
        AlertDialog(onDismissRequest = { errorMessage = null }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } })
    }
}
