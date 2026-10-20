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

package me.kavishdevar.librepods.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** Internal unknown-price state; the UI resolves it in the current locale. */
const val PRICE_UNKNOWN = "__price_unknown__"

interface BillingProvider {
    val isPremium: StateFlow<Boolean>
    val price: StateFlow<String>
    fun purchase(activity: Activity)
    fun queryPurchases()
    fun restorePurchases()
}
