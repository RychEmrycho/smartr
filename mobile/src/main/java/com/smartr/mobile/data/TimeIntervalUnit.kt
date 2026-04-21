package com.smartr.mobile.data

enum class TimeIntervalUnit {
    SECONDS, MINUTES, HOURS;

    fun toMinutes(value: Int): Int = when (this) {
        SECONDS -> value / 60
        MINUTES -> value
        HOURS -> value * 60
    }
}
