package me.kavishdevar.librepods.presentation.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizingTest {
    @Test
    fun `grid items shrink to preserve three gaps on a narrow widget`() {
        assertEquals(46, gridItemSize(WidgetDimensions(widthDp = 110, heightDp = 150)))
    }

    @Test
    fun `grid items keep their preferred size when the widget grows`() {
        assertEquals(65, gridItemSize(WidgetDimensions(widthDp = 190, heightDp = 220)))
    }

    @Test
    fun `wide battery rings fit four items and five gaps`() {
        assertEquals(55, wideBatteryRingSize(WidgetDimensions(widthDp = 250, heightDp = 110)))
        assertEquals(66, wideBatteryRingSize(WidgetDimensions(widthDp = 360, heightDp = 180)))
    }

    @Test
    fun `rings fit the smaller resize minimums and undersized host allocations`() {
        listOf(WidgetDimensions(104, 104), WidgetDimensions(96, 150)).forEach { dimensions ->
            val size = gridItemSize(dimensions)
            assertTrue(2 * size + 18 <= dimensions.widthDp)
            assertTrue(2 * size + 18 <= dimensions.heightDp)
        }
        listOf(WidgetDimensions(200, 80), WidgetDimensions(180, 70)).forEach { dimensions ->
            val size = wideBatteryRingSize(dimensions)
            assertTrue(4 * size + 30 <= dimensions.widthDp)
            assertTrue(size + 34 <= dimensions.heightDp)
        }
    }

    @Test
    fun `wide noise keeps its preferred content size when there is room`() {
        val size = wideNoiseContentSize(WidgetDimensions(360, 80), 16f, 70f)
        assertEquals(28f, size.iconDp, 0.01f)
        assertEquals(1f, size.labelScale, 0.01f)
        assertEquals(4f, size.paddingDp, 0.01f)
        assertTrue(size.showLabels)
    }

    @Test
    fun `wide noise shrinks icons text and spacing on denser launcher grids`() {
        val dimensions = WidgetDimensions(280, 54)
        val size = wideNoiseContentSize(dimensions, 16f, 70f)
        assertTrue(size.showLabels)
        assertTrue(size.iconDp < 28f)
        assertTrue(size.labelScale < 1f)
        assertTrue(size.paddingDp < 4f)
        assertNoiseContentFits(dimensions, size, 16f)
    }

    @Test
    fun `wide noise fits every supported compact height without clipping labels`() {
        for (height in 40..80) {
            val dimensions = WidgetDimensions(200, height)
            val size = wideNoiseContentSize(dimensions, 16f, 70f)
            assertTrue(size.showLabels)
            assertTrue(size.labelScale >= 0.75f)
            assertNoiseContentFits(dimensions, size, 16f)
        }
    }

    @Test
    fun `large font uses icons when the minimum widget cannot fit readable labels`() {
        val dimensions = WidgetDimensions(200, 40)
        val size = wideNoiseContentSize(dimensions, 40f, 160f)
        assertFalse(size.showLabels)
        assertNoiseContentFits(dimensions, size, 40f)
    }

    @Test
    fun `hiding off mode makes more room for the three remaining labels`() {
        val dimensions = WidgetDimensions(200, 80)
        val four = wideNoiseContentSize(dimensions, 16f, 55f, visibleModeCount = 4)
        val three = wideNoiseContentSize(dimensions, 16f, 55f, visibleModeCount = 3)
        assertTrue(three.labelScale > four.labelScale)
        assertEquals(1f, three.labelScale, 0.01f)
        assertNoiseContentFits(dimensions, three, 16f, modeCount = 3)
    }

    private fun assertNoiseContentFits(
        dimensions: WidgetDimensions,
        size: WideNoiseContentSize,
        labelHeightDp: Float,
        modeCount: Int = 4
    ) {
        val textHeight = if (size.showLabels) labelHeightDp * size.labelScale + size.labelGapDp else 0f
        val totalHeight = 2 * size.paddingDp + 2 * size.marginDp + size.iconDp + textHeight
        assertTrue("Content height $totalHeight exceeds ${dimensions.heightDp}", totalHeight <= dimensions.heightDp)
        val iconRowWidth = 2 * size.paddingDp + modeCount * (2 * size.marginDp + size.iconDp)
        assertTrue(iconRowWidth <= dimensions.widthDp)
    }
}
