package com.kisss.visualtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentDurationsPolicyTest {
    @Test
    fun missingPreferenceSeedsDefaultsExactlyOnce() {
        val result = RecentDurationsPolicy.initialize(
            preferenceExists = false,
            encodedValue = null,
        )

        assertEquals(listOf(15, 25, 40), result.durations)
        assertEquals("15,25,40", result.encodedValueToSeed)
    }

    @Test
    fun presentEmptyPreferenceStaysEmptyAndIsNeverReseeded() {
        val result = RecentDurationsPolicy.initialize(
            preferenceExists = true,
            encodedValue = "",
        )

        assertEquals(emptyList<Int>(), result.durations)
        assertNull(result.encodedValueToSeed)
    }

    @Test
    fun malformedStoredValuesAreSanitizedWithoutRestoringDefaults() {
        val result = RecentDurationsPolicy.initialize(
            preferenceExists = true,
            encodedValue = "25,bad,0,25,1441,40,15,30",
        )

        assertEquals(listOf(25, 40, 15), result.durations)
        assertNull(result.encodedValueToSeed)
    }

    @Test
    fun startedDurationMovesToFrontWithoutDuplicatesAndTrimsToThree() {
        val reused = RecentDurationsPolicy.recordStarted(listOf(15, 25, 40), 25)
        val newDuration = RecentDurationsPolicy.recordStarted(reused, 30)

        assertEquals(listOf(25, 15, 40), reused)
        assertEquals(listOf(30, 25, 15), newDuration)
        assertEquals("30,25,15", RecentDurationsPolicy.encode(newDuration))
    }
}
