package com.formsaathi.UI

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formsaathi.R
import com.formsaathi.model.SupportedLanguage

/**
 * A form shipped inside the app so the demo runs with no file picker and no
 * network. Both are parsed by the same real pipeline as an imported PDF.
 */
data class DemoForm(
    val assetName: String,
    val displayName: String,
    val description: String
)

val BundledDemoForms = listOf(
    DemoForm(
        assetName = "FormSaathi_Test_Form_1_Simple.pdf",
        displayName = "Citizen Services Application Form",
        description = "Underlined fields, single column"
    ),
    DemoForm(
        assetName = "FormSaathi_Test_Form_2_Boxed.pdf",
        displayName = "General Application Form",
        description = "Printed answer boxes on the right"
    )
)

@Composable
fun HomeScreen(
    language: SupportedLanguage,
    selectedFileName: String?,
    darkTheme: Boolean = false,
    onSelectForm: () -> Unit,
    onChangeLanguage: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onContinue: () -> Unit = {},
    onClearPdf: () -> Unit = {},
    onTryDemoForm: ((DemoForm) -> Unit)? = null,
    onOpenHarness: (() -> Unit)? = null
) {

    val strings = getHomeStrings(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Top controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedButton(
                onClick = onChangeLanguage
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Change language"
                )

                Spacer(
                    modifier = Modifier.size(6.dp)
                )

                Text(
                    text = languageDisplayName(language)
                )
            }

            IconButton(
                onClick = onToggleTheme
            ) {
                Icon(
                    imageVector = if (darkTheme) {
                        Icons.Default.LightMode
                    } else {
                        Icons.Default.DarkMode
                    },
                    contentDescription = if (darkTheme) {
                        "Switch to light mode"
                    } else {
                        "Switch to dark mode"
                    },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Image(
            painter = painterResource(
                id = R.drawable.form_sathi_icon
            ),
            contentDescription = "FormSathi logo",
            modifier = Modifier.size(150.dp)
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "FormSathi",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = strings.subtitle,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = 0.7f
            ),
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(45.dp)
        )

        Button(
            onClick = onSelectForm,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {

            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.size(8.dp)
            )

            Text(
                text = strings.selectForm,
                fontSize = 17.sp
            )
        }

        if (onTryDemoForm != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = when (language) {
                    SupportedLanguage.HINDI -> "या कोई नमूना फ़ॉर्म आज़माएँ"
                    SupportedLanguage.MARATHI -> "किंवा नमुना फॉर्म वापरून पहा"
                    else -> "Or try a bundled form"
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            BundledDemoForms.forEach { demo ->
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onTryDemoForm(demo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = demo.displayName,
                            fontSize = 15.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = demo.description,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        /*
         * This section appears only after
         * the user has selected a PDF.
         */
        if (selectedFileName != null) {

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Text(
                text = strings.selectedForm,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = selectedFileName,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            // Back / Continue buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = onClearPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.size(5.dp)
                    )

                    Text("Back")
                }

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.primary,
                        contentColor =
                            MaterialTheme.colorScheme.onPrimary
                    )
                ) {

                    Text("Continue")

                    Spacer(
                        modifier = Modifier.size(5.dp)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }

        if (onOpenHarness != null) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenHarness) {
                Text(
                    text = "Developer Test Harness",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun languageDisplayName(
    language: SupportedLanguage
): String {

    return when (language) {

        SupportedLanguage.ENGLISH ->
            "English"

        SupportedLanguage.HINDI ->
            "Hindi"

        SupportedLanguage.MARATHI ->
            "Marathi"


    }
}