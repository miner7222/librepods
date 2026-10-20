package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private fun batteryPacket(vararg components: Battery): ByteArray {
        return ByteArray(7 + 5 * components.size).apply {
            byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x04, 0x00).copyInto(this)
            this[6] = components.size.toByte()
            components.forEachIndexed { index, battery ->
                val offset = 7 + 5 * index
                this[offset] = battery.component.toByte()
                this[offset + 1] = 1
                this[offset + 2] = battery.level.toByte()
                this[offset + 3] = battery.status.toByte()
                this[offset + 4] = 1
            }
        }
    }

    @Test
    fun aSingleBudPacketUpdatesOnlyThatBud() {
        for (component in listOf(BatteryComponent.LEFT, BatteryComponent.RIGHT)) {
            val notification = AirPodsNotifications.BatteryNotification()
            val reading = Battery(component, 73, BatteryStatus.NOT_CHARGING)
            val packet = batteryPacket(reading)
            assertTrue(notification.isBatteryData(packet))
            assertTrue(notification.setBattery(packet))
            assertEquals(reading, notification.getBattery().first { it.component == component })
            assertTrue(notification.getBattery().filter { it.component != component }
                .all { it.status == BatteryStatus.DISCONNECTED })
        }
    }

    @Test
    fun bothBudsWithoutACaseAreAcceptedInEitherOrder() {
        val left = Battery(BatteryComponent.LEFT, 71, BatteryStatus.OPTIMIZED_CHARGING)
        val right = Battery(BatteryComponent.RIGHT, 64, BatteryStatus.NOT_CHARGING)
        for (packet in listOf(batteryPacket(left, right), batteryPacket(right, left))) {
            val notification = AirPodsNotifications.BatteryNotification()
            assertTrue(notification.setBattery(packet))
            assertEquals(listOf(left, right, Battery(BatteryComponent.CASE, 0, BatteryStatus.DISCONNECTED)),
                notification.getBattery())
        }
    }

    @Test
    fun caseAndOneBudNeedNotOccupyFixedRecordPositions() {
        val notification = AirPodsNotifications.BatteryNotification()
        val case = Battery(BatteryComponent.CASE, 80, BatteryStatus.NOT_CHARGING)
        val right = Battery(BatteryComponent.RIGHT, 25, BatteryStatus.CHARGING)
        assertTrue(notification.setBattery(batteryPacket(case, right)))
        assertEquals(listOf(Battery(BatteryComponent.LEFT, 0, BatteryStatus.DISCONNECTED), right, case),
            notification.getBattery())
    }

    @Test
    fun omittedComponentsRetainTheirPreviousReadings() {
        val notification = AirPodsNotifications.BatteryNotification()
        val left = Battery(BatteryComponent.LEFT, 70, BatteryStatus.NOT_CHARGING)
        val right = Battery(BatteryComponent.RIGHT, 75, BatteryStatus.NOT_CHARGING)
        val case = Battery(BatteryComponent.CASE, 80, BatteryStatus.NOT_CHARGING)
        notification.setBattery(batteryPacket(left, right, case))
        val updatedRight = right.copy(level = 65)
        notification.setBattery(batteryPacket(updatedRight))
        assertEquals(listOf(left, updatedRight, case), notification.getBattery())
    }

    @Test
    fun extraUnknownComponentsAreSkippedWithoutLosingKnownRecords() {
        val notification = AirPodsNotifications.BatteryNotification()
        val left = Battery(BatteryComponent.LEFT, 70, BatteryStatus.NOT_CHARGING)
        val right = Battery(BatteryComponent.RIGHT, 75, BatteryStatus.CHARGING)
        val case = Battery(BatteryComponent.CASE, 80, BatteryStatus.NOT_CHARGING)
        assertTrue(notification.setBattery(batteryPacket(
            Battery(0xFF, 55, BatteryStatus.CHARGING), case, right, left
        )))
        assertEquals(listOf(left, right, case), notification.getBattery())
    }

    @Test
    fun componentCountsAreUnsigned() {
        val notification = AirPodsNotifications.BatteryNotification()
        val components = Array(128) { Battery(0xFF, 50, BatteryStatus.NOT_CHARGING) }
        components[127] = Battery(BatteryComponent.LEFT, 60, BatteryStatus.NOT_CHARGING)
        assertTrue(notification.setBattery(batteryPacket(*components)))
        assertEquals(60, notification.getBattery().first { it.component == BatteryComponent.LEFT }.level)
    }

    @Test
    fun unknownOnlyAndEmptyPacketsDoNotRefreshKnownReadings() {
        val notification = AirPodsNotifications.BatteryNotification()
        val packet = batteryPacket(Battery(BatteryComponent.LEFT, 40, BatteryStatus.NOT_CHARGING))
        notification.setBattery(packet)
        val previous = notification.getBattery()
        assertFalse(notification.setBattery(batteryPacket(Battery(99, 50, BatteryStatus.CHARGING))))
        assertFalse(notification.setBattery(batteryPacket()))
        assertEquals(previous, notification.getBattery())
    }

    @Test
    fun everyTruncatedPacketIsRejectedBeforeAnyRecordIsApplied() {
        val notification = AirPodsNotifications.BatteryNotification()
        notification.setBattery(batteryPacket(Battery(BatteryComponent.LEFT, 40, BatteryStatus.NOT_CHARGING)))
        val previous = notification.getBattery()
        val packet = batteryPacket(
            Battery(BatteryComponent.LEFT, 90, BatteryStatus.CHARGING),
            Battery(BatteryComponent.RIGHT, 95, BatteryStatus.CHARGING),
            Battery(BatteryComponent.CASE, 70, BatteryStatus.NOT_CHARGING)
        )
        for (length in packet.indices) {
            val truncated = packet.copyOf(length)
            assertFalse(notification.isBatteryData(truncated))
            assertFalse(notification.setBattery(truncated))
            assertEquals(previous, notification.getBattery())
        }
    }

    @Test
    fun invalidHeadersCountsAndTrailingBytesAreRejected() {
        val notification = AirPodsNotifications.BatteryNotification()
        val packet = batteryPacket(Battery(BatteryComponent.LEFT, 70, BatteryStatus.NOT_CHARGING))
        for (index in 0 until 6) {
            val invalid = packet.copyOf()
            invalid[index] = (invalid[index].toInt() xor 0xFF).toByte()
            assertFalse(notification.setBattery(invalid))
        }
        assertFalse(notification.setBattery(packet.copyOf().apply { this[6] = 2 }))
        assertFalse(notification.setBattery(packet + byteArrayOf(0)))
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

    @Test
    fun `a bud charging in its case counts as charging`() {
        assertTrue(BatteryStatus.isCharging(BatteryStatus.CHARGING))
        assertTrue(BatteryStatus.isCharging(BatteryStatus.OPTIMIZED_CHARGING))
        assertFalse(BatteryStatus.isCharging(BatteryStatus.NOT_CHARGING))
        assertFalse(BatteryStatus.isCharging(BatteryStatus.DISCONNECTED))
        assertFalse(BatteryStatus.isCharging(null))
    }
}
