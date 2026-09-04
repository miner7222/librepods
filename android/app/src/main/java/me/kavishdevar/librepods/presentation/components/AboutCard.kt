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

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import kotlin.io.encoding.ExperimentalEncodingApi
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalIsDarkTheme

@Composable
fun AboutCard(
    modelName: String,
    actualModel: String,
    serialNumbers: List<String>,
    version: String?,
    navigateToVersion: () -> Unit
) {
    val left = stringResource(R.string.left)
    val right = stringResource(R.string.right)
    val inlineIconTint = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (LocalIsDarkTheme.current) 0.6f else 0.46f
    )
    val serialNumberEntries = when (LocalDesignSystem.current) {
        DesignSystem.Apple -> listOf(
            AnnotatedString(serialNumbers[0]) to emptyMap<String, InlineTextContent>(),
            buildAnnotatedString {
                appendInlineContent("leftSerialIcon", left)
                append(" ${serialNumbers[1]}")
            } to mapOf(
                "leftSerialIcon" to InlineTextContent(
                    Placeholder(1.193.em, 1.193.em, PlaceholderVerticalAlign.TextCenter)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sf_l_circle_fill),
                        contentDescription = left,
                        tint = inlineIconTint,
                        modifier = androidx.compose.ui.Modifier.fillMaxSize()
                    )
                }
            ),
            buildAnnotatedString {
                appendInlineContent("rightSerialIcon", right)
                append(" ${serialNumbers[2]}")
            } to mapOf(
                "rightSerialIcon" to InlineTextContent(
                    Placeholder(1.193.em, 1.193.em, PlaceholderVerticalAlign.TextCenter)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sf_r_circle_fill),
                        contentDescription = right,
                        tint = inlineIconTint,
                        modifier = androidx.compose.ui.Modifier.fillMaxSize()
                    )
                }
            )
        )

        DesignSystem.Material -> listOf(
            AnnotatedString(serialNumbers[0]) to emptyMap<String, InlineTextContent>(),
            AnnotatedString("$left ${serialNumbers[1]}") to emptyMap<String, InlineTextContent>(),
            AnnotatedString("$right ${serialNumbers[2]}") to emptyMap<String, InlineTextContent>(),
        )
    }

    val serialNumber = remember { mutableIntStateOf(0) }

    StyledList (title = stringResource(R.string.about)) {
        StyledListItem(
            name = stringResource(R.string.model_name),
            description = modelName
        )

        StyledListItem(
            name = stringResource(R.string.model_number),
            description = actualModel
        )

        StyledListItem (
            name = stringResource(R.string.serial_number),
            annotatedDescription = serialNumberEntries[serialNumber.intValue].first,
            inlineContent = serialNumberEntries[serialNumber.intValue].second,
            onClick = { serialNumber.intValue = (serialNumber.intValue + 1) % serialNumberEntries.size }
        )

        StyledListItem(
            name = stringResource(R.string.version),
            description = version,
            onClick = navigateToVersion,
        )
    }
}
