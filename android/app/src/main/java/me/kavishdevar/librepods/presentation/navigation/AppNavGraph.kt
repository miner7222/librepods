package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.PathEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.data.updates.updates
import me.kavishdevar.librepods.presentation.screens.AccessibilitySettingsScreen
import me.kavishdevar.librepods.presentation.screens.AdaptiveStrengthScreen
import me.kavishdevar.librepods.presentation.screens.AirPodsSettingsRoute
import me.kavishdevar.librepods.presentation.screens.AppSettingsScreen
import me.kavishdevar.librepods.presentation.screens.AudioAndRoutingScreen
import me.kavishdevar.librepods.presentation.screens.BatterySettingsScreen
import me.kavishdevar.librepods.presentation.screens.CallControlScreen
import me.kavishdevar.librepods.presentation.screens.ControlsAndGesturesScreen
import me.kavishdevar.librepods.presentation.screens.EqualizerRoute
import me.kavishdevar.librepods.presentation.screens.HeadTrackingScreen
import me.kavishdevar.librepods.presentation.screens.HearingAidAdjustmentsScreen
import me.kavishdevar.librepods.presentation.screens.HearingAidScreen
import me.kavishdevar.librepods.presentation.screens.HearingProtectionScreen
import me.kavishdevar.librepods.presentation.screens.LoadingScreen
import me.kavishdevar.librepods.presentation.screens.LongPress
import me.kavishdevar.librepods.presentation.screens.ConnectToThisDeviceRoute
import me.kavishdevar.librepods.presentation.screens.MicrophoneSettingsRoute
import me.kavishdevar.librepods.presentation.screens.OpenSourceLicensesScreen
import me.kavishdevar.librepods.presentation.screens.PurchaseScreen
import me.kavishdevar.librepods.presentation.screens.ReleaseNotesScreen
import me.kavishdevar.librepods.presentation.screens.RenameScreen
import me.kavishdevar.librepods.presentation.screens.TransparencySettingsScreen
import me.kavishdevar.librepods.presentation.screens.TroubleshootingScreen
import me.kavishdevar.librepods.presentation.screens.UpdateHearingTestRoute
import me.kavishdevar.librepods.presentation.screens.VersionScreen
import me.kavishdevar.librepods.presentation.screens.onboarding.OnboardingScreen
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.AppSettingsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.PurchaseViewModel

@Composable
fun AppNavGraph(
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    showOnboarding: Boolean = false,
    onboardingComplete: () -> Unit = {},
    backStack: SnapshotStateList<Screen>,
    airPodsViewModel: AirPodsViewModel,
    onScrollStateChanged: (Screen, Boolean) -> Unit = { _, _ -> },
) {
    val navigate: (Screen) -> Unit = { screen ->
        backStack.add(screen)
    }

    fun navigateToPurchase() {
        navigate(Screen.Purchase)
    }

    fun navigateToLeftLongPress() {
        navigate(Screen.LongPress("Left"))
    }

    fun navigateToRightLongPress() {
        navigate(Screen.LongPress("Right"))
    }

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val sharedAxisSlidePx = with(LocalDensity.current) {
        SharedAxisSlideDistance.roundToPx()
    }

    SharedTransitionLayout {
        NavDisplay(
            sharedTransitionScope = this,
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            entryProvider = { screen ->
                when (screen) {
                    Screen.Onboarding ->
                        NavEntry(screen) {
                            OnboardingScreen {
                                onboardingComplete()
                                if (showReleaseNotes) navigate(Screen.ReleaseNotes) else navigate(Screen.AirPodsSettings)
                                backStack.remove(screen)
                            }
                        }
                    Screen.AirPodsSettings ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            AirPodsSettingsRoute(
                                viewModel = airPodsViewModel,
                                navigateToRename = { navigate(Screen.Rename) },
                                navigateToHearingProtection = { navigate(Screen.HearingProtection) },
                                navigateToHearingAid = { navigate(Screen.HearingAid) },
                                navigateToLeftLongPress = ::navigateToLeftLongPress,
                                navigateToRightLongPress = ::navigateToRightLongPress,
                                navigateToPurchase = { navigate(Screen.Purchase) },
                                navigateToAdaptiveStrength = { navigate(Screen.AdaptiveStrength) },
                                navigateToEqualizer = { navigate(Screen.Equalizer) },
                                navigateToHeadTracking = { navigate(Screen.HeadTracking) },
                                navigateToAudioAndRouting = { navigate(Screen.AudioAndRouting) },
                                navigateToControlsAndGestures = { navigate(Screen.ControlsAndGestures) },
                                navigateToAccessibility = { navigate(Screen.Accessibility) },
                                navigateToBattery = { navigate(Screen.Battery) },
                                navigateToVersion = { navigate(Screen.VersionInfo) },
                                navigateToTroubleshooting = { navigate(Screen.Troubleshooting) },
                                navigateToCallControlScreen = { navigate(Screen.CallControl(it)) },
                                navigateToMicrophoneSettings = { navigate(Screen.MicrophoneSettings) },
                                navigateToConnectToThisDevice = { navigate(Screen.ConnectToThisDevice) },
                                onScrollStateChanged = { onScrollStateChanged(screen, it) },
                            )
                        }

                    Screen.AudioAndRouting ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            AudioAndRoutingScreen(
                                viewModel = airPodsViewModel,
                                navigateToAdaptiveStrength = { navigate(Screen.AdaptiveStrength) },
                                navigateToEqualizer = { navigate(Screen.Equalizer) },
                                navigateToMicrophoneSettings = { navigate(Screen.MicrophoneSettings) },
                                navigateToConnectToThisDevice = { navigate(Screen.ConnectToThisDevice) },
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.ControlsAndGestures ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            ControlsAndGesturesScreen(
                                viewModel = airPodsViewModel,
                                navigateToLeftLongPress = ::navigateToLeftLongPress,
                                navigateToRightLongPress = ::navigateToRightLongPress,
                                navigateToCallControlScreen = { navigate(Screen.CallControl(it)) },
                                navigateToHeadTracking = { navigate(Screen.HeadTracking) },
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.Battery ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            BatterySettingsScreen(
                                viewModel = airPodsViewModel,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.Rename ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            RenameScreen(airPodsViewModel)
                        }

                    Screen.AppSettings ->
                        NavEntry(screen) {
                            val vm: AppSettingsViewModel = viewModel()
                            AppSettingsScreen(
                                viewModel = vm,
                                navigateToPurchase = ::navigateToPurchase,
                                navigateToTroubleshooting = { navigate(Screen.Troubleshooting) },
                                navigateToOpenSourceLicenses = { navigate(Screen.OpenSourceLicenses) },
                                navigateToReleaseNotesScreen = { navigate(Screen.ReleaseNotes) },
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.Troubleshooting ->
                        NavEntry(screen) {
                            TroubleshootingScreen(
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.HeadTracking ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            HeadTrackingScreen(
                                airPodsViewModel,
                                ::navigateToPurchase,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.Accessibility ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            AccessibilitySettingsScreen(
                                viewModel = airPodsViewModel,
                                navigateToPurchase = ::navigateToPurchase,
                                navigateToTransparencyCustomization = { navigate(Screen.TransparencyCustomization) },
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.TransparencyCustomization ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            TransparencySettingsScreen(
                                airPodsViewModel,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.HearingAid ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            HearingAidScreen(
                                viewModel = airPodsViewModel,
                                onNavigateHearingAidAdjustments = { navigate(Screen.HearingAidAdjustments) },
                                onNavigateHearingTest = { navigate(Screen.UpdateHearingTest) },
                                onScrollStateChanged = { onScrollStateChanged(screen, it) },
                            )
                        }

                    Screen.HearingAidAdjustments ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            HearingAidAdjustmentsScreen(
                                airPodsViewModel,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.AdaptiveStrength ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            AdaptiveStrengthScreen(airPodsViewModel, ::navigateToPurchase)
                        }

//                Screen.CameraControl ->
//                    NavEntry(screen) {
//                        CameraControlScreen(airPodsViewModel)
//                    }

                    Screen.OpenSourceLicenses ->
                        NavEntry(screen) {
                            OpenSourceLicensesScreen()
                        }

                    Screen.UpdateHearingTest ->
                        NavEntry(screen) {
                            UpdateHearingTestRoute(
                                airPodsViewModel,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.VersionInfo ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            VersionScreen(airPodsViewModel)
                        }

                    Screen.HearingProtection ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            HearingProtectionScreen(
                                viewModel = airPodsViewModel,
                                navigateToPurchase = ::navigateToPurchase,
                                navigateToHearingAid = { navigate(Screen.HearingAid) }
                            )
                        }

                    Screen.Purchase ->
                        NavEntry(screen) {
                            val vm: PurchaseViewModel = viewModel()
                            PurchaseScreen(
                                vm,
                                backStack,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    Screen.Equalizer ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            EqualizerRoute(
                                airPodsViewModel,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    is Screen.LongPress ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            LongPress(
                                viewModel = airPodsViewModel,
                                name = screen.bud,
                                navigateToPurchase = ::navigateToPurchase,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    is Screen.CallControl ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            CallControlScreen(
                                viewModel = airPodsViewModel,
                                action = screen.action,
                                onCallControlValueChanged = { flipped ->
                                    airPodsViewModel.setControlCommandValue(
                                        AACPManager.Companion.ControlCommandIdentifiers.CALL_MANAGEMENT_CONFIG,
                                        if (flipped) byteArrayOf(0x00, 0x02) else byteArrayOf(
                                            0x00,
                                            0x03
                                        )
                                    )
                                },
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    is Screen.MicrophoneSettings ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            MicrophoneSettingsRoute(
                                viewModel = airPodsViewModel,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    is Screen.ConnectToThisDevice ->
                        NavEntry(screen) {
                            if (!airPodsViewModel.isReady) LoadingScreen()
                            ConnectToThisDeviceRoute(
                                viewModel = airPodsViewModel,
                                onScrollStateChanged = { onScrollStateChanged(screen, it) }
                            )
                        }

                    is Screen.ReleaseNotes ->
                        NavEntry(screen) {
                            ReleaseNotesScreen(
                                updates = updates,
                                releaseNotesShown = {
                                    if (showReleaseNotes) {
                                        navigate(Screen.AirPodsSettings)
                                        backStack.remove(screen)
                                        updatesShown()
                                    } else {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                }
                            )
                        }
                }
            },
            transitionSpec = {
                if (m3eEnabled) sharedAxis(back = false, slidePx = sharedAxisSlidePx)
                else appleSlide(back = false)
            },
            popTransitionSpec = {
                if (m3eEnabled) sharedAxis(back = true, slidePx = sharedAxisSlidePx)
                else appleSlide(back = true)
            },
            predictivePopTransitionSpec = {
                if (m3eEnabled) materialPredictiveBack() else appleSlide(back = true)
            },
        )
    }
}

/**
 * Material's emphasized easing, in the path interpolator Material publishes for
 * Android: M 0,0 C 0.05,0 0.133333,0.06 0.166666,0.4 C 0.208333,0.82 0.25,1 1,1. It
 * is a path rather than one cubic, which is why the guidance tells CSS and iOS to
 * fall back to the standard set - Compose can follow the path.
 */
private val EmphasizedEasing = PathEasing(
    Path().apply {
        moveTo(0f, 0f)
        cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
        cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
    }
)

/**
 * What MaterialSharedAxis - the transition Material names as forward-and-backward on
 * Android - actually uses. It reads motionDurationLong1 and the emphasized
 * interpolator off the theme, and slides a fixed
 * mtrl_transition_shared_axis_slide_distance rather than any share of the width,
 * which is how Material keeps a screen from crossing the whole device.
 */
private const val SharedAxisDurationMs = 450
private val SharedAxisSlideDistance = 30.dp

/**
 * Material's fade-through and Google's predictive back spec agree on the handover:
 * the leaving screen is fully transparent 35% of the way through, and only then does
 * the arriving one begin to appear, so the two are never both half visible.
 */
private const val FadeThroughThreshold = 0.35f
private val FadeOutDurationMs = (SharedAxisDurationMs * FadeThroughThreshold).toInt()
private val FadeInDurationMs = SharedAxisDurationMs - FadeOutDurationMs

/** The curve Google gives for a predictive back on a full screen surface. */
private val PredictiveBackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

/**
 * Apple slides the arriving screen the whole width and drags the leaving one a
 * quarter of it behind - the parallax Material names as the iOS default.
 */
private fun appleSlide(back: Boolean): ContentTransform =
    if (back) {
        slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
    } else {
        slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }
    }

/** MaterialSharedAxis along X, in Compose. */
private fun sharedAxis(back: Boolean, slidePx: Int): ContentTransform {
    val direction = if (back) -1 else 1
    return (
        slideInHorizontally(
            initialOffsetX = { direction * slidePx },
            animationSpec = tween(SharedAxisDurationMs, easing = EmphasizedEasing)
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FadeInDurationMs,
                delayMillis = FadeOutDurationMs,
                easing = LinearEasing
            )
        )
    ) togetherWith (
        slideOutHorizontally(
            targetOffsetX = { -direction * slidePx },
            animationSpec = tween(SharedAxisDurationMs, easing = EmphasizedEasing)
        ) + fadeOut(animationSpec = tween(FadeOutDurationMs, easing = LinearEasing))
    )
}

/**
 * Google's motion spec for a predictive back between two full screen surfaces: the
 * screen being left scales to 90% and the one returning from 110%, with the same fade
 * through between them. No sideways travel - the shift in that spec belongs to the
 * shared element variant. What was here slid an eighth of the way across and stopped
 * at three quarters opacity, so the screen never finished leaving; it stopped
 * existing where it stood.
 */
private fun materialPredictiveBack(): ContentTransform = (
    scaleIn(
        initialScale = 1.1f,
        animationSpec = tween(SharedAxisDurationMs, easing = PredictiveBackEasing)
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = FadeInDurationMs,
            delayMillis = FadeOutDurationMs,
            easing = LinearEasing
        )
    )
) togetherWith (
    scaleOut(
        targetScale = 0.9f,
        animationSpec = tween(SharedAxisDurationMs, easing = PredictiveBackEasing)
    ) + fadeOut(animationSpec = tween(FadeOutDurationMs, easing = LinearEasing))
)
