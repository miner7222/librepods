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

package me.kavishdevar.librepods.presentation.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.StartOffset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private const val CYCLE_MS = 6000
private const val SHAKE_START_MS = 2000
private const val OUTLINE_COUNTER = 0.36f
/**
 * Nodding forward foreshortens the head, and the recording squashes the outline
 * 5.9% at the same frame the features reach their highest - but only 1.8% on the
 * swing back, so this follows the forward direction alone. Turning does the same
 * sideways, far more weakly.
 */
private const val NOD_SQUASH = 0.55f
private const val SHAKE_SQUASH = 0.17f
/** iOS lets the push settle before it starts; measured at 0.6s from its first frame. */
private const val START_DELAY_MS = 600

/**
 * The face iOS shows above its head-gesture settings. It is not an SF Symbol —
 * nothing in the catalogue has this one's oval eyes or its nose — so it is drawn
 * here from the proportions measured off a capture, all as fractions of the outer
 * diameter: a 0.068 stroke, eyes one stroke wide and 0.136 tall at 0.311 and
 * 0.680 across, a nose falling from the eye line to 0.573 and hooking left, and a
 * shallow smile spanning 0.327 to 0.659.
 *
 * It nods, pauses, then shakes. Tracked frame by frame off a screen recording,
 * the head does not slide as a whole: the outline stays nearly put while the
 * features swing inside it, the way a head turning reads in two dimensions, and
 * the outline drifts about a third as far the other way. Both gestures ring out
 * over roughly a second — the nod through -0.107, +0.082, -0.045, +0.028 of the
 * diameter, the shake through -0.081, +0.061, -0.048, +0.025.
 */
@Composable
fun HeadGestureFace(
    modifier: Modifier = Modifier,
    diameter: Dp = 110.dp,
    color: Color,
    animated: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "headGesture")

    val nod by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = CYCLE_MS
                0f at 0
                -0.107f at 267
                0.082f at 467
                -0.045f at 667
                0.028f at 867
                0f at 1067
                0f at CYCLE_MS
            }
        ),
        label = "nod"
    )
    val shake by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = CYCLE_MS
                0f at 0
                0f at SHAKE_START_MS
                -0.081f at SHAKE_START_MS + 200
                0.061f at SHAKE_START_MS + 467
                -0.048f at SHAKE_START_MS + 667
                0.025f at SHAKE_START_MS + 867
                0f at SHAKE_START_MS + 1067
                0f at CYCLE_MS
            }
        ),
        label = "shake"
    )

    Canvas(modifier = modifier.size(diameter)) {
        val d = size.minDimension
        val stroke = d * 0.068f
        val cx = size.width / 2f
        val cy = size.height / 2f

        val featureX = if (animated) shake * d else 0f
        val featureY = if (animated) nod * d else 0f
        // The outline drifts the other way, about a third as far.
        val outlineX = -featureX * OUTLINE_COUNTER
        val outlineY = -featureY * OUTLINE_COUNTER

        val squashY = if (animated) 1f - NOD_SQUASH * maxOf(0f, -nod) else 1f
        val squashX = if (animated) 1f - SHAKE_SQUASH * abs(shake) else 1f
        translate(left = outlineX, top = outlineY) {
            scale(scaleX = squashX, scaleY = squashY, pivot = Offset(cx, cy)) {
                drawCircle(
                    color = color,
                    radius = (d - stroke) / 2f,
                    center = Offset(cx, cy),
                    style = Stroke(width = stroke)
                )
            }
        }

        translate(left = featureX, top = featureY) {
            fun eye(centreX: Float) = drawLine(
                color = color,
                start = Offset(centreX, d * 0.323f + stroke / 2f),
                end = Offset(centreX, d * 0.459f - stroke / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            eye(d * 0.311f)
            eye(d * 0.680f)

            // The nose drops from the eye line and hooks back to the left.
            val nose = Path().apply {
                moveTo(d * 0.520f, d * 0.323f + stroke / 2f)
                lineTo(d * 0.520f, d * 0.520f)
                quadraticTo(
                    d * 0.520f, d * 0.573f,
                    d * 0.482f + stroke / 2f, d * 0.573f
                )
            }
            drawPath(
                nose,
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // A shallow arc, drawn as the bottom of an ellipse so its ends lift
            // the way the reference's do.
            val smile = Path().apply {
                val left = d * 0.327f
                val right = d * 0.659f
                val top = d * 0.627f - (d * 0.736f - d * 0.627f)
                addArc(
                    Rect(
                        offset = Offset(left, top),
                        size = Size(right - left, (d * 0.736f - top))
                    ),
                    25f,
                    130f
                )
            }
            drawPath(
                smile,
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}
