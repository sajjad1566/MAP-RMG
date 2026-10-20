package app.vela.ui.settings

import java.util.Locale

/** Navigation matches routes case-insensitively: the map and its settings need separate paths. */
internal const val MAP_ROUTE = "main/map"

/** HUB is the category list; each other destination is one Settings page. */
internal enum class SettingsSection {
    HUB, APPEARANCE, MAP, PLACES, NAVIGATION, VOICE, SEARCH, OFFLINE, SAVED_PLACES,
    // PRIVACY and ABOUT keep their screens and their routes (the privacy controls, the version
    // line and the in-app updater all still work), but the hub no longer lists them: the list
    // shows a Support card in their place, and Support carries the row that opens About.
    PRIVACY, PERFORMANCE, DIAGNOSTICS, ABOUT, SUPPORT;

    val route: String get() = "settings/${name.lowercase(Locale.ROOT)}"
}
