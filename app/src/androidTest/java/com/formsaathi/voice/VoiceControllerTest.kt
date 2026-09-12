package com.formsaathi.voice


import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith



@RunWith(AndroidJUnit4::class)
class VoiceControllerTest {


    @get:Rule
    val permissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.RECORD_AUDIO
        )



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



        controller.stopListening {

            println(
                "VOICE RESULT = $it"
            )

        }



        Thread.sleep(10000)



        controller.release()

    }
}