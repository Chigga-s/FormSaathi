package com.formsaathi

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.formsaathi.UI.FormSaathiNavigation
import com.formsaathi.model.SupportedLanguage

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val navController = rememberNavController()

            var selectedLanguage by remember {
                mutableStateOf(SupportedLanguage.ENGLISH)
            }

            var selectedPdfUri by remember {
                mutableStateOf<Uri?>(null)
            }

            var selectedFileName by remember {
                mutableStateOf<String?>(null)
            }

            /*
             * PDF PICKER
             *
             * This belongs here in MainActivity.
             */
            val pdfPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->

                if (uri != null) {

                    selectedPdfUri = uri

                    selectedFileName = uri.lastPathSegment
                        ?.substringAfterLast("/")
                        ?: "Selected PDF"

                    navController.navigate("processing")
                }
            }

            FormSaathiNavigation(
                navController = navController,

                selectedLanguage = selectedLanguage,

                selectedFileName = selectedFileName,

                onLanguageSelected = { language ->
                    selectedLanguage = language
                },

                onRequestPdf = {
                    pdfPicker.launch(
                        arrayOf("application/pdf")
                    )
                }
            )
        }
    }
}