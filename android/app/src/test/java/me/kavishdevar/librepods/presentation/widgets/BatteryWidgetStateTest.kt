package me.kavishdevar.librepods.presentation.widgets

import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryWidgetStateTest {
    @Test
    fun `matching buds share one slot`() {
        val slots = batteryWidgetSlots(
            batteries = listOf(
                battery(BatteryComponent.LEFT, 81),
                battery(BatteryComponent.RIGHT, 79),
                battery(BatteryComponent.CASE, 64)
            ),
            phoneBattery = phone(92)
        )

        assertEquals(
            listOf(
                BatteryWidgetDevice.PHONE,
                BatteryWidgetDevice.BUDS,
                BatteryWidgetDevice.CASE,
                BatteryWidgetDevice.EMPTY
            ),
            slots.map { it.device }
        )
        assertEquals(79, slots[1].level)
    }

    @Test
    fun `one charging bud keeps the bud slots separate`() {
        val slots = batteryWidgetSlots(
            batteries = listOf(
                battery(BatteryComponent.LEFT, 80, BatteryStatus.CHARGING),
                battery(BatteryComponent.RIGHT, 80),
                battery(BatteryComponent.CASE, 64)
            ),
            phoneBattery = phone(92)
        )

        assertEquals(
            listOf(
                BatteryWidgetDevice.PHONE,
                BatteryWidgetDevice.LEFT_BUD,
                BatteryWidgetDevice.RIGHT_BUD,
                BatteryWidgetDevice.CASE
            ),
            slots.map { it.device }
        )
        assertTrue(slots[1].isCharging)
        assertFalse(slots[2].isCharging)
    }

    @Test
    fun `both charging buds can share one slot`() {
        val slots = batteryWidgetSlots(
            batteries = listOf(
                battery(BatteryComponent.LEFT, 80, BatteryStatus.CHARGING),
                battery(BatteryComponent.RIGHT, 82, BatteryStatus.CHARGING)
            ),
            phoneBattery = null
        )

        assertEquals(BatteryWidgetDevice.BUDS, slots.first().device)
        assertTrue(slots.first().isCharging)
    }

    @Test
    fun `active batteries move ahead of disconnected components`() {
        val slots = batteryWidgetSlots(
            batteries = listOf(
                battery(BatteryComponent.LEFT, 0, BatteryStatus.DISCONNECTED),
                battery(BatteryComponent.RIGHT, 73),
                battery(BatteryComponent.CASE, 61)
            ),
            phoneBattery = phone(92)
        )

        assertEquals(
            listOf(
                BatteryWidgetDevice.PHONE,
                BatteryWidgetDevice.RIGHT_BUD,
                BatteryWidgetDevice.CASE,
                BatteryWidgetDevice.EMPTY
            ),
            slots.map { it.device }
        )
    }

    @Test
    fun `disabled phone battery leaves the last slot empty`() {
        val slots = batteryWidgetSlots(
            batteries = listOf(
                battery(BatteryComponent.LEFT, 80),
                battery(BatteryComponent.RIGHT, 72),
                battery(BatteryComponent.CASE, 64)
            ),
            phoneBattery = null
        )

        assertEquals(
            listOf(
                BatteryWidgetDevice.LEFT_BUD,
                BatteryWidgetDevice.RIGHT_BUD,
                BatteryWidgetDevice.CASE,
                BatteryWidgetDevice.EMPTY
            ),
            slots.map { it.device }
        )
    }

    private fun battery(
        component: Int,
        level: Int,
        status: Int = BatteryStatus.NOT_CHARGING
    ) = Battery(component, level, status)

    private fun phone(level: Int) = BatteryWidgetSlot(
        BatteryWidgetDevice.PHONE,
        level,
        BatteryStatus.NOT_CHARGING
    )
}
