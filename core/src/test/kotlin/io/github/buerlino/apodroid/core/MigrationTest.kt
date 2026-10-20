package io.github.buerlino.apodroid.core

import kotlin.test.Test
import kotlin.test.assertEquals

class MigrationTest {
    private val url = "https://x/APOD-2026-10-19.jpg"

    /** 0.5.0's prefs with a set on [date] of [source]'s picture, while [sources] were ticked. */
    private fun old(sources: String, where: String, source: String = "APOD", date: String? = "2026-10-19", url: String? = this.url) =
        listOfNotNull(
            "sources" to sources,
            "where" to where,
            "wallpaperSource" to source,
            date?.let { "wallpaperDate" to it },
            url?.let { "wallpaperUrl" to it },
            "wallpaperSetAt" to "1000",
            "turnStartedAt" to "900",
        ).toMap()

    private fun record(owner: String, picture: String = owner, url: String? = this.url) =
        listOfNotNull(
            "wallpaperSource" to owner,
            "wallpaperPicture" to picture,
            "wallpaperDate" to "2026-10-19",
            url?.let { "wallpaperUrl" to it },
            "wallpaperStamp" to 900L,
            "wallpaperSetAt" to 1000L,
        ).toMap()

    private fun lock(record: Map<String, Any>) = record.mapKeys { "lock.${it.key}" }

    @Test
    fun apodAloneBoth() {
        val r = record("APOD")
        assertEquals(Migration(mapOf("allSources" to false) + r + lock(r), emptySet()), migrate(old("APOD", "BOTH"), false))
    }

    @Test
    fun apodAloneHomeLeavesTheLockScreenUnknown() {
        assertEquals(Migration(mapOf("allSources" to false) + record("APOD"), emptySet()), migrate(old("APOD", "HOME"), false))
    }

    @Test
    fun apodAloneLockMovesTheRecordToTheLockScreen() {
        assertEquals(Migration(mapOf("allSources" to false) + lock(record("APOD")), homeRecordKeys), migrate(old("APOD", "LOCK"), false))
    }

    @Test
    fun nasaAloneTurnsTheSwitchOn() {
        val r = record("NASA")
        val columns = mapOf("allSources" to true, "home" to "NASA", "lock" to "NASA")
        assertEquals(Migration(columns + r + lock(r), emptySet()), migrate(old("NASA", "BOTH", "NASA"), false))
        assertEquals(
            Migration(mapOf("allSources" to true, "home" to "NASA", "lock" to "") + r, emptySet()),
            migrate(old("NASA", "HOME", "NASA"), false),
        )
        assertEquals(
            Migration(mapOf("allSources" to true, "home" to "", "lock" to "NASA") + lock(r), homeRecordKeys),
            migrate(old("NASA", "LOCK", "NASA"), false),
        )
    }

    @Test
    fun twoSourcesGoInTheColumnsWhereNames() {
        val r = record("EARTH")
        val old = { where: String -> old("APOD,EARTH", where, "EARTH") }
        assertEquals(
            Migration(mapOf("allSources" to true, "home" to "APOD,EARTH", "lock" to "APOD,EARTH") + r + lock(r), emptySet()),
            migrate(old("BOTH"), false),
        )
        assertEquals(
            Migration(mapOf("allSources" to true, "home" to "APOD,EARTH", "lock" to "") + r, emptySet()),
            migrate(old("HOME"), false),
        )
        assertEquals(
            Migration(mapOf("allSources" to true, "home" to "", "lock" to "APOD,EARTH") + lock(r), homeRecordKeys),
            migrate(old("LOCK"), false),
        )
    }

    @Test
    fun theOwnPictureOnAVideoDate() {
        val r = record("APOD", OWN_PICTURE, url = null)
        assertEquals(Migration(mapOf("allSources" to false) + r + lock(r), emptySet()), migrate(old("APOD", "BOTH", url = null), true))
        // With turns too (0.5.0 had none there, but the record says what it showed).
        val on = mapOf("allSources" to true, "home" to "APOD,NASA", "lock" to "")
        assertEquals(Migration(on + r, emptySet()), migrate(old("APOD,NASA", "HOME", url = null), true))
    }

    @Test
    fun aMissingUrlOnAnImageDayIsApodsPicture() {
        // 0.4.0's record (no URL, no source): matched by date.
        val r = record("APOD", url = null)
        val old = old("APOD", "HOME", url = null) - "wallpaperSource"
        assertEquals(Migration(mapOf("allSources" to false) + r, emptySet()), migrate(old, false))
    }

    @Test
    fun apodAloneWithNasaStandingInIsOwnedByApod() {
        val r = record("APOD", picture = "NASA")
        assertEquals(Migration(mapOf("allSources" to false) + r + lock(r), emptySet()), migrate(old("APOD", "BOTH", "NASA"), true))
    }

    @Test
    fun aForgottenRecordKeepsItsOwner() {
        // 0.5.0's switch going on dropped the date.
        val r = mapOf("wallpaperSource" to "NASA", "wallpaperStamp" to 900L, "wallpaperSetAt" to 1000L)
        assertEquals(
            Migration(mapOf("allSources" to true, "home" to "APOD,NASA", "lock" to "APOD,NASA") + r + lock(r), emptySet()),
            migrate(old("APOD,NASA", "BOTH", "NASA", date = null, url = null), false),
        )
    }

    @Test
    fun aFreshInstallWritesOnlyTheMode() {
        assertEquals(Migration(mapOf("allSources" to false), emptySet()), migrate(emptyMap(), false))
    }

    @Test
    fun missingKeysDontThrow() {
        val full = old("APOD,NASA", "LOCK")
        for (key in full.keys) migrate(full - key, false)
        for (key in full.keys) migrate(full - key, true)
        // Bad values too.
        migrate(mapOf("sources" to "X,,", "where" to "?", "wallpaperSource" to "?", "turnStartedAt" to "x", "wallpaperSetAt" to ""), true)
        // Without "where": Both, as 0.5.0 read it.
        val r = record("APOD")
        assertEquals(Migration(mapOf("allSources" to false) + r + lock(r), emptySet()), migrate(old("APOD", "BOTH") - "where", false))
        // Without "sources": APOD alone.
        assertEquals(Migration(mapOf("allSources" to false) + r + lock(r), emptySet()), migrate(old("APOD", "BOTH") - "sources", false))
    }
}
