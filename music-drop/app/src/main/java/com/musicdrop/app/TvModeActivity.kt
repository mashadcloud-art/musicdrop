package com.musicdrop.app

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicdrop.app.ui.theme.FileDropTheme
import com.musicdrop.app.ui.theme.AppThemeMode
import com.musicdrop.app.ui.tv.TvModeChoice
import com.musicdrop.app.ui.tv.TvModeChooser
import com.musicdrop.app.ui.tv.TvMusicApp
import com.musicdrop.app.ui.tv.getTvModeChoice
import com.musicdrop.app.ui.viewmodel.MainViewModel

/**
 * Dedicated Activity registered in the manifest with the LEANBACK_LAUNCHER category.
 *
 * Logic:
 *  1. If running on a phone (not TV) → immediately redirect to [MainActivity].
 *  2. If first TV launch (no saved choice) → show [TvModeChooser].
 *  3. Saved choice == TV   → show [TvMusicApp].
 *  4. Saved choice == MOBILE → redirect to [MainActivity].
 */
class TvModeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // If forced via intent, update preference
        if (intent.getBooleanExtra("force_tv", false)) {
            com.musicdrop.app.ui.tv.saveTvModeChoice(this, TvModeChoice.TV)
        } else if (intent.getBooleanExtra("force_chooser", false)) {
            com.musicdrop.app.ui.tv.clearTvModeChoice(this)
        }

        setContent {
            val viewModel: MainViewModel = viewModel()
            val appTheme by viewModel.appTheme.collectAsState()
            val cardOpacity by viewModel.cardOpacity.collectAsState()
            val appFontFamily by viewModel.appFontFamily.collectAsState()
            val appFontColorOption by viewModel.appFontColorOption.collectAsState()

            FileDropTheme(
                themeMode = appTheme,
                cardOpacity = cardOpacity,
                fontFamilyName = appFontFamily,
                fontColorOption = appFontColorOption
            ) {
                TvRootContent(
                    viewModel = viewModel,
                    onSwitchToMobile = {
                        // Launch full mobile UI and finish TV activity
                        startActivity(
                            Intent(this@TvModeActivity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                        finish()
                    }
                )
            }
        }
    }


}

@Composable
private fun TvRootContent(
    viewModel: MainViewModel,
    onSwitchToMobile: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Read persisted choice; null means "not yet decided"
    var choice by remember { mutableStateOf(getTvModeChoice(context)) }

    when (choice) {
        null -> {
            // First launch — show chooser
            TvModeChooser { picked ->
                choice = picked
                if (picked == TvModeChoice.MOBILE) {
                    onSwitchToMobile()
                }
            }
        }
        TvModeChoice.TV -> {
            TvMusicApp(
                viewModel = viewModel,
                onSwitchToMobile = {
                    choice = null  // clear so chooser shows again if they change mind later
                    onSwitchToMobile()
                }
            )
        }
        TvModeChoice.MOBILE -> {
            // Immediately hand off — effect runs once
            LaunchedEffect(Unit) { onSwitchToMobile() }
        }
    }
}
