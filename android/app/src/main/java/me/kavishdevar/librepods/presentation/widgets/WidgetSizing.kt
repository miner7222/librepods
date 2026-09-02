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

package me.kavishdevar.librepods.presentation.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextPaint
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.os.BundleCompat
import kotlin.math.min
import kotlin.math.roundToInt
import me.kavishdevar.librepods.R

internal data class WidgetDimensions(
    val widthDp: Int,
    val heightDp: Int
)

private const val GRID_ITEM_PREFERRED_DP = 65
private const val GRID_MINIMUM_GAP_DP = 6
private const val WIDE_BATTERY_RING_PREFERRED_DP = 66
private const val WIDE_BATTERY_MINIMUM_GAP_DP = 6
private const val WIDE_BATTERY_LABEL_SPACE_DP = 34

internal fun widgetDimensions(
    context: Context,
    options: Bundle,
    fallbackWidthDp: Int,
    fallbackHeightDp: Int
): WidgetDimensions {
    fun positiveDimension(key: String, fallback: Int): Int =
        options.getInt(key, fallback).takeIf { it > 0 } ?: fallback

    val minWidth = positiveDimension(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallbackWidthDp)
    val maxWidth = positiveDimension(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth)
    val minHeight = positiveDimension(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallbackHeightDp)
    val maxHeight = positiveDimension(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)
    return if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        WidgetDimensions(maxWidth, minHeight)
    } else {
        WidgetDimensions(minWidth, maxHeight)
    }
}

internal fun gridItemSize(dimensions: WidgetDimensions): Int {
    val widthLimit = (dimensions.widthDp - GRID_MINIMUM_GAP_DP * 3) / 2
    val heightLimit = (dimensions.heightDp - GRID_MINIMUM_GAP_DP * 3) / 2
    return min(GRID_ITEM_PREFERRED_DP, min(widthLimit, heightLimit))
        .coerceAtLeast(1)
}

internal fun wideBatteryRingSize(dimensions: WidgetDimensions): Int {
    val widthLimit = (dimensions.widthDp - WIDE_BATTERY_MINIMUM_GAP_DP * 5) / 4
    val heightLimit = dimensions.heightDp - WIDE_BATTERY_LABEL_SPACE_DP
    return min(WIDE_BATTERY_RING_PREFERRED_DP, min(widthLimit, heightLimit))
        .coerceAtLeast(1)
}

internal data class WideNoiseContentSize(
    val iconDp: Float,
    val labelScale: Float,
    val paddingDp: Float,
    val marginDp: Float,
    val labelGapDp: Float,
    val showLabels: Boolean
)

internal fun wideNoiseContentSize(
    dimensions: WidgetDimensions,
    labelHeightDp: Float,
    labelWidthDp: Float,
    visibleModeCount: Int = 4
): WideNoiseContentSize {
    // Scale the entire stack, including both the plate padding and button margins.
    val scale = minOf(
        1f,
        dimensions.widthDp / (62.5f * visibleModeCount),
        dimensions.heightDp / (28f + 2f + labelHeightDp + 16f)
    ).coerceAtLeast(0f)
    val padding = 4f * scale * scale
    val margin = 4f * scale * scale
    val slotWidth = ((dimensions.widthDp - 2 * padding) / visibleModeCount - 2 * margin)
        .coerceAtLeast(1f)
    // Leave room for RemoteViews pixel rounding and TextView line measurement.
    val slotHeight = (dimensions.heightDp - 2 * padding - 2 * margin - 2f).coerceAtLeast(1f)
    // Keep labels at least 9sp relative to the preferred 12sp, with ellipsis for
    // long translations. If the height cannot hold readable text, use icons only.
    val labelScale = min(scale, slotWidth / labelWidthDp.coerceAtLeast(1f)).coerceIn(0.75f, 1f)
    val gap = 2f * scale
    val iconWithLabel = minOf(28f * scale, slotWidth, slotHeight - labelHeightDp * labelScale - gap)
    val showLabels = iconWithLabel >= 16f
    return WideNoiseContentSize(
        iconDp = if (showLabels) iconWithLabel else minOf(28f, slotWidth, slotHeight),
        labelScale = labelScale,
        paddingDp = padding,
        marginDp = margin,
        labelGapDp = if (showLabels) gap else 0f,
        showLabels = showLabels
    )
}

internal fun RemoteViews.applyWideNoiseContentSize(
    context: Context,
    dimensions: WidgetDimensions,
    allowOffMode: Boolean = true
) {
    val metrics = context.resources.displayMetrics
    val preferredTextPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, metrics)
    val labels = intArrayOf(
        R.string.off,
        R.string.transparency,
        R.string.adaptive,
        R.string.noise_cancellation
    ).map(context::getString)
    val paint = TextPaint().apply { textSize = preferredTextPx }
    val visibleLabels = if (allowOffMode) labels else labels.drop(1)
    val size = wideNoiseContentSize(
        dimensions,
        (paint.fontMetrics.bottom - paint.fontMetrics.top) / metrics.density,
        visibleLabels.maxOf(paint::measureText) / metrics.density,
        visibleLabels.size
    )
    val paddingPx = (size.paddingDp * metrics.density).roundToInt()
    setViewPadding(R.id.noise_control_widget, paddingPx, paddingPx, paddingPx, paddingPx)
    val buttonIds = intArrayOf(
        R.id.widget_off_button, R.id.widget_transparency_button,
        R.id.widget_adaptive_button, R.id.widget_anc_button
    )
    val iconIds = intArrayOf(
        R.id.widget_off_icon, R.id.widget_transparency_icon,
        R.id.widget_adaptive_icon, R.id.widget_anc_icon
    )
    val labelIds = intArrayOf(
        R.id.widget_off_label, R.id.widget_transparency_label,
        R.id.widget_adaptive_label, R.id.widget_anc_label
    )
    buttonIds.forEachIndexed { index, buttonId ->
        intArrayOf(
            RemoteViews.MARGIN_START, RemoteViews.MARGIN_END,
            RemoteViews.MARGIN_TOP, RemoteViews.MARGIN_BOTTOM
        ).forEach { edge ->
            setViewLayoutMargin(buttonId, edge, size.marginDp, TypedValue.COMPLEX_UNIT_DIP)
        }
        setContentDescription(buttonId, labels[index])
        setViewLayoutWidth(iconIds[index], size.iconDp, TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutHeight(iconIds[index], size.iconDp, TypedValue.COMPLEX_UNIT_DIP)
        setViewVisibility(labelIds[index], if (size.showLabels) View.VISIBLE else View.GONE)
        setTextViewTextSize(labelIds[index], TypedValue.COMPLEX_UNIT_PX, preferredTextPx * size.labelScale)
        setViewLayoutMargin(labelIds[index], RemoteViews.MARGIN_TOP, size.labelGapDp, TypedValue.COMPLEX_UNIT_DIP)
    }
}

internal fun RemoteViews.applyBatteryGridItemSize(sizeDp: Int) {
    applyBatteryArtworkSize(sizeDp / GRID_ITEM_PREFERRED_DP.toFloat())
    intArrayOf(
        R.id.left_battery_widget_container,
        R.id.right_battery_widget_container,
        R.id.case_battery_widget_container,
        R.id.phone_battery_widget_container
    ).forEach { viewId ->
        setViewLayoutWidth(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutHeight(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }
    intArrayOf(
        R.id.left_battery_ring_container,
        R.id.right_battery_ring_container,
        R.id.case_battery_ring_container,
        R.id.phone_battery_ring_container
    ).forEach { viewId ->
        setViewLayoutWidth(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutHeight(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }
}

internal fun RemoteViews.applyNoiseGridItemSize(sizeDp: Int) {
    intArrayOf(
        R.id.widget_off_button,
        R.id.widget_transparency_button,
        R.id.widget_adaptive_button,
        R.id.widget_anc_button
    ).forEach { viewId ->
        setViewLayoutWidth(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutHeight(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }
    intArrayOf(
        R.id.widget_off_icon, R.id.widget_transparency_icon,
        R.id.widget_adaptive_icon, R.id.widget_anc_icon
    ).forEach { viewId ->
        val iconDp = 34f * sizeDp / GRID_ITEM_PREFERRED_DP
        setViewLayoutWidth(viewId, iconDp, TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutHeight(viewId, iconDp, TypedValue.COMPLEX_UNIT_DIP)
    }
}

internal fun RemoteViews.applyWideBatteryRingSize(sizeDp: Int) {
    val scale = sizeDp / WIDE_BATTERY_RING_PREFERRED_DP.toFloat()
    applyBatteryArtworkSize(scale)
    intArrayOf(
        R.id.phone_battery_widget, R.id.left_battery_widget,
        R.id.right_battery_widget, R.id.case_battery_widget
    ).forEach { viewId ->
        setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, 20f * scale)
        setViewLayoutMargin(viewId, RemoteViews.MARGIN_TOP, 6f * scale, TypedValue.COMPLEX_UNIT_DIP)
    }
    intArrayOf(
        R.id.left_battery_widget_container,
        R.id.right_battery_widget_container,
        R.id.case_battery_widget_container,
        R.id.phone_battery_widget_container
    ).forEach { viewId ->
        setViewLayoutWidth(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }
    intArrayOf(
        R.id.left_battery_ring_container,
        R.id.right_battery_ring_container,
        R.id.case_battery_ring_container,
        R.id.phone_battery_ring_container
    ).forEach { viewId ->
        setViewLayoutWidth(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutHeight(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }
}

private fun RemoteViews.applyBatteryArtworkSize(scale: Float) {
    intArrayOf(
        R.id.phone_battery_icon, R.id.left_battery_icon,
        R.id.right_battery_icon, R.id.case_battery_icon
    ).forEach { viewId ->
        setViewLayoutWidth(viewId, 27f * scale, TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutHeight(viewId, 27f * scale, TypedValue.COMPLEX_UNIT_DIP)
    }
    intArrayOf(
        R.id.phone_charging_icon, R.id.left_charging_icon,
        R.id.right_charging_icon, R.id.case_charging_icon
    ).forEach { viewId ->
        setViewLayoutWidth(viewId, 11f * scale, TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutHeight(viewId, 18f * scale, TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutMargin(viewId, RemoteViews.MARGIN_TOP, -5f * scale, TypedValue.COMPLEX_UNIT_DIP)
    }
}

internal fun AppWidgetManager.sizedRemoteViewsFor(
    context: Context,
    appWidgetId: Int,
    fallbackWidthDp: Int,
    fallbackHeightDp: Int,
    createViews: (WidgetDimensions) -> RemoteViews
): RemoteViews {
    val options = getAppWidgetOptions(appWidgetId)
    val sizes = BundleCompat.getParcelableArrayList(
        options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java
    )
        .orEmpty()
        .filter { it.width.isFinite() && it.height.isFinite() && it.width > 0 && it.height > 0 }
        .distinct()
        .take(16) // RemoteViews limits a size map to 16 layouts.
    if (sizes.isEmpty()) {
        return createViews(widgetDimensions(context, options, fallbackWidthDp, fallbackHeightDp))
    }
    // Let the host pick its exact layout instead of guessing its current size
    // from the portrait/landscape min/max bounds.
    return RemoteViews(sizes.associateWith { size ->
        createViews(WidgetDimensions(size.width.toInt(), size.height.toInt()))
    })
}
