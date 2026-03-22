package com.example.watchai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.example.watchai.ui.WatchAiApp
import com.example.watchai.ui.theme.WatchAiTheme
import com.example.watchai.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WatchAiTheme {
                WatchAiApp()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 用户离开 App（息屏、切换应用）时自动保存当前对话
        viewModel.autoSave()
    }
}
