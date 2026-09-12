package com.formsaathi.formengine

class LabelDetector {

    fun detect(
        blocks: List<OcrBlock>
    ): List<OcrBlock> {
        return blocks.filter(::isPossibleLabel)
    }

    fun normalizeLabel(
        text: String
    ): String {
        var normalized = text.trim().lowercase()
        normalized = normalized.replace(Regex("\\s+"), " ")
        normalized = normalized.replace(Regex("[:;.]+$"),"")
        return normalized.trim()
    }

    private fun isPossibleLabel(
        block: OcrBlock
    ): Boolean {
        val normalized = normalizeLabel(block.text)
        if (normalized.isBlank()) return false

        val width = block.box.right - block.box.left
        val height = block.box.bottom - block.box.top
        if (width <= 0f || height <= 0f) return false

        if (normalized.length > 80) return false

        val wordCount = normalized.split(Regex("\\s+")).size
        if (wordCount > 12) return false

        if (normalized.matches(Regex("^\\d+$"))) return false
        
        return true
    }
}