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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formsaathi.model.SupportedLanguage

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
    onAttachPhoto: () -> Unit = {},
    isRecording: Boolean = false,
    isTranscribing: Boolean = false,
    language: SupportedLanguage = SupportedLanguage.ENGLISH
) {

    val strings = getQuestionStrings(language)

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
            text = strings.questionCounter(questionNumber, totalQuestions),
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
        // ANSWER FIELD + MICROPHONE / PHOTO ATTACH
        // --------------------------------------------------

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
                    text = if (photoAttached) strings.photoAttached else strings.choosePhoto,
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
                    readOnly = isRecording || isTranscribing,
                    onValueChange = onAnswerChanged,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    singleLine = true,
                    label = {
                        Text(strings.answerLabel)
                    },
                    isError = isInvalid
                )

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                // While recording, this is the Stop control: it stays enabled so
                // the user is never left with a running microphone and no way to
                // end it. During transcription every voice control is disabled so
                // a second recording cannot be started over the first.
                IconButton(
                    onClick = onMicrophone,
                    enabled = if (isRecording) !isTranscribing else enabled,
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            contentDescription = when {
                                isTranscribing -> "Transcribing your answer"
                                isRecording -> "Stop recording"
                                else -> "Answer using microphone"
                            }
                        }
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isRecording) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
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
                text = strings.requiredField,
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
        // NAVIGATION
        //
        // Next / Review is the primary action and gets the full-width
        // prominent button. Previous and Skip are secondary and share the
        // row underneath. Every label is a single short word so it cannot
        // wrap to "Previou s" at narrow widths.
        // --------------------------------------------------

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNext,
                enabled = enabled && !isInvalid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    text = if (questionNumber >= totalQuestions) strings.review else strings.next,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = enabled && questionNumber > 1,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text(
                        text = strings.previous,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }

                TextButton(
                    onClick = onSkip,
                    enabled = enabled && canSkip,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.skip,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }

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
