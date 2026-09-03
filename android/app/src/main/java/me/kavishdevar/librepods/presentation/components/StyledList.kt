package me.kavishdevar.librepods.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers.GREEN_DOMINATED_EXAMPLE
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.sectionHeader
import me.kavishdevar.librepods.presentation.theme.secondaryLabel

@Composable
fun StyledList(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    content: @Composable StyledListScope.() -> Unit
) {
    val scope = StyledListScope()
    scope.content()

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val appleMetrics = LocalAppleDesignMetrics.current

    Column(
        modifier = modifier.padding(bottom = if (m3eEnabled) 0.dp else appleMetrics.cardGap)
    ) {
        title?.let {
            Box(
                modifier = Modifier
                    .background(if (m3eEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = if (m3eEnabled) 16.dp else appleMetrics.cardHorizontalInset)
                    .padding(top = 4.dp, bottom = if (m3eEnabled) 12.dp else 4.dp)
            ) {
                Text(
                    text = it,
                    color = if (m3eEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.sectionHeader,
                    style = if (m3eEnabled) MaterialTheme.typography.labelSmallEmphasized else appleMetrics.sectionHeaderStyle
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (m3eEnabled) Color.Transparent else MaterialTheme.colorScheme.surface, RoundedCornerShape(if (m3eEnabled) 24.dp else appleMetrics.cardCornerRadius))
                .clip(RoundedCornerShape(if (m3eEnabled) 24.dp else appleMetrics.cardCornerRadius))
        ) {
            if (m3eEnabled && description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            scope.items.forEachIndexed { index, item ->
                item(index, scope.items.size)
            }
            Spacer(modifier = Modifier.height(if(m3eEnabled) 4.dp else 0.dp))
        }
        if (!m3eEnabled && description != null) {
            Spacer(modifier = Modifier.height(appleMetrics.cardFooterGap))
            Text(
                text = description,
                style = appleMetrics.sectionFooterStyle,
                color = MaterialTheme.colorScheme.secondaryLabel,
                modifier = Modifier.padding(horizontal = appleMetrics.cardHorizontalInset)
            )
        }
    }
}

class StyledListScope {
    internal val items =
        mutableListOf<@Composable (Int, Int) -> Unit>()

    fun item(
        content: @Composable (index: Int, count: Int) -> Unit
    ) {
        items += content
    }
}

@Preview(showBackground = true, wallpaper = GREEN_DOMINATED_EXAMPLE, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun StyledListDemo() {
    LibrePodsTheme(
        m3eEnabled = true
    ) {
        val backgroundC = MaterialTheme.colorScheme.background
        StyledScaffold(
            title = "${backgroundC.red}, ${backgroundC.green}, ${backgroundC.blue}"
        ) {
            Column (
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(56.dp))
                StyledList(
                    title = "hello"
                ) {
                    for (i in 0..2) {
                        StyledListItem(
                            name = i.toString(),
                            onClick = {}
                        )
                    }
                    val checked = remember { mutableStateOf(false) }
                    StyledToggle(
                        label = "Test",
                        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit mollit anim id est laborum.",
                        checked = checked.value,
                        onCheckedChange = { checked.value = it },
                    )
                }
            }
        }
    }
}
