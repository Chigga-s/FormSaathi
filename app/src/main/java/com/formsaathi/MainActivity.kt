package com.formsaathi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.MaterialTheme
import com.formsaathi.UI.FormSaathiNavigation
import com.formsaathi.core.FormViewModel

class MainActivity : ComponentActivity() {
    private lateinit var model: FormViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[FormViewModel::class.java]
        setContent {
            MaterialTheme { FormSaathiNavigation(model) }
        }
    }

    override fun onStop() {
        model.cancelVoice()
        super.onStop()
    }
}
