package app.rocat.ui.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rocat.di.AppViewModelFactory
import app.rocat.scripting.api.model.Script

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundScreen(
    onBack: () -> Unit,
    viewModel: PlaygroundViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playground") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.scripts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No enabled scripts. Enable or add a script first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
        ) {
            ScriptPicker(
                scripts = state.scripts,
                selectedId = state.selectedId,
                onSelect = viewModel::select,
            )
            OutlinedTextField(
                value = state.targetUrl,
                onValueChange = viewModel::onUrlChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                label = { Text("Target URL") },
                singleLine = true,
            )
            Button(
                onClick = viewModel::run,
                enabled = !state.running,
                modifier = Modifier.padding(16.dp),
            ) {
                if (state.running) {
                    CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.running) "Running…" else "Run")
            }

            ResultCard(state.result)
            TestExecutionSection(
                param = state.testParam,
                onParamChange = viewModel::onTestParamChange,
                onSearch = viewModel::runSearch,
                onDetail = viewModel::runDetail,
                executing = state.executing,
                log = state.log,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptPicker(
    scripts: List<Script>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = scripts.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = selected?.name ?: "Select a script",
            onValueChange = {},
            readOnly = true,
            label = { Text("Script") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            scripts.forEach { script ->
                DropdownMenuItem(
                    text = { Text(script.name) },
                    onClick = {
                        onSelect(script.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ResultCard(result: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Result", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (result.isEmpty()) {
                Text(
                    text = "No output yet. Run the script to see its return value here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

/**
 * "Test Execution" section: runs a specific function in the selected script
 * (`search(query)` or `detail(url)`) with a dynamic parameter typed in the UI,
 * then shows the JSON return value in a scrollable log area.
 */
@Composable
private fun TestExecutionSection(
    param: String,
    onParamChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDetail: () -> Unit,
    executing: Boolean,
    log: String,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Test Execution", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Call a specific function inside the script and see its JSON output.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = param,
                onValueChange = onParamChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Parameter (query or URL)") },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSearch, enabled = !executing) {
                    if (executing) {
                        CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Run Search")
                }
                OutlinedButton(onClick = onDetail, enabled = !executing) {
                    Text("Run Detail")
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            if (log.isEmpty()) {
                Text(
                    text = "Function output will appear here as JSON.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}
