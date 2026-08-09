package com.jrlabapps.coffeegrams.design

/**
 * Spoken-digit time formatting for accessibility. TalkBack reads a
 * colon-separated readout (e.g. "2:14") as raw digits, which is awkward to
 * follow aloud — the guided-brew total-elapsed content description uses
 * this instead, mirroring the iOS app's own spoken-form time formatter.
 *
 * Handles 0–59 minutes, which covers every realistic brew (the longest
 * single method's steep/overrun is well under an hour); not intended for
 * durations beyond that.
 */
object TimeFormat {
    /** e.g. `134` -> "two minutes fourteen seconds"; `60` -> "one minute"; `0` -> "zero seconds". */
    fun spoken(totalSeconds: Int): String {
        val clamped = totalSeconds.coerceAtLeast(0)
        val minutes = clamped / 60
        val seconds = clamped % 60

        val minutesPart = if (minutes > 0) "${words(minutes)} minute${if (minutes == 1) "" else "s"}" else null
        val secondsPart = if (seconds > 0 || minutes == 0) {
            "${words(seconds)} second${if (seconds == 1) "" else "s"}"
        } else {
            null
        }

        return listOfNotNull(minutesPart, secondsPart).joinToString(" ")
    }

    private val ones = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen",
    )
    private val tens = listOf("", "", "twenty", "thirty", "forty", "fifty")

    private fun words(n: Int): String = when {
        n < 20 -> ones[n]
        else -> {
            val tensWord = tens[n / 10]
            val onesDigit = n % 10
            if (onesDigit == 0) tensWord else "$tensWord-${ones[onesDigit]}"
        }
    }
}
