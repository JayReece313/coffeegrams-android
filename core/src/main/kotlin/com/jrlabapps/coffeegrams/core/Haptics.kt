package com.jrlabapps.coffeegrams.core

/**
 * Haptic feedback the guided brew fires as it progresses. A port — an
 * abstraction `:core` owns but does not implement — so `:app` can back it
 * with real device vibration and tests can back it with a recording double,
 * mirroring the iOS app's `HapticsPerforming` protocol.
 *
 * [targetReached] is deliberately distinct from [stepChange]: nothing has
 * advanced automatically, the user is being asked to act (finish a pour,
 * plunge a French press). Reusing [stepChange] there would make that
 * distinction invisible.
 */
interface Haptics {
    /** A light tap when the brew advances to a new step. */
    fun stepChange()

    /** A sharper cue when a hands-on step reaches its target and starts overrunning. */
    fun targetReached()

    /** A success buzz when the whole brew finishes. */
    fun finished()
}
