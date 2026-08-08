package app.rocat.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import app.rocat.ui.detail.ScriptDetailScreen
import app.rocat.ui.import.ImportScriptScreen
import app.rocat.ui.playground.PlaygroundScreen
import app.rocat.ui.scripts.ScriptsScreen

/** Lightweight, dependency-free navigation mirroring mihon's extension screens. */
sealed interface Screen {
    data object Scripts : Screen
    data class Detail(val scriptId: String) : Screen
    data object Import : Screen
    data object Playground : Screen
}

private const val KEY_SCRIPTS = "scripts"
private const val KEY_IMPORT = "import"
private const val KEY_PLAYGROUND = "playground"
private const val KEY_DETAIL_PREFIX = "detail:"

private fun encode(screen: Screen): String = when (screen) {
    is Screen.Scripts -> KEY_SCRIPTS
    is Screen.Import -> KEY_IMPORT
    is Screen.Playground -> KEY_PLAYGROUND
    is Screen.Detail -> KEY_DETAIL_PREFIX + screen.scriptId
}

private fun decode(key: String): Screen = when {
    key == KEY_SCRIPTS -> Screen.Scripts
    key == KEY_IMPORT -> Screen.Import
    key == KEY_PLAYGROUND -> Screen.Playground
    key.startsWith(KEY_DETAIL_PREFIX) -> Screen.Detail(key.removePrefix(KEY_DETAIL_PREFIX))
    else -> Screen.Scripts
}

@Composable
fun RoCatApp() {
    val backStack = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { mutableStateListOf(KEY_SCRIPTS) }
    val current = decode(backStack.last())

    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
    }

    fun navigate(screen: Screen) {
        val key = encode(screen)
        // Tab-like behaviour: jumping to an existing destination pops the stack to it.
        val index = backStack.indexOf(key)
        if (index >= 0) {
            while (backStack.size > index + 1) backStack.removeAt(backStack.size - 1)
        } else {
            backStack.add(key)
        }
    }

    BackHandler(enabled = backStack.size > 1) { goBack() }

    Scaffold(
        bottomBar = {
            if (current is Screen.Scripts || current is Screen.Playground) {
                NavigationBar {
                    NavigationBarItem(
                        selected = current is Screen.Scripts,
                        onClick = { navigate(Screen.Scripts) },
                        icon = { Icon(Icons.Filled.Extension, contentDescription = "Scripts") },
                        label = { Text("Scripts") },
                    )
                    NavigationBarItem(
                        selected = current is Screen.Playground,
                        onClick = { navigate(Screen.Playground) },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Playground") },
                        label = { Text("Playground") },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (current) {
                is Screen.Scripts -> ScriptsScreen(
                    onOpenScript = { navigate(Screen.Detail(it)) },
                    onImport = { navigate(Screen.Import) },
                )
                is Screen.Detail -> ScriptDetailScreen(scriptId = current.scriptId, onBack = ::goBack)
                is Screen.Import -> ImportScriptScreen(onBack = ::goBack)
                is Screen.Playground -> PlaygroundScreen(onBack = ::goBack)
            }
        }
    }
}
