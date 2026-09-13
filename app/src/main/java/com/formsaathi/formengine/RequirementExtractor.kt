package com.formsaathi.formengine

import com.formsaathi.model.RequiredDocument

class RequirementExtractor {

    private companion object {
        const val NEARBY_VERTICAL_DISTANCE = 0.05f
    }

    /**
     * Only full document names appear here. Bare keywords used to be aliases too,
     * which turned ordinary field labels into attachment requirements: an
     * "Aadhaar Number" field became a required Aadhaar card, and an
     * "Applicant's Signature" caption became a required signature document.
     */
    private val documentAliases = mapOf(
        "Aadhaar Card" to setOf(
            "aadhaar card",
            "aadhar card",
            "aadhaar card copy",
            "copy of aadhaar card"
        ),

        "PAN Card" to setOf(
            "pan card",
            "copy of pan card"
        ),

        "Income Certificate" to setOf(
            "income certificate",
            "income proof"
        ),

        "Photograph" to setOf(
            "photograph",
            "passport size photo",
            "passport size photograph",
            "passport photo"
        ),

        "Residence Certificate" to setOf(
            "residence certificate",
            "residential certificate",
            "domicile certificate",
            "address proof"
        ),

        "Signature" to setOf(
            "specimen signature",
            "attested signature",
            "signature proof"
        )
    )

    private val requirementPatterns = listOf(
        Regex("\\bself[- ]?attested\\b"),
        Regex("\\boriginal\\b"),
        Regex("\\bphotocopy\\b"),
        Regex("\\bcopy\\b"),
        Regex("\\bcopies\\b"),
        Regex("\\bpassport[- ]?size\\b"),
        Regex("\\b\\d+\\s*(copy|copies)\\b"),
        Regex("\\b(pdf|jpg|jpeg|png)\\b"),
        Regex("\\b\\d+(\\.\\d+)?\\s*(kb|mb)\\b")
    )

    fun extract(
        blocks: List<OcrBlock>
    ): List<RequiredDocument> {

        val found = linkedMapOf<String, MutableSet<String>>()

        for (block in blocks) {
            val normalized = normalizeText(block.text)

            for ((documentName, aliases) in documentAliases) {

                val matched = aliases.any { alias ->
                    containsAlias(normalized, alias)
                }

                if (!matched) {
                    continue
                }

                val requirements = found.getOrPut(documentName) {
                    linkedSetOf()
                }

                extractRequirements(normalized)
                    .forEach(requirements::add)

                nearbyBlocks(block, blocks)
                    .forEach { nearby ->
                        extractRequirements(normalizeText(nearby.text))
                            .forEach(requirements::add)
                    }
            }
        }

        return found.map { (name, requirements) ->
            RequiredDocument(
                name = name,
                requirement = requirements
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ")
            )
        }
    }

    private fun normalizeText(
        text: String
    ): String {
        return text
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }

    private fun containsAlias(
        text: String,
        alias: String
    ): Boolean {
        val pattern = Regex(
            "(^|\\W)${Regex.escape(alias)}($|\\W)"
        )

        return pattern.containsMatchIn(text)
    }

    private fun extractRequirements(
    text: String
    ): List<String> {

        val requirements = mutableListOf<String>()

        val countedCopies = Regex("\\b\\d+\\s*(copy|copies)\\b")
            .findAll(text)
            .map { it.value }
            .toList()

        requirements.addAll(countedCopies)

        for (pattern in requirementPatterns) {
            if (
                countedCopies.isNotEmpty() &&
                (
                    pattern.pattern == "\\bcopy\\b" ||
                    pattern.pattern == "\\bcopies\\b" ||
                    pattern.pattern == "\\b\\d+\\s*(copy|copies)\\b"
                )
            ) {
                continue
            }

            pattern.findAll(text).forEach { match ->
                requirements.add(match.value)
            }
        }

        return requirements.distinct()
    }

    private fun nearbyBlocks(
        target: OcrBlock,
        allBlocks: List<OcrBlock>
    ): List<OcrBlock> {

        return allBlocks.filter { block ->

            if (block.pageIndex != target.pageIndex) {
                return@filter false
            }

            if (
                block.blockIndex == target.blockIndex &&
                block.lineIndex == target.lineIndex
            ) {
                return@filter false
            }

            val distanceBelow =
                block.box.top - target.box.bottom

            val distanceAbove =
                target.box.top - block.box.bottom

            val verticallyNearby =
                distanceBelow in 0f..NEARBY_VERTICAL_DISTANCE ||
                distanceAbove in 0f..NEARBY_VERTICAL_DISTANCE

            verticallyNearby
        }
    }
}