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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

/*
 * Everything below was read off a 60fps screen recording of the iOS screen, played
 * through frame by frame in order. Seeking to a frame by index is what earlier
 * readings of this animation did, and it returns neither the frame asked for nor a
 * usable timestamp - which is how the gesture came to be recorded as half its real
 * speed, twice as wide as it is, and two swings long instead of four.
 */

/** The eyes narrow to a short dash rather than closing outright. */
private const val BLINK_CLOSED = 0.23f
private const val BLINK_CLOSE_MS = 83
private const val BLINK_OPEN_MS = 100

/**
 * One gesture: four swings 200ms apart, each about two thirds of the one before,
 * settling 200ms after the last. These are the eyes' travel as a fraction of the
 * outer diameter; every other part of the face is a multiple of it.
 */
private const val SWING_MS = 200
private const val GESTURE_MS = 1017
private val NOD_SWINGS = listOf(-0.0625f, 0.0505f, -0.0300f, 0.0170f)
private val SHAKE_SWINGS = listOf(-0.0444f, 0.0375f, -0.0301f, 0.0180f)

/** Where each thing starts, measured from the first blink. */
private const val START_DELAY_MS = 250L
private const val NOD_AT_MS = 283L
private const val SECOND_BLINK_AT_MS = 2017L
private const val SHAKE_AT_MS = 2317L

/**
 * The face is not flat, and this is what makes it read as a head rather than a
 * sliding picture. Against the eyes' travel: the nose, standing furthest from the
 * axis the head turns on, goes almost twice as far; the mouth a little further than
 * the eyes; and the outline the other way, by two thirds. Measured at every peak of
 * both gestures, where the ratios hold to within a few percent.
 */
private const val NOSE_TRAVEL = 1.85f
private const val MOUTH_TRAVEL = 1.15f
private const val OUTLINE_TRAVEL = -0.70f

/**
 * Turning also closes the gap between the eyes - the one being turned away from
 * covers about 30% more ground than the one being turned towards, in whichever
 * direction the head goes, so the pair draws together either way.
 */
private const val EYE_CONVERGENCE = 0.13f

/** A head turning away foreshortens along the axis it turns on. */
private const val NOD_SQUASH = 0.55f
private const val SHAKE_SQUASH = 0.30f

/**
 * The face iOS shows above its head-gesture settings. It is not an SF Symbol —
 * nothing in the catalogue has this one's oval eyes or its nose — so it is drawn
 * here from the proportions measured off a capture, all as fractions of the outer
 * diameter: a 0.075 stroke, eyes that wide and 0.136 tall at 0.311 and 0.680
 * across, a thinner 0.059 nose falling from 0.348 to 0.548 and hooking 0.052 back
 * to the left, and a shallow smile spanning 0.327 to 0.659.
 *
 * It blinks, nods, blinks again, shakes, and then leaves the face alone however
 * long the screen stays open — it does not loop.
 */
@Composable
fun HeadGestureFace(
    modifier: Modifier = Modifier,
    diameter: Dp = 110.dp,
    color: Color,
    animated: Boolean = true
) {
    val nodAnim = remember { Animatable(0f) }
    val shakeAnim = remember { Animatable(0f) }
    val blinkAnim = remember { Animatable(1f) }

    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect
        delay(START_DELAY_MS)
        blink(blinkAnim)
        delay(NOD_AT_MS - BLINK_CLOSE_MS - BLINK_OPEN_MS)
        nodAnim.animateTo(0f, swings(NOD_SWINGS))
        delay(SECOND_BLINK_AT_MS - NOD_AT_MS - GESTURE_MS)
        blink(blinkAnim)
        delay(SHAKE_AT_MS - SECOND_BLINK_AT_MS - BLINK_CLOSE_MS - BLINK_OPEN_MS)
        shakeAnim.animateTo(0f, swings(SHAKE_SWINGS))
    }

    val nod = nodAnim.value
    val shake = shakeAnim.value
    val blink = blinkAnim.value

    Canvas(modifier = modifier.size(diameter)) {
        val d = size.minDimension
        // The ring and the eyes are drawn with one weight and the nose with a
        // thinner one: 16.5px and 13px against a 220px face in the capture.
        val stroke = d * 0.075f
        val noseStroke = d * 0.059f
        val cx = size.width / 2f
        val cy = size.height / 2f

        val eyeX = shake * d
        val eyeY = nod * d
        // The pair converges towards whichever way the head has turned.
        val converge = EYE_CONVERGENCE * abs(shake) * d

        val squashY = 1f - NOD_SQUASH * abs(nod)
        val squashX = 1f - SHAKE_SQUASH * abs(shake)
        translate(left = eyeX * OUTLINE_TRAVEL, top = eyeY * OUTLINE_TRAVEL) {
            scale(scaleX = squashX, scaleY = squashY, pivot = Offset(cx, cy)) {
                drawCircle(
                    color = color,
                    radius = (d - stroke) / 2f,
                    center = Offset(cx, cy),
                    style = Stroke(width = stroke)
                )
            }
        }

        // An eye is a capsule that flattens, not a line that shortens: through the
        // blink it holds its full width and loses height until it is thinner than
        // the stroke itself, which a round-capped line can never be - drawn that
        // way it bottoms out half open however far the blink is taken.
        val eyeWidth = stroke
        val eyeMid = d * 0.391f
        val eyeHeight = d * 0.136f * blink
        fun eye(restX: Float, side: Float) {
            val x = restX + eyeX - side * converge
            drawRoundRect(
                color = color,
                topLeft = Offset(x - eyeWidth / 2f, eyeMid - eyeHeight / 2f + eyeY),
                size = Size(eyeWidth, eyeHeight),
                cornerRadius = CornerRadius(minOf(eyeWidth, eyeHeight) / 2f)
            )
        }
        eye(d * 0.311f, -1f)
        eye(d * 0.680f, 1f)

        // The nose drops from the eye line and hooks back to the left.
        translate(left = eyeX * NOSE_TRAVEL, top = eyeY * NOSE_TRAVEL) {
            // Straight down from the eye line, then a rounded corner and a hook
            // running back to the left. The hook had been 0.004 of the diameter
            // long, which is no hook at all - it reaches 0.052, a third of the way
            // back towards the left eye.
            val nose = Path().apply {
                moveTo(d * 0.507f, d * 0.348f)
                lineTo(d * 0.507f, d * 0.514f)
                quadraticTo(
                    d * 0.507f, d * 0.548f,
                    d * 0.455f, d * 0.548f
                )
            }
            drawPath(
                nose,
                color = color,
                style = Stroke(width = noseStroke, cap = StrokeCap.Round)
            )
        }

        // A shallow arc, drawn as the bottom of an ellipse so its ends lift
        // the way the reference's do.
        translate(left = eyeX * MOUTH_TRAVEL, top = eyeY * MOUTH_TRAVEL) {
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

/** Four swings 200ms apart, back to rest 200ms after the last. */
private fun swings(peaks: List<Float>) = keyframes {
    durationMillis = GESTURE_MS
    0f at 0
    peaks.forEachIndexed { i, v -> v at SWING_MS * (i + 1) }
}

private suspend fun blink(anim: Animatable<Float, *>) {
    anim.animateTo(BLINK_CLOSED, tween(BLINK_CLOSE_MS, easing = LinearEasing))
    anim.animateTo(1f, tween(BLINK_OPEN_MS, easing = LinearEasing))
}
