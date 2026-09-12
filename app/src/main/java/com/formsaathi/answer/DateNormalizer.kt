package com.formsaathi.answer

import java.text.SimpleDateFormat
import java.util.Locale

object DateNormalizer {

    /**
     * Converts various date formats into a canonical DD/MM/YYYY string.
     */
    fun normalize(input: String): String {
        var text = input.trim()
        
        // Remove suffixes like st, nd, rd, th (e.g. 12th -> 12)
        text = text.replace(Regex("(?<=\\d)(st|nd|rd|th)\\b", RegexOption.IGNORE_CASE), "")
                   .replace("Sept ", "Sep ", ignoreCase = true)

        val formats = listOf(
            "dd/MM/yyyy",
            "MM/dd/yyyy", // Depending on locale, but standard in some contexts
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "dd MMMM yyyy", // 12 September 2026
            "dd MMM yyyy",  // 12 Sept 2026
            "MMMM dd yyyy", // September 12 2026
            "MMM dd yyyy"   // Sept 12 2026
        )

        // The target format required by validation and PLAN.md
        val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (formatStr in formats) {
            try {
                val sdf = SimpleDateFormat(formatStr, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(text)
                if (date != null) {
                    return targetFormat.format(date)
                }
            } catch (e: Exception) {
                // Ignore and try the next format
            }
        }
        
        // If all parsing fails, normalize basic separators as a fallback
        return text.replace("-", "/").replace(".", "/")
    }
}
