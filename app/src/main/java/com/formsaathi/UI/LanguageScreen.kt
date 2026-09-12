package com.formsaathi.UI

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formsaathi.model.SupportedLanguage

private val FormSathiOrange = androidx.compose.ui.graphics.Color(0xFFFF8A00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onContinue: (SupportedLanguage) -> Unit,
    onBack: () -> Unit
) {

    var selectedLanguage by remember {
        mutableStateOf(SupportedLanguage.ENGLISH)
    }

    val languages = listOf(
        SupportedLanguage.ENGLISH,
        SupportedLanguage.HINDI,
        SupportedLanguage.MARATHI
    )

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "Choose Language",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                )
                .verticalScroll(
                    rememberScrollState()
                ),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Select your preferred language",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            languages.forEach { language ->

                LanguageCard(
                    language = language,
                    selected = language == selectedLanguage,
                    onClick = {
                        selectedLanguage = language
                    }
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = {
                    onContinue(selectedLanguage)
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),

                shape = RoundedCornerShape(12.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = FormSathiOrange
                )
            ) {

                Text(
                    text = "Continue in ${languageDisplayName(selectedLanguage)}",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 17.sp
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}

@Composable
private fun LanguageCard(
    language: SupportedLanguage,
    selected: Boolean,
    onClick: () -> Unit
) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape = RoundedCornerShape(12.dp),

        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            RadioButton(
                selected = selected,
                onClick = onClick
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column {

                Text(
                    text = languageDisplayName(language),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = languageNativeName(language),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.65f
                    )
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

private fun languageNativeName(
    language: SupportedLanguage
): String {

    return when (language) {

        SupportedLanguage.ENGLISH ->
            "English"

        SupportedLanguage.HINDI ->
            "हिन्दी"

        SupportedLanguage.MARATHI ->
            "मराठी"


    }
}