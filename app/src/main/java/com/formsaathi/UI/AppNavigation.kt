package com.formsaathi.UI

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.formsaathi.core.FormSaathiCoordinator
import com.formsaathi.core.FormUiState
import com.formsaathi.core.FormViewModel
import com.formsaathi.model.FieldType
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.pdf.OutputFileManager
import com.formsaathi.pdf.SamplePdfFactory
import kotlinx.coroutines.delay

object Routes {
    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val HOME = "home"
    const val PROCESSING = "processing"
    const val QUESTIONS = "questions"
    const val REVIEW = "review"
    const val RESULT = "result"
    const val HARNESS = "harness"
}

@Composable
fun FormSaathiNavigation(
    model: FormViewModel = viewModel(),
    outputFileManager: OutputFileManager? = null
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val state by model.state.collectAsState()
    val draft by model.draft.collectAsState()
    val recording by model.recording.collectAsState()
    val busy by model.busy.collectAsState()
    val voiceError by model.voiceError.collectAsState()
    val output = outputFileManager ?: remember { OutputFileManager(context) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            model.startRealSession(uri)
            navController.navigate(Routes.PROCESSING)
        }
    }

    val pdfCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { outputUri: Uri? ->
        val targetUri = outputUri ?: run {
            val cacheFile = output.createCachePdfFile(
                "Completed_${model.selectedFileName ?: "Form.pdf"}"
            )
            output.getShareableUri(cacheFile)
        }
        model.generate(targetUri)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            model.startVoice()
        } else {
            model.permissionDenied()
        }
    }

    BackHandler(enabled = !busy && !recording) {
        if (navController.previousBackStackEntry != null) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == Routes.QUESTIONS) {
                val currentQ = state as? FormUiState.Questioning
                if (currentQ?.canGoBack == true) {
                    model.previousField()
                } else {
                    navController.popBackStack()
                }
            } else {
                navController.popBackStack()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // ---------------------------------------------------------
        // SPLASH
        // ---------------------------------------------------------
        composable(Routes.SPLASH) {
            SplashScreen()

            LaunchedEffect(Unit) {
                delay(700)
                navController.navigate(Routes.LANGUAGE) {
                    popUpTo(Routes.SPLASH) {
                        inclusive = true
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // LANGUAGE
        // ---------------------------------------------------------
        composable(Routes.LANGUAGE) {
            LanguageScreen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = { language ->
                    model.selectLanguage(language)
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LANGUAGE) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // ---------------------------------------------------------
        // HOME
        // ---------------------------------------------------------
        composable(Routes.HOME) {
            HomeScreen(
                language = model.language,
                selectedFileName = model.selectedFileName,
                onSelectForm = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                },
                onTrySampleForm = {
                    val sampleFile = SamplePdfFactory.createSamplePdf(context.cacheDir)
                    val sampleUri = Uri.fromFile(sampleFile)
                    model.startSampleSession(sampleUri, sampleFile.name)
                    navController.navigate(Routes.PROCESSING)
                },
                onOpenHarness = {
                    navController.navigate(Routes.HARNESS)
                }
            )
        }

        // ---------------------------------------------------------
        // PROCESSING
        // ---------------------------------------------------------
        composable(Routes.PROCESSING) {
            val stage = when (val s = state) {
                is FormUiState.Parsing -> when {
                    s.stageMessage.contains("render", ignoreCase = true) -> ProcessingStage.RENDERING
                    s.stageMessage.contains("question", ignoreCase = true) ||
                    s.stageMessage.contains("prepare", ignoreCase = true) -> ProcessingStage.PREPARING_QUESTIONS
                    s.stageMessage.contains("ocr", ignoreCase = true) ||
                    s.stageMessage.contains("read", ignoreCase = true) ||
                    s.stageMessage.contains("text", ignoreCase = true) ||
                    s.stageMessage.contains("detect", ignoreCase = true) -> ProcessingStage.READING_TEXT
                    else -> ProcessingStage.PREPARING_QUESTIONS
                }
                else -> ProcessingStage.PREPARING_QUESTIONS
            }
            val errorMessage = (state as? FormUiState.Error)?.message

            ProcessingScreen(
                currentStage = stage,
                errorMessage = errorMessage,
                onRetry = {
                    model.retrySession()
                },
                onCancel = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) {
                            inclusive = true
                        }
                    }
                }
            )

            LaunchedEffect(state) {
                if (state is FormUiState.Questioning) {
                    navController.navigate(Routes.QUESTIONS) {
                        popUpTo(Routes.PROCESSING) {
                            inclusive = true
                        }
                    }
                } else if (state is FormUiState.Reviewing) {
                    navController.navigate(Routes.REVIEW) {
                        popUpTo(Routes.PROCESSING) {
                            inclusive = true
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // QUESTIONS
        // ---------------------------------------------------------
        composable(Routes.QUESTIONS) {
            when (val current = state) {
                is FormUiState.Questioning -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Language switcher row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SupportedLanguage.entries.forEach { lang ->
                                TextButton(
                                    enabled = !busy && !recording,
                                    onClick = { model.selectLanguage(lang) }
                                ) {
                                    Text(
                                        text = when (lang) {
                                            SupportedLanguage.ENGLISH -> "English"
                                            SupportedLanguage.HINDI -> "हिन्दी"
                                            SupportedLanguage.MARATHI -> "मराठी"
                                        },
                                        fontWeight = if (model.language == lang) FontWeight.Bold else FontWeight.Normal,
                                        color = if (model.language == lang) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                        }

                        if (recording) {
                            Text(
                                text = "Recording… tap Stop, or wait up to 10 seconds",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                            Button(
                                onClick = { model.stopVoice() },
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Text("Stop recording")
                            }
                        }

                        if (busy) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        voiceError?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }

                        QuestionScreen(
                            question = QuestionItem(
                                question = current.questionText,
                                answer = draft,
                                required = current.currentField.required,
                                validationMessage = current.validationError
                            ),
                            questionNumber = current.questionIndex + 1,
                            totalQuestions = current.totalQuestions,
                            onAnswerChanged = model::edit,
                            onPrevious = { model.previousField() },
                            onNext = { model.submit() },
                            onSkip = { model.skipCurrentField() },
                            onMicrophone = {
                                if (recording) {
                                    model.stopVoice()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            enabled = !busy && !recording,
                            canSkip = current.canSkip
                        )
                    }
                }
                is FormUiState.Reviewing -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.REVIEW) {
                            popUpTo(Routes.QUESTIONS) {
                                inclusive = true
                            }
                        }
                    }
                }
                else -> {
                    // Waiting or transitioning
                }
            }
        }

        // ---------------------------------------------------------
        // REVIEW
        // ---------------------------------------------------------
        composable(Routes.REVIEW) {
            when (val current = state) {
                is FormUiState.Reviewing -> {
                    ReviewScreen(
                        fields = current.fields.map { field ->
                            ReviewField(
                                fieldName = field.sourceLabel,
                                answer = current.answers[field.id]?.normalizedValue.orEmpty(),
                                lowConfidence = field.confidence < 0.75f,
                                unknown = field.type == FieldType.UNKNOWN,
                                fieldId = field.id
                            )
                        },
                        requiredDocuments = current.documents.map { doc ->
                            ReviewDocument(doc.name, doc.requirement)
                        },
                        onEditField = { reviewField ->
                            model.jumpToField(reviewField.fieldId)
                            navController.navigate(Routes.QUESTIONS)
                        },
                        onCreatePdf = {
                            pdfCreateLauncher.launch("Completed_${model.selectedFileName ?: "Form.pdf"}")
                        },
                        enabled = !busy,
                        isGenerating = current.isGenerating || busy
                    )
                }
                is FormUiState.Completed -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.RESULT) {
                            popUpTo(Routes.REVIEW) {
                                inclusive = true
                            }
                        }
                    }
                }
                is FormUiState.Questioning -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.QUESTIONS)
                    }
                }
                else -> {}
            }
        }

        // ---------------------------------------------------------
        // RESULT
        // ---------------------------------------------------------
        composable(Routes.RESULT) {
            when (val current = state) {
                is FormUiState.Completed -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (current.warnings.isNotEmpty()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Notice: Some answer text was automatically formatted to fit inside the form boxes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB35A00)
                                )
                            }
                        }

                        ResultScreen(
                            success = true,
                            fileName = current.filename,
                            onOpenPdf = {
                                try {
                                    val intent = output.createOpenPdfIntent(current.outputUri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "No PDF viewer installed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onSharePdf = {
                                try {
                                    val intent = output.createSharePdfIntent(current.outputUri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Sharing failed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onBackHome = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }
                }
                else -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) {
                                inclusive = true
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // HARNESS (Developer Test Harness)
        // ---------------------------------------------------------
        composable(Routes.HARNESS) {
            Role4HarnessScreen(
                initialCoordinator = model.coordinator,
                outputFileManager = output,
                onBackToApp = {
                    navController.popBackStack()
                }
            )
        }
    }
}
