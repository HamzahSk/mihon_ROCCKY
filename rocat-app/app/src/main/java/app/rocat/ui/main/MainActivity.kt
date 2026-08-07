package app.rocat.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rocat.scripting.api.model.Script

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoCatTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.scriptsState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("RoCat — Scripts") }) },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            items(state.scripts, key = { it.id }) { script ->
                ScriptItem(script)
            }
        }
    }
}

@Composable
private fun ScriptItem(script: Script) {
    ListItem(
        headlineContent = { Text(script.name) },
        supportingContent = { Text(script.description.ifBlank { script.id }) },
        trailingContent = { Switch(checked = script.enabled, onCheckedChange = null) },
    )
}

@Composable
fun RoCatTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    RoCatTheme { Text("Preview") }
}