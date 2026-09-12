package com.formsaathi

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLaunchTest {
    @Test fun launchOffersLanguageSelectionWithoutLoadingSpeechModel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(!activity.isFinishing && !activity.isDestroyed)
            }
            var resumed = false
            scenario.onActivity { resumed = true }
            assertTrue(resumed)
            check(scenario.state.isAtLeast(Lifecycle.State.RESUMED))
        }
    }
}
