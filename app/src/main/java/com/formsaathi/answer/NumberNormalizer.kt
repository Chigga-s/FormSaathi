package com.formsaathi.answer

object NumberNormalizer {
    
    /**
     * Converts a string with Indian numbering terms (lakh, crore, thousand, hundred) 
     * and currency symbols into a canonical numeric string.
     */
    fun normalize(input: String): String {
        var text = input.lowercase().trim()
        
        // Remove currency symbols and commas
        text = text.replace("₹", "")
                   .replace("rs.", "")
                   .replace("rs", "")
                   .replace("rupees", "")
                   .replace(",", "")
                   .trim()

        // Simple check if it's already just digits
        if (text.all { it.isDigit() || it == '.' }) {
            return text.toDoubleOrNull()?.toLong()?.toString() ?: text
        }

        // Basic English word to number mapping
        val wordToNum = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, 
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, 
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13, 
            "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, 
            "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30, 
            "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70, 
            "eighty" to 80, "ninety" to 90
        )

        var result = 0.0
        var currentToken = 0.0
        
        val tokens = text.split(Regex("\\s+|-"))
        
        for (token in tokens) {
            if (token.isEmpty()) continue
            
            // Handle numeric values mixed with words (e.g. "1.5")
            val numValue = token.toDoubleOrNull()
            if (numValue != null) {
                currentToken += numValue
                continue
            }
            
            // Handle number words
            if (wordToNum.containsKey(token)) {
                currentToken += wordToNum[token]!!
                continue
            }
            
            // Handle multipliers
            when (token) {
                "hundred" -> currentToken *= 100
                "thousand" -> {
                    result += currentToken * 1000
                    currentToken = 0.0
                }
                "lakh", "lakhs", "lac", "lacs" -> {
                    result += currentToken * 100000
                    currentToken = 0.0
                }
                "crore", "crores" -> {
                    result += currentToken * 10000000
                    currentToken = 0.0
                }
            }
        }
        
        result += currentToken
        
        return if (result > 0) {
            result.toLong().toString()
        } else {
            // Fallback: keep only numeric digits if parsing failed completely
            text.replace(Regex("[^0-9.]"), "")
        }
    }
}
