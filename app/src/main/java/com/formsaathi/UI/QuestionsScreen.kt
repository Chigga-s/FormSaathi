package com.formsaathi.UI

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onMicrophone: () -> Unit,
    enabled: Boolean = true,
    canSkip: Boolean = true,
    photoMode: Boolean = false,
    photoAttached: Boolean = false,
    onAttachPhoto: () -> Unit = {}
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

        if (photoMode) {

            OutlinedButton(
                onClick = onAttachPhoto,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(14.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                Text(
                    text = if (photoAttached) {
                        "Photo attached — tap to change"
                    } else {
                        "Choose photo from gallery"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp
                )
            }
        } else {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = question.answer,
                    enabled = enabled,
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
                    enabled = enabled,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            val buttonShape = RoundedCornerShape(16.dp)
            val buttonPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            val buttonTextSize = 14.sp

            OutlinedButton(
                onClick = onPrevious,
                enabled = enabled && questionNumber > 1,
                shape = buttonShape,
                contentPadding = buttonPadding,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text(
                    text = "Previous",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = buttonTextSize
                )
            }

            Button(
                onClick = onSkip,
                enabled = enabled && canSkip,
                shape = buttonShape,
                contentPadding = buttonPadding,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                )
            ) {

                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = "Skip",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = buttonTextSize,
                    color = Color.Black
                )
            }

            Button(
                onClick = onNext,
                enabled = enabled && !isInvalid,
                shape = buttonShape,
                contentPadding = buttonPadding,
                modifier = Modifier
                    .weight(1.2f)
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
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = buttonTextSize
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )
    }
}
