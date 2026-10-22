package com.kisss.visualtime.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatterTest {
    @Test
    fun shortFormatUsesCeilingAndNeverShowsZeroBeforeExpiry() {
        assertEquals("15:00", DurationFormatter.format(900_000L, false, DurationFormat.Short))
        assertEquals("00:02", DurationFormatter.format(1_001L, false, DurationFormat.Short))
        assertEquals("00:01", DurationFormatter.format(1L, false, DurationFormat.Short))
        assertEquals("00:00", DurationFormatter.format(0L, true, DurationFormat.Short))
    }

    @Test
    fun longFormatRemainsFixedThroughHourBoundaryAndCompletion() {
        assertEquals("01:30:00", DurationFormatter.format(5_400_000L, false, DurationFormat.Long))
        assertEquals("01:00:00", DurationFormatter.format(3_599_001L, false, DurationFormat.Long))
        assertEquals("00:59:59", DurationFormatter.format(3_599_000L, false, DurationFormat.Long))
        assertEquals("24:00:00", DurationFormatter.format(86_400_000L, false, DurationFormat.Long))
        assertEquals("00:00:00", DurationFormatter.format(0L, true, DurationFormat.Long))
    }
}
