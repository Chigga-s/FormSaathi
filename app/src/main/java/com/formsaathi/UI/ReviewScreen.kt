package com.formsaathi.UI

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FormSathiOrange =
    androidx.compose.ui.graphics.Color(0xFFFF8A00)

data class ReviewField(
    val fieldName: String,
    val answer: String,
    val lowConfidence: Boolean = false,
    val unknown: Boolean = false,
    val fieldId: String = "",
    val manualStep: Boolean = false
)

data class ReviewDocument(
    val name: String,
    val constraint: String? = null
)

@Composable
fun ReviewScreen(
    fields: List<ReviewField>,
    requiredDocuments: List<ReviewDocument>,
    onEditField: (ReviewField) -> Unit,
    onCreatePdf: () -> Unit,
    enabled: Boolean = true,
    isGenerating: Boolean = false
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            text = "Review your answers",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Check the information before creating your completed PDF.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = 0.7f
            )
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (requiredDocuments.isNotEmpty()) {

                item {
                    Text(
                        text = "Attach these documents with your form",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(requiredDocuments) { document ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = document.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (document.constraint != null) {

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = document.constraint,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.7f
                                    )
                                )
                            }
                        }
                    }
                }

                item {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Your answers",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(fields) { field ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = field.fieldName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = if (field.manualStep) {
                                        "Photo attached — use Edit in flow to change"
                                    } else if (field.answer.isBlank()) {
                                        "No answer"
                                    } else {
                                        field.answer
                                    },
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (field.lowConfidence) {

                                    Spacer(
                                        modifier = Modifier.height(6.dp)
                                    )

                                    Text(
                                        text = "Please verify this answer.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                if (field.unknown) {

                                    Spacer(
                                        modifier = Modifier.height(6.dp)
                                    )

                                    Text(
                                        text = "Manual review required.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Button(
                                enabled = enabled,
                                onClick = {
                                    onEditField(field)
                                },
                                modifier = Modifier.height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit"
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = onCreatePdf,
            enabled = enabled && !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FormSathiOrange,
                contentColor = androidx.compose.ui.graphics.Color.White
            )
        ) {

            Text(
                text = if (isGenerating) "Creating PDF..." else "Create completed PDF",
                fontSize = 17.sp,
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}
