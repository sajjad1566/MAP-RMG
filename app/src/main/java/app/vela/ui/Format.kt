package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Distance-unit preference. Backed by Compose state so reads inside composables
 * (via [formatDistance]) recompose when it flips, and persisted so it sticks.
 * Defaults to imperial where roads are signed in miles (US, UK, Liberia, Myanmar), metric elsewhere.
 */
object Units {
    val imperial = mutableStateOf(false)

    fun init(context: Context) {
        // The DEVICE's locale, not the JVM default: AppLocale.wrap runs before this (in
        // attachBaseContext) and sets the JVM default to the in-app language, which for the
        // plain "English" choice has no country, so a US phone silently flipped to kilometers
        // the moment its owner touched the language picker (seen on the P4a, 2026-09-12).
        val country = android.content.res.Resources.getSystem().configuration.locales.get(0)?.country
            ?: Locale.getDefault().country
        val default = country in setOf("US", "GB", "LR", "MM")
        imperial.value = prefs(context).getBoolean(KEY, default)
    }

    fun set(context: Context, value: Boolean) {
        imperial.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_units", Context.MODE_PRIVATE)
    private const val KEY = "imperial"
}

fun formatDistance(meters: Double): String =
    if (Units.imperial.value) {
        val feet = meters * 3.28084
        // Google-style feet: 50 ft steps at/above 100 ft, 10 ft steps below (min 10, never "0 ft").
        if (feet < 1000) "${if (feet < 100) maxOf(10, (feet / 10).roundToInt() * 10) else (feet / 50).roundToInt() * 50} ft"
        else String.format(Locale.US, "%.1f mi", meters / 1609.344)
    } else {
        if (meters < 1000) "${meters.roundToInt()} m"
        else String.format(Locale.US, "%.1f km", meters / 1000.0)
    }

/** The unit a [formatDistanceSplit] number carries, so a caller can name it in its own language
 *  ("km" in English, "كم" in Arabic) instead of the [formatDistance] string's Latin unit. */
enum class DistanceUnit { METERS, KILOMETERS, FEET, MILES }

/**
 * The nav bar's distance split into the number alone and its unit: ("10.2", KILOMETERS).
 *
 * Two separate runs, because the bar lays the number and the unit out as its own Text nodes: in an
 * RTL layout a single "10.2 km · 11:13 PM" line was reordered by the bidi algorithm and the figures
 * ran together in Arabic. The rounding is copied from [formatDistance] step for step, so the bar and
 * every other surface never disagree by a meter. The digits are Western in every language, matching
 * the distances the rest of the app shows.
 */
fun formatDistanceSplit(meters: Double): Pair<String, DistanceUnit> =
    if (Units.imperial.value) {
        val feet = meters * 3.28084
        if (feet < 1000) {
            val n = if (feet < 100) maxOf(10, (feet / 10).roundToInt() * 10) else (feet / 50).roundToInt() * 50
            n.toString() to DistanceUnit.FEET
        } else String.format(Locale.US, "%.1f", meters / 1609.344) to DistanceUnit.MILES
    } else {
        if (meters < 1000) meters.roundToInt().toString() to DistanceUnit.METERS
        else String.format(Locale.US, "%.1f", meters / 1000.0) to DistanceUnit.KILOMETERS
    }

/** An area in the user's units: sq ft, acres, sq mi, or m², hectares, km². */
fun formatArea(m2: Double): String =
    if (Units.imperial.value) {
        val sqft = m2 * 10.7639
        when {
            sqft < 43_560 / 4 -> String.format(Locale.US, "%,d sq ft", sqft.roundToInt())
            m2 < 2_589_988 -> String.format(Locale.US, "%.1f acres", sqft / 43_560)
            else -> String.format(Locale.US, "%.1f sq mi", m2 / 2_589_988)
        }
    } else when {
        m2 < 10_000 -> String.format(Locale.US, "%,d m\u00B2", m2.roundToInt())
        m2 < 1_000_000 -> String.format(Locale.US, "%.1f ha", m2 / 10_000)
        else -> String.format(Locale.US, "%.1f km\u00B2", m2 / 1_000_000)
    }

fun formatDuration(seconds: Double): String {
    val totalMin = (seconds / 60.0).roundToInt()
    val h = totalMin / 60
    val m = totalMin % 60
    // English keeps its own short form ("6 h 16 min"). Every other language takes the platform's
    // units, so Japanese reads "6 時間 16 分" beside Google's transit times instead of "6 h 16 min"
    // (issue #674). Any failure falls back to the English form.
    val locale = AppLocale.effective()
    if (locale.language != "en") {
        runCatching {
            val f = android.icu.text.MeasureFormat.getInstance(locale, android.icu.text.MeasureFormat.FormatWidth.SHORT)
            val hours = android.icu.util.Measure(h, android.icu.util.MeasureUnit.HOUR)
            val mins = android.icu.util.Measure(maxOf(m, if (totalMin < 1) 1 else 0), android.icu.util.MeasureUnit.MINUTE)
            return when {
                totalMin < 1 -> "<" + f.formatMeasures(mins)
                h == 0 -> f.formatMeasures(mins)
                m == 0 -> f.formatMeasures(hours)
                else -> f.formatMeasures(hours, mins)
            }
        }
    }
    if (totalMin < 1) return "<1 min"
    if (totalMin < 60) return "$totalMin min"
    return if (m == 0) "$h h" else "$h h $m min"
}

/** Current speed as a (value, unit) pair for the speedometer, e.g. (65, "mph") —
 *  imperial vs metric follows the [Units] preference. */
fun formatSpeed(metersPerSecond: Float): Pair<Int, String> =
    if (Units.imperial.value) {
        (metersPerSecond * 2.236936).roundToInt() to "mph"
    } else {
        (metersPerSecond * 3.6).roundToInt() to "km/h"
    }

/** Posted speed limit (given in km/h from OSM via the obf engine or the overlay) as a (value, unit) pair in the display
 *  units. An mph-tagged US road round-trips exactly: 35 mph, which OSM stores as km/h, quantizes to
 *  56.0 km/h → ×0.621371 = 34.8 → rounds back to 35 mph (all 25–85 mph US limits round-trip). */
fun formatSpeedLimit(kmh: Double): Pair<Int, String> =
    if (Units.imperial.value) (kmh * 0.621371).roundToInt() to "mph"
    else kmh.roundToInt() to "km/h"

/** The device's 12/24-hour clock SETTING (Settings > System > Date & time), which is separate
 *  from the locale: a US-English phone set to 24-hour shows "19:42" everywhere else and used to
 *  show "7:42 PM" here (issue #357). Read at startup and on every resume; mirrored into :core's
 *  [app.vela.core.data.ClockFormat] for the transit boards. */
object Clock24 {
    val on = mutableStateOf(false)

    /** The app's own choice (Settings > Appearance > Clock, discussion #637): "system" follows
     *  the device setting, "12" and "24" override it for Vela alone. */
    val mode = mutableStateOf("system")

    fun refresh(context: Context) {
        mode.value = context.getSharedPreferences("vela_settings", Context.MODE_PRIVATE).getString("clock_mode", "system") ?: "system"
        val v = when (mode.value) {
            "24" -> true
            "12" -> false
            else -> android.text.format.DateFormat.is24HourFormat(context)
        }
        on.value = v
        app.vela.core.data.ClockFormat.use24h = v
    }

    fun set(context: Context, value: String) {
        context.getSharedPreferences("vela_settings", Context.MODE_PRIVATE).edit().putString("clock_mode", value).apply()
        refresh(context)
    }
}

/** Wall-clock arrival time for a trip [remainingSeconds] from now, e.g. "7:42 PM" or "19:42",
 *  following the device's 12/24-hour setting, the way Google shows ETA during navigation. */
fun formatArrivalClock(remainingSeconds: Double): String {
    val arrival = (DemoClock.now()?.toLocalTime() ?: java.time.LocalTime.now()).plusSeconds(remainingSeconds.toLong())
    val fmt = if (Clock24.on.value) java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        else java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
    return arrival.format(fmt)
}

/**
 * The nav bar's arrival clock split for its own layout: the time alone and whether it is before
 * noon, or a null marker when the phone asks for a 24-hour clock.
 *
 * The bar draws "11:13" and "ص" as two Text nodes (see [formatDistanceSplit] for why), so this
 * returns the pieces instead of a formatted string. A localized time format would have picked the
 * locale's own numbering system - Arabic-Indic digits beside the bar's Western distance - so the
 * time is formatted with Latin digits explicitly and the AM/PM word comes from the caller's
 * resources. [DemoClock] still wins, so a demo drive keeps its simulated clock.
 */
fun arrivalClockSplit(remainingSeconds: Double): Pair<String, Boolean?> {
    val arrival = (DemoClock.now()?.toLocalTime() ?: java.time.LocalTime.now()).plusSeconds(remainingSeconds.toLong())
    if (Clock24.on.value) {
        return arrival.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm", Locale.US)) to null
    }
    val hour12 = ((arrival.hour + 11) % 12) + 1
    return String.format(Locale.US, "%d:%02d", hour12, arrival.minute) to (arrival.hour < 12)
}

/** A wall-clock time [epochMs] in the phone's zone, "9:23 AM" or "09:23" by the 12/24-hour
 *  setting, like [formatArrivalClock]. */
fun formatClock(epochMs: Long): String {
    val t = java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    val fmt = if (Clock24.on.value) java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        else java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
    return t.format(fmt)
}

/** "Sep 7, 7:42 PM" / "7 Sep, 19:42": a saved trip's or log's timestamp in the device's own date
 *  and time formats. */
fun formatDateTime(context: Context, epochMs: Long): String {
    val d = java.util.Date(epochMs)
    return android.text.format.DateFormat.getMediumDateFormat(context).format(d) + ", " +
        android.text.format.DateFormat.getTimeFormat(context).format(d)
}
