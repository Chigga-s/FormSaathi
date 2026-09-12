package com.formsaathi.UI

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.delay

object Routes {

    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val HOME = "home"
    const val PROCESSING = "processing"
    const val QUESTIONS = "questions"
    const val REVIEW = "review"
    const val RESULT = "result"
}

@Composable
fun FormSaathiNavigation(
    navController: NavHostController,
    selectedLanguage: SupportedLanguage,
    selectedFileName: String?,
    onLanguageSelected: (SupportedLanguage) -> Unit,
    onRequestPdf: () -> Unit,
    onThemeChanged: () -> Unit,
    darkTheme: Boolean,
    onClearPdf: () -> Unit
) {

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // --------------------------------------------------
        // SPLASH
        // --------------------------------------------------

        composable(Routes.SPLASH) {

            SplashScreen()

            LaunchedEffect(Unit) {

                delay(700)

                navController.navigate(
                    Routes.LANGUAGE
                ) {

                    popUpTo(
                        Routes.SPLASH
                    ) {
                        inclusive = true
                    }
                }
            }
        }

        // --------------------------------------------------
        // LANGUAGE
        // --------------------------------------------------

        composable(Routes.LANGUAGE) {

            LanguageScreen(

                onBack = {
                    navController.popBackStack()
                },

                onContinue = { language ->

                    onLanguageSelected(language)

                    navController.navigate(
                        Routes.HOME
                    ) {

                        popUpTo(
                            Routes.LANGUAGE
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // --------------------------------------------------
        // HOME
        // --------------------------------------------------

        composable(Routes.HOME) {

            HomeScreen(

                language = selectedLanguage,

                selectedFileName = selectedFileName,

                darkTheme = darkTheme,

                onSelectForm = {
                    onRequestPdf()
                },

                onChangeLanguage = {

                    navController.navigate(
                        Routes.LANGUAGE
                    )
                },

                onToggleTheme = {
                    onThemeChanged()
                },

                onContinue = {

                    if (selectedFileName != null) {

                        navController.navigate(
                            Routes.PROCESSING
                        )
                    }
                },

                onClearPdf = {
                    onClearPdf()
                }
            )
        }

        // --------------------------------------------------
        // PROCESSING
        // --------------------------------------------------

        composable(Routes.PROCESSING) {

            ProcessingScreen(
                currentStage =
                    ProcessingStage.RENDERING,

                errorMessage = null,

                onRetry = {
                    // Processing engine will be connected later.
                },

                onCancel = {

                    navController.popBackStack()
                }
            )
        }

        // --------------------------------------------------
        // QUESTIONS
        // --------------------------------------------------

        composable(Routes.QUESTIONS) {

            QuestionScreen(

                question = QuestionItem(
                    question = "Sample question",
                    answer = "",
                    required = false,
                    validationMessage = null
                ),

                questionNumber = 1,

                totalQuestions = 1,

                onAnswerChanged = {
                    // Question state will be connected later.
                },

                onPrevious = {
                    navController.popBackStack()
                },

                onNext = {

                    navController.navigate(
                        Routes.REVIEW
                    )
                },

                onSkip = {

                    navController.navigate(
                        Routes.REVIEW
                    )
                },

                onMicrophone = {
                    // Voice input will be connected later.
                }
            )
        }

        // --------------------------------------------------
        // REVIEW
        // --------------------------------------------------

        composable(Routes.REVIEW) {

            ReviewScreen(

                fields = emptyList(),

                requiredDocuments = emptyList(),

                onEditField = {

                    navController.navigate(
                        Routes.QUESTIONS
                    )
                },

                onCreatePdf = {

                    navController.navigate(
                        Routes.RESULT
                    )
                }
            )
        }

        // --------------------------------------------------
        // RESULT
        // --------------------------------------------------

        composable(Routes.RESULT) {

            ResultScreen(

                success = true,

                fileName =
                    selectedFileName
                        ?: "completed_form.pdf",

                onOpenPdf = {
                    // PDF opening will be connected later.
                },

                onSharePdf = {
                    // PDF sharing will be connected later.
                },

                onBackHome = {

                    navController.navigate(
                        Routes.HOME
                    ) {

                        popUpTo(
                            Routes.HOME
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}