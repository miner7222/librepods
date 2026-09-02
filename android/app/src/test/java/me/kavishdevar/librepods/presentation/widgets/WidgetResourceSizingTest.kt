package me.kavishdevar.librepods.presentation.widgets

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class WidgetResourceSizingTest {
    private val resources = File("src/main/res")

    @Test
    fun `noise button backgrounds do not subtract hidden space from responsive content`() {
        val backgrounds = File(resources, "drawable").listFiles().orEmpty()
            .filter { it.name.startsWith("widget_button") && it.extension == "xml" }
        assertTrue("Widget backgrounds must be found", backgrounds.isNotEmpty())
        backgrounds.forEach { file ->
            val padding = parse(file).getElementsByTagName("padding")
            assertEquals("${file.name} adds unaccounted content padding", 0, padding.length)
        }
    }

    private fun parse(file: File): Element = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(file).documentElement
}
