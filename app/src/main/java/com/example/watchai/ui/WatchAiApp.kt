package com.example.watchai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.watchai.ui.screens.ChatScreen
import com.example.watchai.ui.screens.HistoryScreen
import com.example.watchai.ui.screens.SetupScreen
import com.example.watchai.viewmodel.ChatViewModel
import com.example.watchai.viewmodel.Screen

@Composable
fun WatchAiApp() {
    val viewModel: ChatViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.screen) {
        Screen.CHAT    -> ChatScreen(viewModel = viewModel)
        Screen.SETUP   -> SetupScreen(viewModel = viewModel)
        Screen.HISTORY -> HistoryScreen(viewModel = viewModel)
    }
}
