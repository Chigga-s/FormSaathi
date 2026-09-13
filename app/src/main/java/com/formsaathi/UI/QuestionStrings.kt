package com.formsaathi.UI

import com.formsaathi.model.SupportedLanguage

/**
 * Action labels for the question screen in each supported language.
 *
 * Every word is kept short deliberately: these sit in fixed-width buttons, and a
 * label that wraps reads as "Previou s" on a narrow phone.
 */
data class QuestionStrings(
    val questionCounter: (Int, Int) -> String,
    val answerLabel: String,
    val next: String,
    val review: String,
    val previous: String,
    val skip: String,
    val requiredField: String,
    val photoAttached: String,
    val choosePhoto: String,
    val recording: String,
    val transcribing: String
)

fun getQuestionStrings(language: SupportedLanguage): QuestionStrings = when (language) {

    SupportedLanguage.ENGLISH -> QuestionStrings(
        questionCounter = { index, total -> "Question $index of $total" },
        answerLabel = "Your answer",
        next = "Next",
        review = "Review",
        previous = "Previous",
        skip = "Skip",
        requiredField = "This field is required.",
        photoAttached = "Photo attached — tap to change",
        choosePhoto = "Choose photo from gallery",
        recording = "Recording… tap the red stop button, or wait up to 10 seconds",
        transcribing = "Transcribing your answer…"
    )

    SupportedLanguage.HINDI -> QuestionStrings(
        questionCounter = { index, total -> "प्रश्न $index / $total" },
        answerLabel = "आपका उत्तर",
        next = "आगे",
        review = "जाँचें",
        previous = "पीछे",
        skip = "छोड़ें",
        requiredField = "यह जानकारी ज़रूरी है।",
        photoAttached = "फ़ोटो जुड़ी — बदलने के लिए दबाएँ",
        choosePhoto = "गैलरी से फ़ोटो चुनें",
        recording = "रिकॉर्ड हो रहा है… लाल बटन दबाएँ या 10 सेकंड रुकें",
        transcribing = "आपका उत्तर लिखा जा रहा है…"
    )

    SupportedLanguage.MARATHI -> QuestionStrings(
        questionCounter = { index, total -> "प्रश्न $index / $total" },
        answerLabel = "तुमचे उत्तर",
        next = "पुढे",
        review = "तपासा",
        previous = "मागे",
        skip = "वगळा",
        requiredField = "ही माहिती आवश्यक आहे.",
        photoAttached = "फोटो जोडला — बदलण्यासाठी दाबा",
        choosePhoto = "गॅलरीतून फोटो निवडा",
        recording = "रेकॉर्ड होत आहे… लाल बटण दाबा किंवा 10 सेकंद थांबा",
        transcribing = "तुमचे उत्तर लिहिले जात आहे…"
    )
}
