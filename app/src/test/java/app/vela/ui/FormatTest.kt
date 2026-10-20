package app.vela.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The nav bar's split figures (distance number + unit, arrival clock + day period). The bar builds
 * them itself so an RTL line can never reorder them, so these tests pin the two things that must
 * stay true: the number matches what [formatDistance] shows everywhere else, and the clock is Latin
 * digits with the day-period split out.
 */
class FormatTest {
    @After fun restoreDefaults() {
        Units.imperial.value = false
        Clock24.on.value = false
    }

    @Test fun `metric distance splits into the number the app already rounds`() {
        assertEquals("850" to DistanceUnit.METERS, formatDistanceSplit(850.0))
        assertEquals("1.0" to DistanceUnit.KILOMETERS, formatDistanceSplit(1000.0))
        assertEquals("10.2" to DistanceUnit.KILOMETERS, formatDistanceSplit(10_234.0))
    }

    @Test fun `imperial distance keeps the app's feet and mile steps`() {
        Units.imperial.value = true
        assertEquals("70" to DistanceUnit.FEET, formatDistanceSplit(20.0))
        assertEquals("350" to DistanceUnit.FEET, formatDistanceSplit(100.0))
        assertEquals("3.1" to DistanceUnit.MILES, formatDistanceSplit(5000.0))
    }

    @Test fun `split pieces read exactly like the distance every other surface shows`() {
        for (meters in listOf(0.0, 12.0, 999.0, 1000.0, 1534.0, 12_345.0)) {
            val (number, unit) = formatDistanceSplit(meters)
            assertEquals("at $meters m", formatDistance(meters), "$number ${english(unit)}")
        }
        Units.imperial.value = true
        for (meters in listOf(0.0, 20.0, 150.0, 500.0, 5000.0, 40_000.0)) {
            val (number, unit) = formatDistanceSplit(meters)
            assertEquals("at $meters m", formatDistance(meters), "$number ${english(unit)}")
        }
    }

    @Test fun `a 24-hour arrival carries no day-period and stays Latin`() {
        Clock24.on.value = true
        val (time, marker) = arrivalClockSplit(3600.0)
        assertNull(marker)
        assertTrue("24-hour time, got '$time'", Regex("""\d{2}:\d{2}""").matches(time))
    }

    @Test fun `a 12-hour arrival splits the day-period off the time`() {
        Clock24.on.value = false
        val (time, marker) = arrivalClockSplit(3600.0)
        assertTrue("12-hour time, got '$time'", Regex("""\d{1,2}:\d{2}""").matches(time))
        assertTrue("Latin digits only, got '$time'", time.all { it.isDigit() || it == ':' })
        assertNotNull("before-noon flag", marker)
    }

    private fun english(unit: DistanceUnit): String = when (unit) {
        DistanceUnit.METERS -> "m"
        DistanceUnit.KILOMETERS -> "km"
        DistanceUnit.FEET -> "ft"
        DistanceUnit.MILES -> "mi"
    }
}
