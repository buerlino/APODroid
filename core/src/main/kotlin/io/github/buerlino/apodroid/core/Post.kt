package io.github.buerlino.apodroid.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Where the pictures come from, with the newest post's URL. Neither is a documented API (the
 * official api.nasa.gov one is broken, see CLAUDE.md), so parsing is kept tolerant.
 */
enum class Source(internal val latestUrl: String, internal val accept: String, internal val prefix: String) {
    /** The APOD category of science.nasa.gov's WordPress API. */
    APOD("https://science.nasa.gov/wp-json/wp/v2/image-article?categories=22766&per_page=1", "application/json", "APOD"),
    /** NASA Image of the Day: www.nasa.gov's RSS feed. */
    NASA("https://www.nasa.gov/feeds/iotd-feed/", "application/rss+xml, application/xml", "NASA"),
    /** Earth Observatory Image of the Day, in the same WordPress as APOD. */
    EARTH(
        "https://science.nasa.gov/wp-json/wp/v2/posts?science-org=22453&per_page=1&_fields=date,link,title,excerpt,featured_image_url",
        "application/json", "EO",
    ),
}

/** One post of a [Source], only what the app uses. */
data class Post(
    val source: Source,
    /** The post's date (US Eastern, as on the sites). */
    val date: LocalDate,
    /** Entities decoded; for APOD without the `APOD: 2026 October 1 – ` prefix. */
    val title: String,
    /** The picture; null on APOD video days (their still frame is often a generic NASA image, or missing). */
    val imageUrl: String?,
    /** The post's page. */
    val pageUrl: String,
    val isVideo: Boolean,
    /** The paragraph about the picture, plain text; empty if not found. */
    val explanation: String = "",
) {
    /**
     * A name for the saved picture, without extension:
     * `APOD_2026-10-01_Harvest_Moon_with_Erupting_Mount_Etna` (`NASA_…`, `EO_…`). Unique per
     * post (source and date), only letters, digits, `-` and `_`.
     */
    val fileName: String
        get() {
            val words = title.replace(Regex("['’]"), "").split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotEmpty() }
            return (listOf(source.prefix, date.toString()) + words).joinToString("_").take(100).trimEnd('_')
        }

    /**
     * This is the post noted as [date] and [imageUrl] (on the wallpaper, saved, its crop): a
     * site may post a second picture on the same date. No [imageUrl] noted (0.4.0's prefs, the
     * user's own picture) matches by date alone.
     */
    fun isNoted(date: LocalDate?, imageUrl: String?): Boolean =
        this.date == date && (imageUrl == null || imageUrl == this.imageUrl)
}

/**
 * [url] scaled by the host to [height] pixels (`?h=`), on the two hosts that do it; others
 * unchanged. Any query on [url] is dropped first.
 */
fun renditionUrl(url: String, height: Int): String =
    if (scalingHosts.any { "://$it" in url }) "${url.substringBefore('?')}?h=$height" else url

private val scalingHosts = listOf("assets.science.nasa.gov/dynamicimage/", "www.nasa.gov/wp-content/uploads/")

/** The full-size picture: APOD and Earth Observatory have it under `content/dam`, NASA's feed links it. */
fun originalUrl(post: Post): String? = when (post.source) {
    Source.NASA -> post.imageUrl
    Source.APOD, Source.EARTH -> post.imageUrl?.substringBefore('?')?.replace("/dynamicimage/assets/", "/content/dam/")
}

/**
 * The site answered, but not as expected: not the JSON this app reads, no post, a bad date, an
 * image day without a picture, a picture that isn't one. An app update may be needed.
 */
class SiteChanged(message: String, cause: Throwable? = null) : IOException(message, cause)

/** The picture couldn't be written to its file (a full disk, say), whatever the network did. */
class StorageFailed(cause: Throwable) : IOException("Couldn't write the file: $cause", cause)

/** The site answered with [code] instead of 200. */
class HttpStatus(val code: Int) : IOException("HTTP $code")

/**
 * Blocking fetch of [source]'s newest post. Call off the main thread. Throws [SiteChanged]
 * (also for a URL that doesn't parse), [HttpStatus], or another [IOException] for a network failure.
 * [userAgent] goes with the request so the site can tell this app apart (see CLAUDE.md).
 */
fun fetchLatest(source: Source, userAgent: String, url: String = source.latestUrl): Post =
    get(url, source.accept, userAgent) { parseLatest(source, it.bufferedReader().readText()) }

/** Throws [SiteChanged] for anything it can't read. */
fun parseLatest(source: Source, body: String): Post = when (source) {
    Source.APOD -> parseApod(body)
    Source.NASA -> nasaPosts(body).firstOrNull() ?: throw SiteChanged("No item")
    Source.EARTH -> earthPosts(body).firstOrNull() ?: throw SiteChanged("No post")
}

/**
 * Blocking download of [url] to [to]. Throws [SiteChanged] for a URL that doesn't parse,
 * [HttpStatus], [StorageFailed] when [to] can't be written, [InterruptedIOException] when the thread was interrupted (checked once per chunk), or an [IOException] for a network failure, also when fewer bytes arrive than
 * announced; [to] is then deleted.
 */
fun download(url: String, to: File, userAgent: String) {
    try {
        get(url, "image/*", userAgent) { input ->
            val expected = contentLengthLong
            var written = 0L
            val buffer = ByteArray(64 * 1024)
            val out = try {
                to.outputStream()
            } catch (e: IOException) {
                throw StorageFailed(e)
            }
            out.use {
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (Thread.interrupted()) throw InterruptedIOException("Download interrupted")
                    try {
                        it.write(buffer, 0, n)
                    } catch (e: IOException) {
                        throw StorageFailed(e)
                    }
                    written += n
                }
            }
            if (expected >= 0 && written != expected) throw IOException("Got $written of $expected bytes")
        }
    } catch (e: IOException) {
        to.delete()
        throw e
    }
}

private fun <T> get(url: String, accept: String, userAgent: String, read: HttpURLConnection.(InputStream) -> T): T {
    // Whatever it throws (a raw space, no scheme) is the site's URL, not the network.
    val address = runCatching { URI(url).toURL() }.getOrElse { throw SiteChanged("Bad URL: $url", it) }
    val conn = address.openConnection() as HttpURLConnection
    try {
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("Accept", accept)
        conn.setRequestProperty("User-Agent", userAgent)
        if (conn.responseCode != HttpURLConnection.HTTP_OK) throw HttpStatus(conn.responseCode)
        return conn.inputStream.use { conn.read(it) }
    } finally {
        conn.disconnect()
    }
}

private val json = Json { ignoreUnknownKeys = true }

private fun wpPosts(body: String): List<WpPost> = try {
    json.decodeFromString<List<WpPost>>(body)
} catch (e: IllegalArgumentException) { // SerializationException: not the JSON expected
    throw SiteChanged("Unexpected JSON", e)
}

private fun wpDate(date: String): LocalDate = try {
    LocalDate.parse(date.take(10))
} catch (e: DateTimeParseException) {
    throw SiteChanged("Bad date: $date", e)
}

internal fun parseApod(body: String): Post {
    val p = wpPosts(body).firstOrNull() ?: throw SiteChanged("No post")
    // Video days may have no featured_image at all (16 Oct 2026).
    val isVideo = isVideo(p.content?.rendered.orEmpty())
    return Post(
        source = Source.APOD,
        date = wpDate(p.date),
        title = cleanTitle(p.title?.rendered.orEmpty()),
        imageUrl = if (isVideo) null else p.featured_image?.file ?: throw SiteChanged("No image"),
        pageUrl = p.link,
        isVideo = isVideo,
        explanation = explanation(p.content?.rendered.orEmpty()),
    )
}

/**
 * Earth Observatory posts, newest first. `featured_image_url` is a thumbnail: without its query
 * and the `_th` before the extension it's the post's picture.
 */
internal fun earthPosts(body: String): Sequence<Post> = wpPosts(body).asSequence().map { p ->
    val thumbnail = p.featured_image_url?.takeIf { it.isNotBlank() } ?: throw SiteChanged("No image")
    Post(
        source = Source.EARTH,
        date = wpDate(p.date),
        title = decodeEntities(p.title?.rendered.orEmpty()).trim(),
        imageUrl = thumbnail.substringBefore('?').replace(thumbnailSuffix, "$1"),
        pageUrl = p.link,
        isVideo = false,
        explanation = plainText(p.excerpt?.rendered.orEmpty()),
    )
}

private val thumbnailSuffix = Regex("""_th(\.\w+)$""")

/**
 * NASA Image of the Day feed items, newest first, read with regexes as the HTML is: the JDK's
 * XML parser needs different settings on Android to be safe. The feed has no CDATA.
 */
internal fun nasaPosts(body: String): Sequence<Post> = item.findAll(body).map { m ->
    val xml = m.groupValues[1]
    fun element(name: String) = Regex("<$name>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL).find(xml)?.groupValues?.get(1)
    val title = decodeEntities(element("title").orEmpty()).trim()
    val link = decodeEntities(element("link").orEmpty()).trim()
    val pubDate = element("pubDate")?.trim().orEmpty()
    if (title.isEmpty()) throw SiteChanged("No title")
    if (link.isEmpty()) throw SiteChanged("No link")
    Post(
        source = Source.NASA,
        date = try {
            ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME).withZoneSameInstant(eastern).toLocalDate()
        } catch (e: DateTimeException) {
            throw SiteChanged("Bad date: $pubDate", e)
        },
        title = title,
        imageUrl = enclosure.find(xml)?.groupValues?.get(1)?.let(::decodeEntities) ?: throw SiteChanged("No image"),
        pageUrl = link,
        isVideo = false,
        explanation = plainText(element("description").orEmpty()),
    )
}

private val item = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
private val enclosure = Regex("""<enclosure\s[^>]*?url="([^"]+)"""")
private val eastern = ZoneId.of("America/New_York")

private val titlePrefix = Regex("""^APOD:\s*\d{4}\s+\p{L}+(?:\s+\d{1,2})?\s*[-–—]\s*""")

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

/**
 * The hero's `media-detail-hero__description` paragraph up to its first `<br>` (after it come
 * site notes and "Tomorrow's picture"), as plain text without the "Explanation:" label.
 */
internal fun explanation(content: String): String {
    val start = content.indexOf("media-detail-hero__description").takeIf { it >= 0 } ?: return ""
    val from = content.indexOf('>', start) + 1
    val end = content.indexOf("</p>", from).takeIf { it >= 0 } ?: content.length
    val html = content.substring(from, end).split(lineBreak, limit = 2)[0]
    return plainText(html).removePrefix("Explanation:").trim()
}

private fun plainText(html: String) = decodeEntities(html.replace(tag, "")).replace(space, " ").trim()

private val lineBreak = Regex("""<br\s*/?>""")
private val tag = Regex("<[^>]*>")
private val space = Regex("""\s+""")

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
private class WpPost(
    val date: String,
    val link: String,
    val title: Rendered? = null,
    val content: Rendered? = null,
    val excerpt: Rendered? = null,
    val featured_image: Image? = null,
    val featured_image_url: String? = null,
)

@Serializable
private class Rendered(val rendered: String? = null)

@Serializable
private class Image(val file: String? = null)
