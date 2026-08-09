package app.rocat.ui.playground

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rocat.di.AppViewModelFactory
import app.rocat.i18n.StringKey
import app.rocat.i18n.stringResource
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
    val components = viewModel.uiComponents

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(StringKey.playground)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(StringKey.back))
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
                    text = stringResource(StringKey.noEnabledScripts),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "script_picker") {
                ScriptPicker(
                    scripts = state.scripts,
                    selected = state.selectedId,
                    onSelect = viewModel::select,
                )
            }

            item(key = "toolbar") {
                BuildUiToolbar(
                    onRebuild = viewModel::rebuildUi,
                )
            }

            itemsIndexed(components, key = { index, _ -> index }) { _, component ->
                when (component) {
                    is ScriptUIComponent.Input -> InputComponent(
                        component = component,
                        onValueChange = viewModel::updateInputValue,
                    )

                    is ScriptUIComponent.Button -> ButtonComponent(
                        label = component.label,
                        executing = state.executing,
                        onClick = { viewModel.onScriptButton(component.functionName) },
                    )

                    is ScriptUIComponent.Thumbnail -> ThumbnailComponent(url = component.url)

                    is ScriptUIComponent.Video -> VideoComponent(url = component.url)

                    is ScriptUIComponent.LogText -> LogComponent(text = component.text)

                    is ScriptUIComponent.Grid -> GridComponent(
                        grid = component,
                        onItemClick = { item ->
                            viewModel.onGridClick(component.onClickFunction, item.rawJsonPayload)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            item(key = "output") {
                OutputConsole(
                    log = state.log,
                    executing = state.executing,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptPicker(
    scripts: List<Script>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedScript = scripts.firstOrNull { it.id == selected }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedScript?.name ?: stringResource(StringKey.selectScript),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(StringKey.script)) },
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
 * Small toolbar describing the script-driven mode and letting the user re-run the
 * script's `buildUI()` to rebuild the current component list.
 */
@Composable
private fun BuildUiToolbar(
    onRebuild: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(StringKey.scriptDrivenUi),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(StringKey.scriptDrivenUiBody),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onRebuild) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.width(16.dp).height(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(StringKey.buildUi))
            }
        }
    }
}

@Composable
private fun InputComponent(
    component: ScriptUIComponent.Input,
    onValueChange: (String, String) -> Unit,
) {
    OutlinedTextField(
        value = component.value,
        onValueChange = { value -> onValueChange(component.id, value) },
        label = { Text(component.hint) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

@Composable
private fun ButtonComponent(
    label: String,
    executing: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !executing,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        if (executing) {
            CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label)
    }
}

@Composable
private fun ThumbnailComponent(url: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        AsyncImage(
            model = url,
            contentDescription = "Script thumbnail preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .padding(8.dp),
        )
    }
}

@Composable
private fun VideoComponent(url: String) {
    val context = LocalContext.current
    val noVideoPlayer = stringResource(StringKey.noVideoPlayer)
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(StringKey.videoPreview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(url), "video/*")
                    }
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            Toast.makeText(context, noVideoPlayer, Toast.LENGTH_SHORT).show()
                        }
                },
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(StringKey.playVideo))
            }
        }
    }
}

@Composable
private fun LogComponent(text: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}

/**
 * Renders the return value (or error) of the last script button invocation. Reuses the
 * copyable/pretty-printed card so the output behaves like a console.
 */
@Composable
private fun OutputConsole(
    log: String,
    executing: Boolean,
) {
    if (log.isEmpty() && !executing) return

    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(StringKey.output),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (executing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(StringKey.running), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (log.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                CopyableResultCard(
                    title = stringResource(StringKey.console),
                    emptyHint = "",
                    content = log,
                    maxHeight = 320.dp,
                )
            }
        }
    }
}

/**
 * Parses a `{ "media_type": "image" | "video", "media_url": "..." }` payload out of a
 * script's JSON output so it can be previewed inline (kept for legacy scripts).
 */
private sealed interface MediaPreview {
    data class Image(val url: String) : MediaPreview
    data class Video(val url: String) : MediaPreview
}

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

@Composable
private fun MediaPreviewRenderer(preview: MediaPreview, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val noVideoPlayer = stringResource(StringKey.noVideoPlayer)
    when (preview) {
        is MediaPreview.Image -> {
            ElevatedCard(modifier = modifier.fillMaxWidth()) {
                AsyncImage(
                    model = preview.url,
                    contentDescription = "Legacy script media preview",
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
                        stringResource(StringKey.mediaOutputVideo),
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
                                    Toast.makeText(context, noVideoPlayer, Toast.LENGTH_SHORT).show()
                                }
                        },
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(StringKey.playVideo))
                    }
                }
            }
        }
    }
}

/**
 * Scrollable, copyable card used for the console output. Pretty-prints valid JSON,
 * and renders a media preview when the output follows the legacy `media_type` contract.
 */
@Composable
private fun CopyableResultCard(
    title: String,
    emptyHint: String,
    content: String,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val jsonCopied = stringResource(StringKey.jsonCopied)
    val textCopied = stringResource(StringKey.textCopied)
    val displayText = ResultFormatter.prettyJson(content)
    val mediaPreview = remember(content) { parseMediaPreview(content) }

    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = stringResource(StringKey.json),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(displayText))
                            Toast.makeText(context, jsonCopied, Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.width(16.dp).height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(StringKey.copyJson))
                    }
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(displayText))
                            Toast.makeText(context, textCopied, Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.width(16.dp).height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(StringKey.copyText))
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