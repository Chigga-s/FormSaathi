package com.formsaathi.formengine

import com.formsaathi.model.NormalizedRect

data class OcrBlock(
    val text: String,
    val pageIndex: Int,
    val box: NormalizedRect,
    val confidence: Float?,
    val blockIndex: Int,
    val lineIndex: Int
)