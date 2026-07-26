package eu.kanade.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
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
import kotlinx.coroutines.launch
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
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

// ─── Parallax constants ────────────────────────────────────────────────────────
private const val COVER_PARALLAX_FACTOR = 0.15f
private const val BG_PARALLAX_FACTOR = 0.08f
private const val MAX_PARALLAX_OFFSET_DP = 48f
private const val MAX_BLUR_RADIUS = 20

// ─── Status badge colors ───────────────────────────────────────────────────────
private val StatusOngoing = Color(0xFF4CAF50)
private val StatusCompleted = Color(0xFF2196F3)
private val StatusHiatus = Color(0xFFFF9800)
private val StatusCancelled = Color(0xFFF44336)
private val StatusLicensed = Color(0xFF9C27B0)
private val StatusPublishingFinished = Color(0xFF607D8B)

// ─── Shadow elevations ─────────────────────────────────────────────────────────
private val CardElevation = 6.dp
private val ElevatedCardElevation = 12.dp
private val ChipElevation = 2.dp
private val ActionButtonElevation = 4.dp
private val ActionButtonPressedElevation = 8.dp

// ═══════════════════════════════════════════════════════════════════════════════
// MangaInfoBox — top-level container with parallax background
// ═══════════════════════════════════════════════════════════════════════════════

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
    // ── Parallax scroll state ──────────────────────────────────────────────
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    val maxOffsetPx = with(LocalDensity.current) { MAX_PARALLAX_OFFSET_DP * density }

    val parallaxConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val newOffset = (scrollOffset + available.y).coerceIn(-maxOffsetPx, maxOffsetPx)
                val consumed = newOffset - scrollOffset
                scrollOffset = newOffset
                return androidx.compose.ui.geometry.Offset(0f, consumed)
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                scrollOffset = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(parallaxConnection)
            .clipToBounds(),
    ) {
        // ── Backdrop with parallax + dynamic blur ──────────────────────────
        val bgTranslationY = scrollOffset * BG_PARALLAX_FACTOR
        val blurRadius = ((abs(scrollOffset) / maxOffsetPx) * MAX_BLUR_RADIUS)
            .coerceAtLeast(10f)
            .roundToInt()
            .dp

        val backdropGradientColors = listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.background,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { translationY = bgTranslationY },
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(manga)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(colors = backdropGradientColors),
                        )
                    }
                    .blur(blurRadius)
                    .alpha(0.4f),
            )
        }

        // ── Manga & source info ────────────────────────────────────────────
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (!isTabletUi) {
                MangaAndSourceTitlesSmall(
                    appBarPadding = appBarPadding,
                    manga = manga,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                    scrollOffset = scrollOffset,
                )
            } else {
                MangaAndSourceTitlesLarge(
                    appBarPadding = appBarPadding,
                    manga = manga,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                    scrollOffset = scrollOffset,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MangaActionRow — redesigned with animated buttons, tooltips, badge, gradients
// ═══════════════════════════════════════════════════════════════════════════════

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
    val defaultActionColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
    val primaryColor = MaterialTheme.colorScheme.primary

    val nextUpdateDays = remember(nextUpdate) {
        return@remember if (nextUpdate != null) {
            val now = Instant.now()
            now.until(nextUpdate, ChronoUnit.DAYS).toInt().coerceAtLeast(0)
        } else {
            null
        }
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Favorite button ────────────────────────────────────────────
            AnimatedActionIconButton(
                icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (favorite) {
                    stringResource(MR.strings.in_library)
                } else {
                    stringResource(MR.strings.add_to_library)
                },
                isActive = favorite,
                activeColor = primaryColor,
                inactiveColor = defaultActionColor,
                onClick = onAddToLibraryClicked,
                onLongClick = onEditCategory,
                showHeartBurst = favorite,
            )

            // ── Update interval button ─────────────────────────────────────
            AnimatedActionIconButton(
                icon = Icons.Outlined.Timer,
                contentDescription = when (nextUpdateDays) {
                    null -> stringResource(MR.strings.not_applicable)
                    0 -> stringResource(MR.strings.manga_interval_expected_update_soon)
                    else -> pluralStringResource(MR.plurals.day, count = nextUpdateDays, nextUpdateDays)
                },
                isActive = isUserIntervalMode,
                activeColor = primaryColor,
                inactiveColor = defaultActionColor,
                onClick = { onEditIntervalClicked?.invoke() },
            )

            // ── Tracking button with badge ─────────────────────────────────
            AnimatedActionIconButton(
                icon = if (trackingCount == 0) Icons.Outlined.Sync else Icons.Outlined.TrackChanges,
                contentDescription = if (trackingCount == 0) {
                    stringResource(MR.strings.manga_tracking_tab)
                } else {
                    pluralStringResource(MR.plurals.num_trackers, count = trackingCount, trackingCount)
                },
                isActive = trackingCount > 0,
                activeColor = primaryColor,
                inactiveColor = defaultActionColor,
                badgeCount = if (trackingCount > 0) trackingCount else null,
                onClick = onTrackingClicked,
            )

            // ── WebView ────────────────────────────────────────────────────
            if (onWebViewClicked != null) {
                AnimatedActionIconButton(
                    icon = Icons.Outlined.Public,
                    contentDescription = stringResource(MR.strings.action_web_view),
                    isActive = false,
                    activeColor = primaryColor,
                    inactiveColor = defaultActionColor,
                    onClick = onWebViewClicked,
                    onLongClick = onWebViewLongClicked,
                )
            }

            // ── Copy URL ───────────────────────────────────────────────────
            if (onCopyUrlClicked != null) {
                AnimatedActionIconButton(
                    icon = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy URL",
                    isActive = false,
                    activeColor = primaryColor,
                    inactiveColor = defaultActionColor,
                    onClick = onCopyUrlClicked,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ExpandableMangaDescription — gradient overlay, spring animation, animated tags
// ═══════════════════════════════════════════════════════════════════════════════

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
    Column(modifier = modifier.animateContentSize(animationSpec = spring())) {
        val (expanded, onExpanded) = rememberSaveable {
            mutableStateOf(defaultExpandState)
        }
        val desc =
            description.takeIf { !it.isNullOrBlank() } ?: stringResource(MR.strings.description_placeholder)

        // ── "Synopsis" heading ────────────────────────────────────────────
        Text(
            text = stringResource(MR.strings.synopsis_placeholder),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )

        // ── Description with gradient overlay ──────────────────────────────
        MangaSummary(
            description = desc,
            expanded = expanded,
            notes = notes,
            onEditNotesClicked = onEditNotes,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickableNoIndication { onExpanded(!expanded) },
        )

        // ── Tags ───────────────────────────────────────────────────────────
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        tags.forEachIndexed { index, tag ->
                            AnimatedTagsChip(
                                text = tag,
                                index = index,
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
                            AnimatedTagsChip(
                                text = tag,
                                index = 0,
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

// ═══════════════════════════════════════════════════════════════════════════════
// Private: MangaAndSourceTitlesLarge (tablet)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MangaAndSourceTitlesLarge(
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    scrollOffset: Float = 0f,
) {
    val coverTranslationY = scrollOffset * COVER_PARALLAX_FACTOR

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Cover with parallax ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .graphicsLayer { translationY = coverTranslationY },
        ) {
            MangaCover.Book(
                modifier = Modifier
                    .shadow(
                        elevation = CardElevation,
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
        }

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

// ═══════════════════════════════════════════════════════════════════════════════
// Private: MangaAndSourceTitlesSmall (phone)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MangaAndSourceTitlesSmall(
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    scrollOffset: Float = 0f,
) {
    val coverTranslationY = scrollOffset * COVER_PARALLAX_FACTOR

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Cover with parallax ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .graphicsLayer { translationY = coverTranslationY },
        ) {
            MangaCover.Book(
                modifier = Modifier
                    .shadow(
                        elevation = CardElevation,
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
        }

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

// ═══════════════════════════════════════════════════════════════════════════════
// MangaContentInfo — upgraded typography, color-coded status badges, avatars
// ═══════════════════════════════════════════════════════════════════════════════

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

    // ── Title ──────────────────────────────────────────────────────────────
    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.clickableNoIndication(
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

    Spacer(modifier = Modifier.height(4.dp))

    // ── Author ─────────────────────────────────────────────────────────────
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
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = author?.takeIf { it.isNotBlank() }
                ?: stringResource(MR.strings.unknown_author),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.clickableNoIndication(
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
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = artist,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clickableNoIndication(
                    onLongClick = { context.copyToClipboard(artist, artist) },
                    onClick = { doSearch(artist, true) },
                ),
                textAlign = textAlign,
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // ── Status badge + source ──────────────────────────────────────────────
    Row(
        modifier = Modifier.secondaryItemAlpha(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Color-coded status badge
        val statusColor = when (status) {
            SManga.ONGOING.toLong() -> StatusOngoing
            SManga.COMPLETED.toLong() -> StatusCompleted
            SManga.LICENSED.toLong() -> StatusLicensed
            SManga.PUBLISHING_FINISHED.toLong() -> StatusPublishingFinished
            SManga.CANCELLED.toLong() -> StatusCancelled
            SManga.ON_HIATUS.toLong() -> StatusHiatus
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
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

        StatusBadge(
            text = statusText,
            color = statusColor,
            isPulsing = status == SManga.ONGOING.toLong(),
        )

        DotSeparatorText()
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
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Private: StatusBadge — color-coded with pulsing dot for Ongoing
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    isPulsing: Boolean = false,
) {
    val pulseAlpha by animateFloatAsState(
        targetValue = if (isPulsing) 0.3f else 1f,
        animationSpec = if (isPulsing) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessVeryLow,
            )
        } else {
            spring()
        },
        label = "pulse",
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        modifier = Modifier.padding(end = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Pulsing dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (isPulsing) pulseAlpha else 1f)),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Private: descriptionAnnotator — unchanged business logic
// ═══════════════════════════════════════════════════════════════════════════════

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

// ═══════════════════════════════════════════════════════════════════════════════
// MangaSummary — gradient overlay + spring animation
// ═══════════════════════════════════════════════════════════════════════════════

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
        animationSpec = spring(
            dampingRatio = DampingRatioLowBouncy,
            stiffness = StiffnessLow,
        ),
        label = "summary",
    )
    var infoHeight by remember { mutableIntStateOf(0) }
    Layout(
        modifier = modifier.clipToBounds(),
        contents = listOf(
            {
                Text(
                    // Shows at least 3 lines if no notes
                    // when there are notes show 6
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
                // Gradient overlay when collapsed
                val gradientColors = if (expanded) {
                    listOf(Color.Transparent, Color.Transparent)
                } else {
                    listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                }
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = gradientColors,
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY,
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Animated expand/collapse icon
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = DampingRatioLowBouncy,
                            stiffness = StiffnessLow,
                        ),
                        label = "expandIcon",
                    )
                    Icon(
                        painter = rememberAnimatedVectorPainter(
                            AnimatedImageVector.animatedVectorResource(R.drawable.anim_caret_down),
                            atEnd = !expanded,
                        ),
                        contentDescription = stringResource(
                            if (expanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand,
                        ),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = rotation }
                            .scale(
                                scaleX = if (expanded) 1.2f else 1f,
                                scaleY = if (expanded) 1.2f else 1f,
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

// ═══════════════════════════════════════════════════════════════════════════════
// Private: AnimatedTagsChip — with shadow, entry animation, haptic feedback
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnimatedTagsChip(
    text: String,
    index: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation by animateDpAsState(
        targetValue = if (isPressed) ChipElevation + 2.dp else ChipElevation,
        animationSpec = spring(dampingRatio = DampingRatioLowBouncy),
        label = "chipElevation",
    )

    // Entry animation
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = 0.5f + (index * 0.05f),
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialOffsetY = { it * 2 },
        ) + fadeIn(animationSpec = tween(300 + index * 50)),
        exit = slideOutVertically() + fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = elevation,
            shadowElevation = elevation,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            SuggestionChip(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                label = {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                border = null,
                shape = RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                interactionSource = interactionSource,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Private: AnimatedActionIconButton — premium button with ripple, elevation, badge
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RowScope.AnimatedActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    badgeCount: Int? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    showHeartBurst: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()

    val iconColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = spring(dampingRatio = DampingRatioLowBouncy),
        label = "iconColor",
    )

    val containerColor by animateColorAsState(
        targetValue = if (isActive) {
            activeColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
        },
        animationSpec = spring(dampingRatio = DampingRatioLowBouncy),
        label = "containerColor",
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) ActionButtonPressedElevation else ActionButtonElevation,
        animationSpec = spring(dampingRatio = DampingRatioLowBouncy),
        label = "btnElevation",
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = DampingRatioLowBouncy),
        label = "btnScale",
    )

    // Heart burst animation state
    var showBurst by remember { mutableStateOf(false) }
    val burstScale = remember { Animatable(0f) }
    val burstAlpha = remember { Animatable(0f) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            showBurst = true
            burstScale.snapTo(0.5f)
            burstAlpha.snapTo(0.8f)
            launch {
                burstScale.animateTo(
                    targetValue = 2.5f,
                    animationSpec = spring(
                        dampingRatio = 0.3f,
                        stiffness = Spring.StiffnessHigh,
                    ),
                )
            }
            launch {
                burstAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(400),
                )
            }
            showBurst = false
        }
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .scale(scale)
                .shadow(
                    elevation = elevation,
                    shape = RoundedCornerShape(12.dp),
                ),
            interactionSource = interactionSource,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor,
                contentColor = iconColor,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(22.dp),
                    tint = iconColor,
                )

                // Badge counter
                if (badgeCount != null) {
                    Surface(
                        shape = CircleShape,
                        color = activeColor,
                        contentColor = contentColorFor(activeColor),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(if (badgeCount > 9) 20.dp else 16.dp)
                            .animateContentSize(animationSpec = spring()),
                    ) {
                        Text(
                            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(1.dp),
                        )
                    }
                }

                // Heart burst particles
                if (showBurst) {
                    repeat(4) { i ->
                        val angle = i * 90f
                        val xOffset = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * 12f
                        val yOffset = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * 12f
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = activeColor.copy(alpha = burstAlpha.value),
                            modifier = Modifier
                                .size(12.dp)
                                .graphicsLayer {
                                    scaleX = burstScale.value
                                    scaleY = burstScale.value
                                    translationX = xOffset * burstScale.value
                                    translationY = yOffset * burstScale.value
                                    alpha = burstAlpha.value
                                },
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Private: Legacy TagsChip kept for compatibility — delegates to AnimatedTagsChip
// ═══════════════════════════════════════════════════════════════════════════════

private val DefaultTagChipModifier = Modifier.padding(vertical = 4.dp)

@Composable
private fun TagsChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AnimatedTagsChip(
        text = text,
        index = 0,
        onClick = onClick,
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Private: Legacy MangaActionButton kept for compatibility
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RowScope.MangaActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    // This is kept for backward compatibility but delegates to the new system
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = color.copy(alpha = 0.1f),
                contentColor = color,
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}