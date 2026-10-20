package io.github.buerlino.apodroid.core

import org.junit.Assume.assumeTrue
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SourcesTest {
    // Made-up feed items shaped like the real feed (www.nasa.gov/feeds/iotd-feed/).
    private fun item(
        title: String = "A Made-Up Launch",
        pubDate: String = "Fri, 16 Oct 2026 16:33 GMT",
        enclosure: String? = """<enclosure url="https://www.nasa.gov/wp-content/uploads/2026/10/launch.jpg" length="2514968" type="image/jpeg" />""",
        link: String = "https://www.nasa.gov/image-detail/made-up-launch/",
    ) = """
		<item>
			<title>$title</title>
			<link>$link</link>
			<description>A rocket&#039;s plume over   a made-up pad.</description>
			${enclosure.orEmpty()}
			<guid isPermaLink="false">$link</guid>
			<pubDate>$pubDate</pubDate>
			<source url="https://www.nasa.gov/feeds/iotd-feed/">NASA Image of the Day</source>
		</item>"""

    private fun feed(vararg items: String) = """<?xml version="1.0" encoding="UTF-8" ?>
<rss version="2.0"><channel>
	<title>NASA Image of the Day</title>
	<link>https://www.nasa.gov/image-of-the-day/</link>
	${items.joinToString("")}
</channel></rss>"""

    @Test
    fun readsTheNewestFeedItem() {
        val post = parseLatest(Source.NASA, feed(item(), item(title = "Older", pubDate = "Thu, 15 Oct 2026 14:20 GMT")))
        assertEquals(Source.NASA, post.source)
        assertEquals(LocalDate.of(2026, 10, 16), post.date)
        assertEquals("A Made-Up Launch", post.title)
        assertEquals("https://www.nasa.gov/wp-content/uploads/2026/10/launch.jpg", post.imageUrl)
        assertEquals("https://www.nasa.gov/image-detail/made-up-launch/", post.pageUrl)
        assertEquals("A rocket's plume over a made-up pad.", post.explanation)
        assertEquals(false, post.isVideo)
    }

    @Test
    fun cleansFeedTitles() {
        assertEquals("Origin of 'Treble Clef' Waves", parseLatest(Source.NASA, feed(item(title = "Origin of &#039;Treble Clef&#039; Waves"))).title)
        assertEquals("Leading Space", parseLatest(Source.NASA, feed(item(title = " Leading Space"))).title)
    }

    @Test
    fun takesTheEnclosureAsIs() {
        // A WebP typed image/jpeg in the real feed.
        val webp = """<enclosure url="https://www.nasa.gov/wp-content/uploads/2026/09/nebula.webp" length="900" type="image/jpeg" />"""
        assertEquals("https://www.nasa.gov/wp-content/uploads/2026/09/nebula.webp", parseLatest(Source.NASA, feed(item(enclosure = webp))).imageUrl)
    }

    @Test
    fun turnsThePubDateIntoTheUsEasternDate() {
        fun date(pubDate: String) = parseLatest(Source.NASA, feed(item(pubDate = pubDate))).date
        assertEquals(LocalDate.of(2026, 10, 15), date("Fri, 16 Oct 2026 03:00 GMT"))
        assertEquals(LocalDate.of(2026, 10, 16), date("Fri, 16 Oct 2026 16:33:12 GMT"))
        assertEquals(LocalDate.of(2026, 10, 2), date("Fri, 2 Oct 2026 14:00 GMT"))
    }

    @Test
    fun failsWhenTheFeedChanged() {
        assertFailsWith<SiteChanged> { parseLatest(Source.NASA, feed()) }
        assertFailsWith<SiteChanged> { parseLatest(Source.NASA, "<html>Moved</html>") }
        assertFailsWith<SiteChanged> { parseLatest(Source.NASA, feed(item(enclosure = null))) }
        assertFailsWith<SiteChanged> { parseLatest(Source.NASA, feed(item(title = ""))) }
        assertFailsWith<SiteChanged> { parseLatest(Source.NASA, feed(item(link = ""))) }
        assertFailsWith<SiteChanged> { parseLatest(Source.NASA, feed(item(pubDate = "16 October 2026"))) }
    }

    // A made-up Earth Observatory post shaped like the real response (with _fields).
    private fun earth(
        title: String = "Smoke &#038; Clouds over a Made-Up Coast",
        image: String? = "https://assets.science.nasa.gov/dynamicimage/assets/science/esd/eo/images/iotd/2026/coast/coast_20261011_th.jpg?w=720&h=480&fit=clip&crop=faces%2Cfocalpoint",
    ) = """[{"date":"2026-10-16T00:00:00","link":"https://science.nasa.gov/earth/earth-observatory/made-up-coast/",
        "title":{"rendered":"$title"},
        "excerpt":{"rendered":"<p>Satellites saw smoke drift over a <em>made-up</em> coast.</p>\n","protected":false}
        ${image?.let { ",\"featured_image_url\":\"$it\"" }.orEmpty()}}]"""

    @Test
    fun readsTheNewestEarthObservatoryPost() {
        val post = parseLatest(Source.EARTH, earth())
        assertEquals(Source.EARTH, post.source)
        assertEquals(LocalDate.of(2026, 10, 16), post.date)
        assertEquals("Smoke & Clouds over a Made-Up Coast", post.title)
        assertEquals("https://assets.science.nasa.gov/dynamicimage/assets/science/esd/eo/images/iotd/2026/coast/coast_20261011.jpg", post.imageUrl)
        assertEquals("https://science.nasa.gov/earth/earth-observatory/made-up-coast/", post.pageUrl)
        assertEquals("Satellites saw smoke drift over a made-up coast.", post.explanation)
    }

    @Test
    fun keepsAnEarthPictureWithoutTh() {
        val url = "https://assets.science.nasa.gov/dynamicimage/assets/science/esd/eo/images/iotd/2026/coast/coast_thumb.png"
        assertEquals(url, parseLatest(Source.EARTH, earth(image = "$url?w=720")).imageUrl)
    }

    @Test
    fun failsWithoutAnEarthPostOrPicture() {
        assertFailsWith<SiteChanged> { parseLatest(Source.EARTH, "[]") }
        assertFailsWith<SiteChanged> { parseLatest(Source.EARTH, earth(image = null)) }
        assertFailsWith<SiteChanged> { parseLatest(Source.EARTH, earth(image = "")) }
    }

    @Test
    fun makesRenditionUrls() {
        val apod = "https://assets.science.nasa.gov/dynamicimage/assets/science/cds/apod/apod/2026/october/Harvest%20Moon%20Trail.jpg"
        assertEquals("$apod?h=2484", renditionUrl(apod, 2484))
        assertEquals("$apod?h=1340", renditionUrl("$apod?w=1600&fit=clip", 1340))
        val nasa = "https://www.nasa.gov/wp-content/uploads/2026/10/launch.jpg"
        assertEquals("$nasa?h=2484", renditionUrl(nasa, 2484))
        val other = "https://images-assets.nasa.gov/image/NHQ202606170001/NHQ202606170001~large.jpg"
        assertEquals(other, renditionUrl(other, 2484))
    }

    @Test
    fun findsTheOriginals() {
        fun post(source: Source, url: String?) = Post(source, LocalDate.of(2026, 10, 1), "", url, "", url == null)
        assertEquals(
            "https://assets.science.nasa.gov/content/dam/science/cds/apod/apod/2026/october/Moon%20Trail.jpg",
            originalUrl(post(Source.APOD, "https://assets.science.nasa.gov/dynamicimage/assets/science/cds/apod/apod/2026/october/Moon%20Trail.jpg")),
        )
        assertEquals(
            "https://assets.science.nasa.gov/content/dam/science/esd/eo/images/iotd/2026/coast/coast.jpg",
            originalUrl(post(Source.EARTH, "https://assets.science.nasa.gov/dynamicimage/assets/science/esd/eo/images/iotd/2026/coast/coast.jpg?w=720")),
        )
        val nasa = "https://www.nasa.gov/wp-content/uploads/2026/10/launch.jpg"
        assertEquals(nasa, originalUrl(post(Source.NASA, nasa)))
        assertEquals(null, originalUrl(post(Source.APOD, null)))
    }

    @Test
    fun startsFileNamesWithTheSource() {
        fun name(source: Source) = Post(source, LocalDate.of(2026, 3, 3), "A Made-Up Picture", "", "", false).fileName
        assertEquals("APOD_2026-03-03_A_Made_Up_Picture", name(Source.APOD))
        assertEquals("NASA_2026-03-03_A_Made_Up_Picture", name(Source.NASA))
        assertEquals("EO_2026-03-03_A_Made_Up_Picture", name(Source.EARTH))
    }

    /** The real responses in the gitignored `private/`, when they're there (not in CI). */
    @Test
    fun readsEveryRealPost() {
        val feed = File("../private/iotd-feed-2026-10-16.xml")
        val eo = File("../private/eo-last40-2026-10-16.json")
        val apod = File("../private/last100.json")
        assumeTrue(feed.exists() && eo.exists() && apod.exists())
        for ((posts, count) in listOf(nasaPosts(feed.readText()).toList() to 60, earthPosts(eo.readText()).toList() to 40)) {
            assertEquals(count, posts.size)
            for (p in posts) {
                assertTrue(p.title.isNotEmpty() && p.title == p.title.trim() && '&' !in p.title.replace(" & ", ""), p.title)
                val url = p.imageUrl!!
                assertTrue(url.startsWith("https://") && "_th." !in url && '?' !in url, url)
                assertTrue(p.pageUrl.startsWith("https://"), p.pageUrl)
                assertTrue(p.explanation.length > 50 && '<' !in p.explanation, p.explanation)
                assertTrue(p.date.year == 2026, "${p.date}")
                assertEquals(null, p.credit)
            }
        }
        val apods = apodPosts(apod.readText()).toList()
        assertEquals(100, apods.size)
        for (p in apods) {
            val credit = p.credit!!
            assertTrue(Regex("""^[\p{L} &]*Credit[\p{L} &]*: \S""").containsMatchIn(credit), credit)
            assertTrue(Regex("""<|&\w+;|\s[,;:)]|,;|\(\s|;\s*;|\s\s|::""").find(credit) == null, credit)
            assertTrue(credit == credit.trim(' ', ';'), credit)
        }
    }
}
