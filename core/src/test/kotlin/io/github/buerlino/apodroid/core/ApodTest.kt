package io.github.buerlino.apodroid.core

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApodTest {
    // Made-up posts shaped like the real response (a list with one post, many more fields).
    private fun post(
        title: String = "APOD: 2026 March 3 &#8211; A Made&#8217;Up Nebula",
        hero: String = "<figure><a href='https://assets.example/x.jpg'><img src='https://assets.example/x.jpg?w=1600'></a></figure>",
        explanation: String = "<a href='https://science.nasa.gov/mission/x/'>a mission</a>",
        // null leaves the key out, as on 16 Oct 2026 (a video day).
        image: String? = """{"id":7,"file":"https://assets.example/apod/2026/march/nebula.jpg","title":"Nebula"}""",
    ) = """[{"id":1,"date":"2026-03-03T00:05:00","date_gmt":"2026-03-03T05:05:00",
        "link":"https://science.nasa.gov/image-article/apod-2026-march-3-a-made-up-nebula/",
        "title":{"rendered":"$title"},
        "content":{"rendered":"<p>Discover the cosmos!</p><div class='media-detail-hero__media'>$hero</div><h1>Title</h1><p class='p-md media-detail-hero__description'><strong>Explanation:</strong> $explanation</p>"},
        "featured_image_url":"https://assets.example/apod/2026/march/nebula.jpg?w=1600"
        ${image?.let { ",\"featured_image\":$it" }.orEmpty()}}]"""

    @Test
    fun parsesTheFieldsTheAppUses() {
        val apod = parseApod(post())
        assertEquals(LocalDate.of(2026, 3, 3), apod.date)
        assertEquals("A Made’Up Nebula", apod.title)
        assertEquals("https://assets.example/apod/2026/march/nebula.jpg", apod.imageUrl)
        assertEquals("https://science.nasa.gov/image-article/apod-2026-march-3-a-made-up-nebula/", apod.pageUrl)
        assertFalse(apod.isVideo)
        assertEquals("a mission", apod.explanation)
    }

    @Test
    fun cleansTheExplanation() {
        val html = """
            A <a href='https://example.org/'>nebula</a> &amp; its <em>stars</em>&#8217;
            light.<br><br><strong>APOD&#8217;s site has moved</strong><br><strong>Tomorrow&#8217;s picture:</strong> dust
        """
        assertEquals("A nebula & its stars’ light.", parseApod(post(explanation = html)).explanation)
        // Some posts have the label as plain text, some none.
        val hero = "<p class='media-detail-hero__description'>"
        assertEquals("Plain label.", explanation("$hero Explanation: Plain label.</p>"))
        assertEquals("No label.", explanation("$hero No label.</p>"))
        assertEquals("", explanation("<p>No hero description</p>"))
    }

    @Test
    fun cleansTitles() {
        assertEquals("Moon – and Sun", cleanTitle("APOD: 2026 October 1 &#8211; Moon &#8211; and Sun"))
        assertEquals("Galaxy in Aries", cleanTitle("APOD: 2026 September 30 – Galaxy in Aries"))
        assertEquals("Shadow and Rainbow", cleanTitle("APOD: 2026 July 18 -Shadow and Rainbow"))
        assertEquals("Stars & Dust", cleanTitle("APOD: 2026 July 2 &#x2013; Stars &amp; Dust"))
        assertEquals("Caught in the Web", cleanTitle("APOD: 2026 April &#8211; Caught in the Web")) // no day, 3 Apr 2026
        assertEquals("No Prefix &#99999999;", cleanTitle("No Prefix &#99999999;"))
    }

    @Test
    fun makesSafeFileNames() {
        fun name(title: String) = Post(Source.APOD, LocalDate.of(2026, 3, 3), title, "", "", false).fileName
        assertEquals("APOD_2026-03-03_A_MadeUp_Nebula", name("A Made’Up Nebula"))
        assertEquals("APOD_2026-03-03_M31_Andromeda_Stars_Dust", name("M31: Andromeda / Stars & Dust?"))
        assertEquals("APOD_2026-03-03_Comète_Ōmura", name("Comète  Ōmura!"))
        assertEquals("APOD_2026-03-03", name(""))
        val long = name("Words ".repeat(50))
        assertEquals(99, long.length) // cut at 100, trailing "_" dropped
        assertTrue(long.endsWith("Words"))
    }

    @Test
    fun aSecondPictureOnTheSameDateIsAnotherPost() {
        val day = LocalDate.of(2026, 3, 3)
        val post = Post(Source.NASA, day, "Second", "https://example.org/b.jpg", "", false)
        assertTrue(post.isNoted(day, "https://example.org/b.jpg"))
        assertFalse(post.isNoted(day, "https://example.org/a.jpg"))
        assertFalse(post.isNoted(day.minusDays(1), "https://example.org/b.jpg"))
        assertFalse(post.isNoted(null, null))
        // Noted without a URL: 0.4.0's prefs, or the user's own picture.
        assertTrue(post.isNoted(day, null))
        assertTrue(post.copy(imageUrl = null, isVideo = true).isNoted(day, null))
    }

    @Test
    fun detectsVideoDaysFromTheHeroOnly() {
        val mp4 = "<video class='video-js'><source src='https://assets.example/x.mp4' type='video/mp4'></video>"
        val youtube = "<figure class='is-provider-youtube'><iframe src='https://www.youtube.com/embed/abc'></iframe></figure>"
        assertTrue(parseApod(post(hero = mp4)).isVideo)
        assertTrue(parseApod(post(hero = youtube)).isVideo)
        // No still frame on video days, also when the post has one.
        assertEquals(null, parseApod(post(hero = mp4)).imageUrl)
        assertEquals(null, parseApod(post(hero = mp4, image = null)).imageUrl)
        assertEquals(null, parseApod(post(hero = youtube, image = "{\"id\":0}")).imageUrl)
        // An image day that links (or even embeds) a video in the explanation.
        assertFalse(parseApod(post(explanation = "<a href='https://youtu.be/abc'>video</a>$youtube")).isVideo)
    }

    @Test
    fun withoutTheHeroMarkerLooksAboveTheTitle() {
        val youtube = "<iframe src='https://www.youtube.com/embed/abc'></iframe>"
        assertFalse(isVideo("<p>Discover the cosmos!</p><img src='x.jpg'><h1>Title</h1><p>$youtube</p>"))
        assertTrue(isVideo("<p>Discover the cosmos!</p>$youtube<h1>Title</h1>"))
        assertEquals("", explanation("<h1>Title</h1><p>Explanation: no marker</p>"))
    }

    @Test
    fun failsWithoutAPostOrAnImage() {
        assertFailsWith<SiteChanged> { parseApod("[]") }
        assertFailsWith<SiteChanged> { parseApod(post(image = "null")) }
        assertFailsWith<SiteChanged> { parseApod(post(image = null)) }
        assertFailsWith<SiteChanged> { parseApod(post(image = "{\"id\":0}")) }
    }

    @Test
    fun failsWhenTheSiteChanged() {
        assertFailsWith<SiteChanged> { parseApod("<html>Moved</html>") }
        assertFailsWith<SiteChanged> { parseApod("""{"code":"rest_no_route"}""") }
        assertFailsWith<SiteChanged> { parseApod("""[{"date":"2026-03-03T00:05:00"}]""") } // no link
        assertFailsWith<SiteChanged> { parseApod(post().replace("2026-03-03T00:05:00", "3 March 2026")) }
    }

    @Test
    fun fetchesFromTheServer() = withServer(200, post()) { url ->
        assertEquals("A Made’Up Nebula", fetchLatest(Source.APOD, UA, url).title)
    }

    @Test
    fun sendsTheUserAgent() = withServer(200, post()) { url ->
        fetchLatest(Source.APOD, UA, url)
        assertEquals(UA, userAgent)
    }

    @Test
    fun failsOnHttpErrors() = withServer(503, "") { url ->
        assertEquals(503, assertFailsWith<HttpStatus> { fetchLatest(Source.APOD, UA, url) }.code)
    }

    @Test
    fun failsWithoutTheServer() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val url = "http://127.0.0.1:${server.address.port}/"
        server.stop(0)
        assertNetworkFailure(assertFailsWith<IOException> { fetchLatest(Source.APOD, UA, url) })
    }

    @Test
    fun downloadsToAFile() = withServer(200, "made-up picture bytes") { url ->
        val file = File.createTempFile("apod", ".jpg")
        try {
            download(url, file, UA)
            assertEquals("made-up picture bytes", file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    fun deletesTheFileOnHttpErrors() = withServer(404, "") { url ->
        val file = File.createTempFile("apod", ".jpg")
        assertEquals(404, assertFailsWith<HttpStatus> { download(url, file, UA) }.code)
        assertFalse(file.exists())
    }

    @Test
    fun deletesTheFileWhenTheDownloadIsIncomplete() = withServer(200, "made-up", announced = 100) { url ->
        val file = File.createTempFile("apod", ".jpg")
        assertNetworkFailure(assertFailsWith<IOException> { download(url, file, UA) })
        assertFalse(file.exists())
    }

    @Test
    fun aFileThatCantBeWrittenIsAStorageFailureAfterTheRequest() = withServer(200, "made-up picture bytes") { url ->
        userAgent = null
        val file = File(File.createTempFile("apod", "").also { it.delete() }, "missing-folder/a.jpg")
        assertFailsWith<StorageFailed> { download(url, file, UA) }
        assertEquals(UA, userAgent)
        assertFalse(file.exists())
    }

    @Test
    fun anInterruptedThreadEndsTheDownload() = withServer(200, "made-up picture bytes") { url ->
        userAgent = null
        val file = File.createTempFile("apod", ".jpg")
        Thread.currentThread().interrupt()
        assertFailsWith<InterruptedIOException> { download(url, file, UA) }
        assertEquals(UA, userAgent)
        assertFalse(file.exists())
    }

    @Test
    fun aBadUrlIsASiteChangeAndNeverConnects() = withServer(200, "made-up picture bytes") { url ->
        val file = File.createTempFile("apod", ".jpg")
        userAgent = null
        assertFailsWith<SiteChanged> { download("$url/a b.jpg", file, UA) }
        assertFailsWith<SiteChanged> { download("example.org/a.jpg", file, UA) }
        assertEquals(null, userAgent)
        assertFalse(file.exists())
    }

    /** Neither of the typed failures: the app calls the rest a network failure. */
    private fun assertNetworkFailure(e: IOException) {
        assertFalse(e is SiteChanged || e is HttpStatus, "$e")
    }

    private val UA = "APODroid/0.0.0 (+https://example.org)"
    private var userAgent: String? = null

    /** [announced] is the Content-Length sent, by default the body's. */
    private fun withServer(status: Int, body: String, announced: Long? = null, block: (String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            userAgent = ex.requestHeaders.getFirst("User-Agent")
            val bytes = body.toByteArray()
            ex.sendResponseHeaders(status, announced ?: if (bytes.isEmpty()) -1 else bytes.size.toLong())
            runCatching { ex.responseBody.use { it.write(bytes) } } // Too few bytes: closing throws.
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}/wp-json/wp/v2/image-article")
        } finally {
            server.stop(0)
        }
    }
}
