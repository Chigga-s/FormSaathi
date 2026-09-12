package com.formsaathi.contracts

import android.net.Uri
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.GenerationResult
import com.formsaathi.model.ParsedForm
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.model.ValidationResult
import java.io.File

/**
 * Service interface for parsing a government PDF into structured fields and page metadata.
 * Implemented by Role 2 (feature/form-engine).
 */
interface FormParser {
    suspend fun parse(uri: Uri): ParsedForm
}

/**
 * Service interface for providing simplified multilingual questions.
 * Implemented by Role 3 (feature/voice-language).
 */
interface QuestionProvider {
    fun questionFor(
        fieldType: FieldType,
        language: SupportedLanguage
    ): String
}

/**
 * Service interface for offline speech transcription.
 * Implemented by Role 3 (feature/voice-language via whisper.cpp).
 */
interface VoiceService {
    suspend fun transcribe(
        audioFile: File,
        language: SupportedLanguage
    ): String
}

/**
 * Service interface for normalizing raw user answers and validating constraints.
 * Implemented by Role 3 (feature/voice-language).
 */
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
 * Service interface for drawing answers over original PDF pages and producing a flattened PDF.
 * Implemented by Role 4 (feature/core-pdf).
 *
 * NOTE ON FROZEN CONTRACT REFINEMENT:
 * The original frozen contract in PLAN(1).md specified `suspend fun generate(...): Unit`.
 * In this implementation, the return type was refined to `GenerationResult` (wrapping a list
 * of `TextFitWarning`s) so that CORE-5 text-fitting truncation warnings can be surfaced
 * to the review screen and completed session state. This is a frozen-contract change
 * requiring explicit team agreement across all roles before branch integration into main.
 */
interface CompletedPdfGenerator {
    suspend fun generate(
        sourceUri: Uri,
        parsedForm: ParsedForm,
        answers: Map<String, FormAnswer>,
        outputUri: Uri
    ): GenerationResult
}
