package com.smartr.logic

import android.util.Log
import com.smartr.data.AppSettings
import com.smartr.data.TimeIntervalUnit
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.Duration
import java.time.ZoneId

class InactivityEngineTest {

    private val zoneId = ZoneId.of("UTC")
    private val engine = InactivityEngine(zoneId)
    private val baseTime = Instant.parse("2024-01-01T12:00:00Z") // Noon UTC (Not in quiet hours)

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
    }

    private fun mockSettings(
        bufferValue: Int = 60,
        bufferUnit: TimeIntervalUnit = TimeIntervalUnit.SECONDS,
        sitValue: Int = 45,
        sitUnit: TimeIntervalUnit = TimeIntervalUnit.MINUTES
    ): AppSettings {
        val settings = mockk<AppSettings>()
        every { settings.movementBufferValue } returns bufferValue
        every { settings.movementBufferUnit } returns bufferUnit
        every { settings.sitThresholdValue } returns sitValue
        every { settings.sitThresholdUnit } returns sitUnit
        every { settings.isSleeping } returns false
        every { settings.quietStartHour } returns 22
        every { settings.quietEndHour } returns 6
        every { settings.reminderRepeatValue } returns 20
        every { settings.reminderRepeatUnit } returns TimeIntervalUnit.MINUTES
        return settings
    }

    @Test
    fun `evaluate returns watch_off_body when isOffBody is true`() {
        val state = InactivityState(
            sedentaryStart = baseTime,
            lastMovement = null,
            lastReminderAt = null
        )
        val settings = mockSettings()

        val (newState, decision) = engine.evaluate(
            state = state,
            now = baseTime,
            settings = settings,
            movementDetected = false,
            isOffBody = true
        )

        assertNull(newState.sedentaryStart)
        assertFalse(decision.shouldRemind)
        assertEquals("watch_off_body", decision.reason)
    }

    @Test
    fun `sustained movement resets sedentary timer after buffer`() {
        val settings = mockSettings(bufferValue = 60, bufferUnit = TimeIntervalUnit.SECONDS)
        
        var state = InactivityState(
            sedentaryStart = baseTime.minus(Duration.ofHours(1)),
            lastMovement = null,
            lastReminderAt = null
        )

        // Callback 1: Start moving
        val t1 = baseTime
        val (s1, d1) = engine.evaluate(state, t1, settings, movementDetected = true)
        // Short movement doesn't bypass sedentary check
        assertEquals("threshold_reached", d1.reason)
        assertTrue(d1.shouldRemind)
        assertNotNull(s1.sedentaryStart)
        assertEquals(t1, s1.lastSignificantMovementAt)
        
        // Callback 2: Still moving (30s later)
        val t2 = t1.plusSeconds(30)
        val (s2, d2) = engine.evaluate(s1, t2, settings, movementDetected = true)
        // Still sedentary, but just reminded
        assertEquals("repeat_window", d2.reason)
        assertNotNull(s2.sedentaryStart)
        assertEquals(t1, s2.lastSignificantMovementAt)

        // Callback 3: Still moving (another 40s later, total 70s)
        val t3 = t2.plusSeconds(40)
        val (s3, d3) = engine.evaluate(s2, t3, settings, movementDetected = true)
        // Sustained move: reset!
        assertEquals("movement_reset", d3.reason)
        assertNull(s3.sedentaryStart)
    }

    @Test
    fun `movement grace period bridges small gaps`() {
        val settings = mockSettings(bufferValue = 60, bufferUnit = TimeIntervalUnit.SECONDS)
        
        var state = InactivityState(
            sedentaryStart = baseTime.minus(Duration.ofHours(1)),
            lastMovement = null,
            lastReminderAt = null
        )

        // t=0: Move
        val (s1, _) = engine.evaluate(state, baseTime, settings, movementDetected = true)
        
        // t=30s: No move (gap)
        val t2 = baseTime.plusSeconds(30)
        val (s2, _) = engine.evaluate(s1, t2, settings, movementDetected = false)
        assertNotNull(s2.lastSignificantMovementAt) 
        
        // t=70s: Move again
        val t3 = baseTime.plusSeconds(70)
        val (s3, d3) = engine.evaluate(s2, t3, settings, movementDetected = true)
        assertEquals("movement_reset", d3.reason)
        assertNull(s3.sedentaryStart)
    }

    @Test
    fun `large gap resets sustained movement timer`() {
        val settings = mockSettings(bufferValue = 60, bufferUnit = TimeIntervalUnit.SECONDS)
        
        var state = InactivityState(
            sedentaryStart = baseTime.minus(Duration.ofHours(1)),
            lastMovement = null,
            lastReminderAt = null
        )

        // t=0: Move
        val (s1, _) = engine.evaluate(state, baseTime, settings, movementDetected = true)
        
        // t=90s: No move (exceeds 1 min grace period)
        val t2 = baseTime.plusSeconds(90)
        val (s2, _) = engine.evaluate(s1, t2, settings, movementDetected = false)
        assertNull(s2.lastSignificantMovementAt)
        
        // t=120s: Move again
        val t3 = baseTime.plusSeconds(120)
        val (s3, d3) = engine.evaluate(s2, t3, settings, movementDetected = true)
        // Short move + sedentary but we just reminded at t=0
        assertEquals("repeat_window", d3.reason)
        assertNotNull(s3.sedentaryStart)
    }
}
