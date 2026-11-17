package io.github.buerlino.apodroid.core

import org.junit.Assume.assumeTrue
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// SH1 (sharp-0.9.0.md, Testing 1, imageSize): made-up headers, pictures the JDK writes, and the real
// pictures in the gitignored private/ (skipped without).
class ImageSizeTest {
    private fun bytes(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }
    private fun ascii(s: String) = s.map { it.code }.toIntArray()

    /** A JPEG segment: FF [marker], its length (2 + [payload]), then [payload]. */
    private fun segment(marker: Int, payload: ByteArray): ByteArray {
        val n = payload.size + 2
        return bytes(0xFF, marker, n shr 8, n and 0xFF) + payload
    }

    /** A start-of-frame payload: precision 8, [h], [w], 3 components. */
    private fun sof(w: Int, h: Int) =
        bytes(8, h shr 8, h and 0xFF, w shr 8, w and 0xFF, 3, 1, 0x22, 0, 2, 0x11, 1, 3, 0x11, 1)

    private val soi = bytes(0xFF, 0xD8)
    private val app0 = segment(0xE0, bytes(*ascii("JFIF"), 0, 1, 1, 0, 0, 1, 0, 1, 0, 0))
    private val dht = segment(0xC4, ByteArray(30))
    private val dqt = segment(0xDB, ByteArray(65))
    private val sos = segment(0xDA, bytes(1, 1, 0, 0, 63, 0)) + ByteArray(100)

    @Test
    fun jpegBaseline() {
        assertEquals(1500 to 1022, imageSize(soi + app0 + dqt + segment(0xC0, sof(1500, 1022)) + dht + sos))
    }

    @Test
    fun jpegProgressiveAndExtended() {
        assertEquals(4000 to 3000, imageSize(soi + app0 + segment(0xC2, sof(4000, 3000)) + sos))
        assertEquals(800 to 600, imageSize(soi + segment(0xC1, sof(800, 600)) + sos))
        assertEquals(65535 to 1, imageSize(soi + segment(0xCF, sof(65535, 1))))
    }

    /** DHT (C4), JPG (C8) and DAC (CC) sit among the SOF markers' codes but aren't frames. */
    @Test
    fun jpegSkipsDhtJpgAndDac() {
        val notFrames = segment(0xC4, ByteArray(30)) + segment(0xC8, ByteArray(4)) + segment(0xCC, ByteArray(2))
        assertEquals(1280 to 853, imageSize(soi + notFrames + segment(0xC0, sof(1280, 853)) + sos))
    }

    /**
     * 70 KB of APP1 (EXIF, then XMP: one segment holds at most 64 KB) before the frame: found in the whole
     * header, not in its first 64 KB (the app's probe then asks for 512 KB).
     */
    @Test
    fun jpegWithALargeApp1() {
        val app1 = segment(0xE1, ByteArray(35_000 - 4)) + segment(0xE1, ByteArray(35_000 - 4))
        val whole = soi + app0 + app1 + dqt + segment(0xC0, sof(5815, 3877)) + sos
        assertEquals(5815 to 3877, imageSize(whole))
        assertNull(imageSize(whole.copyOf(65_536)))
        assertEquals(5815 to 3877, imageSize(whole.copyOf(524_288.coerceAtMost(whole.size))))
    }

    @Test
    fun jpegFillBytesAndMarkersWithoutALength() {
        val fill = bytes(0xFF, 0xFF, 0xFF)
        val rst = bytes(0xFF, 0xD0, 0xFF, 0x01)
        assertEquals(640 to 480, imageSize(soi + rst + fill + segment(0xC0, sof(640, 480))))
    }

    @Test
    fun jpegWithoutAFrameOrCutOff() {
        assertNull(imageSize(soi + app0 + sos + segment(0xC0, sof(640, 480))), "a scan before the frame")
        assertNull(imageSize(soi + app0 + bytes(0xFF, 0xD9)), "the end before the frame")
        val whole = soi + app0 + segment(0xC0, sof(640, 480))
        for (n in 0 until whole.size - sof(640, 480).size + 5) assertNull(imageSize(whole.copyOf(n)), "cut at $n")
        assertNull(imageSize(soi + app0 + bytes(0x12, 0x34)), "no marker where one belongs")
        assertNull(imageSize(soi + bytes(0xFF, 0xE0, 0, 1)), "a length under 2")
        assertNull(imageSize(soi + segment(0xC0, sof(0, 480))), "width 0 (DNL)")
    }

    private val pngSignature = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @Test
    fun png() {
        val ihdr = bytes(0, 0, 0, 13, *ascii("IHDR"), 0, 0, 0x11, 0x67, 0, 0, 0x15, 0xD8, 8, 6, 0, 0, 0)
        assertEquals(4455 to 5592, imageSize(pngSignature + ihdr))
        assertNull(imageSize(pngSignature + ihdr.copyOf(15)), "cut off")
        assertNull(imageSize(pngSignature + bytes(0, 0, 0, 13, *ascii("IDAT"), 0, 0, 1, 0, 0, 0, 1, 0)), "no IHDR first")
        assertNull(imageSize(pngSignature + bytes(0, 0, 0, 13, *ascii("IHDR"), 0x80, 0, 0, 0, 0, 0, 1, 0)), "over 2³¹")
    }

    @Test
    fun gif() {
        assertEquals(500 to 300, imageSize(bytes(*ascii("GIF89a"), 0xF4, 0x01, 0x2C, 0x01, 0, 0, 0)))
        assertEquals(1 to 65535, imageSize(bytes(*ascii("GIF87a"), 1, 0, 0xFF, 0xFF)))
        assertNull(imageSize(bytes(*ascii("GIF89a"), 0xF4, 0x01, 0x2C)))
        assertNull(imageSize(bytes(*ascii("GIF88a"), 0xF4, 0x01, 0x2C, 0x01)))
    }

    private fun riff(chunk: String, vararg data: Int) =
        bytes(*ascii("RIFF"), 0, 0, 0, 0, *ascii("WEBP"), *ascii(chunk), 0, 0, 0, 0, *data)

    @Test
    fun webpLossy() {
        // The frame tag (3 bytes), the start code 9D 01 2A, then 14 bits each (the top 2 the scale).
        val lossy = riff("VP8 ", 0x30, 0x01, 0x00, 0x9D, 0x01, 0x2A, 0x80, 0x07, 0x38, 0xC4)
        assertEquals(1920 to 1080, imageSize(lossy))
        assertNull(imageSize(riff("VP8 ", 0x30, 0x01, 0x00, 0x9D, 0x01, 0x2B, 0x80, 0x07, 0x38, 0x04)), "no start code")
        assertNull(imageSize(lossy.copyOf(lossy.size - 1)))
    }

    @Test
    fun webpLossless() {
        // 0x2F, then width − 1 (14 bits), height − 1 (14 bits), alpha, version: 1000×750.
        val bits = 999 or (749 shl 14)
        val lossless = riff("VP8L", 0x2F, bits and 0xFF, (bits shr 8) and 0xFF, (bits shr 16) and 0xFF, (bits shr 24) and 0xFF)
        assertEquals(1000 to 750, imageSize(lossless))
        assertNull(imageSize(riff("VP8L", 0x2E, 0, 0, 0, 0)), "no signature")
        assertNull(imageSize(lossless.copyOf(lossless.size - 1)))
    }

    @Test
    fun webpExtended() {
        // Flags and 3 reserved bytes, then the canvas's width − 1 and height − 1, 24 bits each: 6000×4000.
        val w = 5999
        val h = 3999
        val extended = riff("VP8X", 0x10, 0, 0, 0, w and 0xFF, (w shr 8) and 0xFF, w shr 16, h and 0xFF, (h shr 8) and 0xFF, h shr 16)
        assertEquals(6000 to 4000, imageSize(extended))
        assertNull(imageSize(extended.copyOf(extended.size - 1)))
        assertNull(imageSize(riff("VP8Y", 0x10, 0, 0, 0, 1, 0, 0, 1, 0, 0)), "an unknown chunk")
    }

    @Test
    fun garbageTiffAndEmpty() {
        assertNull(imageSize(ByteArray(0)))
        assertNull(imageSize(bytes(0xFF)))
        assertNull(imageSize(bytes(*ascii("<!DOCTYPE html><html><body>Not found</body></html>"))))
        assertNull(imageSize(bytes(0x49, 0x49, 0x2A, 0x00, 8, 0, 0, 0)), "TIFF, little-endian")
        assertNull(imageSize(bytes(0x4D, 0x4D, 0x00, 0x2A, 0, 0, 0, 8)), "TIFF, big-endian")
        assertNull(imageSize(bytes(*ascii("RIFF"), 0, 0, 0, 0, *ascii("WAVE"), *ascii("fmt "))), "RIFF but not WebP")
        val r = java.util.Random(17)
        repeat(2000) {
            val junk = ByteArray(r.nextInt(200)).also(r::nextBytes)
            imageSize(junk) // never throws
        }
    }

    /** Pictures the JDK writes: JPEG, PNG and GIF, the size read back as the JDK writes it. */
    @Test
    fun picturesTheJdkWrites() {
        for ((format, size) in listOf("jpg" to (1500 to 1022), "png" to (982 to 575), "gif" to (321 to 123), "jpg" to (7 to 4000))) {
            val image = BufferedImage(size.first, size.second, BufferedImage.TYPE_INT_RGB)
            val out = ByteArrayOutputStream()
            check(ImageIO.write(image, format, out))
            assertEquals(size, imageSize(out.toByteArray().copyOf(65_536.coerceAtMost(out.size()))), format)
        }
    }

    /**
     * The real pictures in private/ (APOD's renditions, any format): as the JDK reads them, from their first
     * 64 KB; a TIFF (2026-01-10's) has no size here.
     */
    @Test
    fun theRealPictures() {
        val dirs = listOf(File("../private/pictures"), File("../private/pictures-oct-nov"))
        assumeTrue(dirs.any { it.isDirectory })
        var read = 0
        var tiffs = 0
        for (file in dirs.flatMap { it.listFiles()?.toList().orEmpty() }.filter { it.isFile }.sorted()) {
            val image = ImageIO.read(file) ?: continue // a format the JDK doesn't read (WebP)
            val head = file.inputStream().use { it.readNBytes(65_536) }
            if (imageSize(head) == null && head.size > 3 && (head[0] == 0x49.toByte() || head[0] == 0x4D.toByte()) && head[2] + head[3] == 0x2A) {
                tiffs++
                continue
            }
            assertEquals(image.width to image.height, imageSize(head), file.name)
            read++
        }
        assertTrue(read > 200, "$read pictures read")
        assertEquals(1, tiffs, "TIFFs")
    }
}
