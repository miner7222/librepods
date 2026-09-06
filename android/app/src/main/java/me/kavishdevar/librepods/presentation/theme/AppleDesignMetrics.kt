package me.kavishdevar.librepods.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun verticalTile(top: Long, bottom: Long) =
    Brush.verticalGradient(listOf(Color(top), Color(bottom)))

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
    // The header box already adds 4dp above its text, and the style contributes
    // about 3.3dp of line leading: these land header ink 33.5pt below a preceding
    // card and 43.75pt below the navigation bar when the group opens a column.
    val sectionHeaderTopGap = 26.dp
    val sectionHeaderColumnTopInset = 36.5.dp
    // iOS sets a section header 9.5pt above the card beneath it; Compose already
    // contributes about 2.5dp of that as line leading, so the header box adds 7.
    val sectionHeaderBottomGap = 7.dp
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
    /**
     * Apple's tiles are not flat: each is a shallow vertical gradient, and the value
     * that had been standing in for it here was whatever a single sample happened to
     * catch - the grey was its top colour exactly, the other three were nowhere near.
     * Measured off the iOS 27 captures, which give the same pair in both themes.
     */
    val audioAndRoutingIconTile = verticalTile(0xFFEB4962, 0xFFEA4459)
    val controlsAndGesturesIconTile = verticalTile(0xFF9F9FA4, 0xFF8E8E93)
    val accessibilityIconTile = verticalTile(0xFF4090F7, 0xFF3B86F7)
    /** The reference gives hearing health the same blue it gives accessibility. */
    val hearingHealthIconTile = verticalTile(0xFF4090F7, 0xFF3B86F7)
    val batteryIconTile = verticalTile(0xFF70D272, 0xFF65C566)

    val navigationBarHeight = 44.dp

    /**
     * How far the scrolled bar's tint reaches past the bar itself. Apple leaves
     * about 14dp under the back button before the hairline; the bar on its own
     * leaves 4, which is what made the line look stuck to the button.
     */
    val navigationBarScrolledExtra = 11.dp
    // Measured from the navigation bar's bottom, taken as the back button's centre
    // plus half its 44pt height: iOS opens its content 27.5pt below the bar. This
    // also sets how far content may scroll before the bar draws its divider, since
    // that is the same distance.
    val cardColumnTopInset = 27.5.dp
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
    // Left to the typeface, Pretendard sets these paragraphs 15.4 apart; iOS runs
    // them at a flat 16, the same in light and dark and on every screen measured.
    val sectionFooterStyle by lazy {
        TextStyle(
            fontFamily = pretendardFamily,
            fontSize = 13.sp,
            lineHeight = 16.sp,
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
