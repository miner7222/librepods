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

package me.kavishdevar.librepods.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import me.kavishdevar.librepods.utils.BluetoothCryptography
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.iterator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Manager for Bluetooth Low Energy scanning operations specifically for AirPods
 */
@OptIn(ExperimentalEncodingApi::class)
class BLEManager(private val context: Context) {

    data class AirPodsStatus(
        val address: String,
        val lastSeen: Long = System.currentTimeMillis(),
        val paired: Boolean = false,
        val model: String = "Unknown",
        val leftBattery: Int? = null,
        val rightBattery: Int? = null,
        val caseBattery: Int? = null,
        val isLeftInEar: Boolean = false,
        val isRightInEar: Boolean = false,
        val isLeftCharging: Boolean = false,
        val isRightCharging: Boolean = false,
        val isCaseCharging: Boolean = false,
        val lidOpen: Boolean = false,
        val color: String = "Unknown",
        val connectionState: String = "Unknown"
    )

    fun getMostRecentStatus(): AirPodsStatus? {
        return deviceStatusMap.values.maxByOrNull { it.lastSeen }
    }

    interface AirPodsStatusListener {
        fun onDeviceStatusChanged(device: AirPodsStatus, previousStatus: AirPodsStatus?)
        fun onBroadcastFromNewAddress(device: AirPodsStatus)
        fun onLidStateChanged(lidOpen: Boolean)
        fun onEarStateChanged(device: AirPodsStatus, leftInEar: Boolean, rightInEar: Boolean)
        fun onBatteryChanged(device: AirPodsStatus)
        fun onDeviceDisappeared()
    }

    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanCallback: ScanCallback? = null
    private var airPodsStatusListener: AirPodsStatusListener? = null
    private val deviceStatusMap = mutableMapOf<String, AirPodsStatus>()
    private val verifiedAddresses = mutableSetOf<String>()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private var currentGlobalLidState: Boolean? = null
    private var lastLidAdvertNanos: Long = Long.MIN_VALUE
    private var lastBroadcastTime: Long = 0
    private val processedAddresses = mutableSetOf<String>()

    /**
     * The last case level the broadcast actually carried.
     *
     * A case with no bud seated cannot measure itself and sends 0xFF, so the level
     * that stands in for it has to be remembered. It belongs to the case, not to the
     * address the packet arrived on: those are resolvable private addresses, two of
     * them in flight at once and both rotating. Keyed per address, and dropped again
     * whenever an address went stale, the fallback was always empty by the time
     * anything asked for it.
     */
    private var lastValidCaseBattery: Int? = null
    private val modelNames = mapOf(
        0x0E20 to "AirPods Pro",
        0x1420 to "AirPods Pro 2",
        0x2420 to "AirPods Pro 2 (USB-C)",
        0x0220 to "AirPods 1",
        0x0F20 to "AirPods 2",
        0x1320 to "AirPods 3",
        0x1920 to "AirPods 4",
        0x1B20 to "AirPods 4 (ANC)",
        0x0A20 to "AirPods Max",
        0x1F20 to "AirPods Max (USB-C)"
    )

    val colorNames = mapOf(
        0x00 to "White", 0x01 to "Black", 0x02 to "Red", 0x03 to "Blue",
        0x04 to "Pink", 0x05 to "Gray", 0x06 to "Silver", 0x07 to "Gold",
        0x08 to "Rose Gold", 0x09 to "Space Gray", 0x0A to "Dark Blue",
        0x0B to "Light Blue", 0x0C to "Yellow"
    )

    val connStates = mapOf(
        0x00 to "Disconnected", 0x04 to "Idle", 0x05 to "Music",
        0x06 to "Call", 0x07 to "Ringing", 0x09 to "Hanging Up", 0xFF to "Unknown"
    )

    private val cleanupHandler = Handler(Looper.getMainLooper())
    /**
     * Read and written from the scan callback and from whichever thread asks for a
     * scan, which is not always the main one.
     */
    private val scanRetryAttempts = AtomicInteger(0)
    private val missingIrkLoggedForScan = AtomicBoolean(false)
    private val scanRetryRunnable = Runnable {
        startScanning(resetRetryAttempts = false)
    }
    private val cleanupRunnable = object : Runnable {
        override fun run() {
            cleanupStaleDevices()
            checkLidStateTimeout()
            cleanupHandler.postDelayed(this, CLEANUP_INTERVAL_MS)
        }
    }

    /**
     * Stops holding on to the lid state, because it is about to become a guess.
     *
     * Called when the link comes up: the AirPods stop advertising then, so whatever
     * the lid does is out of sight until they start again, and the reading kept
     * across that gap is no longer something the next advertisement can be compared
     * with. The periodic check does this too, but only every ten seconds, and a link
     * can come and go inside one of those.
     */
    fun forgetLidState() {
        currentGlobalLidState = null
    }

    fun setAirPodsStatusListener(listener: AirPodsStatusListener) {
        airPodsStatusListener = listener
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        startScanning(resetRetryAttempts = true)
    }

    @SuppressLint("MissingPermission")
    private fun startScanning(resetRetryAttempts: Boolean) {
        try {
            cancelPendingScanRetry()
            if (resetRetryAttempts) {
                scanRetryAttempts.set(0)
                missingIrkLoggedForScan.set(false)
            }
            Log.d(TAG, "Starting BLE scanner")

            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter = btManager.adapter

            if (btAdapter == null) {
                Log.d(TAG, "No Bluetooth adapter available")
                return
            }

            val previousScanCallback = mScanCallback
            mScanCallback = null
            if (mBluetoothLeScanner != null && previousScanCallback != null) {
                mBluetoothLeScanner?.stopScan(previousScanCallback)
            }

            if (!btAdapter.isEnabled) {
                Log.d(TAG, "Bluetooth is disabled")
                return
            }

            mBluetoothLeScanner = btAdapter.bluetoothLeScanner

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(500L)
                .build()

            val manufacturerData = ByteArray(27)
            val manufacturerDataMask = ByteArray(27)

            manufacturerData[0] = 7
            manufacturerData[1] = 25

            manufacturerDataMask[0] = -1
            manufacturerDataMask[1] = -1

            val scanFilter = ScanFilter.Builder()
                .setManufacturerData(76, manufacturerData, manufacturerDataMask)
                .build()

            mScanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    markScanSuccessful()
                    processedAddresses.clear()
                    publishNewestLidState(result, processScanResult(result))
                }

                override fun onBatchScanResults(results: List<ScanResult>) {
                    markScanSuccessful()
                    processedAddresses.clear()
                    var newest: ScanResult? = null
                    var newestStatus: AirPodsStatus? = null
                    for (result in results) {
                        val status = processScanResult(result) ?: continue
                        if (newest == null || result.timestampNanos > newest.timestampNanos) {
                            newest = result
                            newestStatus = status
                        }
                    }
                    publishNewestLidState(newest, newestStatus)
                }

                override fun onScanFailed(errorCode: Int) {
                    if (mScanCallback !== this) return
                    Log.e(TAG, "BLE scan failed with error code: $errorCode")
                    scheduleScanRetry(errorCode)
                }
            }

            mBluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, mScanCallback)
            Log.d(TAG, "BLE scanner started successfully")

            cleanupHandler.removeCallbacks(cleanupRunnable)
            cleanupHandler.postDelayed(cleanupRunnable, CLEANUP_INTERVAL_MS)
        } catch (t: Throwable) {
            Log.e(TAG, "Error starting BLE scanner", t)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            cancelPendingScanRetry()
            scanRetryAttempts.set(0)
            val scanCallback = mScanCallback
            mScanCallback = null
            if (mBluetoothLeScanner != null && scanCallback != null) {
                Log.d(TAG, "Stopping BLE scanner")
                mBluetoothLeScanner?.stopScan(scanCallback)
            }

            cleanupHandler.removeCallbacks(cleanupRunnable)
        } catch (t: Throwable) {
            Log.e(TAG, "Error stopping BLE scanner", t)
        }
    }

    private fun markScanSuccessful() {
        cancelPendingScanRetry()
        scanRetryAttempts.set(0)
    }

    /**
     * Giving up left the app with no scanner at all until Bluetooth was toggled or
     * the service restarted, which costs the popup, the battery readings and ear
     * detection for the rest of the session. Keep trying at a widening interval,
     * spaced so the retries cannot themselves trip the platform's limit on how
     * often an app may start a scan.
     */
    private fun scheduleScanRetry(errorCode: Int) {
        if (errorCode == ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED) {
            Log.e(TAG, "BLE scan settings unsupported, not retrying")
            return
        }

        val attempt = scanRetryAttempts.incrementAndGet()
        val retryDelay =
            (SCAN_RETRY_BACKOFF_MS * attempt).coerceAtMost(MAX_SCAN_RETRY_BACKOFF_MS)
        Log.d(TAG, "Retrying BLE scan in ${retryDelay}ms (attempt $attempt)")
        cleanupHandler.removeCallbacks(scanRetryRunnable)
        cleanupHandler.postDelayed(scanRetryRunnable, retryDelay)
    }

    private fun cancelPendingScanRetry() {
        cleanupHandler.removeCallbacks(scanRetryRunnable)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getEncryptionKeyFromPreferences(): ByteArray? {
        val keyBase64 = sharedPreferences.getString(AACPManager.Companion.ProximityKeyType.ENC_KEY.name, null)
        return if (keyBase64 != null) {
            try {
                Base64.decode(keyBase64)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode encryption key", e)
                null
            }
        } else {
            null
        }
    }

    @SuppressLint("GetInstance")
    private fun decryptLastBytes(data: ByteArray, key: ByteArray): ByteArray? {
        return try {
            if (data.size < 16) {
                return null
            }

            val block = data.copyOfRange(data.size - 16, data.size)
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            val secretKey = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            cipher.doFinal(block)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting data", e)
            null
        }
    }

    private fun formatBattery(byteVal: Int): Pair<Boolean, Int> {
        val charging = (byteVal and 0x80) != 0
        val level = byteVal and 0x7F
        return Pair(charging, level)
    }

    private fun processScanResult(result: ScanResult): AirPodsStatus? {
        try {
            val scanRecord = result.scanRecord ?: return null
            val address = result.device.address

            if (processedAddresses.contains(address)) {
                return null
            }

            val manufacturerData = scanRecord.getManufacturerSpecificData(76) ?: return null
            if (manufacturerData.size <= 20) return null

            if (!verifiedAddresses.contains(address)) {
                val irk = getIrkFromPreferences()
                if (irk == null) {
                    if (missingIrkLoggedForScan.compareAndSet(false, true)) {
                        Log.d(TAG, "Dropping BLE advertisements because no IRK is available")
                    }
                    return null
                }
                if (!BluetoothCryptography.verifyRPA(address, irk)) {
                    return null
                }
                verifiedAddresses.add(address)
                Log.d(TAG, "RPA verified and added to trusted list: $address")
            }

            processedAddresses.add(address)
            lastBroadcastTime = System.currentTimeMillis()

            val encryptionKey = getEncryptionKeyFromPreferences()
            val decryptedData = if (encryptionKey != null) decryptLastBytes(manufacturerData, encryptionKey) else null
            val parsedStatus = if (decryptedData != null && decryptedData.size == 16) {
                parseProximityMessageWithDecryption(address, manufacturerData, decryptedData)
            } else {
                parseProximityMessage(address, manufacturerData)
            }

            val previousStatus = deviceStatusMap[address]
            deviceStatusMap[address] = parsedStatus

            airPodsStatusListener?.let { listener ->
                if (previousStatus == null) {
                    listener.onBroadcastFromNewAddress(parsedStatus)
                    // A first advertisement carries readings as much as any other
                    // does. The addresses are resolvable private ones, dropped again
                    // after fifteen seconds of quiet, while the broadcast comes in
                    // bursts further apart than that - so most of what arrives is a
                    // first advertisement from an address last seen minutes ago, and
                    // none of it counted as battery news.
                    listener.onBatteryChanged(parsedStatus)
                    Log.d(TAG, "New AirPods device detected: $address")
                } else {
                    // lastSeen moves with every advertisement, so comparing the whole
                    // status made "changed" mean "received" - and the listener writes
                    // a battery snapshot to disk each time it is called.
                    if (parsedStatus.copy(lastSeen = previousStatus.lastSeen) != previousStatus) {
                        listener.onDeviceStatusChanged(parsedStatus, previousStatus)
                    }

                    if (parsedStatus.isLeftInEar != previousStatus.isLeftInEar ||
                        parsedStatus.isRightInEar != previousStatus.isRightInEar) {
                        listener.onEarStateChanged(
                            parsedStatus,
                            parsedStatus.isLeftInEar,
                            parsedStatus.isRightInEar
                        )
                        Log.d(TAG, "Ear state changed - Left: ${parsedStatus.isLeftInEar}, Right: ${parsedStatus.isRightInEar}")
                    }

                    // The charge states count too. A case with both buds out
                    // cannot measure itself and sends its level as unknown whether or
                    // not it is on a charger, so going on the levels alone meant
                    // seating a bud in a charging case was never news.
                    if (parsedStatus.leftBattery != previousStatus.leftBattery ||
                        parsedStatus.rightBattery != previousStatus.rightBattery ||
                        parsedStatus.caseBattery != previousStatus.caseBattery ||
                        parsedStatus.isLeftCharging != previousStatus.isLeftCharging ||
                        parsedStatus.isRightCharging != previousStatus.isRightCharging ||
                        parsedStatus.isCaseCharging != previousStatus.isCaseCharging) {
                        listener.onBatteryChanged(parsedStatus)
                        Log.d(TAG, "Battery changed - Left: ${parsedStatus.leftBattery}, Right: ${parsedStatus.rightBattery}, Case: ${parsedStatus.caseBattery}")
                    }
                }
            }
            return parsedStatus
        } catch (t: Throwable) {
            Log.e(TAG, "Error processing scan result", t)
            return null
        }
    }

    /**
     * A batch holds one advertisement per address, captured at different moments,
     * and the case's two rotating addresses regularly disagree about the lid: the
     * stale one still reads open after the fresh one has reported the close. Only
     * the newest advertisement may move the lid state, or that disagreement shows
     * up as a close immediately followed by an opening, and the popup fires while
     * the user is putting the AirPods away.
     */
    private fun publishNewestLidState(newest: ScanResult?, status: AirPodsStatus?) {
        if (newest == null || status == null) return
        if (newest.timestampNanos < lastLidAdvertNanos) return
        lastLidAdvertNanos = newest.timestampNanos
        airPodsStatusListener?.let { publishLidState(status, it) }
    }

    private fun parseProximityMessageWithDecryption(address: String, data: ByteArray, decrypted: ByteArray): AirPodsStatus {
        val paired = data[2].toInt() == 1
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = modelNames[modelId] ?: "Unknown ($modelId)"

        val status = data[5].toInt() and 0xFF
        val flags = (data[7].toInt() shr 4) and 0x0F
        val lid = data[8].toInt() and 0xFF
        val color = colorNames[data[9].toInt()] ?: "Unknown"
        val conn = connStates[data[10].toInt()] ?: "Unknown (${data[10].toInt()})"

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftByteIndex = if (isFlipped) 2 else 1
        val rightByteIndex = if (isFlipped) 1 else 2

        val (isLeftCharging, leftBattery) = formatBattery(decrypted[leftByteIndex].toInt() and 0xFF)
        val (isRightCharging, rightBattery) = formatBattery(decrypted[rightByteIndex].toInt() and 0xFF)

        // The case's level and its charger sit in different bytes, and only the
        // level is in the encrypted one. A case with no bud seated cannot measure
        // itself and sends 0xFF there, which the level already reads as "unknown" -
        // but the top bit of that byte is the charging bit, so read the same way the
        // sentinel also says "on a charger", and the sheet showed a case charging
        // whenever it could not say how full it was. The unencrypted flags nibble
        // carries the charger on its own: across every advertisement taken with a
        // case going on and off one, its bit 2 tracked the charger exactly while the
        // top bit of the encrypted byte did not.
        val rawCaseBatteryByte = decrypted[3].toInt() and 0xFF
        val rawCaseBattery = rawCaseBatteryByte and 0x7F
        val isCaseCharging = (flags and 0x04) != 0

        val caseBattery = if (rawCaseBatteryByte == 0xFF || rawCaseBattery == 127) {
            lastValidCaseBattery
        } else {
            lastValidCaseBattery = rawCaseBattery
            rawCaseBattery
        }

        val lidOpen = ((lid shr 3) and 0x01) == 0

        return AirPodsStatus(
            address = address,
            lastSeen = System.currentTimeMillis(),
            paired = paired,
            model = model,
            leftBattery = leftBattery,
            rightBattery = rightBattery,
            caseBattery = caseBattery,
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            lidOpen = lidOpen,
            color = color,
            connectionState = conn
        )
    }

    private fun cleanupStaleDevices() {
        val now = System.currentTimeMillis()
        val staleCutoff = now - STALE_DEVICE_TIMEOUT_MS
        val hadDevices = deviceStatusMap.isNotEmpty()

        val staleDevices = deviceStatusMap.filter { it.value.lastSeen < staleCutoff }

        for (device in staleDevices) {
            deviceStatusMap.remove(device.key)
            // Resolvable addresses rotate every few minutes and never come back, so
            // what was learned about one has to go when the address does. Left alone
            // these two grew for as long as the service ran.
            verifiedAddresses.remove(device.key)
            Log.d(TAG, "Removed stale device from tracking: ${device.key}")
        }

        if (hadDevices && deviceStatusMap.isEmpty()) {
            airPodsStatusListener?.onDeviceDisappeared()
        }
    }

    private fun publishLidState(status: AirPodsStatus, listener: AirPodsStatusListener) {
        val previousState = currentGlobalLidState
        if (previousState == status.lidOpen) return
        currentGlobalLidState = status.lidOpen
        // Reading the state for the first time is not the same as watching it
        // change. The sheet answers the case being opened, and a service that has
        // only just started - or that has been connected, with the broadcast
        // suppressed, and is hearing one again - has no such moment to announce. It
        // was putting the sheet up on the strength of the first packet it parsed,
        // which arrived seconds after the case had been closed.
        if (previousState == null && status.lidOpen) {
            Log.d(TAG, "Lid read as open on the first broadcast; not announcing it")
            return
        }
        listener.onLidStateChanged(status.lidOpen)
        Log.d(TAG, "Lid state ${if (status.lidOpen) "opened" else "closed"} (was $previousState)")
    }

    private fun checkLidStateTimeout() {
        // Silence on the proximity broadcast means the AirPods have gone away, and
        // a closed lid is the usual reason - but it is not the only one. They also
        // stop advertising once they are connected, so a link that comes up while
        // the case is open dries the broadcast out and this would decide, fifteen
        // seconds later, that the lid had shut. It had not, and the connect sheet
        // was being retracted on the strength of it. While the link is up the lid
        // is not this timer's to guess at: closing it drops the link, and that is
        // heard as a disconnection instead.
        if (BluetoothConnectionManager.aacpSocket?.isConnected == true) {
            // And while it is up the lid is not ours to remember either. The AirPods
            // stop advertising once they are connected, so whatever the lid does in
            // the meantime happens out of sight; holding the last reading through
            // that leaves the first advertisement after the link drops to be
            // compared against a state from before it came up. A case opened while
            // connected - which is how the link came up at all - then reads as a lid
            // opening now, seconds after the AirPods have gone, and the sheet comes
            // up for it. Forget it instead, and let that advertisement be a reading.
            currentGlobalLidState = null
            return
        }
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBroadcastTime > LID_CLOSE_TIMEOUT_MS && currentGlobalLidState == true) {
            Log.d(TAG, "No broadcasts for ${LID_CLOSE_TIMEOUT_MS}ms, treating the lid state as stale")
            // Silence is a guess, not a reading, and it must not be filed as one.
            // Announce the close so anything watching the lid gets its retraction,
            // but leave the state unknown: the AirPods coming back into range look
            // exactly like this - a stretch of nothing, then advertisements again -
            // and an open lid in that first packet is the state they were already
            // in, not a lid being opened. Recorded as fact, the guess turned that
            // reading into a change and put the sheet up on its own.
            currentGlobalLidState = null
            airPodsStatusListener?.onLidStateChanged(false)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getIrkFromPreferences(): ByteArray? {
        val irkBase64 = sharedPreferences.getString(AACPManager.Companion.ProximityKeyType.IRK.name, null)
        return if (irkBase64 != null) {
            try {
                Base64.decode(irkBase64)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode IRK", e)
                null
            }
        } else {
            null
        }
    }

    private fun parseProximityMessage(address: String, data: ByteArray): AirPodsStatus {
        val paired = data[2].toInt() == 1
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = modelNames[modelId] ?: "Unknown ($modelId)"

        val status = data[5].toInt() and 0xFF
        val podsBattery = data[6].toInt() and 0xFF
        val flagsCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF
        val color = colorNames[data[9].toInt()] ?: "Unknown"
        val conn = connStates[data[10].toInt()] ?: "Unknown (${data[10].toInt()})"

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftBatteryNibble = if (isFlipped) (podsBattery shr 4) and 0x0F else podsBattery and 0x0F
        val rightBatteryNibble = if (isFlipped) podsBattery and 0x0F else (podsBattery shr 4) and 0x0F

        val caseBattery = flagsCase and 0x0F
        val flags = (flagsCase shr 4) and 0x0F

        val isLeftCharging = if (isFlipped) (flags and 0x02) != 0 else (flags and 0x01) != 0
        val isRightCharging = if (isFlipped) (flags and 0x01) != 0 else (flags and 0x02) != 0
        val isCaseCharging = (flags and 0x04) != 0

        val lidOpen = ((lid shr 3) and 0x01) == 0

        fun decodeBattery(n: Int): Int? = when (n) {
            in 0x0..0x9 -> n * 10
            in 0xA..0xE -> 100
            0xF -> null
            else -> null
        }

        return AirPodsStatus(
            address = address,
            lastSeen = System.currentTimeMillis(),
            paired = paired,
            model = model,
            leftBattery = decodeBattery(leftBatteryNibble),
            rightBattery = decodeBattery(rightBatteryNibble),
            caseBattery = decodeBattery(caseBattery),
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            lidOpen = lidOpen,
            color = color,
            connectionState = conn
        )
    }

    companion object {
        private const val TAG = "AirPodsBLE"
        private const val CLEANUP_INTERVAL_MS = 10000L
        private const val STALE_DEVICE_TIMEOUT_MS = 15000L
        private const val LID_CLOSE_TIMEOUT_MS = 15000L
        private const val SCAN_RETRY_BACKOFF_MS = 5000L
        private const val MAX_SCAN_RETRY_BACKOFF_MS = 30000L
    }
}
