package com.darshan.focusfeed

import java.util.Calendar

// Change these numbers to tune the app.
object Config {
    const val CLAUDE_MODEL = "claude-haiku-5-5"
    // Tried in order; fast models first so cards load quickly
    val GEMINI_MODELS = listOf("gemini-flash-lite-latest", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-flash-latest")
    const val UNLOCK_DELAY_MIN = 10   // nudge about 10 min after unlocking (plus or minus 2)
    const val YOUTUBE_DELAY_MIN = 2   // nudge after 2 min in YouTube
    const val MIN_GAP = 35            // random gap between timed nudges, minutes
    const val MAX_GAP = 55

    // Timed nudges: Sun-Thu 17:00-24:00, Fri/Sat 17:00-19:30 and 22:00-24:00
    fun inWindow(c: Calendar): Boolean {
        val d = c.get(Calendar.DAY_OF_WEEK)
        val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        return if (d == Calendar.FRIDAY || d == Calendar.SATURDAY) (m in 1020 until 1170) || (m in 1320 until 1440)
        else m in 1020 until 1440
    }

    // Unlock and YouTube nudges: any time from 08:30 to midnight
    fun awake(c: Calendar): Boolean = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)) in 510 until 1440
}
