package com.jrlabapps.coffeegrams.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the six brand tokens to DESIGN.md's contract table. DESIGN.md says
 * these hex values "are not to be re-derived, eyeballed, or 'improved'
 * independently on Android" — this test is what makes a silent drift from
 * that contract a build failure instead of a code-review miss.
 *
 * Compares by equality against a freshly constructed [Color] rather than
 * inspecting [Color.value]'s bit layout directly — that's an internal
 * representation detail this test has no need to depend on.
 */
class ColorTest {

    @Test
    fun `light tokens match the DESIGN md contract table`() {
        assertEquals(Color(0xFFF4EADB), BackgroundLight)
        assertEquals(Color(0xFFFBF4E9), SurfaceLight)
        assertEquals(Color(0xFF3A2A1E), TextPrimaryLight)
        assertEquals(Color(0xFF6B5647), TextSecondaryLight)
        assertEquals(Color(0xFFC6852E), AccentLight)
        assertEquals(Color(0xFFA96B1E), TimerActiveLight)
    }

    @Test
    fun `dark tokens match the DESIGN md contract table`() {
        assertEquals(Color(0xFF1C140E), BackgroundDark)
        assertEquals(Color(0xFF2A1F16), SurfaceDark)
        assertEquals(Color(0xFFF1E7D8), TextPrimaryDark)
        assertEquals(Color(0xFFB9A897), TextSecondaryDark)
        assertEquals(Color(0xFFE0A34A), AccentDark)
        assertEquals(Color(0xFFE7B45C), TimerActiveDark)
    }
}
