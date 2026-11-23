package com.gymnotepad.app.domain

private const val MIN_REST_SECONDS = 30
private const val MAX_REST_SECONDS = 10 * 60

fun adjustRestDuration(currentSeconds: Int, deltaSeconds: Int): Int =
    (currentSeconds + deltaSeconds).coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS)

fun remainingRestSeconds(endTimeMillis: Long, nowMillis: Long): Int {
    val remainingMillis = (endTimeMillis - nowMillis).coerceAtLeast(0L)
    return ((remainingMillis + 999L) / 1_000L).toInt()
}

fun formatRestDuration(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)
