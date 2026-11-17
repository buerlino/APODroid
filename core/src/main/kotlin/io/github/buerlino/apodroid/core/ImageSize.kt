package io.github.buerlino.apodroid.core

// 0.9.0 (sharp-0.9.0.md, The download and the size): the original's size from its first bytes, so
// the app can tell whether the original is smaller than the screen. SH1; the app's probe in SH2b.

/**
 * The width and height a picture's header names, from its first [bytes] (a `Range` request's 64 KB,
 * say): JPEG (the first SOFn marker, skipping every segment before it by its length), PNG (IHDR),
 * WebP (VP8, VP8L, VP8X) and GIF. As stored, before any EXIF orientation. Null for anything else
 * (TIFF, garbage), for a header cut off before the size, and for a size of 0.
 */
fun imageSize(bytes: ByteArray): Pair<Int, Int>? {
    val size = when {
        bytes.startsWith(0xFF, 0xD8) -> jpegSize(bytes)
        bytes.startsWith(0x89, 'P'.code, 'N'.code, 'G'.code, 0x0D, 0x0A, 0x1A, 0x0A) -> pngSize(bytes)
        bytes.startsWith(*"GIF87a".codes()) || bytes.startsWith(*"GIF89a".codes()) -> gifSize(bytes)
        bytes.startsWith(*"RIFF".codes()) && bytes.has(8, "WEBP") -> webpSize(bytes)
        else -> null
    }
    return size?.takeIf { (w, h) -> w > 0 && h > 0 }
}

/**
 * The first start-of-frame marker's size: SOF0–SOF15 but DHT (C4), JPG (C8) and DAC (CC). Fill
 * bytes (FF) before a marker are skipped; markers without a length (RSTn, TEM) too; every other
 * segment by its length. A scan (SOS) or the end (EOI) before a frame: none.
 */
private fun jpegSize(b: ByteArray): Pair<Int, Int>? {
    var i = 2
    while (true) {
        if (i >= b.size) return null
        if (b.u8(i) != 0xFF) return null
        while (i < b.size && b.u8(i) == 0xFF) i++ // the marker's FF and any fill bytes
        if (i >= b.size) return null
        val marker = b.u8(i++)
        when {
            marker == 0x01 || marker in 0xD0..0xD7 -> continue
            marker == 0xD9 || marker == 0xDA -> return null
        }
        if (i + 2 > b.size) return null
        val length = b.u16be(i)
        if (length < 2) return null
        if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
            // length (2), precision (1), height (2), width (2)
            if (i + 7 > b.size) return null
            return b.u16be(i + 5) to b.u16be(i + 3)
        }
        i += length
    }
}

/** IHDR, the first chunk: its width and height, big-endian, at 16 and 20. */
private fun pngSize(b: ByteArray): Pair<Int, Int>? {
    if (b.size < 24 || !b.has(12, "IHDR")) return null
    val w = b.u32be(16)
    val h = b.u32be(20)
    return if (w in 1..Int.MAX_VALUE && h in 1..Int.MAX_VALUE) w.toInt() to h.toInt() else null
}

/** The logical screen's width and height, little-endian, at 6 and 8. */
private fun gifSize(b: ByteArray): Pair<Int, Int>? = if (b.size < 10) null else b.u16le(6) to b.u16le(8)

/**
 * The first chunk at 12: "VP8 " (lossy: after the 3-byte frame tag and the start code 9D 01 2A, 14 bits
 * each), "VP8L" (lossless: after the signature 2F, width − 1 and height − 1 in 14 bits each) or "VP8X"
 * (extended: the canvas's width − 1 and height − 1 in 24 bits each, at 24 and 27).
 */
private fun webpSize(b: ByteArray): Pair<Int, Int>? = when {
    b.has(12, "VP8 ") -> if (b.size < 30 || b.u8(23) != 0x9D || b.u8(24) != 0x01 || b.u8(25) != 0x2A) null
    else (b.u16le(26) and 0x3FFF) to (b.u16le(28) and 0x3FFF)
    b.has(12, "VP8L") -> if (b.size < 25 || b.u8(20) != 0x2F) null else {
        val bits = b.u8(21) or (b.u8(22) shl 8) or (b.u8(23) shl 16) or (b.u8(24) shl 24)
        ((bits and 0x3FFF) + 1) to (((bits ushr 14) and 0x3FFF) + 1)
    }
    b.has(12, "VP8X") -> if (b.size < 30) null else (b.u24le(24) + 1) to (b.u24le(27) + 1)
    else -> null
}

private fun String.codes() = map { it.code }.toIntArray()

private fun ByteArray.startsWith(vararg prefix: Int) = size >= prefix.size && prefix.indices.all { u8(it) == prefix[it] }

private fun ByteArray.has(at: Int, text: String) = size >= at + text.length && text.indices.all { u8(at + it) == text[it].code }

private fun ByteArray.u8(i: Int) = this[i].toInt() and 0xFF
private fun ByteArray.u16be(i: Int) = (u8(i) shl 8) or u8(i + 1)
private fun ByteArray.u16le(i: Int) = u8(i) or (u8(i + 1) shl 8)
private fun ByteArray.u24le(i: Int) = u8(i) or (u8(i + 1) shl 8) or (u8(i + 2) shl 16)
private fun ByteArray.u32be(i: Int) = (u8(i).toLong() shl 24) or (u8(i + 1).toLong() shl 16) or (u8(i + 2).toLong() shl 8) or u8(i + 3).toLong()
