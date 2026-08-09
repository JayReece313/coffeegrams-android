package com.jrlabapps.coffeegrams.design

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeFormatTest {
    @Test
    fun `zero reads as zero seconds`() {
        assertEquals("zero seconds", TimeFormat.spoken(0))
    }

    @Test
    fun `seconds only, singular and plural`() {
        assertEquals("one second", TimeFormat.spoken(1))
        assertEquals("forty-five seconds", TimeFormat.spoken(45))
    }

    @Test
    fun `exact minute omits the seconds part`() {
        assertEquals("one minute", TimeFormat.spoken(60))
        assertEquals("two minutes", TimeFormat.spoken(120))
    }

    @Test
    fun `minutes and seconds combine`() {
        assertEquals("two minutes fourteen seconds", TimeFormat.spoken(134))
    }

    @Test
    fun `teens and tens format correctly`() {
        assertEquals("thirteen seconds", TimeFormat.spoken(13))
        assertEquals("twenty seconds", TimeFormat.spoken(20))
        assertEquals("twenty-one seconds", TimeFormat.spoken(21))
    }

    @Test
    fun `negative input clamps to zero`() {
        assertEquals("zero seconds", TimeFormat.spoken(-5))
    }
}
