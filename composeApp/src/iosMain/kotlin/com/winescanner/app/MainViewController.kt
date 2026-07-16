package com.winescanner.app

import androidx.compose.ui.window.ComposeUIViewController
import com.winescanner.app.ml.LabelExtractor
import platform.UIKit.UIViewController


fun MainViewController(): UIViewController = ComposeUIViewController {
    App(labelExtractor = LabelExtractor())
}
