package io.github.buerlino.apodroid.core

import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProblemTest {
    @Test
    fun keysAreTheWords060Stores() {
        // A dismissed problem's notice key holds them: another word would show it again.
        assertEquals("network", Problem.Network.key)
        assertEquals("http", Problem.Http(503).key)
        assertEquals("site", Problem.SiteChanged.key)
        assertEquals("storage", Problem.Storage.key)
    }

    @Test
    fun readsTheStoredWordsBack() {
        assertEquals(Problem.Network, Problem.of("network", 0))
        assertEquals(Problem.Http(503), Problem.of("http", 503))
        assertEquals(Problem.SiteChanged, Problem.of("site", 0))
        assertEquals(Problem.Storage, Problem.of("storage", 0))
        assertNull(Problem.of(null, 0))
        assertNull(Problem.of("something else", 0))
    }

    @Test
    fun namesAFailure() {
        assertEquals(Problem.SiteChanged, Problem.of(SiteChanged("no post")))
        assertEquals(Problem.Storage, Problem.of(StorageFailed(IOException("full"))))
        assertEquals(Problem.Http(503), Problem.of(HttpStatus(503)))
        assertEquals(Problem.Network, Problem.of(SocketTimeoutException()))
        assertEquals(Problem.Network, Problem.of(IOException("reset")))
    }
}
