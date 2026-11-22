package io.github.buerlino.apodroid.core

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

// B29 (0.9.2): `:core` runs on Android 10 (minSdk 29), but it's a JVM module, so lint never checks
// its calls, and the JVM has every Java 11+ method. 0.9.0's probe called `InputStream.readNBytes`
// (API 33), and Android 10–12 stored no new picture. Calls known to be missing below API 33.
class AndroidApiTest {
    private val newerThanMinSdk = listOf(
        "readNBytes", "readAllBytes", "transferTo", "nullInputStream", "nullOutputStream",
        "skipNBytes", ".strip()", "List.of(", "Map.of(", "Set.of(",
    )

    @Test
    fun coreCallsNoJavaMethodNewerThanAndroid10() {
        val main = File("src/main/kotlin")
        val hits = main.walk().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                val code = line.substringBefore("//").trim()
                if (code.startsWith("*") || code.startsWith("/*")) return@mapIndexedNotNull null
                newerThanMinSdk.firstOrNull { it in code }?.let { "${file.name}:${i + 1}: $it" }
            }
        }.toList()
        assertEquals(emptyList(), hits)
    }

    /** A stream that hands out at most 3 bytes per read, as a slow network does. */
    private class Trickle(bytes: ByteArray) : InputStream() {
        private val inner = ByteArrayInputStream(bytes)
        override fun read() = inner.read()
        override fun read(b: ByteArray, off: Int, len: Int) = inner.read(b, off, minOf(len, 3))
    }

    @Test
    fun readUpToReadsAcrossShortReads() {
        val bytes = ByteArray(100) { it.toByte() }
        assertContentEquals(bytes.copyOf(64), Trickle(bytes).readUpTo(64))
    }

    @Test
    fun readUpToStopsAtTheEnd() {
        val bytes = ByteArray(10) { it.toByte() }
        assertContentEquals(bytes, Trickle(bytes).readUpTo(64))
        assertContentEquals(ByteArray(0), Trickle(ByteArray(0)).readUpTo(64))
    }
}
