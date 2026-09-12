package com.formsaathi.language

import android.content.Context
import com.formsaathi.contracts.QuestionProvider
import com.formsaathi.model.FieldType
import com.formsaathi.model.SupportedLanguage
import org.json.JSONObject
import java.io.InputStreamReader

class JsonQuestionProvider(private val context: Context) : QuestionProvider {

    private val questions = mutableMapOf<String, Map<String, String>>()

    init {
        loadLanguage(SupportedLanguage.ENGLISH.code, "questions_en.json")
        loadLanguage(SupportedLanguage.HINDI.code, "questions_hi.json")
        loadLanguage(SupportedLanguage.MARATHI.code, "questions_mr.json")
    }

    private fun loadLanguage(langCode: String, filename: String) {
        try {
            context.assets.open(filename).use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val jsonText = reader.readText()
                val jsonObject = JSONObject(jsonText)
                
                val langMap = mutableMapOf<String, String>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    langMap[key] = jsonObject.getString(key)
                }
                questions[langCode] = langMap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback empty map if failed
            questions[langCode] = emptyMap()
        }
    }

    override fun questionFor(fieldType: FieldType, language: SupportedLanguage): String {
        val langMap = questions[language.code]
        val question = langMap?.get(fieldType.name)
        
        if (question != null) {
            return question
        }
        
        // Fallback to English
        val enMap = questions[SupportedLanguage.ENGLISH.code]
        val enQuestion = enMap?.get(fieldType.name)
        
        if (enQuestion != null) {
            return enQuestion
        }
        
        // Final fallback
        return "Please provide an answer for ${fieldType.name}"
    }
}
