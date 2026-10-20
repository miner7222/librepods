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

package me.kavishdevar.librepods.utils

import android.app.LocaleConfig
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

/**
 * The app's own language, chosen in app settings independently of the phone's.
 *
 * The choices are the locales the build ships (the generated LocaleConfig, which
 * follows the res/values-* directories), so a language added there shows up here
 * without touching this file. The platform LocaleManager stores the choice and
 * recreates the activities; an empty list means "follow the system".
 */
object AppLanguage {
    /** Locales the app ships translations for, in the order the build lists them. */
    fun supported(context: Context): List<Locale> {
        val fromConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Null when the manifest declares no LocaleConfig.
            LocaleConfig(context).supportedLocales?.let { list -> List(list.size()) { list[it] } }
        } else {
            readGeneratedConfig(context)
        }
        return fromConfig.orEmpty()
    }

    /** LocaleConfig itself arrived with Android 14; read the file it is built from. */
    private fun readGeneratedConfig(context: Context): List<Locale> {
        val id = context.resources.getIdentifier(
            "_generated_res_locale_config", "xml", context.packageName
        )
        if (id == 0) return emptyList()
        val locales = mutableListOf<Locale>()
        try {
            context.resources.getXml(id).use { parser ->
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                        val tag = parser.getAttributeValue(
                            "http://schemas.android.com/apk/res/android", "name"
                        )
                        if (!tag.isNullOrEmpty()) locales += Locale.forLanguageTag(tag)
                    }
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return locales
    }

    /** The locale the user picked, or null while the app follows the system. */
    fun selected(context: Context): Locale? =
        context.getSystemService(LocaleManager::class.java).applicationLocales.takeUnless { it.isEmpty }?.get(0)

    /** Picks [locale], or follows the system language again when it is null. */
    fun select(context: Context, locale: Locale?) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            if (locale == null) LocaleList.getEmptyLocaleList() else LocaleList(locale)
    }

    /**
     * The shipped entry [selected] stands for. The system settings can store a
     * regional variant (ko-KR) of a language the app ships as plain ko, so fall
     * back from the exact tag to the same language.
     */
    fun match(supported: List<Locale>, selected: Locale?): Locale? {
        if (selected == null) return null
        return supported.firstOrNull { it.toLanguageTag() == selected.toLanguageTag() }
            ?: supported.firstOrNull { it.language == selected.language && it.country == selected.country }
            ?: supported.firstOrNull { it.language == selected.language }
    }

    /** The language's name written in that language. */
    fun displayName(locale: Locale): String =
        locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
}
