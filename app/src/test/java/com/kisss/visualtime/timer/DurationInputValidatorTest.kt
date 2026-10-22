package com.kisss.visualtime.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationInputValidatorTest {
    @Test
    fun acceptsTypedOrPastedWholeMinutesAtInclusiveBoundaries() {
        assertEquals(1, DurationInputValidator.parseValidMinutes("1"))
        assertEquals(30, DurationInputValidator.parseValidMinutes("30"))
        assertEquals(1_440, DurationInputValidator.parseValidMinutes("1440"))
    }

    @Test
    fun rejectsEmptyInvalidFractionalSignedAndOutOfRangeValues() {
        listOf("", "0", "1441", "-1", "+25", "1.5", "twenty", " 25 ").forEach { value ->
            assertNull(value, DurationInputValidator.parseValidMinutes(value))
        }
    }

    @Test
    fun rejectsOverflowPronePastedValuesWithoutThrowing() {
        assertNull(DurationInputValidator.parseValidMinutes("999999999999999999999999999999"))
    }
}
