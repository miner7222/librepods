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

package me.kavishdevar.librepods.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.tooling.preview.Devices.PIXEL_9_PRO_XL
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R

// PretendardJP is a variable font, so each weight is the same file pinned to a
// different point on the 'wght' axis. It replaces SF Pro, which carries no
// Hangul, and stands in for Apple SD Gothic Neo, which cannot be redistributed.
private fun pretendard(weight: FontWeight) = Font(
    R.font.pretendard_jp_variable,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val pretendardFamily = FontFamily(
    pretendard(FontWeight.Thin),
    pretendard(FontWeight.ExtraLight),
    pretendard(FontWeight.Light),
    pretendard(FontWeight.Normal),
    pretendard(FontWeight.Medium),
    pretendard(FontWeight.SemiBold),
    pretendard(FontWeight.Bold),
    pretendard(FontWeight.ExtraBold),
    pretendard(FontWeight.Black)
)

val AppleTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = pretendardFamily),
        displayMedium = displayMedium.copy(fontFamily = pretendardFamily),
        displaySmall = displaySmall.copy(fontFamily = pretendardFamily),

        headlineLarge = headlineLarge.copy(fontFamily = pretendardFamily),
        headlineMedium = headlineMedium.copy(fontFamily = pretendardFamily),
        headlineSmall = headlineSmall.copy(fontFamily = pretendardFamily),

        titleLarge = titleLarge.copy(fontFamily = pretendardFamily),
        titleMedium = titleMedium.copy(fontFamily = pretendardFamily),
        titleSmall = titleSmall.copy(fontFamily = pretendardFamily),

        bodyLarge = bodyLarge.copy(fontFamily = pretendardFamily),
        // Measured against the references on matching words: row titles, values,
        // buttons and input fields sit at iOS's 17, and the description under a
        // title at 13 - the same size as the footer below the card, which already
        // matched. Ours were 16 and 14, so titles read about 5% small and
        // descriptions about 8% large.
        bodyMedium = bodyMedium.copy(
            fontFamily = pretendardFamily,
            fontSize = 17.sp
        ),
        bodySmall = bodySmall.copy(
            fontFamily = pretendardFamily,
            fontSize = 13.sp,
            lineHeight = 17.sp
        ),
        bodySmallEmphasized = bodySmallEmphasized.merge(AppleDesignMetrics.sectionFooterStyle),

        labelLarge = labelLarge.copy(fontFamily = pretendardFamily),

        labelMedium = labelMedium.copy(
            fontFamily = pretendardFamily,
            fontSize = 16.sp,
        ),
        labelMediumEmphasized = labelMediumEmphasized.copy(
            fontFamily = pretendardFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        ),
        labelSmallEmphasized = labelSmallEmphasized.merge(AppleDesignMetrics.sectionHeaderStyle)
    )
}

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun robotoFlex(
    wght: Float = 400f,
    slnt: Float = 0f,
    grad: Float = 0f,
    wdth: Float = 100f,
    xtra: Float = 468f,
    xopq: Float = 96f,
    yopq: Float = 79f,
) = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(
//    Font(
//        resId = R.font.roboto_flex,
        googleFont = GoogleFont("Roboto Flex"),
        fontProvider = provider,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", wght),
            FontVariation.Setting("wdth", wdth),
            FontVariation.Setting("slnt", slnt),
            FontVariation.Setting("grad", grad),
            FontVariation.Setting("xtra", xtra),
            FontVariation.Setting("xopq", xopq),
            FontVariation.Setting("yopq", yopq),
        )
    )
)

/*
 * M3 gives every role a weight, and this app cannot take it from the type scale:
 * the weight is an axis of the variable font, fixed when the family is built, and
 * a family's axis beats whatever fontWeight the style carries. So the weights have
 * to be families, and three cover the scale.
 *
 * Regular carries display, headline, title large and body. Medium carries title
 * medium and small, every label, and the emphasized form of everything Regular
 * carries. Bold carries the emphasized form of title medium and small and of the
 * labels. Nothing else separates a style from its emphasized twin - M3 is explicit
 * that only weight and tracking change - so the extra width and grade the
 * emphasized families used to stretch themselves by are gone with them.
 */
private val regular = robotoFlex(wght = 400f)
private val medium = robotoFlex(wght = 500f)
private val bold = robotoFlex(wght = 700f)


/*
 * Roboto Flex on every role, at the sizes M3 publishes. The scale had been
 * rewritten as well as re-fonted: title sat at the headline sizes, display was
 * shrunk below them, and body and label had both been pushed to 14/16/18 - so the
 * two overlapped exactly and a label no longer read as smaller than the body it
 * sat beside. Only the typeface here is ours; the fifteen sizes are M3's.
 */
val MaterialTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = regular),
        displayMedium = displayMedium.copy(fontFamily = regular),
        displaySmall = displaySmall.copy(fontFamily = regular),
        displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = medium),
        displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = medium),
        displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = medium),

        headlineLarge = headlineLarge.copy(fontFamily = regular),
        headlineMedium = headlineMedium.copy(fontFamily = regular),
        headlineSmall = headlineSmall.copy(fontFamily = regular),
        headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = medium),
        headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = medium),
        headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = medium),

        titleLarge = titleLarge.copy(fontFamily = regular),
        titleMedium = titleMedium.copy(fontFamily = medium),
        titleSmall = titleSmall.copy(fontFamily = medium),
        titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = medium),
        titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = bold),
        titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = bold),

        bodyLarge = bodyLarge.copy(fontFamily = regular),
        bodyMedium = bodyMedium.copy(fontFamily = regular),
        bodySmall = bodySmall.copy(fontFamily = regular),
        bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = medium),
        bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = medium),
        bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = medium),

        labelLarge = labelLarge.copy(fontFamily = medium),
        labelMedium = labelMedium.copy(fontFamily = medium),
        labelSmall = labelSmall.copy(fontFamily = medium),
        labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = bold),
        labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = bold),
        labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = bold),
    )
}

@Preview(
    name = "Typography Showcase",
    showBackground = true,
    device = PIXEL_9_PRO_XL
)
@Composable
private fun TypographyPreview() {
    LibrePodsTheme (m3eEnabled = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Display Large",
                    style = MaterialTheme.typography.displayLarge
                )

                Text(
                    "Display Large Emphasized",
                    style = MaterialTheme.typography.displayLargeEmphasized
                )

                Text(
                    "Display Medium",
                    style = MaterialTheme.typography.displayMedium
                )

                Text(
                    "Display Medium Emphasized",
                    style = MaterialTheme.typography.displayMediumEmphasized
                )

                Text(
                    "Display Small",
                    style = MaterialTheme.typography.displaySmall
                )

                Text(
                    "Display Small Emphasized",
                    style = MaterialTheme.typography.displaySmallEmphasized
                )

                HorizontalDivider()

                Text(
                    "Body Large",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    "Body Large Emphasized",
                    style = MaterialTheme.typography.bodyLargeEmphasized
                )

                Text(
                    "Body Medium",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    "Body Medium Emphasized",
                    style = MaterialTheme.typography.bodyMediumEmphasized
                )

                Text(
                    "Body Small",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "Body Small Emphasized",
                    style = MaterialTheme.typography.bodySmallEmphasized
                )

                HorizontalDivider()

                Text(
                    "Label Large",
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    "Label Large Emphasized",
                    style = MaterialTheme.typography.labelLargeEmphasized
                )

                Text(
                    "Label Medium",
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    "Label Medium Emphasized",
                    style = MaterialTheme.typography.labelMediumEmphasized
                )

                Text(
                    "Label Small",
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    "Label Small Emphasized",
                    style = MaterialTheme.typography.labelSmallEmphasized
                )
            }
        }
    }
}
