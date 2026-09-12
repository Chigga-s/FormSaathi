package com.formsaathi.formengine
import com.formsaathi.model.FieldType

data class FieldMatch(
    val type: FieldType,
    val confidence: Float
)

private data class KeywordRule(
    val type: FieldType,
    val requiredAll: Set<String> = emptySet(),
    val requiredAny: Set<String> = emptySet()
)

class FieldMapper(
    private val labelDetector: LabelDetector = LabelDetector(),    
) {
    private val keywordRules = listOf(
        KeywordRule(
            type = FieldType.FATHER_NAME,
            requiredAll = setOf("father", "name")
        ),

        KeywordRule(
            type = FieldType.MOTHER_NAME,
            requiredAll = setOf("mother", "name")
        ),

        KeywordRule(
            type = FieldType.DATE_OF_BIRTH,
            requiredAll = setOf("date", "birth")
        ),

        KeywordRule(
            type = FieldType.MOBILE,
            requiredAny = setOf("mobile", "phone")
        ),

        KeywordRule(
            type = FieldType.AADHAAR,
            requiredAny = setOf("aadhaar", "aadhar")
        ),

        KeywordRule(
            type = FieldType.PERMANENT_ADDRESS,
            requiredAll = setOf("permanent", "address")
        ),

        KeywordRule(
            type = FieldType.PINCODE,
            requiredAll = setOf("pin", "code")
        ),

        KeywordRule(
            type = FieldType.ANNUAL_INCOME,
            requiredAll = setOf("annual", "income")
        )
    )
    private fun tokens(
    text: String
    ): Set<String> {
        return text
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun tokenSimilarity(
        first: String,
        second: String
    ): Float {
        val firstTokens = tokens(first)
        val secondTokens = tokens(second)
        val intersection = firstTokens.intersect(secondTokens)
        val union = firstTokens.union(secondTokens)
        return if (union.isEmpty()) 0f else intersection.size.toFloat() / union.size.toFloat()
    }

    private fun tokenOverlapMatch(
        text: String
    ): FieldMatch? {
        var bestType = FieldType.UNKNOWN
        var bestScore = 0f
        for ((type, aliases) in FieldAliases.aliases) {
            for (alias in aliases) {
                val score = tokenSimilarity(text, alias)
                if (score > bestScore) {
                    bestScore = score
                    bestType = type
                }
            }
        }
        
        if (bestScore >= 0.7f) {
            return FieldMatch(bestType, 0.80f)
        }
        return null
    }
    private fun strongKeywordMatch(text: String): FieldMatch? {
        for(rule in keywordRules) {
            val allMatches =
                rule.requiredAll.isEmpty() ||
                rule.requiredAll.all { it in text }

            val anyMatches =
                rule.requiredAny.isEmpty() ||
                rule.requiredAny.any { it in text }
            
            if (allMatches && anyMatches) {
                return FieldMatch(rule.type, 0.95f)
            }
        }
        return null
    }
    private fun levenshtein(
    first: String,
    second: String
    ): Int {
        val previous = IntArray(second.length + 1)
        val current = IntArray(second.length + 1)
        for (j in 0..second.length) {
            previous[j] = j
        }
        for (i in 1..first.length) {
            current[0] = i
            for (j in 1..second.length) {
                val substitutionCost = if (first[i - 1] == second[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1, 
                    previous[j] + 1,    
                    previous[j - 1] + substitutionCost 
                )
            }
            System.arraycopy(current, 0, previous, 0, current.size)
        }
        return previous[second.length]
    }
    private fun allowedEditDistance(
    length: Int
    ): Int {
        return when {
            length <= 6 -> 1
            length <= 12 -> 2
            else -> 3
        }
    }
    private fun editDistanceMatch(
    text: String
    ): FieldMatch?{
        var bestType = FieldType.UNKNOWN
        var bestDistance = Int.MAX_VALUE
        var bestAliasLength = 0
        
        for ((type, aliases) in FieldAliases.aliases) {
            for (alias in aliases) {
                if (kotlin.math.abs(alias.length - text.length) > 4) {
                    continue
                }
                val distance = levenshtein(text, alias)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestType = type
                    bestAliasLength = alias.length
                }
            }
        }
        if (bestType != FieldType.UNKNOWN && bestDistance <= allowedEditDistance(bestAliasLength)) {
            return FieldMatch(type = bestType,confidence =  0.65f)
        }
        return null
    }
    fun map(label: String): FieldMatch {
        val normalized = labelDetector.normalizeLabel(label)
        if (normalized.isBlank()) {
            return FieldMatch(FieldType.UNKNOWN, 0f)
        }
        for ((type, aliases) in FieldAliases.aliases) {
            if (normalized in aliases) {
                return FieldMatch(type, 1f)
            }
        }
        val keywordMatch = strongKeywordMatch(normalized)

        if (keywordMatch != null) {
            return keywordMatch
        }
        
        val tokenMatch = tokenOverlapMatch(normalized)

        if (tokenMatch != null) {
            return tokenMatch
        }
        
        val editMatch = editDistanceMatch(normalized)
        
        if (editMatch != null) {
            return editMatch
        }
        
        return FieldMatch(FieldType.UNKNOWN, 0f)
    }
    
}


