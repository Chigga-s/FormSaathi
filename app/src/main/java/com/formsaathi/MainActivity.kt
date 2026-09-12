package com.formsaathi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.formsaathi.UI.FormSaathiNavigation
import com.formsaathi.UI.FormSaathiTheme
import com.formsaathi.core.FormViewModel

class MainActivity : ComponentActivity() {
    private lateinit var model: FormViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[FormViewModel::class.java]
        setContent {
            var darkTheme by remember { mutableStateOf(false) }

            FormSaathiTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    FormSaathiNavigation(
                        model = model,
                        darkTheme = darkTheme,
                        onThemeChanged = { darkTheme = !darkTheme }
                    )
                }
            }
        }
    }

    override fun onStop() {
        model.cancelVoice()
        super.onStop()
    }
}
