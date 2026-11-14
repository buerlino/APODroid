package io.github.buerlino.apodroid

import android.app.WallpaperColors
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import androidx.core.graphics.scale
import io.github.buerlino.apodroid.core.LIVE_DECODE_BUDGET
import io.github.buerlino.apodroid.core.Screen
import io.github.buerlino.apodroid.core.liveDecodeSize
import java.io.File
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Live, the day's pan (0.7.0, L5; live-0.7.0.md, The engine): APODroid's live wallpaper, chosen
 * in Android's picker. It draws home's picture (Decision 10), whatever screens it's on: both
 * while shared, home alone after a split, and as a stray on the lock screen after Android
 * reverted home (until Decision 12's still set replaces it). Only Live's copy ([Store.liveFile],
 * written by `Store.setDue`'s hand-over), never the source's file, which changes with every
 * stored post and goes on a video day; without a copy the newest stored picture of home's first
 * source ([Store.liveFallback]). Panned by the clock ([Store.liveAt]), or whole and still,
 * darkened by the copy's one factor ([Store.liveDarken], BR3; the fallback as it is).
 *
 * In the app's one process (the prefs, the locks and the job runs are per process). A software
 * canvas, one for the engine's life (L1: mixing them breaks the surface); decoded once per
 * picture on a background thread at the screen's portrait height within 50 MB
 * ([liveDecodeSize]), so a rotation decodes nothing. Nothing throws out of it: two crashes of
 * the process within 10 s revert home to the default wallpaper.
 */
class LiveWallpaper : WallpaperService() {
    override fun onCreateEngine(): Engine = LiveEngine()

    private inner class LiveEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var store: Store? = null
        private var listener: OnSharedPreferenceChangeListener? = null
        private val decoder: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "live-decode") }

        /** The decoded picture and what it is; all on the main thread. */
        private var picture: Bitmap? = null
        private var whole = false
        private var factor = 1f
        private var name = "nothing"
        private var colors: WallpaperColors? = null

        /** The key of the picture decoded ([shownKey]) and of the one asked for ([wantedKey]): the file, its time and length, the whole flag, the darkening. */
        private var shownKey: String? = null
        private var wantedKey: String? = null

        private var surface: Size? = null
        private var visible = false
        private var destroyed = false
        private var stray = false

        /** The frame last drawn, so the minute's tick draws only when it moved a pixel. */
        private var drawnFrame: Rect? = null

        private val tick = object : Runnable {
            override fun run() {
                if (destroyed || !visible) return
                draw(onlyIfMoved = true)
                handler.postDelayed(this, TICK_MS)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            safely("start") {
                setOffsetNotificationsEnabled(false)
                val store = Store(applicationContext).also { store = it }
                val flags = if (isPreview) "preview" else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) "$wallpaperFlags" else "home"
                Log.i(TAG, "Live: engine $flags created")
                // Below 14 the system can't say whether home is live without QUERY_ALL_PACKAGES: the id says it.
                if (!isPreview && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) store.noteHomeId()
                // Home's record and the copy's notes change together (Store.setDue's commit).
                listener = store.watch { reload() }
                reload()
            }
        }

        override fun onDestroy() {
            destroyed = true
            handler.removeCallbacksAndMessages(null)
            safely("stop") {
                listener?.let { store?.stopWatching(it) }
                decoder.shutdownNow()
                picture?.recycle()
                picture = null
                Log.i(TAG, "Live: engine ${if (isPreview) "preview" else "home"} destroyed")
            }
            super.onDestroy()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surface = Size(width, height)
            draw()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            draw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surface = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(tick)
            if (!visible) return
            // The re-read covers a commit from an interrupted thread (no listeners, B16) and a rebind.
            reload()
            checkStray()
            runIfOwed()
            draw()
            handler.postDelayed(tick, TICK_MS)
        }

        override fun onWallpaperFlagsChanged(which: Int) {
            Log.i(TAG, "Live: engine flags $which")
            draw()
        }

        override fun onComputeColors(): WallpaperColors? = colors

        /**
         * A stray (14+): a non-preview engine while `getWallpaperInfo(FLAG_SYSTEM)` doesn't name
         * APODroid's is on the lock screen alone after Android reverted home. It still draws
         * home's copy; the next run sets both screens Fill (Decision 12). Logged once a turn.
         */
        private fun checkStray() {
            if (isPreview || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
            val now = safely("check home") { store?.liveOn(Screen.HOME) == false } == true
            if (now && !stray) Log.i(TAG, "Live: home isn't live (stray on the lock screen)")
            stray = now
        }

        /**
         * D, the screen-on check (L8): a non-preview engine that becomes visible starts the alarms'
         * run (job 2, as an alarm does) when [Store.screenOnRunOwed] says so: a check owed, or this
         * engine a [stray] while a record is noted live, and no run started in the last 15 min.
         * Nothing else: the run, its bookkeeping and its retries are the job's.
         */
        private fun runIfOwed() {
            if (isPreview) return
            safely("check the run") {
                val store = store ?: return@safely
                if (store.screenOnRunOwed(stray)) {
                    Log.i(TAG, "Live: screen on, run owed")
                    DailyJob.runNow(applicationContext)
                }
            }
        }

        /**
         * What to draw: Live's copy and its whole flag, else the fallback; decoded on [decoder]
         * when it changed (its file, time, length or the flag), then drawn.
         */
        private fun reload() {
            if (destroyed) return
            safely("read the copy") {
                val store = store ?: return@safely
                val copy = store.liveFile
                val file: File?
                val wantWhole: Boolean
                val wantFactor: Float
                val wantName: String
                if (copy.exists()) {
                    file = copy
                    wantWhole = store.liveWhole
                    wantFactor = store.liveDarken
                    wantName = store.livePicture ?: copy.name
                } else {
                    file = store.liveFallback()
                    wantWhole = false
                    wantFactor = 1f
                    wantName = file?.name?.let { "$it (no copy)" } ?: "nothing"
                }
                val key = file?.let { "${it.path}:${it.lastModified()}:${it.length()}:$wantWhole:$wantFactor" }
                if (key == wantedKey) return@safely
                wantedKey = key
                if (file == null) {
                    show(key, null, false, 1f, wantName)
                    return@safely
                }
                val screen = store.screen
                decoder.execute {
                    val start = SystemClock.uptimeMillis()
                    val bitmap = decode(file, wantWhole, screen)
                    if (bitmap != null) Log.i(TAG, "Live: decoded ${bitmap.width}×${bitmap.height} in ${SystemClock.uptimeMillis() - start} ms")
                    // L9a: the small copy first, while the bitmap is still this thread's (show may
                    // recycle it), then the picture shown at once; the colours (WallpaperColors.fromBitmap,
                    // most of a second on the tablet) after it, so a new engine's first frame isn't black.
                    val small = bitmap?.let { smallCopy(it, wantFactor) }
                    handler.post {
                        if (destroyed || key != wantedKey) bitmap?.recycle() else show(key, bitmap, wantWhole, wantFactor, wantName)
                    }
                    val found = small?.let { colorsOf(it) }
                    if (found != null) handler.post {
                        if (!destroyed && key == shownKey) {
                            colors = found
                            safely("tell the colours") { notifyColorsChanged() }
                        }
                    }
                }
            }
        }

        /** The new picture in place of the old (recycled), drawn; its colours come after it ([reload]). */
        private fun show(key: String?, bitmap: Bitmap?, whole: Boolean, factor: Float, name: String) {
            val old = picture
            picture = bitmap
            this.whole = whole
            this.factor = factor
            this.name = name
            shownKey = key
            drawnFrame = null
            if (old !== bitmap) old?.recycle()
            draw()
        }

        /**
         * On [decoder]. Panned: upright, at the screen's portrait height within the budget; out of
         * memory decodes at half the size, then again, else null (black). Whole: the picture on its
         * background at the screen's portrait size ([wholePicture]), drawn still. A broken copy: null.
         */
        private fun decode(file: File, whole: Boolean, screen: Size): Bitmap? {
            if (whole) {
                return try {
                    wholePicture(file, screen)
                } catch (e: Throwable) {
                    Log.w(TAG, "Live: couldn't decode ${file.name} whole: $e")
                    null
                }
            }
            var budget = LIVE_DECODE_BUDGET
            repeat(3) {
                try {
                    return ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                        val (w, h) = liveDecodeSize(info.size.width, info.size.height, screen.height, budget)
                        if (w != info.size.width || h != info.size.height) decoder.setTargetSize(w, h)
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } catch (e: OutOfMemoryError) {
                    Log.w(TAG, "Live: couldn't decode ${file.name} within $budget bytes: $e")
                    budget /= 4
                } catch (e: Throwable) {
                    Log.w(TAG, "Live: couldn't decode ${file.name}: $e")
                    return null
                }
            }
            return null
        }

        /**
         * A small copy of the whole picture for its colours (not the moving frame, which would
         * recolour the theme all day), [COLOR_HEIGHT] high, darkened by [factor] (BR3: as Android's
         * from a darkened still set); always a new bitmap, never [bitmap]. On [decoder], before
         * [show] may recycle [bitmap].
         */
        private fun smallCopy(bitmap: Bitmap, factor: Float): Bitmap? = try {
            val w = maxOf(1, bitmap.width * COLOR_HEIGHT / bitmap.height)
            val scaled = bitmap.scale(w, minOf(COLOR_HEIGHT, bitmap.height)).let {
                if (it === bitmap) bitmap.copy(Bitmap.Config.ARGB_8888, false) else it
            }
            if (factor < 1f) darkened(scaled, factor) else scaled
        } catch (e: Throwable) {
            Log.w(TAG, "Live: couldn't compute the colours: $e")
            null
        }

        /** The colours of [small] ([smallCopy]), which is recycled; on [decoder], after [show]. */
        private fun colorsOf(small: Bitmap): WallpaperColors? = try {
            WallpaperColors.fromBitmap(small)
        } catch (e: Throwable) {
            Log.w(TAG, "Live: couldn't compute the colours: $e")
            null
        } finally {
            small.recycle()
        }

        /**
         * One frame on the software canvas: black, then the picture panned to the clock's place
         * ([Store.liveAt]) or whole (the screen-sized bitmap's middle in the surface's shape, as
         * Android shows a still set in landscape). With [onlyIfMoved] (the minute's tick) only when
         * the frame moved a pixel. A null canvas skips the frame.
         */
        private fun draw(onlyIfMoved: Boolean = false) {
            if (destroyed) return
            val size = surface ?: return
            safely("draw") {
                val bitmap = picture?.takeIf { !it.isRecycled }
                val position = if (bitmap == null || whole) 0.5f else store?.liveAt(Instant.now()) ?: 0.5f
                val frame = bitmap?.let { wallpaperFrame(it.width, it.height, size, position) }
                if (onlyIfMoved && frame == drawnFrame) return@safely
                val start = SystemClock.uptimeMillis()
                val canvas = try {
                    surfaceHolder.lockCanvas()
                } catch (e: Throwable) {
                    Log.w(TAG, "Live: couldn't draw: $e")
                    null
                }
                if (canvas == null) {
                    Log.w(TAG, "Live: couldn't draw: no canvas")
                    return@safely
                }
                try {
                    canvas.drawColor(Color.BLACK)
                    if (bitmap != null) drawPanned(canvas, bitmap, size, position, factor)
                    drawnFrame = frame
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
                val darker = if (bitmap != null && factor < 1f) String.format(Locale.ROOT, " ×%.2f", factor) else ""
                Log.i(TAG, String.format(Locale.ROOT, "Live: drew %s p=%.2f%s in %d ms", if (bitmap == null) "black ($name)" else name, position, darker, SystemClock.uptimeMillis() - start))
            }
        }

        /** Runs [action], logging anything it throws as `Live: couldn't <what>`: nothing leaves the engine. */
        private inline fun <T> safely(what: String, action: () -> T): T? = try {
            action()
        } catch (e: Throwable) {
            Log.w(TAG, "Live: couldn't $what: $e")
            null
        }
    }

    private companion object {
        const val TAG = "APODroid"

        /** While visible, a frame a minute (only when it moved a pixel). */
        const val TICK_MS = 60_000L

        /** The copy the colours are computed from. */
        const val COLOR_HEIGHT = 112
    }
}
