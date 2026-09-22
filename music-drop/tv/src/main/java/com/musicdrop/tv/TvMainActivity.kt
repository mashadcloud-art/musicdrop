package com.musicdrop.tv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicdrop.tv.viewmodel.TvViewModel

class TvMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Prevent TV from sleeping while watching videos
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            com.musicdrop.tv.ui.theme.MusicDropTvTheme {
                val viewModel: TvViewModel = viewModel()
                TvAppScaffold(viewModel = viewModel)
            }
        }
    }
}
