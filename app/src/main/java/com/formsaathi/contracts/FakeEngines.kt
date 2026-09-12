package com.formsaathi.contracts

import android.net.Uri
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.GenerationResult
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect
import com.formsaathi.model.PageInfo
import com.formsaathi.model.ParsedForm
import com.formsaathi.model.RequiredDocument
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.model.ValidationResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Deterministic fake implementation of FormParser for development, testing, and decoupled UI building.
 */
class FakeFormParser : FormParser {
    override suspend fun parse(uri: Uri): ParsedForm {
        val page1 = PageInfo(
            pageIndex = 0,
            pdfWidthPoints = 595.0f,
            pdfHeightPoints = 842.0f,
            renderedWidthPx = 1190,
            renderedHeightPx = 1684
        )
        val page2 = PageInfo(
            pageIndex = 1,
            pdfWidthPoints = 595.0f,
            pdfHeightPoints = 842.0f,
            renderedWidthPx = 1190,
            renderedHeightPx = 1684
        )

        val fields = listOf(
            FormField(
                id = "field_full_name",
                sourceLabel = "Full Name / आवेदक का पूरा नाम",
                type = FieldType.FULL_NAME,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.15f, 0.40f, 0.18f),
                answerBox = NormalizedRect(0.42f, 0.15f, 0.90f, 0.18f),
                required = true,
                confidence = 0.95f
            ),
            FormField(
                id = "field_father_name",
                sourceLabel = "Father's Name / पिता का नाम",
                type = FieldType.FATHER_NAME,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.20f, 0.40f, 0.23f),
                answerBox = NormalizedRect(0.42f, 0.20f, 0.90f, 0.23f),
                required = true,
                confidence = 0.93f
            ),
            FormField(
                id = "field_dob",
                sourceLabel = "Date of Birth (DD/MM/YYYY) / जन्म तिथि",
                type = FieldType.DATE_OF_BIRTH,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.25f, 0.40f, 0.28f),
                answerBox = NormalizedRect(0.42f, 0.25f, 0.70f, 0.28f),
                required = true,
                confidence = 0.91f
            ),
            FormField(
                id = "field_gender",
                sourceLabel = "Gender / लिंग",
                type = FieldType.GENDER,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.30f, 0.30f, 0.33f),
                answerBox = NormalizedRect(0.32f, 0.30f, 0.60f, 0.33f),
                required = true,
                confidence = 0.89f
            ),
            FormField(
                id = "field_mobile",
                sourceLabel = "Mobile Number / मोबाइल नंबर",
                type = FieldType.MOBILE,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.35f, 0.35f, 0.38f),
                answerBox = NormalizedRect(0.38f, 0.35f, 0.75f, 0.38f),
                required = true,
                confidence = 0.97f
            ),
            FormField(
                id = "field_email",
                sourceLabel = "Email Address / ईमेल",
                type = FieldType.EMAIL,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.40f, 0.35f, 0.43f),
                answerBox = NormalizedRect(0.38f, 0.40f, 0.85f, 0.43f),
                required = false,
                confidence = 0.94f
            ),
            FormField(
                id = "field_aadhaar",
                sourceLabel = "Aadhaar Number / आधार संख्या",
                type = FieldType.AADHAAR,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.45f, 0.38f, 0.48f),
                answerBox = NormalizedRect(0.40f, 0.45f, 0.80f, 0.48f),
                required = true,
                confidence = 0.96f
            ),
            FormField(
                id = "field_permanent_address",
                sourceLabel = "Permanent Address / स्थायी पता",
                type = FieldType.PERMANENT_ADDRESS,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.52f, 0.40f, 0.55f),
                answerBox = NormalizedRect(0.10f, 0.56f, 0.90f, 0.64f),
                required = true,
                confidence = 0.90f
            ),
            FormField(
                id = "field_same_as_permanent",
                sourceLabel = "Is Current Address same as Permanent Address? (Yes/No)",
                type = FieldType.SAME_AS_PERMANENT_ADDRESS,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.66f, 0.60f, 0.69f),
                answerBox = NormalizedRect(0.62f, 0.66f, 0.80f, 0.69f),
                required = true,
                confidence = 0.88f
            ),
            FormField(
                id = "field_current_address",
                sourceLabel = "Current Address / वर्तमान पता",
                type = FieldType.CURRENT_ADDRESS,
                pageIndex = 0,
                labelBox = NormalizedRect(0.10f, 0.71f, 0.40f, 0.74f),
                answerBox = NormalizedRect(0.10f, 0.75f, 0.90f, 0.83f),
                required = false,
                confidence = 0.90f
            ),
            FormField(
                id = "field_category",
                sourceLabel = "Category (General/OBC/SC/ST) / वर्ग",
                type = FieldType.CATEGORY,
                pageIndex = 1,
                labelBox = NormalizedRect(0.10f, 0.15f, 0.35f, 0.18f),
                answerBox = NormalizedRect(0.38f, 0.15f, 0.70f, 0.18f),
                required = true,
                confidence = 0.92f
            ),
            FormField(
                id = "field_annual_income",
                sourceLabel = "Annual Family Income / वार्षिक आय",
                type = FieldType.ANNUAL_INCOME,
                pageIndex = 1,
                labelBox = NormalizedRect(0.10f, 0.22f, 0.40f, 0.25f),
                answerBox = NormalizedRect(0.42f, 0.22f, 0.75f, 0.25f),
                required = true,
                confidence = 0.91f
            )
        )

        val documents = listOf(
            RequiredDocument("Aadhaar Card", "Self-attested photocopy"),
            RequiredDocument("Income Certificate", "Issued within last 6 months")
        )

        return ParsedForm(
            pages = listOf(page1, page2),
            fields = fields,
            documents = documents,
            warnings = emptyList()
        )
    }
}

/**
 * Deterministic fake QuestionProvider returning intuitive localized questions.
 */
class FakeQuestionProvider : QuestionProvider {
    override fun questionFor(fieldType: FieldType, language: SupportedLanguage): String {
        return when (language) {
            SupportedLanguage.ENGLISH -> when (fieldType) {
                FieldType.FULL_NAME -> "What is your full legal name?"
                FieldType.FATHER_NAME -> "What is your father's name?"
                FieldType.MOTHER_NAME -> "What is your mother's name?"
                FieldType.DATE_OF_BIRTH -> "What is your date of birth?"
                FieldType.GENDER -> "What is your gender?"
                FieldType.MOBILE -> "What is your 10-digit mobile number?"
                FieldType.EMAIL -> "What is your email address?"
                FieldType.AADHAAR -> "What is your 12-digit Aadhaar number?"
                FieldType.PERMANENT_ADDRESS -> "What is your permanent address?"
                FieldType.CURRENT_ADDRESS -> "What is your current residence address?"
                FieldType.SAME_AS_PERMANENT_ADDRESS -> "Is your current address the same as your permanent address?"
                FieldType.STATE -> "Which state do you reside in?"
                FieldType.DISTRICT -> "Which district do you reside in?"
                FieldType.PINCODE -> "What is your 6-digit postal PIN code?"
                FieldType.CATEGORY -> "What is your social category (e.g. General, OBC, SC, ST)?"
                FieldType.ANNUAL_INCOME -> "What is your total annual family income in Rupees?"
                FieldType.PHOTO -> "Please attach your passport-size photograph."
                FieldType.SIGNATURE -> "Please provide your signature."
                FieldType.UNKNOWN -> "Please provide the requested detail."
            }
            SupportedLanguage.HINDI -> when (fieldType) {
                FieldType.FULL_NAME -> "आपका पूरा नाम क्या है?"
                FieldType.FATHER_NAME -> "आपके पिता का क्या नाम है?"
                FieldType.MOTHER_NAME -> "आपकी माता का क्या नाम है?"
                FieldType.DATE_OF_BIRTH -> "आपकी जन्म तिथि क्या है?"
                FieldType.GENDER -> "आपका लिंग क्या है?"
                FieldType.MOBILE -> "आपका 10 अंकों का मोबाइल नंबर क्या है?"
                FieldType.EMAIL -> "आपका ईमेल पता क्या है?"
                FieldType.AADHAAR -> "आपका 12 अंकों का आधार नंबर क्या है?"
                FieldType.PERMANENT_ADDRESS -> "आपका स्थायी पता क्या है?"
                FieldType.CURRENT_ADDRESS -> "आपका वर्तमान निवास पता क्या है?"
                FieldType.SAME_AS_PERMANENT_ADDRESS -> "क्या आपका वर्तमान पता आपके स्थायी पते के समान है?"
                FieldType.STATE -> "आपका राज्य कौन सा है?"
                FieldType.DISTRICT -> "आपका जिला कौन सा है?"
                FieldType.PINCODE -> "आपका 6 अंकों का पिन कोड क्या है?"
                FieldType.CATEGORY -> "आपका सामाजिक वर्ग क्या है (जैसे सामान्य, ओबीसी, एससी, एसटी)?"
                FieldType.ANNUAL_INCOME -> "आपकी कुल वार्षिक पारिवारिक आय कितनी है?"
                FieldType.PHOTO -> "कृपया अपनी पासपोर्ट आकार की फोटो संलग्न करें।"
                FieldType.SIGNATURE -> "कृपया अपने हस्ताक्षर करें।"
                FieldType.UNKNOWN -> "कृपया आवश्यक विवरण दर्ज करें।"
            }
            SupportedLanguage.MARATHI -> when (fieldType) {
                FieldType.FULL_NAME -> "आपले पूर्ण नाव काय आहे?"
                FieldType.FATHER_NAME -> "आपल्या वडिलांचे नाव काय आहे?"
                FieldType.MOTHER_NAME -> "आपल्या आईचे नाव काय आहे?"
                FieldType.DATE_OF_BIRTH -> "आपली जन्मतारीख काय आहे?"
                FieldType.GENDER -> "आपले लिंग काय आहे?"
                FieldType.MOBILE -> "आपला 10 अंकी मोबाईल क्रमांक काय आहे?"
                FieldType.EMAIL -> "आपला ईमेल पत्ता काय आहे?"
                FieldType.AADHAAR -> "आपला 12 अंकी आधार क्रमांक काय आहे?"
                FieldType.PERMANENT_ADDRESS -> "आपला कायमचा पत्ता काय आहे?"
                FieldType.CURRENT_ADDRESS -> "आपला सध्याचा राहण्याचा पत्ता काय आहे?"
                FieldType.SAME_AS_PERMANENT_ADDRESS -> "आपला सध्याचा पत्ता कायमच्या पत्त्यासारखाच आहे का?"
                FieldType.STATE -> "आपले राज्य कोणते आहे?"
                FieldType.DISTRICT -> "आपला जिल्हा कोणता आहे?"
                FieldType.PINCODE -> "आपला 6 अंकी पिन कोड काय आहे?"
                FieldType.CATEGORY -> "आपला सामाजिक प्रवर्ग कोणता आहे (उदा. खुला, ओबीसी, एससी, एसटी)?"
                FieldType.ANNUAL_INCOME -> "आपले एकूण वार्षिक कौटुंबिक उत्पन्न किती आहे?"
                FieldType.PHOTO -> "कृपया आपला पासपोर्ट आकाराचा फोटो जोडा."
                FieldType.SIGNATURE -> "कृपया आपली स्वाक्षरी करा."
                FieldType.UNKNOWN -> "कृपया आवश्यक तपशील प्रविष्ट करा."
            }
        }
    }
}

/**
 * Deterministic fake VoiceService returning mock transcription.
 */
class FakeVoiceService(
    private val stubbedResponse: String = "Test Voice Answer"
) : VoiceService {
    override suspend fun transcribe(audioFile: File, language: SupportedLanguage): String {
        return stubbedResponse
    }
}

/**
 * Deterministic fake AnswerProcessor providing standard Indian field normalizations and validations.
 */
class FakeAnswerProcessor : AnswerProcessor {
    override fun normalize(
        fieldType: FieldType,
        rawText: String,
        language: SupportedLanguage
    ): String {
        val trimmed = rawText.trim()
        return when (fieldType) {
            FieldType.MOBILE, FieldType.PINCODE, FieldType.AADHAAR -> {
                // Keep only numeric digits
                trimmed.replace(Regex("[^0-9]"), "")
            }
            FieldType.SAME_AS_PERMANENT_ADDRESS -> {
                val lower = trimmed.lowercase()
                if (lower in listOf("yes", "y", "true", "हाँ", "होय", "हो", "haan", "ha")) "yes" else "no"
            }
            FieldType.ANNUAL_INCOME -> {
                // Convert spoken expressions or remove currency symbols and commas
                val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
                if (digitsOnly.isNotEmpty()) digitsOnly else trimmed
            }
            FieldType.DATE_OF_BIRTH -> {
                // Normalize slashes / hyphens
                trimmed.replace('-', '/')
            }
            FieldType.GENDER -> {
                val lower = trimmed.lowercase()
                when {
                    lower.startsWith("m") || lower.contains("पुरुष") || lower.contains("male") -> "Male"
                    lower.startsWith("f") || lower.contains("महिला") || lower.contains("female") || lower.contains("स्त्री") -> "Female"
                    else -> "Other"
                }
            }
            else -> trimmed
        }
    }

    override fun validate(fieldType: FieldType, value: String): ValidationResult {
        val trimmed = value.trim()
        return when (fieldType) {
            FieldType.MOBILE -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 10) ValidationResult.Valid
                else ValidationResult.Invalid("Mobile number must be exactly 10 digits")
            }
            FieldType.PINCODE -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 6) ValidationResult.Valid
                else ValidationResult.Invalid("PIN code must be exactly 6 digits")
            }
            FieldType.AADHAAR -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 12) ValidationResult.Valid
                else ValidationResult.Invalid("Aadhaar number must be exactly 12 digits")
            }
            FieldType.EMAIL -> {
                if (trimmed.isEmpty()) ValidationResult.Valid
                else if (android.util.Patterns.EMAIL_ADDRESS?.matcher(trimmed)?.matches() == true ||
                    trimmed.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
                ) ValidationResult.Valid
                else ValidationResult.Invalid("Please enter a valid email address")
            }
            FieldType.ANNUAL_INCOME -> {
                val income = trimmed.toLongOrNull()
                if (income != null && income >= 0) ValidationResult.Valid
                else ValidationResult.Invalid("Annual income must be a valid non-negative number")
            }
            FieldType.DATE_OF_BIRTH -> {
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
                    val parsedDate = sdf.parse(trimmed)
                    if (parsedDate != null && parsedDate.before(Date())) ValidationResult.Valid
                    else ValidationResult.Invalid("Date of birth must be in DD/MM/YYYY format and cannot be in the future")
                } catch (e: Exception) {
                    ValidationResult.Invalid("Invalid date. Use DD/MM/YYYY format")
                }
            }
            FieldType.FULL_NAME, FieldType.FATHER_NAME -> {
                if (trimmed.isNotBlank()) ValidationResult.Valid
                else ValidationResult.Invalid("This field cannot be blank")
            }
            else -> ValidationResult.Valid
        }
    }
}

/**
 * Deterministic fake CompletedPdfGenerator that completes immediately for testing/mocking.
 */
class FakeCompletedPdfGenerator : CompletedPdfGenerator {
    var wasGenerateCalled: Boolean = false
        private set
    var lastSourceUri: Uri? = null
        private set
    var lastAnswersCount: Int = 0
        private set

    override suspend fun generate(
        sourceUri: Uri,
        parsedForm: ParsedForm,
        answers: Map<String, FormAnswer>,
        outputUri: Uri
    ): GenerationResult {
        wasGenerateCalled = true
        lastSourceUri = sourceUri
        lastAnswersCount = answers.size
        return GenerationResult()
    }
}

