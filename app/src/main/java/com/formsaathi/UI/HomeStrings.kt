package com.formsaathi.UI

import com.formsaathi.model.SupportedLanguage

data class HomeStrings(
    val subtitle: String,
    val selectForm: String,
    val selectedForm: String
)

fun getHomeStrings(
    language: SupportedLanguage
): HomeStrings {

    return when (language) {

        SupportedLanguage.ENGLISH -> {
            HomeStrings(
                subtitle = "Fill your forms easily",
                selectForm = "Select a form",
                selectedForm = "Selected form"
            )
        }

        SupportedLanguage.HINDI -> {
            HomeStrings(
                subtitle = "अपने फ़ॉर्म आसानी से भरें",
                selectForm = "फ़ॉर्म चुनें",
                selectedForm = "चयनित फ़ॉर्म"
            )
        }

        SupportedLanguage.MARATHI -> {
            HomeStrings(
                subtitle = "तुमचे फॉर्म सहज भरा",
                selectForm = "फॉर्म निवडा",
                selectedForm = "निवडलेला फॉर्म"
            )
        }


        }
    }
