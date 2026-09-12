package com.formsaathi.core

import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConversationEngineTest {

    private lateinit var engine: ConversationEngine
    private lateinit var sampleFields: List<FormField>

    @Before
    fun setUp() {
        engine = ConversationEngine()

        sampleFields = listOf(
            FormField(
                id = "f_name",
                sourceLabel = "Name",
                type = FieldType.FULL_NAME,
                pageIndex = 0,
                labelBox = NormalizedRect(0.1f, 0.1f, 0.3f, 0.15f),
                answerBox = NormalizedRect(0.35f, 0.1f, 0.8f, 0.15f)
            ),
            FormField(
                id = "f_perm_addr",
                sourceLabel = "Permanent Address",
                type = FieldType.PERMANENT_ADDRESS,
                pageIndex = 0,
                labelBox = NormalizedRect(0.1f, 0.2f, 0.3f, 0.25f),
                answerBox = NormalizedRect(0.35f, 0.2f, 0.8f, 0.35f)
            ),
            FormField(
                id = "f_same_addr",
                sourceLabel = "Same Address?",
                type = FieldType.SAME_AS_PERMANENT_ADDRESS,
                pageIndex = 0,
                labelBox = NormalizedRect(0.1f, 0.4f, 0.4f, 0.45f),
                answerBox = NormalizedRect(0.45f, 0.4f, 0.7f, 0.45f)
            ),
            FormField(
                id = "f_curr_addr",
                sourceLabel = "Current Address",
                type = FieldType.CURRENT_ADDRESS,
                pageIndex = 0,
                labelBox = NormalizedRect(0.1f, 0.5f, 0.3f, 0.55f),
                answerBox = NormalizedRect(0.35f, 0.5f, 0.8f, 0.65f)
            ),
            FormField(
                id = "f_mobile",
                sourceLabel = "Mobile",
                type = FieldType.MOBILE,
                pageIndex = 0,
                labelBox = NormalizedRect(0.1f, 0.7f, 0.3f, 0.75f),
                answerBox = NormalizedRect(0.35f, 0.7f, 0.7f, 0.75f)
            )
        )
    }

    @Test
    fun testSameAddressCopiesAnswerWhenYes() {
        val permAnswer = FormAnswer(
            fieldId = "f_perm_addr",
            rawValue = "123 Main St, New Delhi",
            normalizedValue = "123 Main St, New Delhi",
            source = AnswerSource.TYPED
        )
        val sameAddrAnswer = FormAnswer(
            fieldId = "f_same_addr",
            rawValue = "yes",
            normalizedValue = "yes",
            source = AnswerSource.TYPED
        )

        val answers = mapOf("f_perm_addr" to permAnswer, "f_same_addr" to sameAddrAnswer)
        val sameField = sampleFields.first { it.type == FieldType.SAME_AS_PERMANENT_ADDRESS }

        val automated = engine.evaluateAutomatedAnswers(sameField, sameAddrAnswer, sampleFields, answers)
        assertEquals(1, automated.size)
        val copied = automated.first()
        assertEquals("f_curr_addr", copied.fieldId)
        assertEquals("123 Main St, New Delhi", copied.normalizedValue)
        assertEquals(AnswerSource.COPIED_BY_RULE, copied.source)
    }

    @Test
    fun testSameAddressDoesNotCopyWhenNo() {
        val permAnswer = FormAnswer(
            fieldId = "f_perm_addr",
            rawValue = "123 Main St, New Delhi",
            normalizedValue = "123 Main St, New Delhi",
            source = AnswerSource.TYPED
        )
        val sameAddrAnswer = FormAnswer(
            fieldId = "f_same_addr",
            rawValue = "no",
            normalizedValue = "no",
            source = AnswerSource.TYPED
        )

        val answers = mapOf("f_perm_addr" to permAnswer, "f_same_addr" to sameAddrAnswer)
        val sameField = sampleFields.first { it.type == FieldType.SAME_AS_PERMANENT_ADDRESS }

        val automated = engine.evaluateAutomatedAnswers(sameField, sameAddrAnswer, sampleFields, answers)
        assertTrue(automated.isEmpty())
    }

    @Test
    fun testCurrentAddressSkippedWhenSameAddressIsYes() {
        val currentField = sampleFields.first { it.type == FieldType.CURRENT_ADDRESS }
        val answers = mapOf(
            "f_same_addr" to FormAnswer("f_same_addr", "yes", "yes", AnswerSource.TYPED)
        )

        val shouldSkip = engine.shouldSkipField(currentField, sampleFields, answers)
        assertTrue(shouldSkip)
    }

    @Test
    fun testCurrentAddressNotSkippedWhenSameAddressIsNo() {
        val currentField = sampleFields.first { it.type == FieldType.CURRENT_ADDRESS }
        val answers = mapOf(
            "f_same_addr" to FormAnswer("f_same_addr", "no", "no", AnswerSource.TYPED)
        )

        val shouldSkip = engine.shouldSkipField(currentField, sampleFields, answers)
        assertFalse(shouldSkip)
    }

    @Test
    fun testGetNextFieldIndexSkipsCurrentAddressDirectlyToMobile() {
        val answers = mapOf(
            "f_same_addr" to FormAnswer("f_same_addr", "yes", "yes", AnswerSource.TYPED)
        )
        // From same address question (index 2), next should skip current address (index 3) and land on mobile (index 4)
        val nextIndex = engine.getNextFieldIndex(fromIndex = 2, fields = sampleFields, currentAnswers = answers)
        assertEquals(4, nextIndex)
    }

    @Test
    fun testNonAddressFieldsAreNeverSkipped() {
        // Even with "same as permanent" answered yes, non-address fields must never be skipped
        val answers = mapOf(
            "f_same_addr" to FormAnswer("f_same_addr", "yes", "yes", AnswerSource.TYPED)
        )

        val nameField = sampleFields.first { it.type == FieldType.FULL_NAME }
        assertFalse("Name should never be skipped", engine.shouldSkipField(nameField, sampleFields, answers))

        val permAddrField = sampleFields.first { it.type == FieldType.PERMANENT_ADDRESS }
        assertFalse("Permanent address should never be skipped", engine.shouldSkipField(permAddrField, sampleFields, answers))

        val mobileField = sampleFields.first { it.type == FieldType.MOBILE }
        assertFalse("Mobile should never be skipped", engine.shouldSkipField(mobileField, sampleFields, answers))

        val sameAsField = sampleFields.first { it.type == FieldType.SAME_AS_PERMANENT_ADDRESS }
        assertFalse("Same-as question itself should never be skipped", engine.shouldSkipField(sameAsField, sampleFields, answers))
    }

    @Test
    fun testCopiedAnswerCarriesCopiedByRuleSource() {
        val permAnswer = FormAnswer(
            fieldId = "f_perm_addr",
            rawValue = "42 MG Road, Pune 411001",
            normalizedValue = "42 MG Road, Pune 411001",
            source = AnswerSource.TYPED
        )
        val sameAddrAnswer = FormAnswer(
            fieldId = "f_same_addr",
            rawValue = "yes",
            normalizedValue = "yes",
            source = AnswerSource.VOICE
        )

        val answers = mapOf("f_perm_addr" to permAnswer, "f_same_addr" to sameAddrAnswer)
        val sameField = sampleFields.first { it.type == FieldType.SAME_AS_PERMANENT_ADDRESS }

        val automated = engine.evaluateAutomatedAnswers(sameField, sameAddrAnswer, sampleFields, answers)
        assertEquals(1, automated.size)
        val copied = automated.first()
        assertEquals(AnswerSource.COPIED_BY_RULE, copied.source)
        // Copied answer should have the permanent address content, not the same-as answer
        assertEquals("42 MG Road, Pune 411001", copied.rawValue)
        assertEquals("42 MG Road, Pune 411001", copied.normalizedValue)
    }
}
