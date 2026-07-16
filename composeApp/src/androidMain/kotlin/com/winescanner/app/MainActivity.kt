package com.winescanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.winescanner.app.ml.LabelExtractor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val labelExtractor = LabelExtractor(applicationContext)

        setContent {
            App(labelExtractor = labelExtractor)
        }
    }
}
