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

package me.kavishdevar.librepods

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BluetoothConnectionManager
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.NoiseControlMode
import me.kavishdevar.librepods.services.AirPodsService
import me.kavishdevar.librepods.services.ServiceManager

/** Headless entry point for launchers and automation apps; intent extras cannot choose a mode. */
class ToggleListeningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val service = ServiceManager.getService()
            if (service == null || BluetoothConnectionManager.aacpSocket?.isConnected != true) {
                Toast.makeText(this, R.string.airpods_not_connected, Toast.LENGTH_SHORT).show()
                return
            }

            val modes = ListeningModeShortcut.resolveModes(
                getSharedPreferences("settings", MODE_PRIVATE)
                    .getString(ListeningModeShortcut.PREF_KEY, null),
                ListeningModeShortcut.availableModes(service)
            )
            val nextMode = ListeningModeShortcut.nextMode(service.getANC(), modes)
            if (nextMode == null) {
                Toast.makeText(this, R.string.listening_mode_shortcut_unavailable, Toast.LENGTH_SHORT).show()
                return
            }

            val sent = service.aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value,
                nextMode
            )
            val label = if (!sent) R.string.airpods_not_connected else when (nextMode) {
                NoiseControlMode.OFF.ordinal + 1 -> R.string.off
                NoiseControlMode.TRANSPARENCY.ordinal + 1 -> R.string.transparency
                NoiseControlMode.ADAPTIVE.ordinal + 1 -> R.string.adaptive
                else -> R.string.noise_cancellation
            }
            Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
        } finally {
            // Theme.NoDisplay requires finishing before onResume. Keep the caller's task when
            // launched directly. Launcher-supplied TASK_ON_HOME can still return to Home;
            // removing the task or launching Home ourselves cannot restore another app's task.
            finish()
        }
    }
}

internal object ListeningModeShortcut {
    const val PREF_KEY = "toggle_listening_modes"

    fun availableModes(service: AirPodsService?): List<NoiseControlMode> {
        if (service == null || BluetoothConnectionManager.aacpSocket?.isConnected != true ||
            service.listeningModeCapabilities()?.contains(Capability.LISTENING_MODE) != true
        ) return emptyList()
        return service.availableListeningModes()
    }

    /** Keep the stored order (least recently selected first), only if the whole pair is valid. */
    fun resolveModes(stored: String?, supported: List<NoiseControlMode>): List<Int> {
        val available = supported.map { it.ordinal + 1 }.distinct()
        if (available.size < 2) return emptyList()
        val modes = stored?.split(",")?.map { it.toIntOrNull() }
        if (modes?.size == 2 && modes[0] != modes[1] && modes.all { it in available }) {
            return modes.filterNotNull()
        }
        if (stored == null) {
            val defaults = listOf(
                NoiseControlMode.NOISE_CANCELLATION.ordinal + 1,
                NoiseControlMode.TRANSPARENCY.ordinal + 1
            )
            if (defaults.all { it in available }) return defaults
        }
        return available.take(2)
    }

    fun selectMode(stored: String?, supported: List<NoiseControlMode>, mode: Int): List<Int> {
        val modes = resolveModes(stored, supported)
        if (modes.size != 2 || mode in modes || supported.none { it.ordinal + 1 == mode }) {
            return modes
        }
        return listOf(modes.last(), mode)
    }

    fun nextMode(current: Int, modes: List<Int>): Int? =
        if (modes.size != 2 || modes[0] == modes[1]) null
        else if (current == modes[0]) modes[1] else modes[0]
}
