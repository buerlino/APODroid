package io.github.buerlino.apodroid.core

import java.util.Locale

// 0.9.0, Auto's turn (sharp-0.9.0.md, S1 and The rules, hasUp): whether NASA's text for an APOD says
// its picture has an "up" (a horizon, ground, trees, people, Saturn, the Moon, text…), so Size Auto
// keeps it upright. A word table learned from the user's 300 labels (Bernoulli naive Bayes, built by
// private/up-words-build.py), the cautious threshold. SH1; the app in SH2a.

/** The table's path among `:core`'s resources, absolute (R8 may rename this file's package). */
private const val TABLE = "/io/github/buerlino/apodroid/core/up-words.tsv"

/** The table: the prior, the threshold and each word's weight (`word<TAB>weight`), read once. */
internal class UpTable(val prior: Double, val threshold: Double, val weights: Map<String, Double>)

internal val upTable: UpTable by lazy {
    val text = UpTable::class.java.getResourceAsStream(TABLE)?.use { it.readBytes().toString(Charsets.UTF_8) }
        ?: error("$TABLE is missing")
    parseUpTable(text)
}

/** `# … prior -0.817296; threshold -2.0` first, then a word and its weight per line. */
internal fun parseUpTable(text: String): UpTable {
    val lines = text.lines()
    val header = lines.first()
    fun number(name: String) = Regex("""$name (-?[0-9.]+)""").find(header)?.groupValues?.get(1)?.toDouble()
        ?: error("No $name in the header: $header")
    val weights = HashMap<String, Double>(lines.size * 2)
    for (line in lines.drop(1)) {
        if (line.isBlank() || line.startsWith("#")) continue
        val tab = line.indexOf('\t')
        weights[line.substring(0, tab)] = line.substring(tab + 1).trim().toDouble()
    }
    return UpTable(number("prior"), number("threshold"), weights)
}

/** Words the table never counts: the builder's `STOP`, copied exactly. */
private val upStop = (
    "a an the of in and to is on at by for with from as this that it its are be was were or which into near " +
        "over than these those has have can seen image picture featured shown appears apod see explanation"
    ).split(' ').toSet()

/** Dates, seasons and places, dropped so the table holds without them: the builder's `DROP`, copied exactly. */
private val upDrop = (
    "january february march april may june july august september october november december spring summer " +
        "autumn fall winter italy spain chile usa china germany france canada australia arizona california " +
        "utah hawaii norway iceland greenland sunday monday tuesday wednesday thursday friday saturday year " +
        "years week month months day days yesterday tonight today last next"
    ).split(' ').toSet()

private val upWord = Regex("[a-z]+")

/** The explanation's first characters the table read (Python's `[:600]`, by code points). */
private const val UP_EXPLANATION = 600

/**
 * The text the table reads: the [title], then the [alt] text unless it starts with "See Explanation"
 * (NASA's placeholder before about Dec 2025; any case), then the [explanation]'s first 600 characters,
 * joined by spaces.
 */
internal fun upText(title: String, alt: String?, explanation: String): String {
    val parts = mutableListOf(title)
    if (alt != null && !alt.lowercase(Locale.ROOT).startsWith("see explanation")) parts += alt
    parts += explanation.takeCodePoints(UP_EXPLANATION)
    return parts.joinToString(" ")
}

private fun String.takeCodePoints(n: Int): String =
    if (codePointCount(0, length) <= n) this else substring(0, offsetByCodePoints(0, n))

/** The words of [text]: lower-cased runs of a–z longer than 2 letters, without the stop and dropped words, each once. */
internal fun upWords(text: String): Set<String> =
    upWord.findAll(text.lowercase(Locale.ROOT)).map { it.value }
        .filter { it.length > 2 && it !in upStop && it !in upDrop }
        .toSet()

/** The table's score of a post's text: the prior plus the weight of each distinct word it knows (unknown words 0). */
fun upScore(title: String, alt: String?, explanation: String): Double {
    val table = upTable
    return table.prior + upWords(upText(title, alt, explanation)).sumOf { table.weights[it] ?: 0.0 }
}

/**
 * Whether a post's picture has an "up", so Size Auto keeps it upright: [upScore] above the threshold
 * (−2.0). No text, or only words the table doesn't know, is an up (the prior, −0.82).
 */
fun hasUp(title: String, alt: String?, explanation: String): Boolean =
    upScore(title, alt, explanation) > upTable.threshold
