package com.gymnotepad.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RestTimerTest {
    @Test
    fun durationAdjustmentUsesThirtySecondAndTenMinuteBounds() {
        assertEquals(30, adjustRestDuration(30, -30))
        assertEquals(60, adjustRestDuration(30, 30))
        assertEquals(600, adjustRestDuration(600, 30))
    }

    @Test
    fun remainingSecondsRoundUpAndNeverBecomeNegative() {
        assertEquals(2, remainingRestSeconds(2_001, 1_000))
        assertEquals(1, remainingRestSeconds(2_000, 1_999))
        assertEquals(0, remainingRestSeconds(2_000, 2_001))
    }

    @Test
    fun durationIsFormattedAsMinutesAndSeconds() {
        assertEquals("0:30", formatRestDuration(30))
        assertEquals("1:30", formatRestDuration(90))
        assertEquals("10:00", formatRestDuration(600))
    }
}
