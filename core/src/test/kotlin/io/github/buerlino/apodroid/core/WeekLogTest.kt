package io.github.buerlino.apodroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class WeekLogTest {
    private class Taken : RuntimeException("UNIQUE constraint failed: files._data")

    private val tried = mutableListOf<String>()

    /** An insert that finds the names in [stale] taken (a stale row) and makes the others. */
    private fun inserter(stale: Set<String>): (String) -> String? = { name ->
        tried += name
        if (name in stale) throw Taken() else "uri:$name"
    }

    private fun make(from: Int, stale: Set<String>) = WeekLogNames.make(from, inserter(stale)) { it is Taken }

    @Test
    fun theNames() {
        assertEquals("log.txt", WeekLogNames.name(0))
        assertEquals("log (1).txt", WeekLogNames.name(1))
        assertEquals("log (12).txt", WeekLogNames.name(12))
    }

    @Test
    fun aFreeLogTxtIsMadeAtOnce() {
        val made = make(0, emptySet())!!
        assertEquals("uri:log.txt", made.value)
        assertEquals(0, made.index)
        assertEquals(listOf("log.txt"), tried)
    }

    @Test
    fun aStaleRowMovesToTheNextName() {
        val made = make(0, setOf("log.txt"))!!
        assertEquals("uri:log (1).txt", made.value)
        assertEquals(1, made.index)
        assertEquals(listOf("log.txt", "log (1).txt"), tried)
    }

    @Test
    fun twoClearsMoveTwoNamesOn() {
        val made = make(0, setOf("log.txt", "log (1).txt"))!!
        assertEquals("uri:log (2).txt", made.value)
    }

    @Test
    fun itStartsWhereTheProcessLeftOff() {
        val made = make(2, setOf("log.txt", "log (1).txt"))!!
        assertEquals(2, made.index)
        assertEquals(listOf("log (2).txt"), tried)
    }

    @Test
    fun everyNameTakenIsNullAfterTries() {
        val all = (0 until WeekLogNames.TRIES).map { WeekLogNames.name(it) }.toSet()
        assertNull(make(0, all))
        assertEquals(WeekLogNames.TRIES, tried.size)
        tried.clear()
        assertNull(make(WeekLogNames.TRIES, all))
        assertEquals(emptyList<String>(), tried)
    }

    @Test
    fun aNullInsertTriesNoOtherName() {
        assertNull(WeekLogNames.make(0, { tried += it; null as String? }) { true })
        assertEquals(listOf("log.txt"), tried)
    }

    @Test
    fun anotherFailureIsRethrown() {
        try {
            WeekLogNames.make(0, { throw IllegalStateException("full") }) { it is Taken }
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("full", e.message)
        }
    }
}
