package com.formsaathi.contracts

import android.net.Uri
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.GenerationResult
import com.formsaathi.model.ParsedForm
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.model.ValidationResult
import java.io.File

/** Parses a government PDF into structured fields and page metadata. */
interface FormParser {
    suspend fun parse(uri: Uri): ParsedForm
}

/**
 * Stage a parser is currently working through, reported so the UI can show real
 * progress and the coordinator can tell a slow document apart from a stalled one.
 */
enum class ParseStage {
    RENDERING,
    READING_TEXT,
    PREPARING_QUESTIONS
}

/**
 * Implemented by parsers that can report intermediate progress. The coordinator
 * uses the reports to reset its stall watchdog, so a long but healthy OCR run is
 * never interrupted while a genuinely hung one still fails recoverably.
 */
interface ProgressReportingFormParser : FormParser {
    fun setProgressListener(listener: ((ParseStage, Int, Int) -> Unit)?)
}

/** Supplies the simplified question shown for a detected field, per language. */
interface QuestionProvider {
    fun questionFor(
        fieldType: FieldType,
        language: SupportedLanguage
    ): String

    /**
     * Question for one detected field. Implementations must name the label printed
     * on the form when the field type is unknown, so the user always understands
     * what is being asked instead of seeing a generic prompt.
     */
    fun questionFor(
        field: FormField,
        language: SupportedLanguage
    ): String = questionFor(field.type, language)
}

/** Offline speech transcription. Backed by whisper.cpp in the real implementation. */
interface VoiceService {
    suspend fun transcribe(
        audioFile: File,
        language: SupportedLanguage
    ): String
}

/** Normalizes raw user answers and validates them against field constraints. */
interface AnswerProcessor {
    fun normalize(
        fieldType: FieldType,
        rawText: String,
        language: SupportedLanguage
    ): String

    fun validate(
        fieldType: FieldType,
        value: String
    ): ValidationResult
}

/**
 * Draws answers over the original PDF pages and writes a flattened PDF.
 *
 * Returns a [GenerationResult] rather than the `Unit` in PLAN(1).md so text that
 * had to be shrunk or clipped to fit can be reported to the user instead of being
 * silently accepted.
 */
interface CompletedPdfGenerator {
    suspend fun generate(
        sourceUri: Uri,
        parsedForm: ParsedForm,
        answers: Map<String, FormAnswer>,
        outputUri: Uri
    ): GenerationResult
}
