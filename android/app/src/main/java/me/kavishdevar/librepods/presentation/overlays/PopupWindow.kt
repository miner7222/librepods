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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.WindowManager
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
    private var autoCloseHandler = Handler(Looper.getMainLooper())
    private var autoCloseRunnable: Runnable? = null
    private var batteryUpdateReceiver: BroadcastReceiver? = null
    private var dimAnimator: ValueAnimator? = null
    private var showingBudsInCase: Boolean? = null
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

        mView.setOnClickListener {
            close()
        }

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
        ll.minimumHeight = (sheetWidthPx * 1.026f).toInt()
        ll.setOnClickListener {
            close()
        }

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
        ringLayout: OverlayRingLayout = OverlayRingLayout()
    ) {
        try {
            if (mView.windowToken == null && mView.parent == null && !isClosing) {
                mView.findViewById<TextView>(R.id.name).text = name

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

                val vid = mView.findViewById<VideoView>(R.id.video)
                vid.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                vid.setOnErrorListener { _, what, extra ->
                    Log.e("PopupWindow", "Error playing popup video: what=$what extra=$extra")
                    true
                }
                vid.setVideoPath("android.resource://${context.packageName}/$videoRes")
                vid.resolveAdjustedSize(vid.width, vid.height)
                vid.start()
                vid.setOnCompletionListener {
                    vid.start()
                }
                // A surface with nothing drawn into it yet is black, and the card
                // used to open on that. Keep the clip hidden until playback says it
                // has put a frame up, then bring it in - or park it, if a bud has
                // already been taken out and the still is what belongs there.
                vid.setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        if (showingBudsInCase != false) {
                            vid.animate().alpha(1f).setDuration(ARRIVING_FADE_MS).start()
                        } else {
                            vid.pause()
                        }
                    }
                    false
                }

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

                registerBatteryUpdateReceiver()

                autoCloseRunnable = Runnable { close() }
                autoCloseHandler.postDelayed(autoCloseRunnable!!, 12000)
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

        showBudsInCase(showCombinedBuds)

        val badgeVisibility = if (showCombinedBuds) View.GONE else View.VISIBLE
        updateBatteryBadge(R.id.left_battery_badge, badgeVisibility, left?.level)
        updateBatteryBadge(R.id.right_battery_badge, badgeVisibility, right?.level)
        updateBatteryBadge(R.id.case_battery_badge, badgeVisibility, case?.level)

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
     * The clip is the buds resting in their case, so it only holds while the case
     * still reports them as one. The moment a bud is taken out and the battery
     * splits in two, Apple swaps in the still - the same render the settings screen
     * heads with - and swaps back once both are seated again.
     *
     * Both directions are a plain crossfade on alpha, and the clip's visibility is
     * never touched: hiding a VideoView tears its surface down, and bringing it
     * back showed a black frame until playback had drawn into the new one.
     */
    private fun showBudsInCase(inCase: Boolean) {
        if (showingBudsInCase == inCase) return
        val settling = showingBudsInCase == null
        showingBudsInCase = inCase

        val video = mView.findViewById<VideoView>(R.id.video)
        val artwork = mView.findViewById<View>(R.id.artwork)
        video.animate().cancel()
        artwork.animate().cancel()

        if (settling) {
            // The clip stays at nothing either way; the first rendered frame is what
            // brings it in, and only if it is still the one that belongs there.
            artwork.alpha = if (inCase) 0f else 1f
            return
        }

        if (inCase) {
            video.start()
            video.animate().alpha(1f).setDuration(ARRIVING_FADE_MS).start()
            artwork.animate().alpha(0f).setDuration(LEAVING_FADE_MS).start()
            return
        }

        artwork.animate().alpha(1f).setDuration(ARRIVING_FADE_MS).start()
        video.animate().alpha(0f).setDuration(LEAVING_FADE_MS).withEndAction {
            video.pause()
        }.start()
    }

    /**
     * iOS holds the badge at the secondary label's opacity while the component is
     * still filling and takes it to full strength once it reads 100%, the case
     * included.
     */
    private fun updateBatteryBadge(badgeId: Int, visibility: Int, level: Int?) {
        val badge = mView.findViewById<ImageView>(badgeId)
        badge.visibility = visibility

        val full = (level ?: 0) >= 100
        badge.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                if (full) R.color.popup_text else R.color.popup_secondary_text
            )
        )
        badge.alpha = if (full) 1f else BADGE_FILLING_ALPHA
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

            autoCloseRunnable?.let { autoCloseHandler.removeCallbacks(it) }
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
        const val LEAVING_FADE_MS = 200L

        /** What the badge sits at until its component reads 100%. */
        const val BADGE_FILLING_ALPHA = 0.6f

        const val DIM_AMOUNT = 0.3f
        const val BLUR_BEHIND_RADIUS_DP = 48
        val BATTERY_PROGRESS_GREEN = 0xFF21BD44.toInt()
    }
}
