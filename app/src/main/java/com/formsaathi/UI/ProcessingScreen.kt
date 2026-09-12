package com.formsaathi.UI
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FormSathiPurple = Color(0xFF39277A)
private val FormSathiOrange = Color(0xFFFF8A00)

enum class ProcessingStage {
    RENDERING,
    READING_TEXT,
    PREPARING_QUESTIONS
}

@Composable
fun ProcessingScreen(
    currentStage: ProcessingStage,
    errorMessage: String? = null,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        if (errorMessage == null) {

            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = FormSathiPurple
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Text(
                text = "Processing your form",
                fontSize = 25.sp,
                color = FormSathiPurple
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "Please wait while we prepare your form.",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(35.dp)
            )

            ProcessingStageRow(
                title = "Rendering PDF",
                stage = ProcessingStage.RENDERING,
                currentStage = currentStage
            )

            ProcessingStageRow(
                title = "Reading text",
                stage = ProcessingStage.READING_TEXT,
                currentStage = currentStage
            )

            ProcessingStageRow(
                title = "Preparing questions",
                stage = ProcessingStage.PREPARING_QUESTIONS,
                currentStage = currentStage
            )

            Spacer(
                modifier = Modifier.height(35.dp)
            )

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                )
            ) {
                Text(
                    text = "Cancel",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }

        } else {

            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Processing error",
                modifier = Modifier.size(64.dp),
                tint = Color.Red
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Unable to process form",
                fontSize = 24.sp,
                color = FormSathiPurple
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = errorMessage,
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FormSathiPurple
                )
            ) {
                Text(
                    text = "Try again",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                )
            ) {
                Text(
                    text = "Back to Home",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ProcessingStageRow(
    title: String,
    stage: ProcessingStage,
    currentStage: ProcessingStage
) {

    val stageNumber = stage.ordinal
    val currentNumber = currentStage.ordinal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        when {

            stageNumber < currentNumber -> {

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    modifier = Modifier.size(28.dp),
                    tint = FormSathiPurple
                )
            }

            stageNumber == currentNumber -> {

                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = FormSathiOrange,
                    strokeWidth = 3.dp
                )
            }

            else -> {

                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Pending",
                    modifier = Modifier.size(26.dp),
                    tint = Color.LightGray
                )
            }
        }

        Spacer(
            modifier = Modifier.size(14.dp)
        )

        Text(
            text = title,
            fontSize = 17.sp,
            color = when {

                stageNumber == currentNumber ->
                    Color.Black

                stageNumber < currentNumber ->
                    FormSathiPurple

                else ->
                    Color.Gray
            }
        )
    }
}