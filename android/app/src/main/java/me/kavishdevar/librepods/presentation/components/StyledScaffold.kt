/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.flow.collect
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalIsDarkTheme

/**
 * How far content may scroll before it reaches the bar at all. The Apple screens
 * start their content one cardColumnTopInset below the bar's bottom edge, so
 * until that much has gone by nothing is behind the bar and the divider would be
 * marking an overlap that has not happened. Material lays its own bar directly on
 * the content, so it has no such slack.
 */
@Composable
private fun topBarOverlapThresholdPx(): Int {
    val appleMetrics = LocalAppleDesignMetrics.current
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    return with(LocalDensity.current) {
        if (m3eEnabled) 0 else appleMetrics.cardColumnTopInset.roundToPx()
    }
}

/**
 * The bar's divider follows a scroll container, but a container can leave the
 * composition while its screen stays — the AirPods settings swap in a
 * non-scrolling "not connected" state without changing screen — and the last
 * value reported would otherwise stick, drawing a divider over content that
 * cannot scroll. Reset on the way out.
 */
@Composable
private fun ResetScrollReportOnDispose(onScrollStateChanged: (Boolean) -> Unit) {
    // Keyed on Unit deliberately: the callers build this lambda inline, so keying
    // on it would re-run the effect on recomposition and report a spurious reset
    // mid-scroll.
    val currentCallback by rememberUpdatedState(onScrollStateChanged)
    DisposableEffect(Unit) {
        onDispose { currentCallback(false) }
    }
}

@Composable
internal fun ReportStyledScaffoldScrollState(
    scrollState: ScrollState,
    onScrollStateChanged: (Boolean) -> Unit
) {
    val threshold = topBarOverlapThresholdPx()
    LaunchedEffect(scrollState, onScrollStateChanged, threshold) {
        snapshotFlow { scrollState.value > threshold }
            .collect { onScrollStateChanged(it) }
    }
    ResetScrollReportOnDispose(onScrollStateChanged)
}

@Composable
internal fun ReportStyledScaffoldScrollState(
    listState: LazyListState,
    onScrollStateChanged: (Boolean) -> Unit
) {
    val threshold = topBarOverlapThresholdPx()
    LaunchedEffect(listState, onScrollStateChanged, threshold) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > threshold
        }.collect { onScrollStateChanged(it) }
    }
    ResetScrollReportOnDispose(onScrollStateChanged)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledScaffold(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    title: String,
    showBackButton: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actionButtons: List<@Composable (backdrop: LayerBackdrop) -> Unit> = emptyList(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    isContentScrolled: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDarkTheme = LocalIsDarkTheme.current

    when (LocalDesignSystem.current) {
        DesignSystem.Material -> {
            Scaffold(
                // A screen that is all list has to sit on a container tone:
                // M3's segmented list item is itself surface, so the 2dp gaps and
                // the rounded ends only show against something darker. The plain
                // "body is surface" rule is for screens whose content is not the
                // list itself.
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
                    ) {
                        TopAppBar(
                            navigationIcon = {
                                if (showBackButton) {
                                    Row {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        FilledTonalIconButton(
                                            onClick = onNavigateBack,
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Narrow)),
                                            shape = IconButtonDefaults.mediumRoundShape
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Default.ArrowBack,
                                                contentDescription = "",
                                                modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                                            )
                                        }
                                    }
                                }
                            },
                            title = {
                                Crossfade(targetState = title) {
                                    Text(
                                        text = it,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = if (showBackButton) 8.dp else 12.dp, end = 12.dp),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            },
                            actions = {
                                actionButtons.forEach { actionButton ->
                                    actionButton(rememberLayerBackdrop())
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            },
                            // M3 lifts the bar a tone once content passes
                            // under it. It names surface container for that,
                            // measured from a body on surface; ours already
                            // starts there, so the pair moves up one step and
                            // keeps the same one-tone gap.
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor =
                                    if (isContentScrolled) MaterialTheme.colorScheme.surfaceContainerHigh
                                    else MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    }
                },
            ) { paddingValues ->
                Box(
                    modifier = modifier
                        .then(if (visible) Modifier.padding(paddingValues) else Modifier)
                        .fillMaxSize()
                ) {
                    content()
                }
            }
        }
        DesignSystem.Apple -> {
            val appleMetrics = LocalAppleDesignMetrics.current
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                val topPadding = paddingValues.calculateTopPadding()
                val startPadding = paddingValues.calculateLeftPadding(LocalLayoutDirection.current)
                val endPadding = paddingValues.calculateRightPadding(LocalLayoutDirection.current)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = startPadding, end = endPadding)
                ) {
                    val contentBackdrop = rememberLayerBackdrop()
                    val backdrop = rememberLayerBackdrop()
                    AnimatedVisibility(
                        visible = showBackButton,
                        // Apple's back button has no entrance of its own: it holds its size
                        // and fades with the screen, over about 150ms in and 100ms out,
                        // measured off a 60fps capture of the bar. It also has no layer to
                        // scale, which is what left an edge behind on the last frames.
                        enter = fadeIn(animationSpec = tween(150)),
                        // No scale on the way out. Shrinking the layer that samples the
                        // backdrop leaves a clipped edge on the last frames, and at 100ms
                        // it lands right as the button vanishes.
                        exit = fadeOut(animationSpec = tween(100)),
                        modifier = Modifier
                            .zIndex(3f)
                            .padding(top = topPadding, start = 8.dp)
                            .align(Alignment.TopStart)
                    ) {
                        StyledIconButton(
                            onClick = onNavigateBack,
                            icon = R.drawable.sf_chevron_backward,
                            contentDescription = stringResource(R.string.back),
                            // Measured off Apple's bar: the chevron sits 3% of the
                            // diameter left of centre, which on a 45dp button is 1.4.
                            opticalOffsetX = (-1.4).dp,
                            backdrop = backdrop
                        )
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + scaleIn(
                            initialScale = 0f,
                            animationSpec = tween()
                        ),
                        exit = fadeOut() + scaleOut(
                            targetScale = 0.5f,
                            animationSpec = tween(100)
                        ),
                        modifier = Modifier
                            .zIndex(2f)
                            .height(
                                appleMetrics.navigationBarHeight + topPadding +
                                    appleMetrics.navigationBarScrolledExtra
                            )
                            .fillMaxWidth()
                    ){
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AnimatedVisibility(
                                visible = isContentScrolled,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawBackdrop(
                                            backdrop = contentBackdrop,
                                            exportedBackdrop = backdrop,
                                            shape = { RectangleShape },
                                            highlight = { Highlight.Ambient.copy(alpha = 0f) },
                                            shadow = { null },
                                            effects = {
                                                vibrancy()
                                                blur(6f.dp.toPx())
                                            },
                                            onDrawSurface = {
                                                // Only ever drawn once content is
                                                // under the bar, so this cannot
                                                // band against an empty page. The
                                                // tint sits a shade above the page
                                                // rather than level with it, which
                                                // is what lifts the bar off the
                                                // content passing under it.
                                                drawRect(
                                                    if (isDarkTheme) Color.Black.copy(0.55f)
                                                    else Color(0xFFF9F9FE).copy(alpha = 0.85f)
                                                )
                                                // Apple closes the bar with a
                                                // hairline, not a shadow.
                                                val hairline = 0.5.dp.toPx()
                                                drawRect(
                                                    color = if (isDarkTheme) Color.White.copy(0.15f)
                                                    else Color.Black.copy(0.12f),
                                                    topLeft = Offset(0f, size.height - hairline),
                                                    size = Size(size.width, hairline)
                                                )
                                            }
                                        )
                                )
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                Spacer(modifier = Modifier.height(topPadding + 12.dp))
                                Crossfade(targetState = title) {
                                    Text(
                                        text = it,
                                        style = appleMetrics.navigationBarTitleStyle,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = visible && actionButtons.isNotEmpty(),
                        enter = fadeIn() + scaleIn(
                            initialScale = 0f,
                            animationSpec = tween()
                        ),
                        exit = fadeOut() + scaleOut(
                            targetScale = 0.5f,
                            animationSpec = tween(100)
                        ),
                        modifier = Modifier
                            .zIndex(3f)
                            .padding(top = topPadding, end = 8.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Row{
                            actionButtons.forEach { actionButton ->
                                actionButton(backdrop)
                            }
                        }
                    }

                    Box(
                        modifier = modifier
                            .layerBackdrop(contentBackdrop)
                            .fillMaxSize()
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
