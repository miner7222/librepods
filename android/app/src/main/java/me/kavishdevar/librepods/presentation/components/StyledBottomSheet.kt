package me.kavishdevar.librepods.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    backdrop: LayerBackdrop,
    content: @Composable (innerBackdrop: LayerBackdrop, progress: Float) -> Unit
) {
    if (!visible) return

    val isDarkTheme = isSystemInDarkTheme()
    val isApple = LocalDesignSystem.current == DesignSystem.Apple
    val sheetState = rememberModalBottomSheetState(false) // move this to parent composable

    val isExpanded =  sheetState.targetValue == SheetValue.Expanded

    val progress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        label = "sheetProgress"
    )

    val animatedCorner = lerp(48.dp, 42.dp, progress)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isApple) Color.Transparent else BottomSheetDefaults.ContainerColor,
        dragHandle = if (isApple) {
            // The iOS grabber is drawn inside the glass instead.
            { }
        } else {
            { BottomSheetDefaults.DragHandle() }
        },
        shape = RoundedCornerShape(animatedCorner),
        scrimColor = Color.Transparent,
        modifier = Modifier.padding(4.dp)
    ) {
        val innerBackdrop = rememberLayerBackdrop()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(animatedCorner))
                // Material draws its own sheet surface; the glass is the iOS look.
                .then(if (!isApple) Modifier else Modifier.drawBackdrop(
                    backdrop = backdrop,
                    exportedBackdrop = innerBackdrop,
                    shape = { RoundedCornerShape(animatedCorner) },
                    effects = {
                        vibrancy()
                        blur(24f.dp.toPx())
                        lens(12f.dp.toPx(), 48f.dp.toPx(), true)
                    },
                    onDrawSurface = {
                        drawRect(
                            if (isDarkTheme) Color.DarkGray.copy(alpha = 0.3f) else Color(
                                0xFFE0E0E0
                            ).copy(alpha = 0.45f)
                        )
                        // iOS 27 separates glass from whatever sits behind it
                        // with a subtle dark ring rather than a bare edge.
                        drawRoundRect(
                            color = Color.Black.copy(alpha = if (isDarkTheme) 0.35f else 0.12f),
                            cornerRadius = CornerRadius(animatedCorner.toPx()),
                            style = Stroke(width = 1f.dp.toPx())
                        )
                    }
                ))
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The sheet resizes, and iOS puts a grabber on anything that
                // does - it is the only cue that dragging will do something.
                if (isApple) Box(
                    modifier = Modifier
                        .padding(bottom = 11.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .background(
                            if (isDarkTheme) Color.White.copy(alpha = 0.3f)
                            else Color.Black.copy(alpha = 0.2f),
                            RoundedCornerShape(2.5.dp)
                        )
                )
                content(innerBackdrop, progress)
            }
        }
    }
}
