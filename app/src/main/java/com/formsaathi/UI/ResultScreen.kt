package com.formsaathi.UI

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FormSathiPurple = Color(0xFF39277A)

@Composable
fun ResultScreen(
    success: Boolean,
    fileName: String?,
    errorMessage: String? = null,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onBackHome: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        if (success) {

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "PDF created successfully",
                modifier = Modifier.size(72.dp),
                tint = FormSathiPurple
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "PDF created successfully",
                fontSize = 25.sp,
                color = FormSathiPurple
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = fileName ?: "Completed form.pdf",
                fontSize = 17.sp,
                color = Color.DarkGray
            )

            Spacer(
                modifier = Modifier.height(35.dp)
            )

            Button(
                onClick = onOpenPdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FormSathiPurple
                )
            ) {

                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.height(1.dp)
                )

                Text(
                    text = "Open PDF",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onSharePdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FormSathiPurple
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null
                )

                Text(
                    text = "Share PDF",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

        } else {

            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "PDF creation failed",
                modifier = Modifier.height(72.dp),
                tint = Color.Red
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Could not create PDF",
                fontSize = 25.sp,
                color = FormSathiPurple
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = errorMessage
                    ?: "Something went wrong while creating the completed form.",
                fontSize = 16.sp,
                color = Color.DarkGray
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Button(
            onClick = onBackHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {

            Text(
                text = "Back to Home"
            )
        }
    }
}