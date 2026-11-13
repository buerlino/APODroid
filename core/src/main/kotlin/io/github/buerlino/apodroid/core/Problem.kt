package io.github.buerlino.apodroid.core

/**
 * Why a source's last check failed, for the page to name. [key] is the word stored for it
 * (`<source>.problem`, with `httpCode` for [Http]) and part of a dismissed problem's notice key:
 * 0.6.0's words, never to change (a dismissed problem would show again).
 */
sealed class Problem(val key: String) {
    data object Network : Problem("network")
    data class Http(val code: Int) : Problem("http")
    data object SiteChanged : Problem("site")
    data object Storage : Problem("storage")

    companion object {
        /** The problem stored as [key] (an HTTP one with [httpCode]); null for none or an unknown word. */
        fun of(key: String?, httpCode: Int): Problem? = when (key) {
            Network.key -> Network
            "http" -> Http(httpCode)
            SiteChanged.key -> SiteChanged
            Storage.key -> Storage
            else -> null
        }

        /** What a failed check ran into: the typed failures, else the network (a read timeout too). */
        fun of(failure: Exception): Problem = when (failure) {
            is io.github.buerlino.apodroid.core.SiteChanged -> SiteChanged
            is StorageFailed -> Storage
            is HttpStatus -> Http(failure.code)
            else -> Network
        }
    }
}
