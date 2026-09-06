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


package me.kavishdevar.librepods.presentation.overlays

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import me.kavishdevar.librepods.data.FallbackArtwork
import me.kavishdevar.librepods.data.OverlayRingLayout
import me.kavishdevar.librepods.data.unifiedBudBattery
import me.kavishdevar.librepods.presentation.widgets.BatteryRing
import me.kavishdevar.librepods.presentation.theme.withAppNightMode

// 93% of a turn leaves about 25 degrees open just before twelve o'clock,
// which is where the charging bolt sits.
/**
 * The ring's outer diameter.
 *
 * A fixed size, not a fraction of the card: measured off two references on phones of
 * different widths it comes to 36.3 and 36.7 points either way, while the card around
 * it grows with the screen. 37 is that, and the 38 that briefly stood here came from
 * reading the first of those as a proportion.
 */
private const val POPUP_RING_DP = 37

@SuppressLint("InflateParams", "ClickableViewAccessibility")
class PopupWindow(
    baseContext: Context,
    private val onCloseCallback: () -> Unit = {}
) {
    // Its own window, so the app's appearance has to be carried in by hand.
    private val context: Context = baseContext.withAppNightMode()
    private val mView: View
    private var isClosing = false
    private var batteryUpdateReceiver: BroadcastReceiver? = null
    private var dimAnimator: ValueAnimator? = null
    private var showingBudsInCase: Boolean? = null
    private var showingArrangement: List<Boolean>? = null
    private var videoRendered = false
    private var pendingCells: (() -> Unit)? = null
    private var pendingInCase = false
    private var paintedInCase: Boolean? = null
    private var fadingArtwork = false
    private var artworkRingLayout = OverlayRingLayout()
    private var sheetWidthPx = 0

    @Suppress("DEPRECATION")
    private val mParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        height = WindowManager.LayoutParams.WRAP_CONTENT
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val marginPx = (displayMetrics.widthPixels * 0.0357f).toInt()
        sheetWidthPx = if (screenWidthDp >= 600) {
            (400 * displayMetrics.density).toInt()
        } else {
            displayMetrics.widthPixels - 2 * marginPx
        }
        width = sheetWidthPx
        y = marginPx
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        dimAmount = 0f
        flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }

    private val mWindowManager: WindowManager

    init {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mView = layoutInflater.inflate(R.layout.popup_window, null)
        mParams.x = 0

        val closeButton = mView.findViewById<ImageButton>(R.id.close_button)
        closeButton.setOnClickListener {
            close()
        }

        // The glyph stays small to match iOS, but a 30dp target is under the
        // 44pt minimum, which is what an assistive pointer actually aims at.
        closeButton.post {
            val parent = closeButton.parent as? View ?: return@post
            val minimum = (44f * context.resources.displayMetrics.density).toInt()
            val bounds = Rect().also { closeButton.getHitRect(it) }
            val growX = ((minimum - bounds.width()) / 2).coerceAtLeast(0)
            val growY = ((minimum - bounds.height()) / 2).coerceAtLeast(0)
            bounds.inset(-growX, -growY)
            parent.touchDelegate = TouchDelegate(bounds, closeButton)
        }

        val ll = mView.findViewById<LinearLayout>(R.id.linear_layout)
        // The reference card is 1.1019 times as tall as it is wide - measured off a
        // recording of it, and steady across every frame. Ours held to 1.026, which
        // is where the room under the readings went: the layout inside was already
        // in proportion, the card around it was not.
        ll.minimumHeight = (sheetWidthPx * 1.1019f).toInt()

        @Suppress("DEPRECATION")
        mView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        mView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val touchY = event.rawY
                val popupTop = mView.top
                if (touchY < popupTop) {
                    close()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
        mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (mWindowManager.isCrossWindowBlurEnabled) {
            mParams.flags = mParams.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            mParams.blurBehindRadius =
                (BLUR_BEHIND_RADIUS_DP * context.resources.displayMetrics.density).toInt()
        }
    }

    @SuppressLint("InlinedApi", "SetTextI18s")
    fun open(
        name: String = "AirPods Pro",
        batteryNotification: AirPodsNotifications.BatteryNotification,
        videoRes: Int = FallbackArtwork.Pro.connected,
        budsRes: Int = FallbackArtwork.Pro.buds,
        caseRes: Int = FallbackArtwork.Pro.chargingCase,
        ringLayout: OverlayRingLayout = OverlayRingLayout(),
        artworkScale: Float = 1f
    ) {
        try {
            if (mView.windowToken == null && mView.parent == null && !isClosing) {
                mView.findViewById<TextView>(R.id.name).text = name

                artworkRingLayout = ringLayout
                mView.findViewById<Guideline>(R.id.ring_guide_combined)
                    .setGuidelinePercent(ringLayout.budPair)
                mView.findViewById<Guideline>(R.id.ring_guide_left)
                    .setGuidelinePercent(ringLayout.leftBud)
                mView.findViewById<Guideline>(R.id.ring_guide_right)
                    .setGuidelinePercent(ringLayout.rightBud)
                mView.findViewById<Guideline>(R.id.ring_guide_case)
                    .setGuidelinePercent(ringLayout.chargingCase)

                mView.findViewById<ImageView>(R.id.artwork_buds).setImageResource(budsRes)
                mView.findViewById<ImageView>(R.id.artwork_case).setImageResource(caseRes)

                // The canvas is the card for most models; for the ones whose renders
                // are drawn large inside it, a little less than the card.
                if (artworkScale != 1f) {
                    val group = mView.findViewById<View>(R.id.artwork_group)
                    group.layoutParams = (group.layoutParams as LinearLayout.LayoutParams).apply {
                        width = (sheetWidthPx * artworkScale).toInt()
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                }

                val vid = mView.findViewById<VideoView>(R.id.video)
                vid.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                vid.setOnErrorListener { _, what, extra ->
                    Log.e("PopupWindow", "Error playing popup video: what=$what extra=$extra")
                    true
                }
                vid.setVideoPath("android.resource://${context.packageName}/$videoRes")
                vid.resolveAdjustedSize(vid.width, vid.height)
                vid.setOnCompletionListener {
                    vid.start()
                }
                // A surface with nothing drawn into it yet is black, and the card
                // used to open on that. Keep the clip hidden until playback says it
                // has put a frame up, then bring it in - or leave it at nothing, if a
                // bud has already been taken out and the still is what belongs there.
                // It keeps playing either way; the reference's clip runs behind the
                // still the whole time it is up.
                vid.setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        videoRendered = true
                        if (showingBudsInCase != false) {
                            vid.animate().alpha(1f).setDuration(ARRIVING_FADE_MS).start()
                        }
                    }
                    false
                }
                vid.start()

                updateBatteryStatus(batteryNotification)

                try {
                    mWindowManager.addView(mView, mParams)
                } catch (e: Exception) {
                    Log.e("PopupWindow", "Error adding popup view: ${e.message}")
                    onCloseCallback()
                    return
                }

                val displayMetrics = mView.context.resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels

                mView.translationY = screenHeight.toFloat()
                mView.alpha = 1f

                mView.post {
                    if (isClosing || mView.parent == null) return@post
                    mView.translationY = offscreenTranslation()
                    SpringAnimation(mView, DynamicAnimation.TRANSLATION_Y, 0f).apply {
                        spring = SpringForce(0f)
                            .setDampingRatio(PRESENT_DAMPING_RATIO)
                            .setStiffness(PRESENT_STIFFNESS)
                        start()
                    }
                    animateDim(DIM_AMOUNT, PRESENT_DIM_DURATION_MS)
                }

                // Nothing times the sheet out. On a recording of the reference it
                // is still up half a minute after it arrived, through the buds being
                // taken out and put back, and it goes when the lid closes or the
                // AirPods drop off - both of which already take this one down - or
                // when the reader dismisses it. Twelve seconds took it away while it
                // was still the answer to a case that was standing open.
                registerBatteryUpdateReceiver()
            }
        } catch (e: Exception) {
            Log.e("PopupWindow", "Error opening popup: ${e.message}")
            onCloseCallback()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBatteryUpdateReceiver() {
        batteryUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AirPodsNotifications.BATTERY_DATA) {
                    val batteryList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra("data", Battery::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra("data")
                    }
                    if (batteryList != null) {
                        updateBatteryStatusFromList(batteryList)
                    }
                }
            }
        }

        val filter = IntentFilter(AirPodsNotifications.BATTERY_DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(batteryUpdateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(batteryUpdateReceiver, filter)
        }
    }

    private fun unregisterBatteryUpdateReceiver() {
        batteryUpdateReceiver?.let {
            try {
                context.unregisterReceiver(it)
                batteryUpdateReceiver = null
            } catch (e: Exception) {
                Log.e("PopupWindow", "Error unregistering battery receiver: ${e.message}")
            }
        }
    }

    @SuppressLint("SetTextI18s")
    private fun updateBatteryStatusFromList(batteryList: List<Battery>) {
        val left = batteryList.find { it.component == BatteryComponent.LEFT }
        val right = batteryList.find { it.component == BatteryComponent.RIGHT }
        val case = batteryList.find { it.component == BatteryComponent.CASE }
        val combinedBuds = unifiedBudBattery(batteryList)
        val showCombinedBuds = combinedBuds != null

        // Everything that changes the shape of the card, not just which artwork is
        // up: the buds' rings merging, a badge coming or going, the case dropping
        // out.
        showArrangement(
            showCombinedBuds,
            listOf(
                showCombinedBuds,
                combinedBuds?.level != null,
                !showCombinedBuds && left?.level != null,
                !showCombinedBuds && right?.level != null,
                case?.level != null
            )
        ) {
            applyBatteryCells(left, right, case, combinedBuds, showCombinedBuds)
        }
    }

    /**
     * The readings, held back until the sheet is empty when the artwork is changing
     * under them: the buds' two rings become one at the same moment the clip
     * replaces the still, and letting that happen in front of the reader turns a
     * fade into a flicker.
     */
    @SuppressLint("SetTextI18n")
    private fun applyBatteryCells(
        left: Battery?,
        right: Battery?,
        case: Battery?,
        combinedBuds: Battery?,
        showCombinedBuds: Boolean
    ) {
        val badgeVisibility = if (showCombinedBuds) View.GONE else View.VISIBLE
        updateBatteryBadge(R.id.left_battery_badge, badgeVisibility)
        updateBatteryBadge(R.id.right_battery_badge, badgeVisibility)
        updateBatteryBadge(R.id.case_battery_badge, badgeVisibility)

        updateBatteryCell(
            R.id.combined_buds_battery_cell,
            R.id.combined_buds_battery,
            R.id.combined_buds_battery_ring,
            R.id.combined_buds_battery_icon,
            R.id.combined_buds_charging_icon,
            R.id.combined_buds_charging_icon_outline,
            combinedBuds?.level,
            combinedBuds?.status
        )
        updateBatteryCell(
            R.id.left_battery_cell,
            R.id.left_battery,
            R.id.popup_left_battery_ring,
            R.id.popup_left_battery_icon,
            R.id.popup_left_charging_icon,
            R.id.popup_left_charging_icon_outline,
            if (showCombinedBuds) null else left?.level,
            if (showCombinedBuds) null else left?.status
        )
        updateBatteryCell(
            R.id.right_battery_cell,
            R.id.right_battery,
            R.id.popup_right_battery_ring,
            R.id.popup_right_battery_icon,
            R.id.popup_right_charging_icon,
            R.id.popup_right_charging_icon_outline,
            if (showCombinedBuds) null else right?.level,
            if (showCombinedBuds) null else right?.status
        )
        updateBatteryCell(
            R.id.case_battery_cell,
            R.id.case_battery,
            R.id.popup_case_battery_ring,
            R.id.popup_case_battery_icon,
            R.id.popup_case_charging_icon,
            R.id.popup_case_charging_icon_outline,
            case?.level,
            case?.status
        )
    }

    /**
     * The card's whole arrangement: which of the two renders is up, how many rings
     * sit under it, and which badges they carry.
     *
     * The clip is the pair being in the same place - both seated in the case or both
     * out of it. Take one bud out and leave the other in and the reference swaps in
     * the still, the same render the settings screen heads with, and swaps back the
     * moment they match again. That is the reading the merged ring already carries:
     * the buds merge when their charge states agree, which they do both in the case
     * and both out of it, and split when one of them is seated and the other is not.
     * An earlier reading of this made the clip mean 'in the case' and asked the case
     * for its own battery to confirm it, which left the still up with both buds out
     * and the case saying nothing.
     *
     * The two renders do not put the case in the same place: the still has its
     * right edge at 87.8 percent of the canvas while the clip breathes between 85.1
     * and 75.9, so any moment with both of them up shows the case in two places at
     * once. The reference never has one. It fades everything that is changing - the
     * artwork and the readings under it - out to nothing over 300ms, holds the empty
     * card for about 90, and brings the new state back over 900. Frame by frame
     * there is a stretch where the card carries nothing but its title, which is
     * what lets the geometry change without anyone watching it.
     *
     * The clip's visibility is never touched: hiding a VideoView tears its surface
     * down, and bringing it back showed a black frame until playback had drawn into
     * the new one.
     */
    private fun showArrangement(
        inCase: Boolean,
        cells: List<Boolean>,
        applyCells: () -> Unit
    ) {
        val arrangement = cells + inCase
        val settling = showingArrangement == null
        val changed = showingArrangement != arrangement
        showingArrangement = arrangement
        showingBudsInCase = inCase

        // A swap already on its way down takes the new state as its destination
        // rather than starting again. The readings arrive several times a second and
        // the parts of an arrangement do not all land in the same packet - the buds'
        // rings merge on the buds alone, the artwork waits for the case to report as
        // well - so a single change of state reaches here two or three times. Every
        // one of them applied on the spot is the flicker the fade exists to avoid,
        // and every one of them restarting the fade is a swap that never finishes.
        if (pendingCells != null) {
            pendingCells = applyCells
            pendingInCase = inCase
            // The one restart worth making: the picture is changing after all, and
            // the fade that is running began for the rings alone.
            if (paintedInCase != inCase && !fadingArtwork) fadeOut(true)
            return
        }
        if (!changed) {
            applyCells()
            return
        }
        if (settling) {
            applyArtwork(inCase)
            applyCells()
            return
        }

        pendingCells = applyCells
        pendingInCase = inCase
        fadeOut(paintedInCase != inCase)
    }

    /**
     * Takes the card down to nothing, changes it there, and brings it back.
     *
     * Only what is actually changing goes: taking a second bud out merges its ring
     * into the first's without touching the render behind them, and fading a picture
     * out and back to the identical picture reads as a blink. The rings always
     * travel because the arrangement is what brought us here; the artwork joins them
     * only when the clip and the still are trading places.
     */
    private fun fadeOut(includeArtwork: Boolean) {
        fadingArtwork = includeArtwork
        val artworkGroup = mView.findViewById<View>(R.id.artwork_group)
        val ringRow = mView.findViewById<View>(R.id.ring_row)
        if (includeArtwork) artworkGroup.animate().cancel()
        ringRow.animate().cancel()

        val ease = AccelerateDecelerateInterpolator()
        if (includeArtwork) {
            artworkGroup.animate().alpha(0f).setDuration(FADE_OUT_MS)
                .setInterpolator(ease).start()
        }
        ringRow.animate().alpha(0f).setDuration(FADE_OUT_MS).setInterpolator(ease)
            .withEndAction {
                applyArtwork(pendingInCase)
                pendingCells?.invoke()
                pendingCells = null
                val returning =
                    if (fadingArtwork) arrayOf(artworkGroup, ringRow) else arrayOf(ringRow)
                fadingArtwork = false
                for (v in returning) {
                    v.animate().alpha(1f).setStartDelay(FADE_HOLD_MS)
                        .setDuration(FADE_IN_MS).setInterpolator(ease)
                        .withEndAction { v.animate().setStartDelay(0) }
                        .start()
                }
            }.start()
    }

    /**
     * Which of the two layers is up, and where the rings belong under it.
     *
     * Called with the card empty, so everything here is instant. The clip is left
     * running whichever layer is showing: on the reference its three-second breath
     * keeps its cadence unbroken straight through ten seconds of the still being up,
     * so it is not paused behind it and it does not start from the top on the way
     * back - it fades in already part-way through, wherever the loop has got to.
     */
    private fun applyArtwork(inCase: Boolean) {
        paintedInCase = inCase
        mView.findViewById<Guideline>(R.id.ring_guide_combined).setGuidelinePercent(
            if (inCase) artworkRingLayout.movingBudPair else artworkRingLayout.budPair
        )
        mView.findViewById<Guideline>(R.id.ring_guide_case).setGuidelinePercent(
            if (inCase) artworkRingLayout.movingCase else artworkRingLayout.chargingCase
        )
        mView.findViewById<View>(R.id.artwork).alpha = if (inCase) 0f else 1f
        // Until the surface has drawn once it is black, not transparent, so the clip
        // is left at nothing and the rendering callback brings it in instead.
        mView.findViewById<VideoView>(R.id.video).alpha =
            if (inCase && videoRendered) 1f else 0f
    }

    /**
     * The sheet draws its readings at full strength whatever the level says. That is
     * what separates it from the rings at the top of settings, which are in the
     * secondary label throughout - on the captures the sheet shows 100% and 91% in
     * the same black, and settings shows the same two in the same grey. This was
     * dimming the badge until its component reached 100%, which is neither.
     */
    private fun updateBatteryBadge(badgeId: Int, visibility: Int) {
        val badge = mView.findViewById<ImageView>(badgeId)
        badge.visibility = visibility
        badge.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.popup_text)
        )
        badge.alpha = 1f
    }

    private fun updateBatteryCell(
        cellId: Int,
        percentageId: Int,
        ringId: Int,
        deviceIconId: Int,
        chargingIconId: Int,
        chargingOutlineId: Int,
        level: Int?,
        status: Int?
    ) {
        val cell = mView.findViewById<View>(cellId)
        val percentage = mView.findViewById<TextView>(percentageId)
        if (level == null || status == null || status == BatteryStatus.DISCONNECTED) {
            cell.visibility = View.GONE
            percentage.text = ""
            return
        }

        cell.visibility = View.VISIBLE
        percentage.text = "$level%"
        val chargingVisible =
            status == BatteryStatus.CHARGING || status == BatteryStatus.OPTIMIZED_CHARGING
        mView.findViewById<ImageView>(ringId).setImageBitmap(
            BatteryRing.bitmap(
                context,
                POPUP_RING_DP,
                level,
                ContextCompat.getColor(context, R.color.popup_ring_track),
                BATTERY_PROGRESS_GREEN
            )
        )
        // Charging swaps the device glyph for a bolt inside the ring, the way
        // the settings screen and iOS both show it.
        mView.findViewById<ImageView>(chargingOutlineId).visibility = View.GONE
        mView.findViewById<ImageView>(chargingIconId).apply {
            visibility = if (chargingVisible) View.VISIBLE else View.GONE
            imageTintList = ColorStateList.valueOf(BATTERY_PROGRESS_GREEN)
        }
        // iOS shows the device in the artwork above, never inside the ring, so
        // the ring holds the bolt or nothing at all.
        mView.findViewById<ImageView>(deviceIconId).visibility = View.GONE
    }

    @SuppressLint("SetTextI18s")
    fun updateBatteryStatus(batteryNotification: AirPodsNotifications.BatteryNotification) {
        val batteryStatus = batteryNotification.getBattery()
        updateBatteryStatusFromList(batteryStatus)
    }

    /**
     * Distance that hides the card below the screen edge. The card is bottom
     * anchored, so its own height is the whole travel; iOS moves the card by
     * just that much rather than across the full screen.
     */
    private fun offscreenTranslation(): Float {
        val height = mView.height
        return if (height > 0) {
            height.toFloat()
        } else {
            mView.context.resources.displayMetrics.heightPixels.toFloat()
        }
    }

    private fun animateDim(to: Float, durationMs: Long) {
        dimAnimator?.cancel()
        dimAnimator = ValueAnimator.ofFloat(mParams.dimAmount, to).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                mParams.dimAmount = animation.animatedValue as Float
                try {
                    if (mView.parent != null) {
                        mWindowManager.updateViewLayout(mView, mParams)
                    }
                } catch (e: Exception) {
                    Log.e("PopupWindow", "Error updating dim: ${e.message}")
                }
            }
            start()
        }
    }

    fun close() {
        try {
            if (isClosing) return
            isClosing = true

            unregisterBatteryUpdateReceiver()

            val vid = mView.findViewById<VideoView>(R.id.video)
            vid.stopPlayback()

            val target = offscreenTranslation()
            animateDim(0f, DISMISS_DIM_DURATION_MS)
            SpringAnimation(mView, DynamicAnimation.TRANSLATION_Y, target).apply {
                spring = SpringForce(target)
                    .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                    .setStiffness(DISMISS_STIFFNESS)
                addEndListener { _, _, _, _ ->
                    try {
                        mView.visibility = View.GONE
                        if (mView.parent != null) {
                            mWindowManager.removeView(mView)
                        }
                    } catch (e: Exception) {
                        Log.e("PopupWindow", "Error removing view: ${e.message}")
                    } finally {
                        isClosing = false
                        onCloseCallback()
                    }
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("PopupWindow", "Error closing popup: ${e.message}")
            isClosing = false
            onCloseCallback()
        }
    }

    private companion object {
        /**
         * iOS presents the connect card with a spring of roughly 0.5 s response
         * and 0.86 damping fraction: a short travel that settles almost without
         * a visible bounce. Stiffness is that response as (2 * PI / 0.5)^2.
         */
        const val PRESENT_STIFFNESS = 158f
        const val PRESENT_DAMPING_RATIO = 0.86f
        const val PRESENT_DIM_DURATION_MS = 320L

        /** Dismissal is quicker and never overshoots past the screen edge. */
        const val DISMISS_STIFFNESS = 900f
        const val DISMISS_DIM_DURATION_MS = 240L

        /** Whatever is arriving lands before the one it replaces has finished leaving. */
        const val ARRIVING_FADE_MS = 120L

        /**
         * The swap, read off a recording of the iOS sheet at 60fps in both directions.
         *
         * Leaving takes 280 and 330ms, the empty card holds for about 90, and arriving
         * takes 950 and 850. Both curves are the symmetric one - a fifth of the way
         * across at a quarter of the time, half at half - which is what
         * AccelerateDecelerateInterpolator draws.
         */
        const val FADE_OUT_MS = 300L
        const val FADE_HOLD_MS = 90L
        const val FADE_IN_MS = 900L

        const val DIM_AMOUNT = 0.3f
        const val BLUR_BEHIND_RADIUS_DP = 48
        val BATTERY_PROGRESS_GREEN = 0xFF21BD44.toInt()
    }
}
