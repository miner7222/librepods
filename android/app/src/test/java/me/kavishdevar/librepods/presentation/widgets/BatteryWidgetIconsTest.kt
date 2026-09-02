package me.kavishdevar.librepods.presentation.widgets

import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPods3
import me.kavishdevar.librepods.data.AirPods4ANC
import me.kavishdevar.librepods.data.AirPodsPro1
import me.kavishdevar.librepods.data.AirPodsPro2USBC
import me.kavishdevar.librepods.data.AirPodsPro3
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryWidgetIconsTest {
    @Test
    fun `standard generations select their matching bud shapes`() {
        assertEquals(
            R.drawable.sf_airpod_gen3_left,
            batteryWidgetIcons(AirPods3()).leftBud
        )
        assertEquals(
            R.drawable.sf_airpods_gen4_right,
            batteryWidgetIcons(AirPods4ANC()).rightBud
        )
    }

    @Test
    fun `pro generations do not share one generic icon`() {
        assertEquals(
            R.drawable.sf_airpods_pro_gen1,
            batteryWidgetIcons(AirPodsPro1()).buds
        )
        assertEquals(
            R.drawable.sf_airpods_pro,
            batteryWidgetIcons(AirPodsPro2USBC()).buds
        )
        assertEquals(
            R.drawable.sf_airpods_pro_gen3,
            batteryWidgetIcons(AirPodsPro3()).buds
        )
    }

    @Test
    fun `phone uses the extracted iPhone symbol`() {
        assertEquals(
            R.drawable.sf_iphone_gen3,
            batteryWidgetIcons(null).forDevice(BatteryWidgetDevice.PHONE)
        )
    }
}
