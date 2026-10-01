package io.github.buerlino.apodroid.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate

/** One APOD post, only what the app uses. */
data class Apod(
    /** The APOD date (US Eastern, as on the site). */
    val date: LocalDate,
    /** Without the `APOD: 2026 October 1 – ` prefix, entities decoded. */
    val title: String,
    /** The picture; on video days a still frame or a generic NASA image. */
    val imageUrl: String,
    /** The post on science.nasa.gov. */
    val pageUrl: String,
    val isVideo: Boolean,
) {
    /**
     * A name for the saved picture, without extension: `APOD_2026-10-01_Harvest_Moon_with_Mount_Etna`.
     * Unique per APOD (the date), only letters, digits, `-` and `_`.
     */
    val fileName: String
        get() {
            val words = title.replace(Regex("['’]"), "").split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotEmpty() }
            return (listOf("APOD", date.toString()) + words).joinToString("_").take(100).trimEnd('_')
        }
}

/**
 * The newest post in the APOD category of science.nasa.gov's WordPress API. Not a documented
 * API (the official api.nasa.gov one is broken, see CLAUDE.md), so parsing is kept tolerant.
 */
const val LATEST_URL = "https://science.nasa.gov/wp-json/wp/v2/image-article?categories=22766&per_page=1"

/** Blocking fetch of the newest APOD. Call off the main thread. Throws on network or format errors. */
fun fetchLatest(url: String = LATEST_URL): Apod =
    get(url, "application/json") { parseLatest(it.bufferedReader().readText()) }

/**
 * Blocking download of [url] to [to]. Throws on network errors and when fewer bytes arrive than
 * announced; [to] is then deleted.
 */
fun download(url: String, to: File) {
    try {
        get(url, "image/*") { input ->
            val expected = contentLengthLong
            val written = to.outputStream().use { input.copyTo(it) }
            if (expected >= 0 && written != expected) throw IOException("Got $written of $expected bytes")
        }
    } catch (e: IOException) {
        to.delete()
        throw e
    }
}

private fun <T> get(url: String, accept: String, read: HttpURLConnection.(InputStream) -> T): T {
    val conn = URI(url).toURL().openConnection() as HttpURLConnection
    try {
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("Accept", accept)
        if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${conn.responseCode}")
        return conn.inputStream.use { conn.read(it) }
    } finally {
        conn.disconnect()
    }
}

private val json = Json { ignoreUnknownKeys = true }

fun parseLatest(body: String): Apod {
    val p = json.decodeFromString<List<Post>>(body).firstOrNull() ?: throw IOException("No post")
    return Apod(
        date = LocalDate.parse(p.date.take(10)),
        title = cleanTitle(p.title?.rendered.orEmpty()),
        imageUrl = p.featured_image?.file ?: throw IOException("No image"),
        pageUrl = p.link,
        isVideo = isVideo(p.content?.rendered.orEmpty()),
    )
}

private val titlePrefix = Regex("""^APOD:\s*\d{4}\s+\p{L}+\s+\d{1,2}\s*[-–—]\s*""")

internal fun cleanTitle(rendered: String): String = decodeEntities(rendered).replace(titlePrefix, "").trim()

/**
 * Video days have `<video>` (an mp4) or `<iframe>` (YouTube) in the hero block, image days `<img>`.
 * Only the hero counts: image days often link YouTube in the explanation below it.
 */
internal fun isVideo(content: String): Boolean {
    val start = content.indexOf("media-detail-hero__media").coerceAtLeast(0)
    val end = content.indexOf("<h1", start).takeIf { it >= 0 } ?: content.length
    val hero = content.substring(start, end)
    return "<video" in hero || "<iframe" in hero
}

private val entity = Regex("""&(#\d+|#[xX][0-9a-fA-F]+|amp|lt|gt|quot|apos|nbsp);""")
private val named = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ")

/** Titles use numeric entities (`&#8211;`, `&#8217;`) and a few named ones. */
internal fun decodeEntities(s: String): String = entity.replace(s) { m ->
    val e = m.groupValues[1]
    val code = when {
        e.startsWith("#x", ignoreCase = true) -> e.drop(2).toIntOrNull(16)
        e.startsWith("#") -> e.drop(1).toIntOrNull()
        else -> return@replace named.getValue(e)
    }
    if (code != null && Character.isValidCodePoint(code)) String(Character.toChars(code)) else m.value
}

@Suppress("PropertyName")
@Serializable
private class Post(
    val date: String,
    val link: String,
    val title: Rendered? = null,
    val content: Rendered? = null,
    val featured_image: Image? = null,
)

@Serializable
private class Rendered(val rendered: String? = null)

@Serializable
private class Image(val file: String? = null)
