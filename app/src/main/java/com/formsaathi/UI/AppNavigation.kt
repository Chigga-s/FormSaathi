package com.formsaathi.UI

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.formsaathi.core.EngineFactory
import com.formsaathi.core.FormSaathiCoordinator
import com.formsaathi.core.FormUiState
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.pdf.OutputFileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    navController: NavHostController,
    selectedLanguage: SupportedLanguage,
    selectedFileName: String?,
    onLanguageSelected: (SupportedLanguage) -> Unit,
    onRequestPdf: () -> Unit,
    coordinator: FormSaathiCoordinator? = null,
    outputFileManager: OutputFileManager? = null,
    onRequestSampleForm: (() -> Unit)? = null,
    onRequestCreatePdf: (() -> Unit)? = null,
    onOpenHarness: (() -> Unit)? = null,
    harnessScreen: (@Composable () -> Unit)? = null,
    onRetryProcessing: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeCoordinator = coordinator ?: remember {
        EngineFactory.createMockCoordinator(context, useRealPdfGenerator = true)
    }
    val activeOutputManager = outputFileManager ?: remember {
        OutputFileManager(context)
    }
    val uiState by activeCoordinator.uiState.collectAsState()

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
                    onLanguageSelected(language)
                    activeCoordinator.switchLanguage(language)
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
                language = selectedLanguage,
                selectedFileName = selectedFileName,
                onSelectForm = onRequestPdf,
                onTrySampleForm = onRequestSampleForm,
                onOpenHarness = onOpenHarness
            )
        }

        // ---------------------------------------------------------
        // PROCESSING
        // ---------------------------------------------------------
        composable(Routes.PROCESSING) {
            val state = uiState
            val stage = when (state) {
                is FormUiState.Parsing -> when {
                    state.stageMessage.contains("render", ignoreCase = true) -> ProcessingStage.RENDERING
                    state.stageMessage.contains("question", ignoreCase = true) ||
                    state.stageMessage.contains("prepare", ignoreCase = true) -> ProcessingStage.PREPARING_QUESTIONS
                    state.stageMessage.contains("ocr", ignoreCase = true) ||
                    state.stageMessage.contains("read", ignoreCase = true) ||
                    state.stageMessage.contains("text", ignoreCase = true) ||
                    state.stageMessage.contains("detect", ignoreCase = true) -> ProcessingStage.READING_TEXT
                    else -> ProcessingStage.PREPARING_QUESTIONS
                }
                else -> ProcessingStage.PREPARING_QUESTIONS
            }
            val errorMessage = (state as? FormUiState.Error)?.message

            ProcessingScreen(
                currentStage = stage,
                errorMessage = errorMessage,
                onRetry = {
                    onRetryProcessing?.invoke() ?: onRequestSampleForm?.invoke()
                },
                onCancel = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) {
                            inclusive = true
                        }
                    }
                }
            )

            LaunchedEffect(uiState) {
                if (uiState is FormUiState.Questioning) {
                    navController.navigate(Routes.QUESTIONS) {
                        popUpTo(Routes.PROCESSING) {
                            inclusive = true
                        }
                    }
                } else if (uiState is FormUiState.Reviewing) {
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
            val state = uiState as? FormUiState.Questioning
            if (state != null) {
                var typedAnswer by remember(state.currentField.id) {
                    mutableStateOf(state.currentAnswer?.rawValue ?: "")
                }

                QuestionScreen(
                    question = QuestionItem(
                        question = state.questionText,
                        answer = typedAnswer,
                        required = state.currentField.required,
                        validationMessage = state.validationError
                    ),
                    questionNumber = state.questionIndex + 1,
                    totalQuestions = state.totalQuestions,
                    onAnswerChanged = { typedAnswer = it },
                    onPrevious = {
                        activeCoordinator.previousField()
                    },
                    onNext = {
                        scope.launch {
                            activeCoordinator.submitAnswer(typedAnswer, AnswerSource.TYPED)
                        }
                    },
                    onSkip = {
                        activeCoordinator.skipCurrentField()
                    },
                    onMicrophone = {
                        Toast.makeText(
                            context,
                            "Voice input: Listening simulated. Type answer or proceed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            LaunchedEffect(uiState) {
                if (uiState is FormUiState.Reviewing) {
                    navController.navigate(Routes.REVIEW) {
                        popUpTo(Routes.QUESTIONS) {
                            inclusive = true
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // REVIEW
        // ---------------------------------------------------------
        composable(Routes.REVIEW) {
            val state = uiState as? FormUiState.Reviewing
            if (state != null) {
                val reviewFields = state.fields.map { field ->
                    val ans = state.answers[field.id]
                    ReviewField(
                        fieldName = field.sourceLabel,
                        answer = ans?.normalizedValue ?: "",
                        lowConfidence = field.confidence in 0.01f..0.7f,
                        unknown = ans == null
                    )
                }

                val reviewDocs = state.documents.map { doc ->
                    ReviewDocument(
                        name = doc.name,
                        constraint = doc.requirement
                    )
                }

                ReviewScreen(
                    fields = reviewFields,
                    requiredDocuments = reviewDocs,
                    onEditField = { reviewField ->
                        val targetField = state.fields.find { it.sourceLabel == reviewField.fieldName }
                            ?: state.fields.firstOrNull()
                        if (targetField != null) {
                            activeCoordinator.jumpToField(targetField.id)
                            navController.navigate(Routes.QUESTIONS)
                        }
                    },
                    onCreatePdf = {
                        if (onRequestCreatePdf != null) {
                            onRequestCreatePdf()
                        } else {
                            val targetFile = activeOutputManager.createCachePdfFile(
                                "Completed_${selectedFileName ?: "Form.pdf"}"
                            )
                            val targetUri = activeOutputManager.getShareableUri(targetFile)
                            scope.launch {
                                activeCoordinator.generatePdf(targetUri)
                            }
                        }
                    },
                    isGenerating = state.isGenerating
                )
            }

            LaunchedEffect(uiState) {
                if (uiState is FormUiState.Completed) {
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.REVIEW) {
                            inclusive = true
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // RESULT
        // ---------------------------------------------------------
        composable(Routes.RESULT) {
            val state = uiState as? FormUiState.Completed
            val errorState = uiState as? FormUiState.Error

            ResultScreen(
                success = state != null,
                fileName = state?.filename ?: selectedFileName ?: "completed_form.pdf",
                errorMessage = errorState?.message,
                onOpenPdf = {
                    if (state != null) {
                        try {
                            val openIntent = activeOutputManager.createOpenPdfIntent(state.outputUri)
                            context.startActivity(openIntent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "No PDF viewer app found: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onSharePdf = {
                    if (state != null) {
                        try {
                            val shareIntent = activeOutputManager.createSharePdfIntent(state.outputUri)
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Sharing failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
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

        // ---------------------------------------------------------
        // HARNESS (Developer Debug Screen)
        // ---------------------------------------------------------
        if (harnessScreen != null) {
            composable(Routes.HARNESS) {
                harnessScreen()
            }
        }
    }
}