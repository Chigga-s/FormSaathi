package com.formsaathi.UI

data class QuestionItem(
    val question: String,
    val answer: String = "",
    val required: Boolean = false,
    val validationMessage: String? = null
)