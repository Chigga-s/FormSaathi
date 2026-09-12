package com.formsaathi

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.formsaathi.UI.FormSaathiNavigation
import com.formsaathi.UI.ProcessingStage
import com.formsaathi.UI.Routes
import com.formsaathi.core.EngineFactory
import com.formsaathi.core.FormSaathiCoordinator
import com.formsaathi.core.FormSessionLauncher
import com.formsaathi.core.FormUiState
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.model.ValidationResult
import com.formsaathi.pdf.OutputFileManager
import com.formsaathi.pdf.SamplePdfFactory
import kotlinx.coroutines.launch

/**
 * Main Activity for FormSaathi.
 * Integrates Role 1's Compose UI flow (Splash -> Language -> Home -> Processing -> Questions -> Review -> Result)
 * with Role 4's Session Coordinator / PDF Generator and Role 2's ML Kit OCR Form Parser.
 * Also provides an accessible Developer Test Harness route for low-level diagnostics.
 */
class MainActivity : ComponentActivity() {

    private lateinit var outputFileManager: OutputFileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        outputFileManager = OutputFileManager(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    var selectedLanguage by remember {
                        mutableStateOf(SupportedLanguage.ENGLISH)
                    }

                    var selectedPdfUri by remember {
                        mutableStateOf<Uri?>(null)
                    }

                    var selectedFileName by remember {
                        mutableStateOf<String?>(null)
                    }

                    val sessionLauncher = remember {
                        FormSessionLauncher { useRealParser ->
                            if (useRealParser) {
                                EngineFactory.createRealCoordinator(context)
                            } else {
                                EngineFactory.createMockCoordinator(
                                    context = context,
                                    useRealPdfGenerator = true
                                )
                            }
                        }
                    }

                    var activeCoordinator by remember {
                        mutableStateOf(sessionLauncher.activeCoordinator)
                    }

                    // SAF Document Picker for Source PDF
                    val pdfPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            selectedPdfUri = uri
                            selectedFileName = uri.lastPathSegment
                                ?.substringAfterLast("/")
                                ?: "Selected_Form.pdf"
                            activeCoordinator = sessionLauncher.startRealSession(uri, selectedLanguage, scope)
                            navController.navigate(Routes.PROCESSING)
                        }
                    }

                    // SAF Document Creator for Output PDF
                    val pdfCreateLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/pdf")
                    ) { outputUri: Uri? ->
                        val targetUri = outputUri ?: run {
                            val cacheFile = outputFileManager.createCachePdfFile(
                                "Completed_${selectedFileName ?: "Form.pdf"}"
                            )
                            outputFileManager.getShareableUri(cacheFile)
                        }
                        scope.launch {
                            activeCoordinator.generatePdf(targetUri)
                        }
                    }

                    FormSaathiNavigation(
                        navController = navController,
                        selectedLanguage = selectedLanguage,
                        selectedFileName = selectedFileName,
                        coordinator = activeCoordinator,
                        outputFileManager = outputFileManager,
                        onLanguageSelected = { lang ->
                            selectedLanguage = lang
                            activeCoordinator.switchLanguage(lang)
                        },
                        onRequestPdf = {
                            pdfPicker.launch(arrayOf("application/pdf"))
                        },
                        onRequestSampleForm = {
                            val sampleFile = SamplePdfFactory.createSamplePdf(context.cacheDir)
                            val sampleUri = Uri.fromFile(sampleFile)
                            selectedPdfUri = sampleUri
                            selectedFileName = sampleFile.name
                            activeCoordinator = sessionLauncher.startSampleSession(sampleUri, selectedLanguage, scope)
                            navController.navigate(Routes.PROCESSING)
                        },
                        onRetryProcessing = {
                            val uri = selectedPdfUri
                            if (uri != null) {
                                activeCoordinator = sessionLauncher.retrySession(uri, selectedLanguage, scope)
                            } else {
                                pdfPicker.launch(arrayOf("application/pdf"))
                            }
                        },
                        onRequestCreatePdf = {
                            pdfCreateLauncher.launch("Completed_${selectedFileName ?: "Form.pdf"}")
                        },
                        onOpenHarness = {
                            navController.navigate(Routes.HARNESS)
                        },
                        harnessScreen = {
                            Role4HarnessScreen(
                                initialCoordinator = activeCoordinator,
                                outputFileManager = outputFileManager,
                                onBackToApp = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Developer Test Harness Screen for inspecting low-level engine details,
 * switching between Fake and Real OCR parsers, and verifying coordinates.
 */
@Composable
fun Role4HarnessScreen(
    initialCoordinator: FormSaathiCoordinator,
    outputFileManager: OutputFileManager,
    onBackToApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var useRealFormParser by remember { mutableStateOf(false) }
    val coordinator = remember(useRealFormParser) {
        if (useRealFormParser) {
            EngineFactory.createRealCoordinator(context)
        } else {
            initialCoordinator
        }
    }
    val uiState by coordinator.uiState.collectAsState()

    // SAF Document Picker for Source PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                coordinator.startSession(uri, SupportedLanguage.ENGLISH)
            }
        }
    }

    // SAF Document Creator for Completed PDF Output
    val pdfCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { outputUri: Uri? ->
        if (outputUri != null) {
            scope.launch {
                coordinator.generatePdf(outputUri)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBackToApp != null) {
            OutlinedButton(onClick = onBackToApp) {
                Text("← Back to FormSaathi App")
            }
        }

        Text(
            text = "FormSaathi (Developer Test Harness)",
            style = MaterialTheme.typography.titleLarge
        )

        when (val state = uiState) {
            is FormUiState.Idle -> {
                Text("Select a government form PDF or start a mock session to test.")

                // Toggle between FakeFormParser and Role 2's RealFormParser (ML Kit OCR)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = useRealFormParser,
                        onCheckedChange = { useRealFormParser = it }
                    )
                    Text(
                        text = if (useRealFormParser) "Parser: Role 2 RealFormParser (ML Kit OCR)" else "Parser: FakeFormParser (Mock Fields)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Text("Pick PDF Form")
                    }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val sampleFile = SamplePdfFactory.createSamplePdf(context.cacheDir)
                            coordinator.startSession(Uri.fromFile(sampleFile), SupportedLanguage.ENGLISH)
                        }
                    }) {
                        Text("Quick Mock Session")
                    }
                }
            }

            is FormUiState.Parsing -> {
                Text("Processing: ${state.stageMessage}")
            }

            is FormUiState.Questioning -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Question ${state.questionIndex + 1} of ${state.totalQuestions}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.questionText,
                            style = MaterialTheme.typography.titleMedium
                        )

                        var typedInput by remember(state.currentField.id) {
                            mutableStateOf(state.currentAnswer?.rawValue ?: "")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = typedInput,
                            onValueChange = { typedInput = it },
                            label = { Text(state.currentField.sourceLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.validationError != null
                        )

                        if (state.validationError != null) {
                            Text(
                                text = state.validationError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    coordinator.submitAnswer(typedInput, AnswerSource.TYPED)
                                }
                            }) {
                                Text("Submit")
                            }

                            if (state.canSkip) {
                                OutlinedButton(onClick = { coordinator.skipCurrentField() }) {
                                    Text("Skip")
                                }
                            }

                            if (state.canGoBack) {
                                OutlinedButton(onClick = { coordinator.previousField() }) {
                                    Text("Back")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { coordinator.switchLanguage(SupportedLanguage.ENGLISH) }) {
                                Text("EN")
                            }
                            OutlinedButton(onClick = { coordinator.switchLanguage(SupportedLanguage.HINDI) }) {
                                Text("HI")
                            }
                            OutlinedButton(onClick = { coordinator.switchLanguage(SupportedLanguage.MARATHI) }) {
                                Text("MR")
                            }
                        }
                    }
                }
            }

            is FormUiState.Reviewing -> {
                Text(
                    text = "Review Answers & Requirements",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Total answers recorded: ${state.answers.size}")

                state.fields.forEach { field ->
                    val existingAnswer = state.answers[field.id]
                    ReviewFieldEditor(
                        fieldId = field.id,
                        fieldLabel = field.sourceLabel,
                        currentValue = existingAnswer?.normalizedValue ?: "",
                        source = existingAnswer?.source?.name ?: "—",
                        onSave = { newText ->
                            coordinator.updateAnswerInReview(field.id, newText)
                        },
                        onJumpToQuestion = {
                            coordinator.jumpToField(field.id)
                        }
                    )
                }

                if (state.documents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Required Documents:", style = MaterialTheme.typography.titleSmall)
                    state.documents.forEach { doc ->
                        Text("• ${doc.name}: ${doc.requirement ?: "Required"}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { pdfCreateLauncher.launch("Completed_Form.pdf") },
                    enabled = !state.isGenerating
                ) {
                    Text(if (state.isGenerating) "Generating PDF..." else "Generate Completed PDF")
                }
            }

            is FormUiState.Completed -> {
                Text(
                    text = "PDF Generated Successfully!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Output: ${state.filename}")

                if (state.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠ Text Fit Warnings (${state.warnings.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    state.warnings.forEach { warning ->
                        Text(
                            text = "• ${warning.fieldLabel}: ${warning.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        try {
                            val openIntent = outputFileManager.createOpenPdfIntent(state.outputUri)
                            context.startActivity(openIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No PDF viewer app found: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Text("Open PDF")
                    }

                    OutlinedButton(onClick = {
                        try {
                            val shareIntent = outputFileManager.createSharePdfIntent(state.outputUri)
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Text("Share PDF")
                    }
                }
            }

            is FormUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = {
                    scope.launch {
                        coordinator.startSession(Uri.EMPTY, SupportedLanguage.ENGLISH)
                    }
                }) {
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * Inline editor for a single field on the Review screen.
 */
@Composable
fun ReviewFieldEditor(
    fieldId: String,
    fieldLabel: String,
    currentValue: String,
    source: String,
    onSave: (String) -> ValidationResult,
    onJumpToQuestion: () -> Unit
) {
    var editing by remember(fieldId) { mutableStateOf(false) }
    var editText by remember(fieldId, currentValue) { mutableStateOf(currentValue) }
    var validationError by remember(fieldId) { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (currentValue.isBlank())
                MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = fieldLabel,
                style = MaterialTheme.typography.labelMedium
            )

            if (editing) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = {
                        editText = it
                        validationError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationError != null,
                    singleLine = true
                )
                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val result = onSave(editText)
                        if (result is ValidationResult.Invalid) {
                            validationError = result.message
                        } else {
                            editing = false
                            validationError = null
                        }
                    }) {
                        Text("Save")
                    }
                    OutlinedButton(onClick = {
                        editing = false
                        editText = currentValue
                        validationError = null
                    }) {
                        Text("Cancel")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentValue.ifBlank { "(not answered)" },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editing = true }) {
                        Text("Edit")
                    }
                    TextButton(onClick = onJumpToQuestion) {
                        Text("Edit in flow")
                    }
                }
            }
        }
    }
}
