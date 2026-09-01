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
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import me.kavishdevar.librepods.data.FallbackArtwork

@SuppressLint("InflateParams", "ClickableViewAccessibility")
class PopupWindow(
    private val context: Context,
    private val onCloseCallback: () -> Unit = {}
) {
    private val mView: View
    private var isClosing = false
    private var autoCloseHandler = Handler(Looper.getMainLooper())
    private var autoCloseRunnable: Runnable? = null
    private var batteryUpdateReceiver: BroadcastReceiver? = null
    private var dimAnimator: ValueAnimator? = null

    @Suppress("DEPRECATION")
    private val mParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        height = WindowManager.LayoutParams.WRAP_CONTENT
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        width = if (screenWidthDp >= 600) {
            (400 * displayMetrics.density).toInt()
        } else {
            WindowManager.LayoutParams.MATCH_PARENT
        }
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
        mParams.y = 0

        mView.setOnClickListener {
            close()
        }

        mView.findViewById<ImageButton>(R.id.close_button).setOnClickListener {
            close()
        }

        val ll = mView.findViewById<LinearLayout>(R.id.linear_layout)
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
    }

    @SuppressLint("InlinedApi", "SetTextI18s")
    fun open(name: String = "AirPods Pro", batteryNotification: AirPodsNotifications.BatteryNotification, videoRes: Int = FallbackArtwork.Pro.connected) {
        try {
            if (mView.windowToken == null && mView.parent == null && !isClosing) {
                mView.findViewById<TextView>(R.id.name).text = name

                updateBatteryStatus(batteryNotification)

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

                try {
                    mWindowManager.addView(mView, mParams)
                } catch (e: Exception) {
                    e.printStackTrace()
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

    private fun updateBatteryStatusFromList(batteryList: List<Battery>) {
        val batteryLeftText = mView.findViewById<TextView>(R.id.left_battery)
        val batteryRightText = mView.findViewById<TextView>(R.id.right_battery)
        val batteryCaseText = mView.findViewById<TextView>(R.id.case_battery)

        batteryLeftText.text = batteryList.find { it.component == BatteryComponent.LEFT }?.let {
            if (it.status != BatteryStatus.DISCONNECTED) {
                "\uDBC3\uDC8E    ${it.level}%"
            } else {
                ""
            }
        } ?: ""

        batteryRightText.text = batteryList.find { it.component == BatteryComponent.RIGHT }?.let {
            if (it.status != BatteryStatus.DISCONNECTED) {
                "\uDBC3\uDC8D    ${it.level}%"
            } else {
                ""
            }
        } ?: ""

        batteryCaseText.text = batteryList.find { it.component == BatteryComponent.CASE }?.let {
            if (it.status != BatteryStatus.DISCONNECTED) {
                "\uDBC3\uDE6C    ${it.level}%"
            } else {
                ""
            }
        } ?: ""
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

        const val DIM_AMOUNT = 0.3f
    }
}
