package app.rocat.ui.playground

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rocat.di.AppViewModelFactory
import app.rocat.scripting.api.model.Script
import coil3.compose.AsyncImage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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

            TestFunctionSection(
                function = state.testFunction,
                suggestions = state.testFunctionSuggestions,
                onFunctionChange = viewModel::onTestFunctionChange,
                args = state.testArgs,
                onAddArg = viewModel::addArg,
                onRemoveArg = viewModel::removeArg,
                onValueChange = viewModel::updateArgValue,
                onRun = viewModel::runFunction,
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

/**
 * A parsed media payload returned by a script function, following the contract
 * `{ "media_type": "image" | "video", "media_url": "..." }`.
 */
private sealed interface MediaPreview {
    data class Image(val url: String) : MediaPreview
    data class Video(val url: String) : MediaPreview
}

/**
 * Extracts a [MediaPreview] from the script's JSON output when it follows the media
 * contract; returns null for anything else (plain JSON, arrays, invalid text).
 */
private fun parseMediaPreview(raw: String): MediaPreview? {
    val element = try {
        Json.parseToJsonElement(raw)
    } catch (e: Exception) {
        return null
    }
    val obj = element as? JsonObject ?: return null
    val mediaType = (obj["media_type"] as? JsonPrimitive)?.content ?: return null
    val url = (obj["media_url"] as? JsonPrimitive)?.content ?: return null
    return when (mediaType) {
        "image" -> MediaPreview.Image(url)
        "video" -> MediaPreview.Video(url)
        else -> null
    }
}

/**
 * Renders a [MediaPreview] above the JSON log: images are loaded with Coil's
 * [AsyncImage], videos show a "Play Video" button that opens the system video player.
 */
@Composable
private fun MediaPreviewRenderer(preview: MediaPreview, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    when (preview) {
        is MediaPreview.Image -> {
            ElevatedCard(modifier = modifier.fillMaxWidth()) {
                AsyncImage(
                    model = preview.url,
                    contentDescription = "Script media preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .padding(8.dp),
                )
            }
        }

        is MediaPreview.Video -> {
            ElevatedCard(modifier = modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Media output (video)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(preview.url), "video/*")
                            }
                            runCatching { context.startActivity(intent) }
                                .onFailure {
                                    Toast.makeText(context, "No video player available", Toast.LENGTH_SHORT).show()
                                }
                        },
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play Video")
                    }
                }
            }
        }
    }
}

/**
 * Card that renders [content] in a scrollable monospace text area and offers a small
 * action bar to copy the output to the clipboard either as pretty-printed JSON or as
 * the raw text/HTML. When the output follows the media contract, a preview of the
 * image/video is rendered above the text.
 */
@Composable
private fun CopyableResultCard(
    title: String,
    emptyHint: String,
    content: String,
    maxHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val displayText = ResultFormatter.prettyJson(content)
    val mediaPreview = remember(content) { parseMediaPreview(content) }

    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))

            if (content.isEmpty()) {
                Text(
                    text = emptyHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (mediaPreview != null) {
                    MediaPreviewRenderer(preview = mediaPreview)
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "JSON",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(displayText))
                            Toast.makeText(context, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.width(16.dp).height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy JSON")
                    }
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(displayText))
                            Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.width(16.dp).height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy Text")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SelectionContainer {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

/**
 * "Test Execution" section: pick any function inside the selected script and feed it
 * a dynamic, user-editable list of arguments. Returns the JSON output in a
 * scrollable, copyable log area.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestFunctionSection(
    function: String,
    suggestions: List<String>,
    onFunctionChange: (String) -> Unit,
    args: List<String>,
    onAddArg: () -> Unit,
    onRemoveArg: (Int) -> Unit,
    onValueChange: (Int, String) -> Unit,
    onRun: () -> Unit,
    executing: Boolean,
    log: String,
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Test Function",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Invoke any public function inside the script. Add as many value inputs as it needs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = function,
                    onValueChange = { onFunctionChange(it); expanded = false },
                    readOnly = false,
                    label = { Text("Function name") },
                    placeholder = { Text("e.g. search, detail, main") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)) },
                            onClick = {
                                onFunctionChange(suggestion)
                                expanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Inputs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            args.forEachIndexed { index, value ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { onValueChange(index, it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Argument Value (e.g. URL)") },
                        singleLine = true,
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { onRemoveArg(index) },
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Remove input",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            TextButton(onClick = onAddArg, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add Input")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onRun,
                enabled = !executing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (executing) {
                    CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (executing) "Running…" else "Run Function")
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
                Spacer(Modifier.height(8.dp))
                CopyableResultCard(
                    title = "Log",
                    emptyHint = "",
                    content = log,
                    maxHeight = 320.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}