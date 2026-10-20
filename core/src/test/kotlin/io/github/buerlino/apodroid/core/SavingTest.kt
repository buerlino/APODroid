package io.github.buerlino.apodroid.core

import io.github.buerlino.apodroid.core.Screen.HOME
import io.github.buerlino.apodroid.core.Screen.LOCK
import io.github.buerlino.apodroid.core.Source.APOD
import io.github.buerlino.apodroid.core.Source.EARTH
import io.github.buerlino.apodroid.core.Source.NASA
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SavingTest {
    private val d1 = LocalDate.parse("2026-10-19")
    private val d2 = LocalDate.parse("2026-10-20")
    private val all = Source.entries

    private fun post(source: Source, date: LocalDate = d2, video: Boolean = false) =
        Post(source, date, "$source $date", if (video) null else "https://x/$source-$date.jpg", "https://p/$source-$date", video)

    private val posts = all.associateWith { post(it) }

    private fun stored(posts: Map<Source, Post> = this.posts, files: Set<Source> = posts.keys, standIn: StandIn = StandIn.SKIP) =
        Stored(posts, files, true, standIn)

    /** The posts [toSave] picks, none tried before. */
    private fun toSave(inUse: List<Source>, stored: Stored, saved: Map<Source, Picture.Of>) =
        toSave(inUse, stored, saved, emptyMap()).map { it.post }

    private fun cols(home: List<Source>, lock: List<Source> = home) = mapOf(HOME to home, LOCK to lock)

    // The sources in use

    @Test
    fun theTickedOnesOnEitherScreenInOrder() {
        assertEquals(listOf(APOD, EARTH), sourcesInUse(cols(listOf(EARTH), listOf(APOD)), emptyList(), null, false))
        assertEquals(emptyList(), sourcesInUse(cols(emptyList()), emptyList(), null, false))
    }

    @Test
    fun aHeldPicturesSourceIsInUse() {
        assertEquals(listOf(APOD, NASA), sourcesInUse(cols(listOf(APOD)), listOf(NASA), null, false))
    }

    @Test
    fun theStandInOnlyOnAnApodVideoDayWithApodTickedOrHeld() {
        assertEquals(listOf(APOD, NASA), sourcesInUse(cols(listOf(APOD)), emptyList(), NASA, true))
        assertEquals(listOf(APOD), sourcesInUse(cols(listOf(APOD)), emptyList(), NASA, false))
        assertEquals(listOf(EARTH), sourcesInUse(cols(listOf(EARTH)), emptyList(), NASA, true))
        assertEquals(listOf(APOD, NASA, EARTH), sourcesInUse(cols(listOf(EARTH)), listOf(APOD), NASA, true))
    }

    // What is saved

    @Test
    fun eachSourceInUseInOrder() {
        assertEquals(listOf(posts[APOD], posts[EARTH]), toSave(listOf(EARTH, APOD), stored(), emptyMap()))
    }

    @Test
    fun notAVideoNorAPostWithoutItsPicture() {
        val video = posts + (APOD to post(APOD, video = true))
        assertEquals(listOf(posts[NASA]), toSave(all, stored(video, files = setOf(NASA)), emptyMap()))
    }

    @Test
    fun notOneSavedBefore() {
        val saved = mapOf(APOD to posts.getValue(APOD).picture(), NASA to post(NASA, d1).picture())
        assertEquals(listOf(posts[NASA], posts[EARTH]), toSave(all, stored(), saved))
        // Another picture on the same date counts as unsaved; a save noted without a URL (0.4.0) matches by date.
        val other = posts + (APOD to posts.getValue(APOD).copy(imageUrl = "https://x/second.jpg"))
        assertEquals(listOf(other[APOD]), toSave(listOf(APOD), stored(other), saved))
        assertEquals(emptyList(), toSave(listOf(APOD), stored(other), mapOf(APOD to Picture.Of(APOD, d2, null))))
    }

    @Test
    fun onAnApodVideoDayTheStandInIsSavedAsItself() {
        val video = posts + (APOD to post(APOD, video = true))
        val stored = stored(video, files = setOf(NASA, EARTH), standIn = StandIn.NASA)
        val inUse = sourcesInUse(cols(listOf(APOD)), emptyList(), NASA, true)
        assertEquals(listOf(posts[NASA]), toSave(inUse, stored, emptyMap()))
        // "Skip it" or the user's own picture: nothing.
        for (standIn in listOf(StandIn.SKIP, StandIn.OWN)) {
            assertEquals(emptyList(), toSave(sourcesInUse(cols(listOf(APOD)), emptyList(), null, true), stored(video, setOf(NASA, EARTH), standIn), emptyMap()))
        }
    }

    // The original once per post

    @Test
    fun theOriginalFirstThenTheStoredPictureAfterATry() {
        assertEquals(listOf(ToSave(posts.getValue(APOD), true)), toSave(listOf(APOD), stored(), emptyMap(), emptyMap()))
        val tried = mapOf(APOD to posts.getValue(APOD).imageUrl!!)
        assertEquals(listOf(ToSave(posts.getValue(APOD), false)), toSave(listOf(APOD), stored(), emptyMap(), tried))
    }

    @Test
    fun aNewPostAfterATriedOneTriesItsOriginal() {
        val tried = mapOf(APOD to post(APOD, d1).imageUrl!!, NASA to posts.getValue(NASA).imageUrl!!)
        assertEquals(
            listOf(ToSave(posts.getValue(APOD), true), ToSave(posts.getValue(NASA), false)),
            toSave(listOf(APOD, NASA), stored(), emptyMap(), tried),
        )
    }
}
