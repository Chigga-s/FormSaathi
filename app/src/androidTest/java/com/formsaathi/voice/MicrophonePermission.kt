package com.formsaathi.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

/**
 * Grants RECORD_AUDIO to the app under test, reporting whether it worked.
 *
 * Some OEM builds block UiAutomation's direct grant call, so the shell `pm grant`
 * path is tried as well. Tests that need the microphone skip themselves when both
 * fail rather than reporting a pass they did not earn.
 */
object MicrophonePermission {

    fun grant(): Boolean {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (isGranted()) return true

        try {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.RECORD_AUDIO
            )
        } catch (_: Exception) {
        }
        if (isGranted()) return true

        try {
            val descriptor = instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} ${Manifest.permission.RECORD_AUDIO}"
            )
            FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        } catch (_: Exception) {
        }
        return isGranted()
    }

    private fun isGranted(): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }
}
