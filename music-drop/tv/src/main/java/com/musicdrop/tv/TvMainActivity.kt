package com.musicdrop.tv

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.musicdrop.tv.ui.components.TvFocusRegistry
import com.musicdrop.tv.ui.theme.MusicDropTvTheme
import com.musicdrop.tv.viewmodel.TvViewModel

class TvMainActivity : ComponentActivity() {

    private val tvViewModel: TvViewModel by viewModels()

    // Player key handler takes highest priority when video is playing
    var playerKeyHandler: ((Int) -> Boolean)? = null

    // General browse screen D-pad listener
    var dpadListener: ((KeyEvent) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep TV display awake while watching videos
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Ensure window decor view requests focus so TV remote D-pad is active immediately
        window.decorView.post {
            window.decorView.requestFocus()
        }

        setContent {
            MusicDropTvTheme {
                TvAppScaffold(viewModel = tvViewModel)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode

        if (action == KeyEvent.ACTION_DOWN) {
            // 1. If player is active, give player first priority for remote control (Up Next shelf, Seek, Back)
            if (playerKeyHandler?.invoke(keyCode) == true) {
                return true
            }

            // 2. Instant Hardware OK / ENTER / DPAD_CENTER / NUMPAD_ENTER button:
            // Triggers the currently focused button's onClick immediately
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                keyCode == KeyEvent.KEYCODE_BUTTON_SELECT
            ) {
                if (TvFocusRegistry.triggerActiveClick()) {
                    return true
                }
            }

            // 3. Instant Hardware BACK button fallback: Closes video player if open and returns to browsing
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (tvViewModel.currentVideo.value != null) {
                    tvViewModel.closePlayer()
                    return true
                }
            }

            // 4. Hardware D-pad Arrow Navigation (Up, Down, Left, Right)
            if (dpadListener?.invoke(event) == true) {
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }
}
