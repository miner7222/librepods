/*
    LibrePods - AirPods liberated from Apple's ecosystem
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue

/**
 * Draws the battery ring as a bitmap.
 *
 * A `<shape android:shape="ring">` fills its sweep as a path, so both ends are
 * radial straight edges and there is no way to round them from XML. Drawing the
 * arc ourselves gets the rounded ends the reference has, and puts the gap, the
 * stroke width and the colours in one place for the widgets and the popup alike.
 */
object BatteryRing {

    /** Rings are about a tenth of their diameter thick, popup and widget alike. */
    const val WIDGET_STROKE_RATIO = 1f / 10.5f

    fun bitmap(
        context: Context,
        sizeDp: Int,
        level: Int,
        trackColor: Int,
        progressColor: Int,
        strokeRatio: Float = WIDGET_STROKE_RATIO
    ): Bitmap {
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            sizeDp.toFloat(),
            context.resources.displayMetrics
        ).toInt().coerceAtLeast(1)

        val bitmap = createBitmap(size)
        val canvas = Canvas(bitmap)
        val stroke = size * strokeRatio
        val bounds = RectF(
            stroke / 2f,
            stroke / 2f,
            size - stroke / 2f,
            size - stroke / 2f
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }

        // Twelve o'clock is -90 degrees. The ring is always whole - the bolt
        // sits on top of it rather than in a gap cut out for it.
        val start = -90f

        paint.color = trackColor
        canvas.drawArc(bounds, start, 360f, false, paint)

        val fraction = level.coerceIn(0, 100) / 100f
        if (fraction > 0f) {
            paint.color = progressColor
            canvas.drawArc(bounds, start, 360f * fraction, false, paint)
        }
        return bitmap
    }

    private fun createBitmap(size: Int): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
}
