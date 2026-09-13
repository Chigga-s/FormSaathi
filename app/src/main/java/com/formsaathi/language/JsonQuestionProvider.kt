package com.formsaathi.language

import android.content.Context
import com.formsaathi.contracts.QuestionProvider
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormField
import com.formsaathi.model.SupportedLanguage
import org.json.JSONObject

class JsonQuestionProvider(private val context: Context) : QuestionProvider {

    private val questions = mutableMapOf<String, Map<String, String>>()

    init {
        loadLanguage(SupportedLanguage.ENGLISH.code, "questions_en.json")
        loadLanguage(SupportedLanguage.HINDI.code, "questions_hi.json")
        loadLanguage(SupportedLanguage.MARATHI.code, "questions_mr.json")
    }

    private fun loadLanguage(langCode: String, filename: String) {
        questions[langCode] = try {
            context.assets.open(filename).use { input ->
                val json = JSONObject(input.reader().readText())
                json.keys().asSequence().associateWith { json.getString(it) }
            }
        } catch (_: Exception) {
            // A missing or malformed file falls back to English at lookup time;
            // the question flow must never fail because of a resource problem.
            emptyMap()
        }
    }

    private fun lookup(key: String, language: SupportedLanguage): String? {
        return questions[language.code]?.get(key)
            ?: questions[SupportedLanguage.ENGLISH.code]?.get(key)
    }

    override fun questionFor(fieldType: FieldType, language: SupportedLanguage): String {
        return lookup(fieldType.name, language)
            ?: "Please provide an answer for ${fieldType.name}"
    }

    /**
     * Unknown fields are asked using the label printed on the form, so the user
     * can always tell what is being requested. A bare "please provide the
     * requested detail" is never shown for a field that has a readable label.
     */
    override fun questionFor(field: FormField, language: SupportedLanguage): String {
        if (field.type != FieldType.UNKNOWN) {
            return questionFor(field.type, language)
        }
        val label = cleanLabel(field.sourceLabel)
        if (label.isEmpty()) {
            return questionFor(FieldType.UNKNOWN, language)
        }
        val template = lookup(UNKNOWN_WITH_LABEL_KEY, language) ?: "Please enter: %s"
        return template.replace("%s", label)
    }

    /** Strips trailing field punctuation so the label reads naturally in a question. */
    private fun cleanLabel(raw: String): String {
        return raw.trim()
            .replace(Regex("\\s+"), " ")
            .trimEnd(':', '-', '–', '—', '*', '.', ' ')
            .trim()
    }

    private companion object {
        const val UNKNOWN_WITH_LABEL_KEY = "UNKNOWN_WITH_LABEL"
    }
}
