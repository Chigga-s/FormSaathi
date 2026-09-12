package com.formsaathi.answer

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

object DateNormalizer {
    val canonicalFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    private val formats = listOf(
        "d/M/uuuu", "d-M-uuuu", "d.M.uuuu", "uuuu-MM-dd",
        "d MMMM uuuu", "d MMM uuuu", "MMMM d uuuu", "MMM d uuuu"
    ).map {
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(it)
            .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)
    }

    fun normalize(input: String): String {
        val text = input.trim()
            .replace(Regex("(?<=\\d)(st|nd|rd|th)\\b", RegexOption.IGNORE_CASE), "")
            .replace("Sept ", "Sep ", ignoreCase = true)
        for (format in formats) {
            try {
                return LocalDate.parse(text, format).format(canonicalFormat)
            } catch (_: DateTimeParseException) {
            }
        }
        return input.trim()
    }
}
