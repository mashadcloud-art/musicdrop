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
                        // Explicitly switch to mobile
                        com.musicdrop.app.ui.tv.saveTvModeChoice(this@TvModeActivity, TvModeChoice.MOBILE)
                        startActivity(
                            Intent(this@TvModeActivity, MainActivity::class.java)
                                .putExtra("force_mobile", true)
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

    // Ensure TV mode is saved as active
    LaunchedEffect(Unit) {
        com.musicdrop.app.ui.tv.saveTvModeChoice(context, TvModeChoice.TV)
    }

    TvMusicApp(
        viewModel = viewModel,
        onSwitchToMobile = onSwitchToMobile
    )
}
