package com.formsaathi.answer

import java.math.BigDecimal
import java.util.Locale

object NumberNormalizer {
    private val digitWords = listOf(
        listOf("zero", "शून्य", "शुन्य"), listOf("one", "एक"),
        listOf("two", "दो", "दोन"), listOf("three", "तीन"),
        listOf("four", "चार"), listOf("five", "पांच", "पाँच", "पाच"),
        listOf("six", "छह", "सहा"), listOf("seven", "सात"),
        listOf("eight", "आठ"), listOf("nine", "नौ", "नऊ")
    ).flatMapIndexed { digit, words -> words.map { it to digit } }.toMap()

    private val numberWords = digitWords + mapOf(
        "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
        "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
        "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30,
        "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
        "eighty" to 80, "ninety" to 90, "दस" to 10, "दहा" to 10,
        "बीस" to 20, "वीस" to 20, "तीस" to 30, "चालीस" to 40,
        "चाळीस" to 40, "पचास" to 50, "पन्नास" to 50, "साठ" to 60,
        "सत्तर" to 70, "अस्सी" to 80, "ऐंशी" to 80, "नब्बे" to 90, "नव्वद" to 90
    )

    private val scales = mapOf(
        "hundred" to 100, "सौ" to 100, "शंभर" to 100,
        "thousand" to 1000, "हजार" to 1000, "हज़ार" to 1000,
        "lakh" to 100000, "lakhs" to 100000, "lac" to 100000,
        "lacs" to 100000, "लाख" to 100000,
        "crore" to 10000000, "crores" to 10000000, "करोड़" to 10000000, "कोटी" to 10000000
    )

    private fun asciiDigits(text: String): String = text.map { char ->
        if (char.isDigit()) ('0'.code + Character.digit(char, 10)).toChar() else char
    }.joinToString("")

    fun normalizeDigits(input: String): String {
        val text = asciiDigits(input.trim()).lowercase(Locale.ROOT)
        val tokens = text.split(Regex("[\\s,()-]+" )).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return input.trim()
        val output = StringBuilder()
        for (token in tokens) {
            when {
                token.matches(Regex("[0-9]+")) -> output.append(token)
                digitWords.containsKey(token) -> output.append(digitWords.getValue(token))
                else -> return input.trim()
            }
        }
        return output.toString()
    }

    fun normalize(input: String): String {
        val original = input.trim()
        val text = asciiDigits(original).lowercase(Locale.ROOT)
            .replace("₹", "")
            .replace(Regex("\\b(rs\\.?|rupees|रुपये|रुपए)\\b\\.?"), "")
            .replace(",", "").trim()
        text.toBigDecimalOrNull()?.let { return it.stripTrailingZeros().toPlainString() }
        if (text.startsWith("-")) return original
        var total = BigDecimal.ZERO
        var group = BigDecimal.ZERO
        var seenNumber = false
        for (token in text.split(Regex("[\\s-]+"))) {
            val number = numberWords[token]?.toBigDecimal() ?: token.toBigDecimalOrNull()
            val scale = scales[token]
            when {
                number != null && number.signum() >= 0 -> {
                    group += number
                    seenNumber = true
                }
                scale != null && seenNumber -> {
                    group *= scale.toBigDecimal()
                    if (scale >= 1000) {
                        total += group
                        group = BigDecimal.ZERO
                        seenNumber = false
                    }
                }
                else -> return original
            }
        }
        return (total + group).stripTrailingZeros().toPlainString()
    }
}
