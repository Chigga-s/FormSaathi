package com.formsaathi.UI

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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FormSathiPurple = Color(0xFF39277A)

@Composable
fun QuestionScreen(
    question: QuestionItem,
    questionNumber: Int,
    totalQuestions: Int,
    onAnswerChanged: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onMicrophone: () -> Unit
) {

    val isInvalid =
        question.required && question.answer.isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Question $questionNumber of $totalQuestions",
            fontSize = 17.sp,
            color = FormSathiPurple,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(35.dp)
        )

        Text(
            text = question.question,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = question.answer,
                onValueChange = onAnswerChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                singleLine = true,
                label = {
                    Text("Your answer")
                },
                isError = isInvalid
            )

            Spacer(
                modifier = Modifier.size(8.dp)
            )

            IconButton(
                onClick = onMicrophone,
                modifier = Modifier
                    .size(56.dp)
                    .semantics {
                        contentDescription =
                            "Answer using microphone"
                    }
            ) {

                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = FormSathiPurple
                )
            }
        }

        if (isInvalid) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "This field is required.",
                modifier = Modifier.fillMaxWidth(),
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        if (question.validationMessage != null) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = question.validationMessage,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = onPrevious,
                enabled = questionNumber > 1,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text("Previous")
            }

            Button(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                )
            ) {

                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.size(4.dp)
                )

                Text(
                    text = "Skip",
                    color = Color.Black
                )
            }

            Button(
                onClick = onNext,
                enabled = !isInvalid,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FormSathiPurple
                )
            ) {

                Text(
                    text = if (
                        questionNumber == totalQuestions
                    ) {
                        "Review"
                    } else {
                        "Next"
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )
    }
}