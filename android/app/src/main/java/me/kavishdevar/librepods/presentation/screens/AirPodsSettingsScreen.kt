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

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.screens

// import me.kavishdevar.librepods.utils.RadareOffsetFinder
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import me.kavishdevar.librepods.presentation.components.ReportStyledScaffoldScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.graphics.shapes.Morph
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.delay
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.ATTHandles
import me.kavishdevar.librepods.data.AirPodsPro3
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.MaterialIcons
import me.kavishdevar.librepods.presentation.components.AboutCard
import me.kavishdevar.librepods.presentation.components.AppleDisconnectedContent
import me.kavishdevar.librepods.presentation.components.AppleSettingsIconTile
import me.kavishdevar.librepods.presentation.components.AudioSettings
import me.kavishdevar.librepods.presentation.components.BatteryView
import me.kavishdevar.librepods.presentation.components.CallControlSettings
import me.kavishdevar.librepods.presentation.components.ConnectionSettings
import me.kavishdevar.librepods.presentation.components.HearingHealthSettings
import me.kavishdevar.librepods.presentation.components.MaterialButtonStyle
import me.kavishdevar.librepods.presentation.components.NoiseControlSettings
import me.kavishdevar.librepods.presentation.components.PressAndHoldSettings
import me.kavishdevar.librepods.presentation.components.StyledButton
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledSlider
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.theme.AppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAppleDesignMetrics
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.theme.sectionHeader
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.demoState
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Composable
fun AirPodsSettingsRoute(
    viewModel: AirPodsViewModel,
    navigateToRename: () -> Unit,
    navigateToHearingProtection: () -> Unit,
    navigateToHearingAid: () -> Unit,
    navigateToLeftLongPress: () -> Unit,
    navigateToRightLongPress: () -> Unit,
    navigateToPurchase: () -> Unit,
    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit,
    navigateToHeadTracking: () -> Unit,
    navigateToAudioAndRouting: () -> Unit,
    navigateToControlsAndGestures: () -> Unit,
    navigateToAccessibility: () -> Unit,
    navigateToBattery: () -> Unit,
    navigateToVersion: () -> Unit,
    navigateToTroubleshooting: () -> Unit,
    navigateToCallControlScreen: (action: String) -> Unit,
    navigateToMicrophoneSettings: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
        if (m3eEnabled) 0.dp else LocalAppleDesignMetrics.current.navigationBarHeight +
            LocalAppleDesignMetrics.current.cardColumnTopInset
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp

    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        AirPodsSettingsScreen(
            state = state,

            topPadding = topPadding,
            bottomPadding = bottomPadding,
            onScrollStateChanged = onScrollStateChanged,

            setControlCommandInt = viewModel::setControlCommandInt,
            setControlCommandBoolean = viewModel::setControlCommandBoolean,
//        setControlCommandValue = viewModel::setControlCommandValue,
            setControlCommandByte = viewModel::setControlCommandByte,

            setATTCharacteristicValue = viewModel::setATTCharacteristicValue,

            onAutomaticEarDetectionChanged = viewModel::setAutomaticEarDetectionEnabled,
            onAutomaticConnectionChanged = viewModel::setAutomaticConnectionEnabled,
            setDynamicEndOfCharge = viewModel::setDynamicEndOfCharge,
            setOffListeningMode = viewModel::setOffListeningMode,
            disconnect = viewModel::disconnect,

            navigateToRename = navigateToRename,
            navigateToHearingProtection = navigateToHearingProtection,
            navigateToHearingAid = navigateToHearingAid,
            navigateToLeftLongPress = navigateToLeftLongPress,
            navigateToRightLongPress = navigateToRightLongPress,
            navigateToPurchase = navigateToPurchase,
            navigateToAdaptiveStrength = navigateToAdaptiveStrength,
            navigateToEqualizer = navigateToEqualizer,
            navigateToHeadTracking = navigateToHeadTracking,
            navigateToAudioAndRouting = navigateToAudioAndRouting,
            navigateToControlsAndGestures = navigateToControlsAndGestures,
            navigateToAccessibility = navigateToAccessibility,
            navigateToBattery = navigateToBattery,
            navigateToVersion = navigateToVersion,
            navigateToTroubleshooting = navigateToTroubleshooting,
            navigateToCallControlScreen = navigateToCallControlScreen,
            navigateToMicrophoneSettings = navigateToMicrophoneSettings,

            activateDemoMode = viewModel::activateDemoMode,
            reconnectFromSavedMac = viewModel::reconnectFromSavedMac
        )
    }
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Composable
private fun MediaVolumeSettings() {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    // The slider position is a float so the fill follows the finger; the stream has
    // only ~16 steps, and feeding those back made the handle stick between them.
    // appliedVolume is what we last wrote, so the drag neither re-reads the stream
    // every frame nor mistakes the echo of its own write for someone else's change.
    var appliedVolume by remember(audioManager) {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    var volumePosition by remember(audioManager) {
        mutableFloatStateOf(appliedVolume.toFloat())
    }

    DisposableEffect(context, audioManager) {
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val systemVolume =
                        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (systemVolume != appliedVolume) {
                        appliedVolume = systemVolume
                        volumePosition = systemVolume.toFloat()
                    }
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(volumeReceiver, filter)
        onDispose {
            context.unregisterReceiver(volumeReceiver)
        }
    }

    val appleMetrics = LocalAppleDesignMetrics.current
    val volumeLabel = stringResource(R.string.volume)

    Column(
        modifier = Modifier.padding(
            bottom = if (m3eEnabled) 0.dp else appleMetrics.cardGap
        )
    ) {
        if (!m3eEnabled) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = appleMetrics.cardHorizontalInset)
                    .padding(top = 4.dp, bottom = appleMetrics.sectionHeaderBottomGap)
            ) {
                Text(
                    text = volumeLabel,
                    color = MaterialTheme.colorScheme.sectionHeader,
                    style = appleMetrics.sectionHeaderStyle
                )
            }
        }

        StyledSlider(
            label = if (m3eEnabled) volumeLabel else null,
            value = volumePosition,
            onValueChange = { value ->
                volumePosition = value
                val target = value.roundToInt().coerceIn(0, maxVolume)
                if (target != appliedVolume) {
                    appliedVolume = target
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                    } catch (e: Exception) {
                        Log.e("AirPodsSettings", "Failed to set volume", e)
                    }
                }
            },
            valueRange = 0f..maxVolume.toFloat(),
            startIcon = R.drawable.sf_speaker_wave_3_fill,
            independent = true,
            prominent = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
@Composable
fun AirPodsSettingsScreen(
        state: AirPodsUiState,

        topPadding: Dp = 16.dp,
        bottomPadding: Dp = 16.dp,
        onScrollStateChanged: (Boolean) -> Unit = {},

        setControlCommandInt: (AACPManager.Companion.ControlCommandIdentifiers, Int) -> Unit,
        setControlCommandBoolean: (AACPManager.Companion.ControlCommandIdentifiers, Boolean) -> Unit,
//        setControlCommandValue: (AACPManager.Companion.ControlCommandIdentifiers, ByteArray) -> Unit,
        setControlCommandByte: (AACPManager.Companion.ControlCommandIdentifiers, Byte) -> Unit,
        setATTCharacteristicValue: (ATTHandles, ByteArray) -> Unit,

        onAutomaticEarDetectionChanged: (Boolean) -> Unit,
        onAutomaticConnectionChanged: (Boolean) -> Unit,
        setDynamicEndOfCharge: (Boolean) -> Unit,
        setOffListeningMode: (Boolean) -> Unit,
        disconnect: () -> Unit,

        navigateToRename: () -> Unit,
        navigateToHearingProtection: () -> Unit,
        navigateToHearingAid: () -> Unit,
        navigateToLeftLongPress: () -> Unit,
        navigateToRightLongPress: () -> Unit,
        navigateToPurchase: () -> Unit,
        navigateToAdaptiveStrength: () -> Unit,
        navigateToEqualizer: () -> Unit,
        navigateToHeadTracking: () -> Unit,
        navigateToAudioAndRouting: () -> Unit,
        navigateToControlsAndGestures: () -> Unit,
        navigateToAccessibility: () -> Unit,
        navigateToBattery: () -> Unit,
        navigateToVersion: () -> Unit,
        navigateToTroubleshooting: () -> Unit,
        navigateToCallControlScreen: (action: String) -> Unit,
        navigateToMicrophoneSettings: () -> Unit,

        activateDemoMode: () -> Unit,
        reconnectFromSavedMac: () -> Unit,
) {
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val sharedPreferences = LocalContext.current.getSharedPreferences("settings", MODE_PRIVATE)
    var deviceName by remember {
        mutableStateOf(
            TextFieldValue(
                sharedPreferences.getString("name", state.deviceName).toString()
            )
        )
    }

    var lastConnectedModelNumber by remember(sharedPreferences) {
        mutableStateOf(sharedPreferences.getString("airpods_model_number", null))
    }

    val preferenceChangeListener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "name" -> deviceName =
                    TextFieldValue(sharedPreferences.getString("name", "AirPods Pro").toString())
                "airpods_model_number" -> lastConnectedModelNumber =
                    sharedPreferences.getString("airpods_model_number", null)
            }
        }
    }

    DisposableEffect(Unit) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        onDispose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }

    if (state.isLocallyConnected) {
        val capabilities = state.capabilities

        val listState = rememberLazyListState()
        ReportStyledScaffoldScrollState(listState, onScrollStateChanged)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = LocalAppleDesignMetrics.current.cardHorizontalInset)
        ) {
            item(key = "top_padding") { Spacer(modifier = Modifier.height(topPadding)) }
            item(key = "play_update_banner") {
                if (state.timeUntilFOSSPremiumExpiry > 0L) {
                    val context = LocalContext.current
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF32829B), RoundedCornerShape(LocalAppleDesignMetrics.current.cardCornerRadius))
                            .clip(RoundedCornerShape(LocalAppleDesignMetrics.current.cardCornerRadius))
                            .clickable {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = "mailto:".toUri()
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("billing@kavish.xyz"))
                                    putExtra(Intent.EXTRA_SUBJECT, "LibrePods Play billing error")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Please enter your GitHub username to restore your premium access:\n\nGitHub username: "
                                    )
                                }
                                context.startActivity(emailIntent)
                            }) {
                        Text(
                            text = stringResource(
                                R.string.play_foss_premium_banner,
                                maxOf(
                                    1,
                                    TimeUnit.MILLISECONDS.toDays(state.timeUntilFOSSPremiumExpiry)
                                        .toInt()
                                )
                            ), modifier = Modifier.padding(16.dp), style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily(Font(R.font.pretendard))
                            )
                        )
                    }
                }
            }

            item(key = "battery") {
                BatteryView(
                    batteryList = state.battery,
                    budsRes = state.instance?.model?.budsRes ?: R.drawable.airpods_pro_2_buds,
                    caseRes = state.instance?.model?.caseRes ?: R.drawable.airpods_pro_2_case
                )
            }
            item(key = "spacer_battery") {
                Spacer(modifier = Modifier.height(32.dp))
            }

            item(key = "name") {
                StyledListItem(
                    name = stringResource(R.string.name),
                    description = deviceName.text,
                    onClick = navigateToRename,
                )
            }

            val hasHearingAidCapability =
                state.instance?.model?.capabilities?.contains(Capability.HEARING_AID) == true
            val hasPPECapability =
                state.instance?.model?.capabilities?.contains(Capability.PPE) == true

            if (hasHearingAidCapability || hasPPECapability) {
                if (hasPPECapability || state.vendorIdHook) {
                    item(key = "spacer_hearing_health") {
                        Spacer(modifier = Modifier.height(if (m3eEnabled) 24.dp else 0.dp))
                    }
                }
                item(key = "hearing_health") {
                    HearingHealthSettings(
                        hasPPECapability = hasPPECapability,
                        hasHearingAidCapability = hasHearingAidCapability,
                        vendorIdHook = state.vendorIdHook,
                        navigateToHearingProtection = navigateToHearingProtection,
                        navigateToHearingAid = navigateToHearingAid
                    )
                }
            }

            if (capabilities.contains(Capability.LISTENING_MODE)) {
                item(key = "spacer_noise") {
                    Spacer(modifier = Modifier.height(if (m3eEnabled) 16.dp else 0.dp))
                }
                item(key = "noise_control") {
                    NoiseControlSettings(
                        showOffListeningMode = state.offListeningMode,
                        noiseControlModeValue = state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE]?.getOrNull(
                            0
                        )?.toInt() ?: 3,
                        onNoiseControlModeChanged = {
                            setControlCommandInt(
                                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE, it
                            )
                        },
                    )
                }
            }

            item(key = "media_volume") {
                MediaVolumeSettings()
            }

            if (!m3eEnabled) {
                if (capabilities.contains(Capability.OFF_LISTENING_MODE)) {
                    item(key = "spacer_off_listening") { Spacer(modifier = Modifier.height(0.dp)) }
                    item(key = "off_listening") {
                        StyledToggle(
                            label = stringResource(R.string.off_listening_mode),
                            description = stringResource(R.string.off_listening_mode_description),
                            checked = state.offListeningMode,
                            onCheckedChange = setOffListeningMode
                        )
                    }
                }
            }

            if (m3eEnabled) {
                if (capabilities.contains(Capability.STEM_CONFIG)) {
                    item(key = "spacer_press_hold") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item(key = "press_hold") {
                        PressAndHoldSettings(
                            leftAction = state.leftAction,
                            rightAction = state.rightAction,
                            navigateToLeftLongPress = navigateToLeftLongPress,
                            navigateToRightLongPress = navigateToRightLongPress
                        )
                    }
                }

                item(key = "spacer_call") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item(key = "call_control") {
                    val bytes =
                        state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CALL_MANAGEMENT_CONFIG]?.take(
                            2
                        )?.toByteArray() ?: byteArrayOf(0x00, 0x00)
                    val flipped = try {
                        bytes[1] == 0x02.toByte()
                    } catch (_: Exception) {
                        false
                    }
                    CallControlSettings(
                        flipped = flipped,
                        navigateToCallControlScreen = navigateToCallControlScreen
                    )
                }
            }

//                if (capabilities.contains(Capability.STEM_CONFIG) && !BuildConfig.PLAY_BUILD) {
//                    item(key = "spacer_camera") { Spacer(modifier = Modifier.height(16.dp)) }
//                    item(key = "camera_control") {
//                        StyledListItem(
//                            to = "camera_control",
//                            name = stringResource(R.string.camera_remote),
//                            descriptionRes = stringResource(R.string.camera_control_description),
//                            titleRes = stringResource(R.string.camera_control),
//                            navController = navController
//                        )
//                    }
//                }

            item(key = "upgrade_button") {
                if (!state.isPremium) {
                    Spacer(modifier = Modifier.height(28.dp))
                    StyledButton(
                        onClick = navigateToPurchase,
                        backdrop = rememberLayerBackdrop(),
                        modifier = Modifier.fillMaxWidth(),
                        maxScale = 0.05f,
                        surfaceColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            stringResource(R.string.unlock_advanced_features),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (m3eEnabled) {
                item(key = "spacer_audio") { Spacer(modifier = Modifier.height(16.dp)) }
                item(key = "audio") {
                    val model = state.instance?.model ?: AirPodsPro3()
                    val adaptiveVolumeCapability =
                        model.capabilities.contains(Capability.ADAPTIVE_VOLUME)
                    val conversationalAwarenessCapability =
                        model.capabilities.contains(Capability.CONVERSATION_AWARENESS)
                    val loudSoundReductionCapability =
                        model.capabilities.contains(Capability.LOUD_SOUND_REDUCTION)
                    val adaptiveAudioCapability =
                        model.capabilities.contains(Capability.ADAPTIVE_AUDIO)

                    val adaptiveVolumeChecked =
                        state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG]?.getOrNull(
                            0
                        ) == 0x01.toByte()
                    val conversationalAwarenessChecked =
                        state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG]?.getOrNull(
                            0
                        ) == 0x01.toByte()

                    AudioSettings(
                        adaptiveVolumeCapability = adaptiveVolumeCapability,
                        conversationalAwarenessCapability = conversationalAwarenessCapability,
                        loudSoundReductionCapability = loudSoundReductionCapability,
                        adaptiveAudioCapability = adaptiveAudioCapability,
                        customEqCapability = true,
                        adaptiveVolumeChecked = adaptiveVolumeChecked,
                        onAdaptiveVolumeCheckedChange = { checked ->
                            setControlCommandBoolean(
                                AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG,
                                checked
                            )
                        },
                        conversationalAwarenessChecked = conversationalAwarenessChecked && state.isPremium,
                        onConversationalAwarenessCheckedChange = { checked ->
                            setControlCommandBoolean(
                                AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG,
                                checked
                            )
                        },
                        loudSoundReductionChecked = state.loudSoundReductionEnabled,
                        onLoudSoundReductionCheckedChange = { checked ->
                            setATTCharacteristicValue(
                                ATTHandles.LOUD_SOUND_REDUCTION,
                                byteArrayOf(if (checked) 0x01.toByte() else 0x00.toByte())
                            )
                        },
                        navigateToAdaptiveStrength = navigateToAdaptiveStrength,
                        navigateToEqualizer = navigateToEqualizer,
                        vendorIdHook = state.vendorIdHook,
                        isPremium = state.isPremium
                    )
                }

                item(key = "spacer_connection") { Spacer(modifier = Modifier.height(16.dp)) }
                item(key = "connection") {
                    ConnectionSettings(
                        automaticEarDetectionEnabled = state.automaticEarDetectionEnabled,
                        onAutomaticEarDetectionChanged = onAutomaticEarDetectionChanged,
                        automaticConnectionEnabled = state.automaticConnectionEnabled,
                        onAutomaticConnectionChanged = onAutomaticConnectionChanged
                    )
                }

                item(key = "spacer_microphone") { Spacer(modifier = Modifier.height(16.dp)) }
                item(key = "microphone") {
                    val id = AACPManager.Companion.ControlCommandIdentifiers.MIC_MODE

                    val selectedModeText = when (state.controlStates[id]?.getOrNull(0) ?: 0x00.toByte()) {
                        0x00.toByte() -> stringResource(R.string.microphone_automatic)
                        0x01.toByte() -> stringResource(R.string.microphone_always_right)
                        0x02.toByte() -> stringResource(R.string.microphone_always_left)
                        else -> stringResource(R.string.microphone_automatic)
                    }

                    StyledListItem(
                        name = stringResource(R.string.microphone_mode),
                        description = selectedModeText,
                        onClick = navigateToMicrophoneSettings
                    )
                }

                if (capabilities.contains(Capability.SLEEP_DETECTION)) {
                    item(key = "spacer_sleep") { Spacer(modifier = Modifier.height(16.dp)) }
                    item(key = "sleep_detection") {
                        val id = AACPManager.Companion.ControlCommandIdentifiers.SLEEP_DETECTION_CONFIG
                        StyledToggle(
                            label = stringResource(R.string.sleep_detection),
                            checked = state.controlStates[id]?.getOrNull(0) == 0x01.toByte(),
                            onCheckedChange = { setControlCommandBoolean(id, it) },
                            enabled = state.isPremium
                        )
                    }
                }

                if (capabilities.contains(Capability.HEAD_GESTURES)) {
                    item(key = "spacer_head_tracking") { Spacer(modifier = Modifier.height(16.dp)) }
                    item(key = "head_tracking") {
                        StyledListItem(
                            name = stringResource(R.string.head_gestures),
                            description = if (sharedPreferences.getBoolean(
                                    "head_gestures", false
                                )
                            ) stringResource(R.string.on) else stringResource(R.string.off),
                            onClick = navigateToHeadTracking
                        )
                    }
                }

                if (capabilities.contains(Capability.OPTIMIZED_CHARGE_LIMIT)) {
                    item(key = "spacer_dynamic_end_of_charge") { Spacer(modifier = Modifier.height(16.dp)) }
                    item(key = "dynamic_end_of_charge") {
                        StyledToggle(
                            label = stringResource(R.string.optimized_charging),
                            description = stringResource(R.string.optimized_charging_description),
                            checked = state.dynamicEndOfCharge,
                            onCheckedChange = setDynamicEndOfCharge
                        )
                    }
                }

                item(key = "spacer_accessibility") { Spacer(modifier = Modifier.height(16.dp)) }
                item(key = "accessibility") {
                    StyledListItem(
                        name = stringResource(R.string.accessibility), onClick = navigateToAccessibility
                    )
                }
            } else {
                item(key = "spacer_settings_hub") { Spacer(modifier = Modifier.height(0.dp)) }
                item(key = "settings_hub") {
                    StyledList {
                        StyledListItem(
                            name = stringResource(R.string.audio_and_routing),
                            onClick = navigateToAudioAndRouting,
                            leadingContent = {
                                AppleSettingsIconTile(
                                    drawableRes = R.drawable.sf_speaker_wave_3_fill,
                                    containerColor = AppleDesignMetrics.audioAndRoutingIconTileColor
                                )
                            }
                        )
                        StyledListItem(
                            name = stringResource(R.string.controls_and_gestures),
                            onClick = navigateToControlsAndGestures,
                            leadingContent = {
                                AppleSettingsIconTile(
                                    drawableRes = R.drawable.sf_hand_pinch_fill,
                                    containerColor = AppleDesignMetrics.controlsAndGesturesIconTileColor
                                )
                            }
                        )
                        StyledListItem(
                            name = stringResource(R.string.accessibility),
                            onClick = navigateToAccessibility,
                            leadingContent = {
                                AppleSettingsIconTile(
                                    drawableRes = R.drawable.sf_accessibility,
                                    containerColor = AppleDesignMetrics.accessibilityIconTileColor
                                )
                            }
                        )
                    }
                }

                item(key = "spacer_battery_settings") { Spacer(modifier = Modifier.height(0.dp)) }
                item(key = "battery_settings") {
                    StyledList {
                        StyledListItem(
                            name = stringResource(R.string.battery),
                            onClick = navigateToBattery,
                            leadingContent = {
                                AppleSettingsIconTile(
                                    drawableRes = R.drawable.sf_battery_100percent,
                                    containerColor = AppleDesignMetrics.batteryIconTileColor
                                )
                            }
                        )
                    }
                }
            }

            if (m3eEnabled) {
                if (capabilities.contains(Capability.OFF_LISTENING_MODE)) {
                    item(key = "spacer_off_listening") { Spacer(modifier = Modifier.height(16.dp)) }
                    item(key = "off_listening") {
                        StyledToggle(
                            label = stringResource(R.string.off_listening_mode),
                            description = stringResource(R.string.off_listening_mode_description),
                            checked = state.offListeningMode,
                            onCheckedChange = setOffListeningMode
                        )
                    }
                }
            }

            item(key = "spacer_about") {
                Spacer(modifier = Modifier.height(if (m3eEnabled) 32.dp else 0.dp))
            }
            item(key = "about") {
                AboutCard(
                    modelName = state.modelName,
                    actualModel = state.actualModel,
                    serialNumbers = state.serialNumbers,
                    version = state.version3,
                    navigateToVersion = navigateToVersion
                )
            }

            item(key = "spacer_disconnect") { Spacer(modifier = Modifier.height(28.dp)) }
            item(key = "disconnect_button") {
                StyledButton(
                    onClick = disconnect,
                    backdrop = rememberLayerBackdrop(),
                    isInteractive = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.disconnect),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

//                item(key = "spacer_debug") { Spacer(modifier = Modifier.height(16.dp)) }
//                item(key = "debug") { StyledListItem("debug", "Debug", navController) }

            item(key = "bottom_padding") { Spacer(modifier = Modifier.height(bottomPadding)) }
        }
    } else {
        val backdrop = rememberLayerBackdrop()
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = rememberLayerBackdrop(),
                    exportedBackdrop = backdrop,
                    shape = { RoundedCornerShape(0.dp) },
                    highlight = {
                        Highlight.Ambient.copy(alpha = 0f)
                    },
                    effects = {}
                )
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = bottomPadding),
            contentAlignment = Alignment.Center
        ) {
            val tapCount = remember { mutableIntStateOf(0) }
            val lastTapTime = remember { mutableLongStateOf(0L) }

            var reconnecting by remember { mutableStateOf(false) }

            LaunchedEffect(reconnecting) {
                if (reconnecting) {
                    delay(5.seconds)
                    reconnecting = false
                }
            }

            when (LocalDesignSystem.current) {
                DesignSystem.Material -> {
                    val polygons = remember {
                        listOf(
                            MaterialShapes.Cookie9Sided,
                            MaterialShapes.Clover4Leaf,
                            MaterialShapes.SoftBurst,
                            MaterialShapes.Sunny,
                            MaterialShapes.Pentagon,
                            MaterialShapes.Cookie4Sided,
                            MaterialShapes.Oval,
                        )
                    }

                    val morphs = remember {
                        buildList {
                            for (i in polygons.indices) {
                                add(
                                    Morph(
                                        polygons[i].normalized(),
                                        polygons[(i + 1) % polygons.size].normalized()
                                    )
                                )
                            }
                        }
                    }

                    var currentMorphIndex by remember { mutableIntStateOf(0) }

                    val morphProgress = remember { Animatable(0f) }

                    LaunchedEffect(reconnecting) {
                        if (!reconnecting) {
                            currentMorphIndex = 0
                            morphProgress.snapTo(0f)
                            return@LaunchedEffect
                        }

                        while (reconnecting) {
                            morphProgress.snapTo(0f)

                            morphProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 650,
                                    easing = FastOutSlowInEasing
                                )
                            )

                            currentMorphIndex = (currentMorphIndex + 1) % morphs.size
                        }
                    }

                    val path = remember { Path() }
                    val scaleMatrix = remember { Matrix() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        activateDemoMode()
                                    }
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            val now = System.currentTimeMillis()

                                            if (now - lastTapTime.longValue > 400) {
                                                tapCount.intValue = 0
                                            }

                                            tapCount.intValue++
                                            lastTapTime.longValue = now

                                            if (tapCount.intValue >= 5) {
                                                tapCount.intValue = 0
                                                activateDemoMode()
                                            }
                                        }
                                    )
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val primaryContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                            val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

                            val animatedShapeColor by animateColorAsState(if (reconnecting) primaryContainerColor else secondaryContainerColor)

                            if (state.connectionSuccessful) {
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceBright,
                                            CircleShape
                                        )
                                        .clickable(
                                            interactionSource = null,
                                            indication = ripple(
                                                bounded = false,
                                                radius = 120.dp
                                            ),
                                            enabled = !reconnecting,
                                            onClick = {}
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    if (!reconnecting) {
                                                        currentMorphIndex = 1
                                                        reconnecting = true
                                                        reconnectFromSavedMac()
                                                    }
                                                },
                                                onPress = {
                                                    if (!reconnecting) {
                                                        morphProgress.animateTo(
                                                            targetValue = 1f,
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            )
                                                        )
                                                        tryAwaitRelease()
                                                        morphProgress.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        .drawWithContent {
                                            val activeMorph = morphs[currentMorphIndex]

                                            val shapePath = activeMorph.toPath(
                                                progress = morphProgress.value,
                                                path = path
                                            )

                                            val bounds = shapePath.getBounds()

                                            val scale = min(
                                                size.width / bounds.width,
                                                size.height / bounds.height
                                            ) * 0.8f

                                            scaleMatrix.reset()

                                            scaleMatrix.scale(x = scale, y = scale)

                                            shapePath.transform(scaleMatrix)

                                            shapePath.translate(size.center - shapePath.getBounds().center)

                                            drawPath(
                                                path = shapePath,
                                                color = animatedShapeColor
                                            )

                                            drawContent()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (reconnecting) MaterialIcons.bluetooth_searching else MaterialIcons.headset_off,
                                        contentDescription = null,
                                        modifier = Modifier.size(84.dp),
                                        tint = if (reconnecting) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Spacer(Modifier.height(40.dp))

                                Text(
                                    text = if (reconnecting) stringResource(R.string.reconnecting) else stringResource(R.string.tap_to_reconnect),
                                    style = MaterialTheme.typography.labelSmallEmphasized,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.airpods_not_connected),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (sharedPreferences.getBoolean("bypass_device_check.v2", false)) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.compatibility_check_bypassed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (!BuildConfig.PLAY_BUILD) {
                            OutlinedButton(
                                onClick = navigateToTroubleshooting,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(24.dp)
                            ) {
                                Text(
                                    stringResource(
                                        R.string.troubleshooting
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }

                DesignSystem.Apple -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(top = topPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppleDisconnectedContent(
                            canReconnect = state.connectionSuccessful,
                            reconnecting = reconnecting,
                            showTroubleshooting = !BuildConfig.PLAY_BUILD,
                            onReconnect = { reconnectFromSavedMac(); reconnecting = true },
                            onTroubleshooting = navigateToTroubleshooting,
                            lastConnectedModelNumber = lastConnectedModelNumber,
                            modifier = Modifier.fillMaxWidth(),
                            messageModifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTime.longValue > 400) {
                                        tapCount.intValue = 0
                                    }
                                    tapCount.intValue++
                                    lastTapTime.longValue = now
                                    if (tapCount.intValue >= 5) {
                                        tapCount.intValue = 0
                                        activateDemoMode()
                                    }
                                })
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Apple")
@Composable
fun AirPodsSettingsScreenPreviewApple() {
    LibrePodsTheme(
        m3eEnabled = false
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AirPodsSettingsScreen(
                state = demoState,

                setControlCommandInt = { _, _ -> },
                setControlCommandBoolean = { _, _ -> },
                setControlCommandByte = { _, _ -> },
                setATTCharacteristicValue = { _, _ -> },

                onAutomaticEarDetectionChanged = {},
                onAutomaticConnectionChanged = {},
                setDynamicEndOfCharge = {},
                setOffListeningMode = {},
                disconnect = {},

                navigateToRename = {},
                navigateToHearingProtection = {},
                navigateToHearingAid = {},
                navigateToLeftLongPress = {},
                navigateToRightLongPress = {},
                navigateToPurchase = {},
                navigateToAdaptiveStrength = {},
                navigateToEqualizer = {},
                navigateToHeadTracking = {},
                navigateToAudioAndRouting = {},
                navigateToControlsAndGestures = {},
                navigateToAccessibility = {},
                navigateToBattery = {},
                navigateToVersion = {},
                navigateToTroubleshooting = {},
                navigateToCallControlScreen = {},
                navigateToMicrophoneSettings = {},

                activateDemoMode = {},
                reconnectFromSavedMac = {}
            )
        }
    }
}


@Preview(name = "Material")
@Composable
fun AirPodsSettingsScreenPreviewMaterial() {
    LibrePodsTheme(
        m3eEnabled = true
    ) {
        Box (
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AirPodsSettingsScreen(
                state = demoState,

                setControlCommandInt = { _, _ -> },
                setControlCommandBoolean = { _, _ -> },
                setControlCommandByte = { _, _ -> },
                setATTCharacteristicValue = { _, _ -> },

                onAutomaticEarDetectionChanged = {},
                onAutomaticConnectionChanged = {},
                setDynamicEndOfCharge = {},
                setOffListeningMode = {},
                disconnect = {},

                navigateToRename = {},
                navigateToHearingProtection = {},
                navigateToHearingAid = {},
                navigateToLeftLongPress = {},
                navigateToRightLongPress = {},
                navigateToPurchase = {},
                navigateToAdaptiveStrength = {},
                navigateToEqualizer = {},
                navigateToHeadTracking = {},
                navigateToAudioAndRouting = {},
                navigateToControlsAndGestures = {},
                navigateToAccessibility = {},
                navigateToBattery = {},
                navigateToVersion = {},
                navigateToTroubleshooting = {},
                navigateToCallControlScreen = {},
                navigateToMicrophoneSettings = {},

                activateDemoMode = {},
                reconnectFromSavedMac = {}
            )
        }
    }
}
