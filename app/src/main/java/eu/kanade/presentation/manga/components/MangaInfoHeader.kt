package eu.kanade.presentation.manga.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.delay
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.Surface
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

object MangaInfoHeaderDefaults {
    val SPRING_FAST = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val SPRING_MEDIUM = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val SPRING_SLOW = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
    )

    const val CoverParallaxFactor = 0.3f
    const val BackgroundParallaxFactor = 0.15f
    const val MaxScrollOffset = 500f
    const val BackdropBlurMin = 10f
    const val BackdropBlurMax = 25f

    val StatusOngoingColor = Color(0xFF4CAF50)
    val StatusCompletedColor = Color(0xFF2196F3)
    val StatusHiatusColor = Color(0xFFFF9800)
    val StatusCancelledColor = Color(0xFFF44336)
}

@Composable
fun MangaInfoBox(
    isTabletUi: Boolean,
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrollOffset by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        val maxOffset = MangaInfoHeaderDefaults.MaxScrollOffset
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                scrollOffset = (scrollOffset + available.y)
                    .coerceIn(-maxOffset, maxOffset)
                return Offset.Zero
            }
        }
    }

    val coverTranslationY by remember {
        derivedStateOf<Float> {
            scrollOffset * MangaInfoHeaderDefaults.CoverParallaxFactor
        }
    }
    val backgroundTranslationY by remember {
        derivedStateOf<Float> {
            scrollOffset * MangaInfoHeaderDefaults.BackgroundParallaxFactor
        }
    }
    val backdropBlurRadius by remember {
        derivedStateOf<Dp> {
            val normalized = abs(scrollOffset) / MangaInfoHeaderDefaults.MaxScrollOffset
            val radius = MangaInfoHeaderDefaults.BackdropBlurMin +
                (MangaInfoHeaderDefaults.BackdropBlurMax - MangaInfoHeaderDefaults.BackdropBlurMin) *
                normalized.coerceIn(0f, 1f)
            radius.dp
        }
    }
    val backdropAlpha by remember {
        derivedStateOf<Float> {
            val normalized = abs(scrollOffset) / MangaInfoHeaderDefaults.MaxScrollOffset
            (0.4f + 0.2f * normalized.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        }
    }

    val backdropGradientColors = listOf(
        Color.Transparent,
        MaterialTheme.colorScheme.background,
    )

    Box(
        modifier = modifier.nestedScroll(nestedScrollConnection),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(manga)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .offset(y = backgroundTranslationY.dp)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(colors = backdropGradientColors),
                    )
                }
                .blur(backdropBlurRadius)
                .alpha(backdropAlpha),
        )

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (!isTabletUi) {
                MangaAndSourceTitlesSmall(
                    appBarPadding = appBarPadding,
                    manga = manga,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                    coverTranslationY = coverTranslationY,
                )
            } else {
                MangaAndSourceTitlesLarge(
                    appBarPadding = appBarPadding,
                    manga = manga,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                    coverTranslationY = coverTranslationY,
                )
            }
        }
    }
}

@Composable
fun MangaActionRow(
    favorite: Boolean,
    trackingCount: Int,
    nextUpdate: Instant?,
    isUserIntervalMode: Boolean,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onCopyUrlClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onEditIntervalClicked: (() -> Unit)?,
    onEditCategory: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val defaultActionButtonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)

    val nextUpdateDays = remember(nextUpdate) {
        return@remember if (nextUpdate != null) {
            val now = Instant.now()
            now.until(nextUpdate, ChronoUnit.DAYS).toInt().coerceAtLeast(0)
        } else {
            null
        }
    }

    val containerColor by animateColorAsState(
        targetValue = if (favorite) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        },
        animationSpec = spring<Color>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "actionRowContainer",
    )

    val containerElevation by animateFloatAsState(
        targetValue = if (favorite) 4f else 1f,
        animationSpec = MangaInfoHeaderDefaults.SPRING_MEDIUM,
        label = "containerElevation",
    )

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(
                elevation = containerElevation.dp,
                shape = RoundedCornerShape(16.dp),
            )
            .background(
                color = containerColor,
                shape = RoundedCornerShape(16.dp),
            )
            .then(
                Modifier.border(
                    BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MangaActionButton(
                title = if (favorite) stringResource(MR.strings.in_library) else stringResource(MR.strings.add_to_library),
                icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                color = if (favorite) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
                onClick = onAddToLibraryClicked,
                onLongClick = onEditCategory,
                isSelected = favorite,
            )

            MangaActionButton(
                title = when (nextUpdateDays) {
                    null -> stringResource(MR.strings.not_applicable)
                    0 -> stringResource(MR.strings.manga_interval_expected_update_soon)
                    else -> pluralStringResource(MR.plurals.day, count = nextUpdateDays, nextUpdateDays)
                },
                icon = Icons.Default.HourglassEmpty,
                color = if (isUserIntervalMode) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
                onClick = { onEditIntervalClicked?.invoke() },
                isSelected = isUserIntervalMode,
            )

            MangaActionButton(
                title = if (trackingCount == 0) {
                    stringResource(MR.strings.manga_tracking_tab)
                } else {
                    pluralStringResource(MR.plurals.num_trackers, count = trackingCount, trackingCount)
                },
                icon = if (trackingCount == 0) Icons.Outlined.Sync else Icons.Outlined.Done,
                color = if (trackingCount == 0) defaultActionButtonColor else MaterialTheme.colorScheme.primary,
                onClick = onTrackingClicked,
                badgeCount = if (trackingCount > 0) trackingCount else null,
                isSelected = trackingCount > 0,
            )

            if (onWebViewClicked != null) {
                MangaActionButton(
                    title = stringResource(MR.strings.action_web_view),
                    icon = Icons.Outlined.Public,
                    color = defaultActionButtonColor,
                    onClick = onWebViewClicked,
                    onLongClick = onWebViewLongClicked,
                )
            }

            if (onCopyUrlClicked != null) {
                MangaActionButton(
                    title = "Copy URL",
                    icon = androidx.compose.material.icons.Icons.Outlined.ContentCopy,
                    color = defaultActionButtonColor,
                    onClick = onCopyUrlClicked,
                )
            }
        }
    }
}

@Composable
fun ExpandableMangaDescription(
    defaultExpandState: Boolean,
    description: String?,
    tagsProvider: () -> List<String>?,
    notes: String,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onEditNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val (expanded, onExpanded) = rememberSaveable {
            mutableStateOf(defaultExpandState)
        }
        val desc =
            description.takeIf { !it.isNullOrBlank() } ?: stringResource(MR.strings.description_placeholder)

        Text(
            text = stringResource(MR.strings.synopsis_placeholder),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )

        MangaSummary(
            description = desc,
            expanded = expanded,
            notes = notes,
            onEditNotesClicked = onEditNotes,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickableNoIndication { onExpanded(!expanded) },
        )

        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(vertical = 12.dp)
                    .animateContentSize(animationSpec = spring())
                    .fillMaxWidth(),
            ) {
                var showMenu by remember { mutableStateOf(false) }
                var tagSelected by remember { mutableStateOf("") }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(MR.strings.action_search)) },
                        onClick = {
                            onTagSearch(tagSelected)
                            showMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(MR.strings.action_copy_to_clipboard)) },
                        onClick = {
                            onCopyTagToClipboard(tagSelected)
                            showMenu = false
                        },
                    )
                }

                if (expanded) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        tags.forEach { tag ->
                            TagsChip(
                                modifier = DEFAULT_TAG_CHIP_MODIFIER,
                                text = tag,
                                onClick = {
                                    tagSelected = tag
                                    showMenu = true
                                },
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        items(items = tags) { tag ->
                            TagsChip(
                                modifier = DEFAULT_TAG_CHIP_MODIFIER,
                                text = tag,
                                onClick = {
                                    tagSelected = tag
                                    showMenu = true
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaAndSourceTitlesLarge(
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    coverTranslationY: Float = 0f,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val coverElevation by animateFloatAsState(
            targetValue = 8.dp.value,
            animationSpec = MangaInfoHeaderDefaults.SPRING_MEDIUM,
            label = "coverElevation",
        )

        MangaCover.Book(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .graphicsLayer {
                    translationY = coverTranslationY
                }
                .shadow(
                    elevation = coverElevation.dp,
                    shape = MaterialTheme.shapes.medium,
                )
                .clip(MaterialTheme.shapes.medium),
            data = ImageRequest.Builder(LocalContext.current)
                .data(manga)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(MR.strings.manga_cover),
            onClick = onCoverClick,
        )

        Spacer(modifier = Modifier.height(16.dp))
        MangaContentInfo(
            title = manga.title,
            author = manga.author,
            artist = manga.artist,
            status = manga.status,
            sourceName = sourceName,
            isStubSource = isStubSource,
            doSearch = doSearch,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MangaAndSourceTitlesSmall(
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    coverTranslationY: Float = 0f,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val coverElevation by animateFloatAsState(
            targetValue = 8.dp.value,
            animationSpec = MangaInfoHeaderDefaults.SPRING_MEDIUM,
            label = "coverElevation",
        )

        MangaCover.Book(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .graphicsLayer {
                    translationY = coverTranslationY
                }
                .shadow(
                    elevation = coverElevation.dp,
                    shape = MaterialTheme.shapes.medium,
                )
                .clip(MaterialTheme.shapes.medium),
            data = ImageRequest.Builder(LocalContext.current)
                .data(manga)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(MR.strings.manga_cover),
            onClick = onCoverClick,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MangaContentInfo(
                title = manga.title,
                author = manga.author,
                artist = manga.artist,
                status = manga.status,
                sourceName = sourceName,
                isStubSource = isStubSource,
                doSearch = doSearch,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ColumnScope.MangaContentInfo(
    title: String,
    author: String?,
    artist: String?,
    status: Long,
    sourceName: String,
    isStubSource: Boolean,
    doSearch: (query: String, global: Boolean) -> Unit,
    textAlign: TextAlign? = LocalTextStyle.current.textAlign,
) {
    val context = LocalContext.current

    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier
            .clickableNoIndication(
                onLongClick = {
                    if (title.isNotBlank()) {
                        context.copyToClipboard(title, title)
                    }
                },
                onClick = { if (title.isNotBlank()) doSearch(title, true) },
            ),
        textAlign = textAlign,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PersonOutline,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = author?.takeIf { it.isNotBlank() }
                ?: stringResource(MR.strings.unknown_author),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .clickableNoIndication(
                    onLongClick = {
                        if (!author.isNullOrBlank()) {
                            context.copyToClipboard(author, author)
                        }
                    },
                    onClick = { if (!author.isNullOrBlank()) doSearch(author, true) },
                ),
            textAlign = textAlign,
        )
    }

    if (!artist.isNullOrBlank() && author != artist) {
        Row(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Brush,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = artist,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .clickableNoIndication(
                        onLongClick = { context.copyToClipboard(artist, artist) },
                        onClick = { doSearch(artist, true) },
                    ),
                textAlign = textAlign,
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(status = status)
        Spacer(modifier = Modifier.width(8.dp))
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            if (isStubSource) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = sourceName,
                modifier = Modifier.clickableNoIndication {
                    doSearch(sourceName, false)
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: Long) {
    val statusColor = when (status) {
        SManga.ONGOING.toLong() -> MangaInfoHeaderDefaults.StatusOngoingColor
        SManga.COMPLETED.toLong() -> MangaInfoHeaderDefaults.StatusCompletedColor
        SManga.ON_HIATUS.toLong() -> MangaInfoHeaderDefaults.StatusHiatusColor
        SManga.CANCELLED.toLong() -> MangaInfoHeaderDefaults.StatusCancelledColor
        else -> MaterialTheme.colorScheme.outline
    }

    val statusText = when (status) {
        SManga.ONGOING.toLong() -> stringResource(MR.strings.ongoing)
        SManga.COMPLETED.toLong() -> stringResource(MR.strings.completed)
        SManga.LICENSED.toLong() -> stringResource(MR.strings.licensed)
        SManga.PUBLISHING_FINISHED.toLong() -> stringResource(MR.strings.publishing_finished)
        SManga.CANCELLED.toLong() -> stringResource(MR.strings.cancelled)
        SManga.ON_HIATUS.toLong() -> stringResource(MR.strings.on_hiatus)
        else -> stringResource(MR.strings.unknown)
    }

    val badgeShape: Shape = RoundedCornerShape(6.dp)

    if (status == SManga.ONGOING.toLong()) {
        val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable<Float>(
                animation = tween(durationMillis = 1200),
            ),
            label = "pulseAlpha",
        )

        Box(
            modifier = Modifier
                .background(color = statusColor.copy(alpha = 0.15f), shape = badgeShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .alpha(pulseAlpha),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .background(color = statusColor.copy(alpha = 0.15f), shape = badgeShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
        }
    }
}

@Composable
private fun descriptionAnnotator(loadImages: Boolean, linkStyle: SpanStyle) = remember(loadImages, linkStyle) {
    markdownAnnotator(
        annotate = { content, child ->
            if (!loadImages && child.type == MarkdownElementTypes.IMAGE) {
                val inlineLink = child.findChildOfType(MarkdownElementTypes.INLINE_LINK)

                val url = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getUnescapedTextInNode(content)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.AUTOLINK)
                        ?.findChildOfType(MarkdownTokenTypes.AUTOLINK)
                        ?.getUnescapedTextInNode(content)
                    ?: return@markdownAnnotator false

                val textNode = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TITLE)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TEXT)
                val altText = textNode?.findChildOfType(MarkdownTokenTypes.TEXT)
                    ?.getUnescapedTextInNode(content).orEmpty()

                withLink(LinkAnnotation.Url(url = url)) {
                    pushStyle(linkStyle)
                    appendInlineContent(MARKDOWN_INLINE_IMAGE_TAG)
                    append(altText)
                    pop()
                }

                return@markdownAnnotator true
            }

            if (child.type in DISALLOWED_MARKDOWN_TYPES) {
                append(content.substring(child.startOffset, child.endOffset))
                return@markdownAnnotator true
            }

            false
        },
        config = markdownAnnotatorConfig(
            eolAsNewLine = true,
        ),
    )
}

@Composable
private fun MangaSummary(
    description: String,
    notes: String,
    expanded: Boolean,
    onEditNotesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = remember { Injekt.get<UiPreferences>() }
    val loadImages = remember { preferences.imagesInDescription.get() }
    val animProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = MangaInfoHeaderDefaults.SPRING_MEDIUM,
        label = "summary",
    )
    var infoHeight by remember { mutableIntStateOf(0) }
    Layout(
        modifier = modifier.clipToBounds(),
        contents = listOf(
            {
                Text(
                    text = if (notes.isBlank()) "\n\n" else "\n\n\n\n\n",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            {
                Column(
                    modifier = Modifier.onSizeChanged { size ->
                        infoHeight = size.height
                    },
                ) {
                    MangaNotesSection(
                        content = notes,
                        expanded = expanded,
                        onEditNotes = onEditNotesClicked,
                    )
                    SelectionContainer {
                        MarkdownRender(
                            content = description,
                            modifier = Modifier.secondaryItemAlpha(),
                            annotator = descriptionAnnotator(
                                loadImages = loadImages,
                                linkStyle = getMarkdownLinkStyle().toSpanStyle(),
                            ),
                            loadImages = loadImages,
                        )
                    }
                }
            },
            {
                Box(
                    modifier = Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_caret_down)
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = MangaInfoHeaderDefaults.SPRING_MEDIUM,
                        label = "expandRotation",
                    )
                    Icon(
                        painter = rememberAnimatedVectorPainter(image, !expanded),
                        contentDescription = stringResource(
                            if (expanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand,
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = rotation }
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                }
            },
        ),
    ) { (shrunk, actual, scrim), constraints ->
        val shrunkHeight = shrunk.single()
            .measure(constraints)
            .height
        val heightDelta = infoHeight - shrunkHeight
        val scrimHeight = 24.dp.roundToPx()

        val actualPlaceable = actual.single()
            .measure(constraints)
        val scrimPlaceable = scrim.single()
            .measure(Constraints.fixed(width = constraints.maxWidth, height = scrimHeight))

        val currentHeight = shrunkHeight + ((heightDelta + scrimHeight) * animProgress).roundToInt()
        layout(constraints.maxWidth, currentHeight) {
            actualPlaceable.place(0, 0)

            val scrimY = currentHeight - scrimHeight
            scrimPlaceable.place(0, scrimY)
        }
    }
}

private val DEFAULT_TAG_CHIP_MODIFIER = Modifier.padding(vertical = 4.dp)

@Composable
private fun TagsChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            modifier = modifier
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp),
                ),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowScope.MangaActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    badgeCount: Int? = null,
    isSelected: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 6f else if (isSelected) 3f else 1f,
        animationSpec = MangaInfoHeaderDefaults.SPRING_FAST,
        label = "buttonElevation",
    )

    Surface(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
        onClick = onClick,
        onLongClick = onLongClick,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = if (isSelected) 0.15f else 0.08f),
        tonalElevation = elevation.dp,
        shadowElevation = elevation.dp,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(20.dp),
                    )
                    if (badgeCount != null) {
                        val badgeScale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessHigh,
                            ),
                            label = "badgeScale",
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = 8.dp, y = (-4).dp)
                                .graphicsLayer {
                                    scaleX = badgeScale
                                    scaleY = badgeScale
                                }
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
