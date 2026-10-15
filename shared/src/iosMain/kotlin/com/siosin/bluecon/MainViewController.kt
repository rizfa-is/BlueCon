package com.siosin.bluecon

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

// iOS is parked while the SDK's Android backend is being developed.
fun MainViewController() = ComposeUIViewController {
    MaterialTheme { Text("BlueCon Bluetooth scanning is currently available on Android.") }
}
