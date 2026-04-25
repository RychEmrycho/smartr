package com.smartr.logic

import com.smartr.data.AppSettings
import com.smartr.data.TimeIntervalUnit
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class InactivityEngineTest {

    private val engine = InactivityEngine()

    @Test
    fun `evaluate returns watch_off_body when isOffBody is true`() {
        // Arrange
        val state = InactivityState(
            sedentaryStart = Instant.now(),
            lastMovement = null,
            lastReminderAt = null
        )
        val settings = mockk<AppSettings>()
        val now = Instant.now()

        // Act
        val (newState, decision) = engine.evaluate(
            state = state,
            now = now,
            settings = settings,
            movementDetected = false,
            isOffBody = true
        )

        // Assert
        assertNull(newState.sedentaryStart)
        assertFalse(decision.shouldRemind)
        assertEquals("watch_off_body", decision.reason)
    }

    @Test
    fun `evaluate resets timers when movement is detected`() {
        // Arrange
        val state = InactivityState(
            sedentaryStart = Instant.now().minusSeconds(1000),
            lastMovement = null,
            lastReminderAt = null
        )
        val settings = mockk<AppSettings>()
        val now = Instant.now()

        // Act
        val (newState, decision) = engine.evaluate(
            state = state,
            now = now,
            settings = settings,
            movementDetected = true,
            isOffBody = false
        )

        // Assert
        assertNull(newState.sedentaryStart)
        assertEquals(now, newState.lastMovement)
        assertFalse(decision.shouldRemind)
        assertEquals("movement_reset", decision.reason)
    }
}
