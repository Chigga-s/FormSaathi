package com.formsaathi.UI

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --------------------------------------------------
        // QUESTION NUMBER
        // --------------------------------------------------

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Question $questionNumber of $totalQuestions",
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // --------------------------------------------------
        // QUESTION
        // --------------------------------------------------

        Spacer(
            modifier = Modifier.height(35.dp)
        )

        Text(
            text = question.question,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // --------------------------------------------------
        // ANSWER FIELD + MICROPHONE
        // --------------------------------------------------

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
                modifier = Modifier.width(8.dp)
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // --------------------------------------------------
        // REQUIRED FIELD ERROR
        // --------------------------------------------------

        if (isInvalid) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "This field is required.",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }

        // --------------------------------------------------
        // VALIDATION MESSAGE
        // --------------------------------------------------

        if (question.validationMessage != null) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = question.validationMessage,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }

        // --------------------------------------------------
        // PUSH BUTTONS TO BOTTOM
        // --------------------------------------------------

        Spacer(
            modifier = Modifier.weight(1f)
        )

        // --------------------------------------------------
        // NAVIGATION BUTTONS
        //
        // Previous = full width
        // Skip + Next = half width each
        // --------------------------------------------------

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ----------------------------------------------
            // PREVIOUS
            // ----------------------------------------------

            Button(
                onClick = onPrevious,
                enabled = questionNumber > 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor =
                        MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor =
                        MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(
                    horizontal = 16.dp
                )
            ) {

                Text(
                    text = "Previous",
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }

            // ----------------------------------------------
            // SKIP + NEXT
            // ----------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ------------------------------------------
                // SKIP
                // ------------------------------------------

                Button(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 8.dp
                    )
                ) {

                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = "Skip",
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                }

                // ------------------------------------------
                // NEXT / REVIEW
                // ------------------------------------------

                Button(
                    onClick = onNext,
                    enabled = !isInvalid,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.primary,
                        contentColor =
                            MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 8.dp
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
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // --------------------------------------------------
        // BOTTOM SPACING
        // --------------------------------------------------

        Spacer(
            modifier = Modifier.height(16.dp)
        )
    }
}


// ==========================================================
// LIGHT MODE PREVIEW
// ==========================================================

@Preview(
    showBackground = true,
    name = "Light Mode"
)
@Composable
fun QuestionScreenLightPreview() {

    FormSaathiTheme(
        darkTheme = false
    ) {

        QuestionScreen(
            question = QuestionItem(
                question = "What is your full name?",
                answer = "",
                required = true,
                validationMessage = null
            ),
            questionNumber = 1,
            totalQuestions = 5,
            onAnswerChanged = {},
            onPrevious = {},
            onNext = {},
            onSkip = {},
            onMicrophone = {}
        )
    }
}


// ==========================================================
// DARK MODE PREVIEW
// ==========================================================

@Preview(
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun QuestionScreenDarkPreview() {

    FormSaathiTheme(
        darkTheme = true
    ) {

        QuestionScreen(
            question = QuestionItem(
                question = "What is your full name?",
                answer = "",
                required = true,
                validationMessage = null
            ),
            questionNumber = 1,
            totalQuestions = 5,
            onAnswerChanged = {},
            onPrevious = {},
            onNext = {},
            onSkip = {},
            onMicrophone = {}
        )
    }
}