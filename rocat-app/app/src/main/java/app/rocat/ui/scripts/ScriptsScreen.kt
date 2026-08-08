package app.rocat.ui.scripts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rocat.di.AppViewModelFactory
import app.rocat.scripting.api.model.Script

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsScreen(
    onOpenScript: (String) -> Unit,
    onImport: () -> Unit,
    viewModel: ScriptsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.scriptsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scripts") },
                actions = {
                    IconButton(onClick = onImport) {
                        Icon(Icons.Filled.Add, contentDescription = "Add script")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.loading -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }

            state.scripts.isEmpty() -> EmptyScripts(onImport, innerPadding)

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            ) {
                items(state.scripts, key = { it.id }) { script ->
                    ScriptListItem(
                        script = script,
                        onToggle = { viewModel.setEnabled(script.id, it) },
                        onClick = { onOpenScript(script.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScripts(onImport: () -> Unit, innerPadding: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No scripts installed",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.heightIn(min = 8.dp))
        Text(
            text = "Import a userscript to start scraping. Go to the Playground tab to test it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.heightIn(min = 16.dp))
        FilledTonalButton(onClick = onImport) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add script")
        }
    }
}

@Composable
private fun ScriptListItem(
    script: Script,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        ListItem(
            headlineContent = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = script.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "v${script.version}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    StatusChip(enabled = script.enabled)
                }
            },
            supportingContent = {
                Text(
                    text = script.description.ifBlank { script.author.ifBlank { "No description" } },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = {
                Switch(checked = script.enabled, onCheckedChange = onToggle)
            },
        )
    }
}

@Composable
private fun StatusChip(enabled: Boolean) {
    val bg = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = if (enabled) "Active" else "Inactive",
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
