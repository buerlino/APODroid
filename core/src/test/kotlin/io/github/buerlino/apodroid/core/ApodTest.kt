package io.github.buerlino.apodroid.core

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
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
        image: String = """{"id":7,"file":"https://assets.example/apod/2026/march/nebula.jpg","title":"Nebula"}""",
    ) = """[{"id":1,"date":"2026-03-03T00:05:00","date_gmt":"2026-03-03T05:05:00",
        "link":"https://science.nasa.gov/image-article/apod-2026-march-3-a-made-up-nebula/",
        "title":{"rendered":"$title"},
        "content":{"rendered":"<p>Discover the cosmos!</p><div class='media-detail-hero__media'>$hero</div><h1>Title</h1><p><strong>Explanation:</strong> $explanation</p>"},
        "featured_image_url":"https://assets.example/apod/2026/march/nebula.jpg?w=1600",
        "featured_image":$image}]"""

    @Test
    fun parsesTheFieldsTheAppUses() {
        val apod = parseLatest(post())
        assertEquals(LocalDate.of(2026, 3, 3), apod.date)
        assertEquals("A Made’Up Nebula", apod.title)
        assertEquals("https://assets.example/apod/2026/march/nebula.jpg", apod.imageUrl)
        assertEquals("https://science.nasa.gov/image-article/apod-2026-march-3-a-made-up-nebula/", apod.pageUrl)
        assertFalse(apod.isVideo)
    }

    @Test
    fun cleansTitles() {
        assertEquals("Moon – and Sun", cleanTitle("APOD: 2026 October 1 &#8211; Moon &#8211; and Sun"))
        assertEquals("Galaxy in Aries", cleanTitle("APOD: 2026 September 30 – Galaxy in Aries"))
        assertEquals("Shadow and Rainbow", cleanTitle("APOD: 2026 July 18 -Shadow and Rainbow"))
        assertEquals("Stars & Dust", cleanTitle("APOD: 2026 July 2 &#x2013; Stars &amp; Dust"))
        assertEquals("No Prefix &#99999999;", cleanTitle("No Prefix &#99999999;"))
    }

    @Test
    fun makesSafeFileNames() {
        fun name(title: String) = Apod(LocalDate.of(2026, 3, 3), title, "", "", false).fileName
        assertEquals("APOD_2026-03-03_A_MadeUp_Nebula", name("A Made’Up Nebula"))
        assertEquals("APOD_2026-03-03_M31_Andromeda_Stars_Dust", name("M31: Andromeda / Stars & Dust?"))
        assertEquals("APOD_2026-03-03_Comète_Ōmura", name("Comète  Ōmura!"))
        assertEquals("APOD_2026-03-03", name(""))
        val long = name("Words ".repeat(50))
        assertEquals(99, long.length) // cut at 100, trailing "_" dropped
        assertTrue(long.endsWith("Words"))
    }

    @Test
    fun detectsVideoDaysFromTheHeroOnly() {
        val mp4 = "<video class='video-js'><source src='https://assets.example/x.mp4' type='video/mp4'></video>"
        val youtube = "<figure class='is-provider-youtube'><iframe src='https://www.youtube.com/embed/abc'></iframe></figure>"
        assertTrue(parseLatest(post(hero = mp4)).isVideo)
        assertTrue(parseLatest(post(hero = youtube)).isVideo)
        // An image day that links (or even embeds) a video in the explanation.
        assertFalse(parseLatest(post(explanation = "<a href='https://youtu.be/abc'>video</a>$youtube")).isVideo)
    }

    @Test
    fun failsWithoutAPostOrAnImage() {
        assertFailsWith<IOException> { parseLatest("[]") }
        assertFailsWith<IOException> { parseLatest(post(image = "null")) }
        assertFailsWith<IOException> { parseLatest(post(image = "{\"id\":0}")) }
    }

    @Test
    fun fetchesFromTheServer() = withServer(200, post()) { url ->
        assertEquals("A Made’Up Nebula", fetchLatest(url).title)
    }

    @Test
    fun failsOnHttpErrors() = withServer(503, "") { url ->
        assertFailsWith<IOException> { fetchLatest(url) }
    }

    @Test
    fun downloadsToAFile() = withServer(200, "made-up picture bytes") { url ->
        val file = File.createTempFile("apod", ".jpg")
        try {
            download(url, file)
            assertEquals("made-up picture bytes", file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    fun deletesTheFileOnHttpErrors() = withServer(404, "") { url ->
        val file = File.createTempFile("apod", ".jpg")
        assertFailsWith<IOException> { download(url, file) }
        assertFalse(file.exists())
    }

    private fun withServer(status: Int, body: String, block: (String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            val bytes = body.toByteArray()
            ex.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}/wp-json/wp/v2/image-article")
        } finally {
            server.stop(0)
        }
    }
}
