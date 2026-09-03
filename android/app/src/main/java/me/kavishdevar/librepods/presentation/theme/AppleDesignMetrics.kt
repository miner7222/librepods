package me.kavishdevar.librepods.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppleDesignMetrics {
    val listRowMinHeight = 54.dp
    // A row whose description pushes it past listRowMinHeight loses the breathing
    // room that minimum was providing, so the text ends up against the card edge and
    // the divider. Measured against iOS: this lands a two-line row on its 66pt height
    // and 67pt pitch. 14dp overshot both by 4dp.
    val stackedRowVerticalPadding = 12.dp
    // iOS leaves 10.5pt between a card's bottom edge and the ink of the footer
    // underneath it; the footer style's own leading covers 2.5dp of that.
    val cardFooterGap = 8.dp
    // iOS's measured gap between adjacent cards.
    val cardGap = 35.dp
    val cardCornerRadius = 28.dp
    val cardHorizontalInset = 16.dp

    val settingsHubIconTileSize = 28.dp
    val settingsHubIconTileCornerRadius = 6.dp
    // The glyph inside the tile is sized by WIDTH, not by the drawable's own 24dp
    // height. Measured off the iOS 27 tiles, all four glyphs are ~19.6pt wide while
    // their heights range 9-19pt; since these drawables' glyphs fill 93-95% of their
    // viewport width, a fixed 21dp width reproduces every reference height to within
    // half a point. Sizing by height instead makes the battery glyph overflow the tile.
    val settingsHubIconGlyphWidth = 21.dp
    // StyledListItem supplies 12.dp after leadingContent; the tile supplies
    // the remaining 3.dp of the measured 15.dp icon-to-label gap.
    val settingsHubIconLabelGapAdjustment = 3.dp
    val settingsHubIconTint = Color.White
    val audioAndRoutingIconTileColor = Color(0xFFFF2D55)
    val controlsAndGesturesIconTileColor = Color(0xFF9F9FA4)
    val accessibilityIconTileColor = Color(0xFF0092FF)
    val batteryIconTileColor = Color(0xFF41D565)

    val navigationBarHeight = 44.dp
    val cardColumnTopInset = 17.5.dp
    val navigationBarTitleStyle by lazy {
        TextStyle(
            fontFamily = pretendardFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    // iOS 26 grew the grouped section header to the navigation title's size and
    // weight, leaving only colour to separate them. Its ink matches the row titles
    // beneath it (13.0pt against 12.5-13.0pt for the same glyph profile), and its
    // stems run 0.118 of the em where those Regular titles run 0.088 - a ratio that
    // puts it at semibold, not regular.
    val sectionHeaderStyle by lazy {
        TextStyle(
            fontFamily = pretendardFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    val sectionFooterStyle by lazy {
        TextStyle(
            fontFamily = pretendardFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }

    // Measured off an iOS 26 capture: the track runs about 64x27 pt, longer and
    // a little flatter than the 51x31 of the older UISwitch, around a round
    // thumb still inset by 2 pt on each side.
    val switchTrackWidth = 64.dp
    val switchTrackHeight = 27.dp
    // The thumb is a stadium, wider than it is tall. Its height has to be the
    // track height minus twice the 2dp inset, otherwise the inset is 2dp on the
    // sides but 3dp top and bottom and the corner curves stop being concentric
    // with the track's. Both corner radii stay derived from these for that
    // reason; do not turn them into independent constants.
    val switchThumbWidth = 38.dp
    val switchThumbHeight = 23.dp

    val sliderThumbWidth = 40.dp
    val sliderThumbHeight = 24.dp
    val sliderThumbCornerRadius = 28.dp
}

val LocalAppleDesignMetrics = compositionLocalOf {
    AppleDesignMetrics
}
