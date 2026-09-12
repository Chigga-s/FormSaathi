package com.formsaathi.voice


import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceControllerTest {

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        try {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.RECORD_AUDIO
            )
        } catch (_: Exception) {
        }
    }



    @Test
    fun realVoiceTest() {


        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext



        val controller =
            VoiceController(
                context
            )



        controller.startListening()



        println(
            "Recording started. Speak now..."
        )



        Thread.sleep(5000)



        val transcript = kotlinx.coroutines.runBlocking {
            controller.stopListening(com.formsaathi.model.SupportedLanguage.ENGLISH)
        }
        org.junit.Assert.assertTrue(transcript.isNotBlank())



        Thread.sleep(10000)



        controller.close()

    }
}
