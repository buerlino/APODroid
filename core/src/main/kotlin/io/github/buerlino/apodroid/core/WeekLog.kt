package io.github.buerlino.apodroid.core

/**
 * The week log's file names (LOG; B31, 2026-11-22). The app makes `Download/APODroid/log.txt`
 * through MediaStore; after `pm clear`, "Clear storage" or a reinstall the old row stays, owned by
 * no one the app can see, so the app's lookup doesn't find it. MediaStore renames an insert only
 * when the file is on disk; a row whose file is gone (the Android 17 emulator, E17b and E17c) makes
 * the insert of the same path fail (`UNIQUE constraint failed: files._data`), every time. So the
 * names are tried in turn, `log.txt`, `log (1).txt`, `log (2).txt`, …, at most [TRIES].
 */
object WeekLogNames {
    const val TRIES = 10

    /** The [n]th name: `log.txt`, then `log (n).txt`. */
    fun name(n: Int): String = if (n <= 0) "log.txt" else "log ($n).txt"

    /** What [make] made: the [value] (the row's URI) and the index of the name it took. */
    class Made<T>(val value: T, val index: Int)

    /**
     * [insert]s under the names from index [from] on: one that throws an exception [taken] says is
     * a name in use goes on to the next, and so does a null (MediaProvider returns null on `UNIQUE
     * constraint failed`, B38); any other exception is rethrown. Null when every name up to [TRIES] was taken.
     */
    fun <T : Any> make(from: Int, insert: (String) -> T?, taken: (Exception) -> Boolean): Made<T>? {
        for (n in maxOf(from, 0) until TRIES) {
            val value = try {
                insert(name(n))
            } catch (e: Exception) {
                if (taken(e)) continue else throw e
            }
            if (value != null) return Made(value, n)
        }
        return null
    }
}
