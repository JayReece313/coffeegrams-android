package com.jrlabapps.coffeegrams.platform

import android.os.SystemClock
import com.jrlabapps.coffeegrams.core.MonotonicClock

/** The live [MonotonicClock] adapter, backed by [SystemClock.elapsedRealtime]. */
class LiveMonotonicClock : MonotonicClock {
    override val now: Double
        get() = SystemClock.elapsedRealtime() / 1000.0
}
