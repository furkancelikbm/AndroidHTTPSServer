package com.example.mobilserver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilserver.view.ServerScreen
import com.example.mobilserver.viewmodel.ServerViewModel



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: ServerViewModel = viewModel()
            ServerScreen(viewModel)
        }
    }
}
