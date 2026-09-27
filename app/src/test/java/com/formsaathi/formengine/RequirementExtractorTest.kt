package com.formsaathi.formengine

import com.formsaathi.model.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Test

class RequirementExtractorTest {

    @Test
    fun aadhaarWithSelfAttested_extractsDocumentAndRequirement() {
        val block = OcrBlock(
            text = "Self-attested Aadhaar Card",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.50f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val extractor = RequirementExtractor()

        val result = extractor.extract(
            blocks = listOf(block)
        )

        assertEquals(1, result.size)
        assertEquals("Aadhaar Card", result[0].name)
        assertEquals("self-attested", result[0].requirement)
    }
    @Test
    fun nearbyRequirementLine_isAttachedToDocument() {
        val documentBlock = OcrBlock(
            text = "Aadhaar Card",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.40f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val requirementBlock = OcrBlock(
            text = "2 copies",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.25f,
                right = 0.30f,
                bottom = 0.28f
            ),
            confidence = 0.95f,
            blockIndex = 1,
            lineIndex = 0
        )

        val extractor = RequirementExtractor()

        val result = extractor.extract(
            blocks = listOf(
                documentBlock,
                requirementBlock
            )
        )

        assertEquals(1, result.size)
        assertEquals("Aadhaar Card", result[0].name)
        assertEquals("2 copies", result[0].requirement)
    }
    
    @Test
    fun repeatedDocument_isDeduplicated() {
        val first = OcrBlock(
            text = "Aadhaar Card",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.40f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val second = OcrBlock(
            text = "Aadhaar Card",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.40f,
                right = 0.40f,
                bottom = 0.44f
            ),
            confidence = 0.95f,
            blockIndex = 1,
            lineIndex = 0
        )

        val extractor = RequirementExtractor()

        val result = extractor.extract(
            blocks = listOf(first, second)
        )

        assertEquals(1, result.size)
        assertEquals("Aadhaar Card", result[0].name)
    }
    
    @Test
    fun requirementOnDifferentPage_isIgnored() {
        val documentBlock = OcrBlock(
            text = "Aadhaar Card",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.40f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val otherPageRequirement = OcrBlock(
            text = "Self-attested",
            pageIndex = 1,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.25f,
                right = 0.30f,
                bottom = 0.28f
            ),
            confidence = 0.95f,
            blockIndex = 1,
            lineIndex = 0
        )

        val extractor = RequirementExtractor()

        val result = extractor.extract(
            blocks = listOf(
                documentBlock,
                otherPageRequirement
            )
        )

        assertEquals(1, result.size)
        assertEquals("Aadhaar Card", result[0].name)
        assertEquals(null, result[0].requirement)
    }
    
    @Test
    fun multipleDifferentDocuments_areExtracted() {
        val aadhaar = OcrBlock(
            text = "Self-attested Aadhaar Card",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.50f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val pan = OcrBlock(
            text = "PAN Card",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.40f,
                right = 0.40f,
                bottom = 0.44f
            ),
            confidence = 0.95f,
            blockIndex = 1,
            lineIndex = 0
        )

        val incomeCertificate = OcrBlock(
            text = "Original Income Certificate",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.60f,
                right = 0.55f,
                bottom = 0.64f
            ),
            confidence = 0.95f,
            blockIndex = 2,
            lineIndex = 0
        )

        val extractor = RequirementExtractor()

        val result = extractor.extract(
            blocks = listOf(
                aadhaar,
                pan,
                incomeCertificate
            )
        )

        assertEquals(3, result.size)

        val byName = result.associateBy { it.name }

        assertEquals(
            "self-attested",
            byName["Aadhaar Card"]?.requirement
        )

        assertEquals(
            null,
            byName["PAN Card"]?.requirement
        )

        assertEquals(
            "original",
            byName["Income Certificate"]?.requirement
        )
    }
}
