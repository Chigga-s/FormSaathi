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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.formsaathi.core.EngineFactory
import com.formsaathi.core.FormSaathiCoordinator
import com.formsaathi.core.FormUiState
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.pdf.OutputFileManager
import kotlinx.coroutines.launch

/**
 * Entry Activity providing an interactive test harness for Role 4.
 * Demonstrates the complete lifecycle: PDF import, session coordination,
 * conditional rules, dynamic review, and completed PDF generation/sharing.
 * Role 1 will replace the screen composables with their polished UI designs.
 */
class MainActivity : ComponentActivity() {

    private lateinit var coordinator: FormSaathiCoordinator
    private lateinit var outputFileManager: OutputFileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        outputFileManager = OutputFileManager(this)
        // Backed by real PDF generator and mock engines until Role 2 & 3 merge
        coordinator = EngineFactory.createMockCoordinator(
            context = this,
            useRealPdfGenerator = true
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Role4HarnessScreen(
                        coordinator = coordinator,
                        outputFileManager = outputFileManager
                    )
                }
            }
        }
    }
}

@Composable
fun Role4HarnessScreen(
    coordinator: FormSaathiCoordinator,
    outputFileManager: OutputFileManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        Text(
            text = "FormSaathi (Role 4 Engine Harness)",
            style = MaterialTheme.typography.titleLarge
        )

        when (val state = uiState) {
            is FormUiState.Idle -> {
                Text("Select a government form PDF or start a mock session to test.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Text("Pick PDF Form")
                    }
                    OutlinedButton(onClick = {
                        scope.launch {
                            // Run mock session using sample file URI
                            val sampleCacheFile = outputFileManager.createCachePdfFile("sample_input.pdf")
                            coordinator.startSession(Uri.fromFile(sampleCacheFile), SupportedLanguage.ENGLISH)
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
                state.answers.forEach { (fieldId, answer) ->
                    Text("• $fieldId: ${answer.normalizedValue} (${answer.source})")
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
