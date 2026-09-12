package com.formsaathi.UI

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formsaathi.R
import com.formsaathi.model.SupportedLanguage

private val FormSathiPurple = Color(0xFF39277A)

@Composable
fun HomeScreen(
    language: SupportedLanguage,
    selectedFileName: String?,
    onSelectForm: () -> Unit,
    onTrySampleForm: (() -> Unit)? = null,
    onOpenHarness: (() -> Unit)? = null
) {

    val strings = getHomeStrings(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Top
    ) {

        Spacer(
            modifier = Modifier.height(40.dp)
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
            color = FormSathiPurple,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = strings.subtitle,
            color = Color.DarkGray,
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(50.dp)
        )

        Button(
            onClick = onSelectForm,

            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = FormSathiPurple
            )
        ) {

            Text(
                text = strings.selectForm,
                color = Color.White,
                fontSize = 17.sp
            )
        }

        if (onTrySampleForm != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onTrySampleForm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = when (language) {
                        SupportedLanguage.HINDI -> "नमूना फ़ॉर्म आज़माएँ (त्वरित डेमो)"
                        SupportedLanguage.MARATHI -> "नमुना फॉर्म वापरून पहा (डेमो)"
                        else -> "Try Sample Form (Quick Demo)"
                    },
                    fontSize = 16.sp,
                    color = FormSathiPurple
                )
            }
        }

        if (selectedFileName != null) {

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Text(
                text = strings.selectedForm,
                color = FormSathiPurple,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = selectedFileName,
                fontSize = 16.sp
            )
        }

        if (onOpenHarness != null) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenHarness) {
                Text(
                    text = "Developer Test Harness",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}