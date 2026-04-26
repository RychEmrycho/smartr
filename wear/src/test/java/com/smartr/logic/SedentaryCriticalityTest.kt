package com.smartr.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SedentaryCriticalityTest {

    @Test
    fun `fromDuration returns correct criticality levels`() {
        val threshold = 3600 // 1 hour

        // Low: < 50% of threshold
        assertEquals(SedentaryCriticality.LOW, SedentaryCriticality.fromDuration(0, threshold))
        assertEquals(SedentaryCriticality.LOW, SedentaryCriticality.fromDuration(1799, threshold))

        // Medium: 50% to < 100% of threshold
        assertEquals(SedentaryCriticality.MEDIUM, SedentaryCriticality.fromDuration(1800, threshold))
        assertEquals(SedentaryCriticality.MEDIUM, SedentaryCriticality.fromDuration(3599, threshold))

        // High: >= 100% of threshold
        assertEquals(SedentaryCriticality.HIGH, SedentaryCriticality.fromDuration(3600, threshold))
        assertEquals(SedentaryCriticality.HIGH, SedentaryCriticality.fromDuration(7200, threshold))
    }

    @Test
    fun `emojis are correct`() {
        assertEquals("🙂", SedentaryCriticality.LOW.emoji)
        assertEquals("⚠️", SedentaryCriticality.MEDIUM.emoji)
        assertEquals("🚨", SedentaryCriticality.HIGH.emoji)
    }
}
