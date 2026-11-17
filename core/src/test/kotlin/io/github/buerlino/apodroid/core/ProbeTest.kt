package io.github.buerlino.apodroid.core

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// SH2b (sharp-0.9.0.md, The download and the size): the probe of the original on a local server
// that answers a range with 206 or ignores it with 200, and the choice of the original.
class ProbeTest {
    private fun bytes(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }

    private fun segment(marker: Int, payload: ByteArray): ByteArray {
        val n = payload.size + 2
        return bytes(0xFF, marker, n shr 8, n and 0xFF) + payload
    }

    /** A JPEG [w]×[h] whose frame comes after [app1s] APP1 segments of 65,000 bytes each (an EXIF block, say). */
    private fun jpeg(w: Int, h: Int, app1s: Int = 0): ByteArray {
        var b = bytes(0xFF, 0xD8)
        repeat(app1s) { b += segment(0xE1, ByteArray(64_998)) }
        b += segment(0xC0, bytes(8, h shr 8, h and 0xFF, w shr 8, w and 0xFF, 1, 1, 0x11, 0))
        return b + segment(0xDA, bytes(1, 1, 0, 0, 63, 0)) + ByteArray(200_000) + bytes(0xFF, 0xD9)
    }

    private val png = bytes(0x89, 'P'.code, 'N'.code, 'G'.code, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13,
        'I'.code, 'H'.code, 'D'.code, 'R'.code, 0, 0, 3, 0xD6, 0, 0, 2, 0x3F, 8, 6, 0, 0, 0) + ByteArray(600_000)

    private val tiff = bytes('I'.code, 'I'.code, 42, 0) + ByteArray(700_000)

    private val UA = "APODroid/0.0.0 (+https://example.org)"

    private class Asked(val ranges: MutableList<String?> = mutableListOf(), val paths: MutableList<String> = mutableListOf())

    /** Serves [body]; with [honoursRange] a `Range: bytes=0-n` gets 206 and those bytes, else 200 and all of it. */
    private fun withServer(body: ByteArray, status: Int = 200, honoursRange: Boolean = true, path: String = "/x.jpg", block: (String, Asked) -> Unit) {
        val asked = Asked()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            val range = ex.requestHeaders.getFirst("Range")
            asked.ranges += range
            asked.paths += ex.requestURI.rawPath
            val end = range?.removePrefix("bytes=0-")?.toIntOrNull()
            if (status != 200) {
                ex.sendResponseHeaders(status, -1)
            } else if (honoursRange && end != null) {
                val part = body.copyOf(minOf(end + 1, body.size))
                ex.responseHeaders.add("Content-Range", "bytes 0-${part.size - 1}/${body.size}")
                ex.sendResponseHeaders(206, part.size.toLong())
                runCatching { ex.responseBody.use { it.write(part) } }
            } else {
                ex.sendResponseHeaders(200, body.size.toLong())
                runCatching { ex.responseBody.use { it.write(body) } } // the client closes early
            }
            ex.close()
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}$path", asked)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun aJpegsSizeFromTheFirst64Kb() = withServer(jpeg(1500, 1022)) { url, asked ->
        assertEquals(1500 to 1022, originalSize(url, UA))
        assertEquals(listOf<String?>("bytes=0-65535"), asked.ranges)
    }

    @Test
    fun aJpegWhoseFrameLiesPast64KbIsAskedAgainFor512Kb() = withServer(jpeg(4000, 3000, app1s = 2)) { url, asked ->
        assertEquals(4000 to 3000, originalSize(url, UA))
        assertEquals(listOf<String?>("bytes=0-65535", "bytes=0-524287"), asked.ranges)
    }

    @Test
    fun aServerIgnoringTheRangeIsReadOnlyThatFar() = withServer(jpeg(4000, 3000, app1s = 2), honoursRange = false) { url, asked ->
        assertEquals(PROBE_BYTES, firstBytes(url, UA, PROBE_BYTES).size)
        assertEquals(4000 to 3000, originalSize(url, UA))
        assertEquals(3, asked.ranges.size)
    }

    @Test
    fun theFirstBytesAreTheFilesOwn() = withServer(png) { url, _ ->
        assertContentEquals(png.copyOf(PROBE_BYTES), firstBytes(url, UA, PROBE_BYTES))
    }

    @Test
    fun aShortFileIsReadWhole() = withServer(jpeg(10, 10).copyOf(1000)) { url, _ ->
        assertEquals(1000, firstBytes(url, UA, PROBE_BYTES).size)
    }

    @Test
    fun aPngNeedsOneRequest() = withServer(png) { url, asked ->
        assertEquals(982 to 575, originalSize(url, UA))
        assertEquals(1, asked.ranges.size)
    }

    @Test
    fun aTiffHasNoSizeAndNoSecondRequest() = withServer(tiff) { url, asked ->
        assertNull(originalSize(url, UA))
        assertEquals(1, asked.ranges.size)
    }

    @Test
    fun anHttpErrorThrows() = withServer(ByteArray(0), status = 404) { url, _ ->
        assertEquals(404, assertFailsWith<HttpStatus> { originalSize(url, UA) }.code)
    }

    @Test
    fun anEncodedUrlIsNotEncodedTwice() = withServer(jpeg(1500, 1000), path = "/Jesli%C4%8Dky%20x.jpg") { url, asked ->
        assertEquals(1500 to 1000, originalSize(url, UA))
        assertEquals("/Jesli%C4%8Dky%20x.jpg", asked.paths.single())
    }

    @Test
    fun aRawSpaceIsTheSitesUrl() {
        assertFailsWith<SiteChanged> { originalSize("https://example.org/a b.jpg", UA) }
    }

    @Test
    fun probeMoreOnlyForAJpegCutOffAt64Kb() {
        val cut = jpeg(4000, 3000, app1s = 2).copyOf(PROBE_BYTES)
        assertTrue(probeMore(cut))
        assertFalse(probeMore(jpeg(4000, 3000).copyOf(PROBE_BYTES)), "the size is there")
        assertFalse(probeMore(cut.copyOf(1000)), "the whole file was shorter")
        assertFalse(probeMore(tiff.copyOf(PROBE_BYTES)))
        assertFalse(probeMore(ByteArray(PROBE_BYTES)))
    }

    @Test
    fun theOriginalWhenItWouldntCoverTheScreen() {
        // The FP6's 1116×2484 (sharp-0.9.0.md, What the pictures say).
        assertTrue(takesOriginal(1500, 1022, 1116, 2484), "7 Nov: 2.43× enlarged")
        assertTrue(takesOriginal(982, 575, 1116, 2484), "5 Nov's PNG")
        assertFalse(takesOriginal(4000, 3000, 1116, 2484))
        assertTrue(takesOriginal(4000, 2484, 1116, 2484), "exactly the screen's height: 1:1")
        assertFalse(takesOriginal(4000, 2485, 1116, 2484))
        assertTrue(takesOriginal(1116, 5000, 1116, 2484), "exactly the screen's width")
        assertFalse(takesOriginal(1117, 2485, 1116, 2484))
        assertTrue(takesOriginal(1000, 3000, 1116, 2484), "narrower than the screen")
        assertTrue(takesOriginal(640, 480, 1080, 2400))
    }
}
