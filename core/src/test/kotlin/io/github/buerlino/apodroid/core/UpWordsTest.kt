package io.github.buerlino.apodroid.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// SH1 (sharp-0.9.0.md, Testing 1, hasUp): the builder's expected scores, the tokenizer's edges, and the
// user's 300 labelled posts in the gitignored private/ (skipped without).
class UpWordsTest {
    private fun score(title: String, alt: String? = null, explanation: String = "") = upScore(title, alt, explanation)

    @Test
    fun theTablesHeader() {
        assertEquals(-0.817296, upTable.prior)
        assertEquals(-2.0, upTable.threshold)
        assertEquals(3915, upTable.weights.size)
        assertEquals(2.063693, upTable.weights["saturn"])
        assertEquals(-3.299944, upTable.weights["ngc"])
    }

    /** The builder's scores (±1e-4). */
    @Test
    fun theExpectedScores() {
        assertEquals(10.3718, score("Trees and a lake below the horizon at sunset"), 1e-4)
        assertEquals(-8.5074, score("A spiral galaxy and its nebulae in a field of stars"), 1e-4)
        assertEquals(2.1526, score("Saturn and its rings"), 1e-4)
        assertEquals(-0.8173, score(""), 1e-4)
        assertEquals(-0.8173, score("Zzzz qqq"), 1e-4)
        assertTrue(hasUp("Trees and a lake below the horizon at sunset", null, ""))
        assertFalse(hasUp("A spiral galaxy and its nebulae in a field of stars", null, ""))
        assertTrue(hasUp("Saturn and its rings", null, ""))
        assertTrue(hasUp("", null, ""), "no text stays upright")
        assertTrue(hasUp("Zzzz qqq", null, ""), "unknown words stay upright")
    }

    @Test
    fun theTokenizer() {
        assertEquals(setOf("saturn", "rings"), upWords("SATURN's Rings, 2026 in Chile"), "case, digits, a dropped place, an apostrophe's s")
        assertEquals(setOf("ngc", "hubble", "view"), upWords("NGC 1234: Hubble’s view"), "a curly apostrophe")
        assertEquals(setOf("rings"), upWords("rings Rings RINGS"), "each word once")
        assertEquals(emptySet(), upWords("of the a an it is to on by The APOD Explanation see"), "stop words")
        assertEquals(emptySet(), upWords("November winter Sunday last year"), "dropped words")
        assertEquals(setOf("gal", "axy"), upWords("gal4axy"), "digits split a word")
        assertEquals(setOf("moon", "aurora"), upWords("Moon-aurora ab xy"), "a hyphen splits, two letters drop")
        assertEquals(setOf("caf", "borealis"), upWords("café borealis"), "a–z only")
        assertEquals(score("Saturn"), score("SATURN"))
    }

    @Test
    fun thePlaceholderAlt() {
        val placeholder = "See Explanation.  Clicking on the picture will download the highest resolution version available."
        assertTrue(upWords(placeholder).isNotEmpty(), "the placeholder has words of its own")
        assertEquals(score("Saturn"), score("Saturn", placeholder), 0.0, "the placeholder isn't read")
        assertEquals(score("Saturn"), score("Saturn", "SEE EXPLANATION: horizon trees"), 0.0, "any case")
        assertEquals(score("Saturn horizon trees"), score("Saturn", "Horizon trees"), 1e-12, "a real alt is read")
        assertEquals(score("Saturn"), score("Saturn", null), 0.0)
        assertEquals(score("Saturn"), score("Saturn", ""), 0.0)
    }

    /** The explanation's first 600 characters only, counted by code points as Python's `[:600]`. */
    @Test
    fun theExplanationsFirst600Characters() {
        assertFalse("satur" in upTable.weights)
        val saturn = score("", null, "saturn")
        assertEquals(saturn, score("", null, "x".repeat(593) + " saturn"), 1e-12, "600 characters: all of it")
        assertEquals(upTable.prior, score("", null, "x".repeat(594) + " saturn"), 1e-12, "601: \"satur\"")
        assertEquals(saturn, score("", null, "🌌".repeat(593) + " saturn"), 1e-12, "code points, not UTF-16 units")
        assertEquals(upTable.prior, score("", null, "🌌".repeat(594) + " saturn"), 1e-12)
        assertEquals(score("Saturn"), score("Saturn", null, "x".repeat(600) + " nebula galaxy"), 1e-12, "the title isn't counted in the 600")
    }

    @Test
    fun theTableParses() {
        val t = parseUpTable("# words; prior -1.5; threshold -2.0\nhorizon\t2.5\n\nngc\t-3\n")
        assertEquals(-1.5, t.prior)
        assertEquals(-2.0, t.threshold)
        assertEquals(mapOf("horizon" to 2.5, "ngc" to -3.0), t.weights)
    }

    /** The user's 300 labels (S1): in-sample the table turns none they wanted upright and 162 of the 196 OK sideways. */
    @Test
    fun theLabelledPosts() {
        val posts = File("../private/apod-landscape-300-2026-11-16.json")
        val labels = File("../private/up-labels-2026-11-17.json")
        assumeTrue(posts.exists() && labels.exists())
        val byDate = (Json.parseToJsonElement(posts.readText()) as JsonArray).associate {
            val o = it.jsonObject
            o.string("date") to o
        }
        val answers = (Json.parseToJsonElement(labels.readText()) as JsonObject).mapValues { it.value.jsonPrimitive.content }
        assertEquals(300, answers.size)
        var wronglyTurned = 0
        var turned = 0
        var upright = 0
        var ok = 0
        for ((date, answer) in answers) {
            if (answer == "unsure") continue
            val p = byDate.getValue(date)
            val up = answer == "no"
            val turns = !hasUp(p.string("title"), p.string("alt"), p.string("explanation"))
            if (up) {
                upright++
                if (turns) wronglyTurned++
            } else {
                ok++
                if (turns) turned++
            }
        }
        assertEquals(86, upright)
        assertEquals(196, ok)
        assertEquals(0, wronglyTurned)
        assertEquals(162, turned)
    }

    private fun JsonObject.string(key: String) = (get(key) as? JsonPrimitive)?.content.orEmpty()
}
