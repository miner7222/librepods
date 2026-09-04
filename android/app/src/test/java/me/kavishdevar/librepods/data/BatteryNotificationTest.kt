package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryNotificationTest {
    @Test
    fun disconnectedRestoreMapsChargingStatusesToNotCharging() {
        assertEquals(
            BatteryStatus.NOT_CHARGING,
            batteryStatusForDisconnectedRestore(BatteryStatus.CHARGING)
        )
        assertEquals(
            BatteryStatus.NOT_CHARGING,
            batteryStatusForDisconnectedRestore(BatteryStatus.OPTIMIZED_CHARGING)
        )
    }

    @Test
    fun disconnectedRestorePreservesNonChargingStatuses() {
        assertEquals(
            BatteryStatus.NOT_CHARGING,
            batteryStatusForDisconnectedRestore(BatteryStatus.NOT_CHARGING)
        )
        assertEquals(
            BatteryStatus.DISCONNECTED,
            batteryStatusForDisconnectedRestore(BatteryStatus.DISCONNECTED)
        )
    }

    @Test
    fun restoredSnapshotUsesTheSameRangeValidation() {
        val notification = AirPodsNotifications.BatteryNotification()

        notification.restoreBatterySnapshot(
            listOf(
                Battery(BatteryComponent.LEFT, 127, BatteryStatus.CHARGING),
                Battery(BatteryComponent.RIGHT, 55, BatteryStatus.OPTIMIZED_CHARGING),
                Battery(BatteryComponent.CASE, 80, BatteryStatus.DISCONNECTED)
            )
        )

        assertEquals(
            listOf(
                Battery(BatteryComponent.LEFT, 0, BatteryStatus.DISCONNECTED),
                Battery(BatteryComponent.RIGHT, 55, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 80, BatteryStatus.DISCONNECTED)
            ),
            notification.getBattery()
        )
    }

    @Test
    fun packetPreservesInRangeLevelsComponentsAndStatuses() {
        val notification = AirPodsNotifications.BatteryNotification()

        notification.setBattery(
            batteryPacket(
                Battery(BatteryComponent.RIGHT, 65, BatteryStatus.CHARGING),
                Battery(BatteryComponent.LEFT, 42, BatteryStatus.DISCONNECTED),
                Battery(BatteryComponent.CASE, 100, BatteryStatus.OPTIMIZED_CHARGING)
            )
        )

        assertEquals(
            listOf(
                Battery(BatteryComponent.LEFT, 42, BatteryStatus.DISCONNECTED),
                Battery(BatteryComponent.RIGHT, 65, BatteryStatus.CHARGING),
                Battery(BatteryComponent.CASE, 100, BatteryStatus.OPTIMIZED_CHARGING)
            ),
            notification.getBattery()
        )
    }

    @Test
    fun packetMarksOutOfRangeLevelsUnavailable() {
        val notification = AirPodsNotifications.BatteryNotification()

        notification.setBattery(
            batteryPacket(
                Battery(BatteryComponent.LEFT, 127, BatteryStatus.CHARGING),
                Battery(BatteryComponent.RIGHT, 101, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 42, BatteryStatus.NOT_CHARGING)
            )
        )

        assertEquals(
            listOf(
                Battery(BatteryComponent.LEFT, 0, BatteryStatus.DISCONNECTED),
                Battery(BatteryComponent.RIGHT, 0, BatteryStatus.DISCONNECTED),
                Battery(BatteryComponent.CASE, 42, BatteryStatus.NOT_CHARGING)
            ),
            notification.getBattery()
        )
    }

    @Test
    fun packetMarksSignExtendedLevelsUnavailable() {
        val notification = AirPodsNotifications.BatteryNotification()

        val packet = batteryPacket(
            Battery(BatteryComponent.LEFT, 0, BatteryStatus.NOT_CHARGING),
            Battery(BatteryComponent.RIGHT, 55, BatteryStatus.NOT_CHARGING),
            Battery(BatteryComponent.CASE, 80, BatteryStatus.NOT_CHARGING)
        )
        packet[9] = 0xFF.toByte()

        notification.setBattery(packet)

        assertEquals(
            listOf(
                Battery(BatteryComponent.LEFT, 0, BatteryStatus.DISCONNECTED),
                Battery(BatteryComponent.RIGHT, 55, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 80, BatteryStatus.NOT_CHARGING)
            ),
            notification.getBattery()
        )
    }

    @Test
    fun directLevelsUseTheSameRangeValidation() {
        val notification = AirPodsNotifications.BatteryNotification()

        notification.setBatteryDirect(
            leftLevel = 127,
            leftCharging = true,
            rightLevel = 101,
            rightCharging = false,
            caseLevel = 42,
            caseCharging = false
        )

        assertEquals(
            listOf(
                Battery(BatteryComponent.LEFT, 0, BatteryStatus.DISCONNECTED),
                Battery(BatteryComponent.RIGHT, 0, BatteryStatus.DISCONNECTED),
                Battery(BatteryComponent.CASE, 42, BatteryStatus.NOT_CHARGING)
            ),
            notification.getBattery()
        )
    }

    @Test
    fun disconnectedComponentFallsBackToTheRememberedLevel() {
        val notification = AirPodsNotifications.BatteryNotification()

        // Both buds out of the case: the buds still report, the case does not.
        notification.setBattery(
            batteryPacket(
                Battery(BatteryComponent.LEFT, 80, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 78, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 0, BatteryStatus.DISCONNECTED)
            )
        )
        notification.fillDisconnectedFrom(
            listOf(
                Battery(BatteryComponent.LEFT, 55, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 55, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 64, BatteryStatus.CHARGING)
            )
        )

        assertEquals(
            listOf(
                Battery(BatteryComponent.LEFT, 80, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 78, BatteryStatus.NOT_CHARGING),
                // remembered, and no longer claiming to be charging
                Battery(BatteryComponent.CASE, 64, BatteryStatus.NOT_CHARGING)
            ),
            notification.getBattery()
        )
    }

    @Test
    fun aComponentWithNothingRememberedStaysUnavailable() {
        val notification = AirPodsNotifications.BatteryNotification()

        notification.setBattery(
            batteryPacket(
                Battery(BatteryComponent.LEFT, 80, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 78, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 0, BatteryStatus.DISCONNECTED)
            )
        )
        notification.fillDisconnectedFrom(emptyList())

        assertEquals(
            Battery(BatteryComponent.CASE, 0, BatteryStatus.DISCONNECTED),
            notification.getBattery().first { it.component == BatteryComponent.CASE }
        )
    }

    @Test
    fun aDisconnectClearsTheChargingStateAndFallsBackToRememberedLevels() {
        val notification = AirPodsNotifications.BatteryNotification()

        // Buds charging in the case, then the lid closes and the link dies.
        notification.setBattery(
            batteryPacket(
                Battery(BatteryComponent.LEFT, 90, BatteryStatus.CHARGING),
                Battery(BatteryComponent.RIGHT, 90, BatteryStatus.CHARGING),
                Battery(BatteryComponent.CASE, 70, BatteryStatus.NOT_CHARGING)
            )
        )
        notification.markAllDisconnected()
        notification.fillDisconnectedFrom(
            listOf(
                Battery(BatteryComponent.LEFT, 90, BatteryStatus.CHARGING),
                Battery(BatteryComponent.RIGHT, 90, BatteryStatus.CHARGING),
                Battery(BatteryComponent.CASE, 70, BatteryStatus.NOT_CHARGING)
            )
        )

        assertEquals(
            listOf(
                Battery(BatteryComponent.LEFT, 90, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 90, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 70, BatteryStatus.NOT_CHARGING)
            ),
            notification.getBattery()
        )
    }

    private fun batteryPacket(first: Battery, second: Battery, case: Battery): ByteArray {
        return ByteArray(22).apply {
            this[7] = first.component.toByte()
            this[9] = first.level.toByte()
            this[10] = first.status.toByte()
            this[12] = second.component.toByte()
            this[14] = second.level.toByte()
            this[15] = second.status.toByte()
            this[17] = case.component.toByte()
            this[19] = case.level.toByte()
            this[20] = case.status.toByte()
        }
    }

    @Test
    fun caseWithoutAReadingIsMarkedUnavailable() {
        val notification = AirPodsNotifications.BatteryNotification()

        notification.setBattery(
            batteryPacket(
                Battery(BatteryComponent.LEFT, 100, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 100, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 0, BatteryStatus.CHARGING)
            )
        )

        assertEquals(
            listOf(
                Battery(BatteryComponent.LEFT, 100, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 100, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 0, BatteryStatus.DISCONNECTED)
            ),
            notification.getBattery()
        )
    }

    @Test
    fun budsAtZeroAreStillARealReading() {
        val notification = AirPodsNotifications.BatteryNotification()

        notification.setBattery(
            batteryPacket(
                Battery(BatteryComponent.LEFT, 0, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 0, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 50, BatteryStatus.NOT_CHARGING)
            )
        )

        assertEquals(
            listOf(
                Battery(BatteryComponent.LEFT, 0, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.RIGHT, 0, BatteryStatus.NOT_CHARGING),
                Battery(BatteryComponent.CASE, 50, BatteryStatus.NOT_CHARGING)
            ),
            notification.getBattery()
        )
    }
}
