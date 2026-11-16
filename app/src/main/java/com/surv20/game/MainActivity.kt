package com.surv20.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.surv20.game.ui.GameTheme

// Android: starts the shared game from :shared with the Android platform.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val platform = AndroidPlatform(
            context = this,
            version = "${BuildConfig.VERSION_NAME} · build ${BuildConfig.BUILD_TIME}",
        )
        setContent {
            GameTheme {
                GameApp(platform)
            }
        }
    }
}
