package com.formsaathi.core

import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect
import com.formsaathi.model.PageInfo
import com.formsaathi.model.ParsedForm
import com.formsaathi.model.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormSessionTest {

    private lateinit var session: FormSession
    private lateinit var conversationEngine: ConversationEngine

    @Before
    fun setUp() {
        conversationEngine = ConversationEngine()
        val fields = listOf(
            FormField("f1", "Name", FieldType.FULL_NAME, 0, NormalizedRect(0f, 0f, 1f, 1f), NormalizedRect(0f, 0f, 1f, 1f)),
            FormField("f2", "Mobile", FieldType.MOBILE, 0, NormalizedRect(0f, 0f, 1f, 1f), NormalizedRect(0f, 0f, 1f, 1f)),
            FormField("f3", "Email", FieldType.EMAIL, 0, NormalizedRect(0f, 0f, 1f, 1f), NormalizedRect(0f, 0f, 1f, 1f))
        )
        val parsedForm = ParsedForm(
            pages = listOf(PageInfo(0, 595f, 842f, 1190, 1684)),
            fields = fields,
            documents = emptyList()
        )
        session = FormSession(parsedForm, SupportedLanguage.ENGLISH)
    }

    @Test
    fun testInitialState() {
        assertEquals(0, session.currentFieldIndex)
        assertEquals("f1", session.getCurrentField()?.id)
        assertFalse(session.canGoBack())
        assertEquals(3, session.getActiveQuestionsCount(conversationEngine))
    }

    @Test
    fun testRecordAnswerAndAdvance() {
        session.setAnswer(FormAnswer("f1", "Akarsh", "Akarsh", AnswerSource.TYPED))
        assertEquals("Akarsh", session.getCurrentAnswer()?.normalizedValue)

        val hasNext = session.advance(conversationEngine)
        assertTrue(hasNext)
        assertEquals(1, session.currentFieldIndex)
        assertEquals("f2", session.getCurrentField()?.id)
        assertTrue(session.canGoBack())
    }

    @Test
    fun testGoBack() {
        session.advance(conversationEngine)
        assertEquals(1, session.currentFieldIndex)
        assertTrue(session.canGoBack())

        val didGoBack = session.goBack()
        assertTrue(didGoBack)
        assertEquals(0, session.currentFieldIndex)
        assertFalse(session.canGoBack())
    }

    @Test
    fun testSkipRemovesPreviousAnswerAndAdvances() {
        session.setAnswer(FormAnswer("f1", "Akarsh", "Akarsh", AnswerSource.TYPED))
        session.skip(conversationEngine)

        // f1 should now have no answer
        assertNull(session.answers["f1"])
        assertEquals(1, session.currentFieldIndex)
    }

    @Test
    fun testJumpToField() {
        session.jumpToField(2)
        assertEquals(2, session.currentFieldIndex)
        assertEquals("f3", session.getCurrentField()?.id)
        assertTrue(session.canGoBack())
    }

    @Test
    fun testGetActiveQuestionPositionWithSkippedFields() {
        // Build a session with the address conditional rule scenario
        val fieldsWithAddress = listOf(
            FormField("f1", "Name", FieldType.FULL_NAME, 0, NormalizedRect(0f, 0f, 1f, 1f), NormalizedRect(0f, 0f, 1f, 1f)),
            FormField("f2", "Perm Addr", FieldType.PERMANENT_ADDRESS, 0, NormalizedRect(0f, 0f, 1f, 1f), NormalizedRect(0f, 0f, 1f, 1f)),
            FormField("f3", "Same?", FieldType.SAME_AS_PERMANENT_ADDRESS, 0, NormalizedRect(0f, 0f, 1f, 1f), NormalizedRect(0f, 0f, 1f, 1f)),
            FormField("f4", "Curr Addr", FieldType.CURRENT_ADDRESS, 0, NormalizedRect(0f, 0f, 1f, 1f), NormalizedRect(0f, 0f, 1f, 1f)),
            FormField("f5", "Mobile", FieldType.MOBILE, 0, NormalizedRect(0f, 0f, 1f, 1f), NormalizedRect(0f, 0f, 1f, 1f))
        )
        val parsed = ParsedForm(
            pages = listOf(PageInfo(0, 595f, 842f, 1190, 1684)),
            fields = fieldsWithAddress,
            documents = emptyList()
        )
        val addrSession = FormSession(parsed, SupportedLanguage.ENGLISH)

        // Set "same as permanent" to yes — this skips f4 (current address)
        addrSession.setAnswer(FormAnswer("f3", "yes", "yes", AnswerSource.TYPED))

        // Position at field index 0 (Name) → active position should be 0
        assertEquals(0, addrSession.getActiveQuestionPosition(conversationEngine))

        // Advance to field index 4 (Mobile), which skips f4 (Current Address)
        // In the active field list: [f1=0, f2=1, f3=2, f5=3] — f4 is skipped
        addrSession.jumpToField(4)
        val position = addrSession.getActiveQuestionPosition(conversationEngine)
        assertEquals(3, position) // 0-based: Name=0, PermAddr=1, Same=2, Mobile=3

        // Total active should be 4 (not 5)
        assertEquals(4, addrSession.getActiveQuestionsCount(conversationEngine))
    }
}
