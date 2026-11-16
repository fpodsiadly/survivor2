package com.surv20.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.surv20.game.res.Res
import com.surv20.game.res.app_title
import com.surv20.game.res.play
import com.surv20.game.res.prototype_note
import com.surv20.game.res.stress_test
import com.surv20.game.ui.RunView
import org.jetbrains.compose.resources.stringResource

private enum class Screen { MENU, RUN, STRESS }

// Shell of the game. The main screen (SPEC W38) grows with PLAN E4; for now: play and, in debug builds,
// the performance test of SPEC W43.
@Composable
fun GameApp(platform: GamePlatform) {
    var screen by remember { mutableStateOf(Screen.MENU) }
    when (screen) {
        Screen.RUN -> RunView(stress = false, showFps = platform.isDebugBuild, log = platform::log, onExit = { screen = Screen.MENU })
        Screen.STRESS -> RunView(stress = true, showFps = true, log = platform::log, onExit = { screen = Screen.MENU })
        Screen.MENU -> Menu(platform, onPlay = { screen = Screen.RUN }, onStress = { screen = Screen.STRESS })
    }
}

@Composable
private fun Menu(platform: GamePlatform, onPlay: () -> Unit, onStress: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.app_title), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(Res.string.prototype_note), color = MaterialTheme.colorScheme.onBackground)
        Button(onClick = onPlay) { Text(stringResource(Res.string.play)) }
        if (platform.isDebugBuild) OutlinedButton(onClick = onStress) { Text(stringResource(Res.string.stress_test)) }
        Text(platform.version, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}
