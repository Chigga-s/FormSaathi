package com.formsaathi.formengine

import android.graphics.Rect
import com.formsaathi.model.NormalizedRect

fun Rect.toNormalizedRect(
    imageWidth: Int,
    imageHeight: Int
): NormalizedRect {
    if (imageWidth <= 0 || imageHeight <= 0) {
        return NormalizedRect(0f, 0f, 0f, 0f)
    }
    val normalizedLeft = (left.toFloat() / imageWidth).coerceIn(0f, 1f)
    val normalizedTop = (top.toFloat() / imageHeight).coerceIn(0f, 1f)
    val normalizedRight = (right.toFloat() / imageWidth).coerceIn(0f, 1f)
    val normalizedBottom = (bottom.toFloat() / imageHeight).coerceIn(0f, 1f)
    return NormalizedRect(
        left = normalizedLeft,
        top = normalizedTop,
        right = normalizedRight,
        bottom = normalizedBottom
    )
}