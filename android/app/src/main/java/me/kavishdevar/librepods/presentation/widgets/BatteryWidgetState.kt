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

package me.kavishdevar.librepods.presentation.widgets

import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import me.kavishdevar.librepods.data.unifiedBudBattery

internal enum class BatteryWidgetDevice {
    PHONE,
    BUDS,
    LEFT_BUD,
    RIGHT_BUD,
    CASE,
    EMPTY
}

internal data class BatteryWidgetSlot(
    val device: BatteryWidgetDevice,
    val level: Int? = null,
    val status: Int = BatteryStatus.DISCONNECTED
) {
    val hasValue: Boolean
        get() = device != BatteryWidgetDevice.EMPTY && level != null

    val isCharging: Boolean
        get() = status == BatteryStatus.CHARGING || status == BatteryStatus.OPTIMIZED_CHARGING
}

internal fun batteryWidgetSlots(
    batteries: List<Battery>,
    phoneBattery: BatteryWidgetSlot?
): List<BatteryWidgetSlot> {
    val batteriesByComponent = batteries.associateBy { it.component }
    val slots = mutableListOf<BatteryWidgetSlot>()

    phoneBattery?.let(slots::add)

    val combinedBuds = unifiedBudBattery(batteries)
    if (combinedBuds != null) {
        slots += combinedBuds.toWidgetSlot(BatteryWidgetDevice.BUDS)
    } else {
        batteriesByComponent[BatteryComponent.LEFT]
            ?.takeUnless { it.status == BatteryStatus.DISCONNECTED }
            ?.let { slots += it.toWidgetSlot(BatteryWidgetDevice.LEFT_BUD) }
        batteriesByComponent[BatteryComponent.RIGHT]
            ?.takeUnless { it.status == BatteryStatus.DISCONNECTED }
            ?.let { slots += it.toWidgetSlot(BatteryWidgetDevice.RIGHT_BUD) }
    }

    batteriesByComponent[BatteryComponent.CASE]
        ?.takeUnless { it.status == BatteryStatus.DISCONNECTED }
        ?.let { slots += it.toWidgetSlot(BatteryWidgetDevice.CASE) }

    return buildList {
        addAll(slots.take(BATTERY_WIDGET_SLOT_COUNT))
        repeat(BATTERY_WIDGET_SLOT_COUNT - size) {
            add(BatteryWidgetSlot(BatteryWidgetDevice.EMPTY))
        }
    }
}

private fun Battery.toWidgetSlot(device: BatteryWidgetDevice) = BatteryWidgetSlot(
    device = device,
    level = level,
    status = status
)

private const val BATTERY_WIDGET_SLOT_COUNT = 4
