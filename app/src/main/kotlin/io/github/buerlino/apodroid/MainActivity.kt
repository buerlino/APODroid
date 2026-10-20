package io.github.buerlino.apodroid

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.buerlino.apodroid.core.Picture
import io.github.buerlino.apodroid.core.Post
import io.github.buerlino.apodroid.core.Screen
import io.github.buerlino.apodroid.core.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var store: Store

    /** The stored APOD: whether today is a video day. */
    private var apod by mutableStateOf<Post?>(null)

    /** The shown tab's post, whose picture, ⛶, ★ and explanation the page shows. */
    private var post by mutableStateOf<Post?>(null)

    /** The tab the user chose; null opens on the source on the wallpaper. Survives rotation, not a restart. */
    private var tab by mutableStateOf<Source?>(null)
    private var picture by mutableStateOf<ImageBitmap?>(null)
    private var loading by mutableStateOf(false)

    /**
     * [load] was called while one ran (a tick, a choice or a pull meanwhile): it runs once more
     * after, with force if any of those calls had it; null when none came.
     */
    private var loadAgain: Boolean? = null
    private var refreshing by mutableStateOf(false)
    private var failed by mutableStateOf(false)
    private var problem by mutableStateOf<Problem?>(null)

    /** Whose check [problem] and [checkedAt] are ([readProblem]). */
    private var problemSource by mutableStateOf(Source.APOD)
    private var checkedAt by mutableStateOf<Instant?>(null)

    /** What each screen shows, from its record ([Store.screenState]): the chip, the opening tab, the status lines. */
    private var screens by mutableStateOf(emptyMap<Screen, ScreenState>())

    /** APOD mode's screen ([Store.pageScreen]): the lock screen's record with "Where" = Lock. */
    private var pageScreen by mutableStateOf(Screen.HOME)

    /** On an APOD video day: the stand-in is stored ([Store.tabPicture] of APOD), for the video notice with the switch on. */
    private var standInReady by mutableStateOf(false)
    private var saved by mutableStateOf(false)

    /** ★ is downloading the original; taps wait for it. */
    private var saving by mutableStateOf(false)
    private var daily by mutableStateOf(false)
    private var wifiOnly by mutableStateOf(false)
    private var saveEvery by mutableStateOf(false)
    private var where by mutableStateOf(Where.BOTH)
    private var fit by mutableStateOf(false)
    private var videoDays by mutableStateOf(VideoDays.KEEP)

    /** The ticked sources ([Store.sources]), on any screen. */
    private var ticked by mutableStateOf(listOf(Source.APOD))

    /** The switch "All NASA pictures" ([Store.allSources]): the grid instead of "Where". */
    private var allSources by mutableStateOf(false)

    /** The grid's ticks per screen ([Store.columns]); a column with two or more takes turns every [interval]. */
    private var columns by mutableStateOf(emptyMap<Screen, List<Source>>())
    private var interval by mutableStateOf(Interval.DAY)

    private var weekdayNoticeHidden by mutableStateOf(false)

    /** The weekday dialog shows. */
    private var weekdayDialog by mutableStateOf(false)
    private var hasFallback by mutableStateOf(false)
    private var batteryLimited by mutableStateOf(false)
    private var mayHibernate by mutableStateOf(false)
    private var pauseHintHidden by mutableStateOf(false)
    private var videoNoticeDate by mutableStateOf<String?>(null)
    private var videoNoticeHidden by mutableStateOf(false)
    private var problemNoticeKey by mutableStateOf<String?>(null)
    private var cropping by mutableStateOf(false)
    private var cropPosition by mutableFloatStateOf(0.5f)
    private val screen by lazy { store.screen }

    /** While resumed: follows a set or a new post by the job ([follow]). */
    private var watcher: OnSharedPreferenceChangeListener? = null
    private var following: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        weekdayDialog = savedInstanceState?.getBoolean("weekdayDialog") == true
        tab = savedInstanceState?.getString("tab")?.let { name -> Source.entries.firstOrNull { it.name == name } }
        daily = store.daily
        wifiOnly = store.wifiOnly
        saveEvery = store.saveEvery
        pauseHintHidden = store.pauseHintHidden
        videoNoticeDate = store.videoNoticeDate
        videoNoticeHidden = store.videoNoticeHidden
        problemNoticeKey = store.problemNoticeKey
        where = store.where
        fit = store.fit
        videoDays = store.videoDays
        ticked = store.sources
        allSources = store.allSources
        columns = store.columns
        interval = store.interval
        weekdayNoticeHidden = store.weekdayNoticeHidden
        hasFallback = store.fallbackFile.exists()
        enableEdgeToEdge()
        setContent {
            val colors = if (Build.VERSION.SDK_INT >= 31) dynamicDarkColorScheme(this) else darkColorScheme()
            MaterialTheme(colorScheme = colors) {
                Surface(Modifier.fillMaxSize()) { Page() }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("tab", tab?.name)
        outState.putBoolean("weekdayDialog", weekdayDialog)
    }

    override fun onResume() {
        super.onResume()
        // Hibernation or a force stop deleted the job; scheduling runs it at once.
        if (daily && !DailyJob.ensure(this)) Log.w("APODroid", "Couldn't schedule the daily job again")
        checkPausing()
        load()
        watcher = store.watch(::follow)
    }

    override fun onPause() {
        super.onPause()
        watcher?.let(store::stopWatching)
        watcher = null
    }

    /**
     * Redraws after the job set the wallpaper or stored a post, or a save ended, while the page
     * is open. A [load] running redraws after it anyway; one store fires once per key, so once
     * is enough.
     */
    private fun follow() {
        if (loading || following?.isActive == true) return
        following = lifecycleScope.launch {
            readStatus()
            if (isStale()) show() else saved = withContext(Dispatchers.IO) { store.isSaved(shown) }
        }
    }

    /**
     * Whether Android may pause the job of an app that isn't opened (keep-running.md in the
     * skill): the restricted standby bucket after about 8 days (Android 13+; later or never
     * before) unless battery use is Unrestricted, and hibernation (Android 12+) unless the user
     * turned it off for this app.
     */
    private fun checkPausing() {
        batteryLimited = Build.VERSION.SDK_INT >= 33 &&
            !getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        mayHibernate = Build.VERSION.SDK_INT >= 31 && !packageManager.isAutoRevokeWhitelisted
    }

    /** APOD is ticked alone: no turns, and the video-day setting applies. */
    private val apodAlone
        get() = ticked == listOf(Source.APOD)

    /**
     * The tabs: with the switch "All NASA pictures" on all three; in APOD mode APOD, on an APOD
     * video day also the source chosen for video days.
     */
    private val sources
        get() = if (allSources) Source.entries else listOfNotNull(Source.APOD, videoDays.source.takeIf { apod?.isVideo == true })

    /** The source of the picture on [screen]; none for the user's own picture or a screen never set. */
    private fun pictureSource(screen: Screen) = (screens[screen]?.picture as? Picture.Of)?.source

    /**
     * The tab shown: the one chosen, else the source of the picture on the wallpaper (a stand-in's
     * own tab), else the first. With the switch on the home screen's picture, else the lock
     * screen's; in APOD mode the one on the screen "Where" names ([pageScreen]).
     */
    private val shown
        get() = sources.let { s ->
            val onScreens = if (allSources) Screen.entries.mapNotNull(::pictureSource) else listOfNotNull(pictureSource(pageScreen))
            tab?.takeIf { it in s } ?: onScreens.firstOrNull { it in s } ?: s.first()
        }

    /**
     * Shows the shown tab's stored post (also when the daily job stored a new one while the page
     * was in the background), then runs [Store.update]: fetches the newest of each source in use
     * unless the stored one is today's or was checked within the hour (always with [force], pull
     * to refresh) and sets the wallpaper while the daily change is on. Called while one runs, it
     * runs once more after, since that one may have read the settings before they changed.
     */
    private fun load(force: Boolean = false) {
        if (loading) {
            loadAgain = force || loadAgain == true
            return
        }
        loading = true
        failed = false
        lifecycleScope.launch {
            readStatus()
            if (isStale()) show()
            val result = withContext(Dispatchers.IO) { runCatching { store.update(force, page = true) } }
            // Also when this update found nothing new: another one (the job's) may have.
            readStatus()
            if (isStale()) show()
            result.onSuccess { if (it != null) Log.i("APODroid", "Page: $it") }
            result.onFailure { Log.w("APODroid", "Update failed: $it", it) }
            failed = result.isFailure
            saved = withContext(Dispatchers.IO) { store.isSaved(shown) }
            loading = false
            val again = loadAgain
            loadAgain = null
            // A pull meanwhile keeps its spinner until its own fetch ends.
            if (again != true) refreshing = false
            if (again != null) load(again)
        }
    }

    /** The page doesn't show the shown tab's stored post (or has no picture for it yet). */
    private fun isStale() = picture == null || post?.source != shown || store.post(shown) != post

    /**
     * What the last check and the last sets stored (also the job's), and the stored APOD, which
     * decides the tabs.
     */
    private fun readStatus() {
        apod = store.apod
        pageScreen = store.pageScreen
        screens = Screen.entries.associateWith(store::screenState)
        standInReady = apod?.isVideo == true && store.tabPicture(Source.APOD) != null
        readProblem()
    }

    /**
     * One problem box. In APOD mode the first tab with a problem (APOD's first). With the switch
     * on (all three fetched) the shown tab's, else the first ticked source's.
     */
    private fun readProblem() {
        val candidates = if (allSources) listOf(shown) + ticked else sources
        problemSource = candidates.firstOrNull { store.problem(it) != null } ?: shown
        problem = store.problem(problemSource)
        checkedAt = store.checkedAt(problemSource)
    }

    /**
     * The cause in a sentence, with since when nothing has worked once that's more than a day;
     * named when it isn't APOD's or two or more sources are ticked.
     */
    private fun problemText(problem: Problem): String {
        val since = checkedAt?.takeIf { it < Instant.now().minus(1, ChronoUnit.DAYS) }
            ?.let { " since " + it.atZone(ZoneId.systemDefault()).format(dayFormat) }
        val name = if (problemSource == Source.APOD && apodAlone) "" else "${problemSource.label}: "
        return name + when (problem) {
            Problem.Network -> "Couldn't reach NASA's site${since.orEmpty()}. Check the connection."
            is Problem.Http -> if (since == null) {
                "NASA's site answered with an error (${problem.code}). The app tries again later."
            } else {
                "NASA's site has answered with an error (${problem.code})$since. The app tries again later."
            }
            Problem.Storage -> "Couldn't store the picture. Check the free space."
            Problem.SiteChanged -> "NASA's site has changed${since?.let { "; nothing new$it" }.orEmpty()}. " +
                "An app update may be needed."
        }
    }

    /**
     * Under the daily switch: when the wallpaper last changed, from where when it isn't APOD or
     * two or more sources are ticked, or that a video day kept it ([ScreenState.kept]). In APOD
     * mode one line, for the screen "Where" names. With the switch on one line while both
     * screens show the same picture, else one per screen that was set or kept ("Home screen
     * changed …", "Lock screen: video day, wallpaper kept").
     */
    private fun statusLines(): List<String> {
        fun line(name: String, state: ScreenState, setAt: Instant? = state.setAt): String? = when {
            state.kept -> if (name == "Wallpaper") "Video day, wallpaper kept" else "$name: video day, wallpaper kept"
            else -> setAt?.let {
                val time = if (DateFormat.is24HourFormat(this)) "d MMM, HH:mm" else "d MMM, h:mm\u00A0a"
                "$name changed " + it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(time, Locale.ENGLISH)) +
                    pictureSuffix(state.picture)
            }
        }
        if (!allSources) return listOfNotNull(screens[pageScreen]?.let { line("Wallpaper", it) })
        val home = screens[Screen.HOME]
        val lock = screens[Screen.LOCK]
        if (home != null && lock != null && home.picture != null && home.picture == lock.picture && home.kept == lock.kept) {
            return listOfNotNull(line("Wallpaper", home, listOfNotNull(home.setAt, lock.setAt).maxOrNull()))
        }
        return listOfNotNull(home?.let { line("Home screen", it) }, lock?.let { line("Lock screen", it) })
    }

    /** " · NASA" on the status line, none for APOD's with APOD ticked alone; the user's own " · My picture" with the switch on. */
    private fun pictureSuffix(picture: Picture?): String = when (picture) {
        is Picture.Of -> if (picture.source == Source.APOD && apodAlone) "" else " · ${picture.source.shortLabel}"
        Picture.Own -> if (allSources) " · My picture" else ""
        null -> ""
    }

    /**
     * The chip on [post]'s picture: the screens whose record shows it, by the picture's source (a
     * stand-in's on its own tab; the user's own picture has no tab). With the switch on "On home
     * screen", "On lock screen" or "On wallpaper" (both); in APOD mode "On wallpaper" for the
     * screen "Where" names.
     */
    private fun chipText(post: Post): String? {
        val on = (if (allSources) Screen.entries else listOf(pageScreen)).filter { screen ->
            (screens[screen]?.picture as? Picture.Of)?.let { it.source == post.source && post.isNoted(it.date, it.url) } == true
        }
        return when {
            on.isEmpty() -> null
            !allSources || on.size == 2 -> "On wallpaper"
            on.single() == Screen.HOME -> "On home screen"
            else -> "On lock screen"
        }
    }

    private fun pullToRefresh() {
        refreshing = true
        load(force = true)
    }

    /**
     * Shows the shown tab's stored post. Another tab, date or picture closes crop mode; a changed
     * title or explanation for the same date doesn't.
     */
    private suspend fun show() {
        val source = shown
        val stored = store.post(source)
        // Video days have no picture (an entry stored by 0.3.1 may still have a still frame).
        val bitmap = if (stored == null || stored.isVideo) null else withContext(Dispatchers.IO) {
            store.imageFile(source).takeIf { it.exists() }?.let { decodeForScreen(it) }?.asImageBitmap()
        }
        val isSaved = withContext(Dispatchers.IO) { store.isSaved(source) }
        // Another tab was chosen meanwhile: its show() draws it.
        if (source != shown) return
        if (source != post?.source || stored?.date != post?.date || stored?.imageUrl != post?.imageUrl) {
            cropping = false
            cropPosition = store.cropPosition(stored)
        }
        post = stored
        picture = bitmap
        saved = isSaved
    }

    private fun showTab(source: Source) {
        tab = source
        cropping = false
        // With the switch on the problem box follows the shown tab.
        readProblem()
        lifecycleScope.launch { show() }
    }

    /**
     * Decodes [file] halved as often as it stays at least screen-wide, and to at most 8 MP. Full
     * size can be too large to draw (2 Oct 2026: a 4455×5592 PNG, 99.6 MB as a bitmap).
     */
    private fun decodeForScreen(file: File): Bitmap? {
        val bounds = imageBounds(file) ?: return null
        val width = bounds.outWidth.toLong()
        val pixels = width * bounds.outHeight
        val screenWidth = resources.displayMetrics.widthPixels
        var sample = 1
        while (width / (sample * 2) >= screenWidth || pixels / (sample * sample) > 8_000_000) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun switchDaily(on: Boolean) {
        if (on && !DailyJob.schedule(this)) {
            toast("Couldn't schedule the daily change")
            return
        }
        if (!on) DailyJob.cancel(this)
        daily = on
        store.daily = on
        // Sets today's picture again, also when it was set before (the wallpaper may have changed since).
        // Off the main thread: forgetting waits for a set the job may be doing.
        if (on) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { store.forgetWallpaper() }
                load()
            }
        }
    }

    private fun openAppInfo() {
        startActivity(Intent(ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
    }

    private fun hidePauseHint() {
        pauseHintHidden = true
        store.pauseHintHidden = true
    }

    private fun dismissVideoNotice(forever: Boolean) {
        if (forever) {
            videoNoticeHidden = true
            store.videoNoticeHidden = true
        } else {
            val date = apod?.date?.toString()
            videoNoticeDate = date
            store.videoNoticeDate = date
        }
    }

    /** Kind, code and day: what [dismissProblem] hides until one of them changes. */
    private fun problemKey(problem: Problem): String {
        val kind = when (problem) {
            Problem.Network -> "network"
            is Problem.Http -> "http:${problem.code}"
            Problem.SiteChanged -> "site"
            Problem.Storage -> "storage"
        }
        val source = if (problemSource == Source.APOD) "" else "${problemSource.name.lowercase()}:"
        return "$source$kind:${LocalDate.now()}"
    }

    private fun dismissProblem(problem: Problem) {
        val key = problemKey(problem)
        problemNoticeKey = key
        store.problemNoticeKey = key
    }

    /**
     * Whether the shown tab has a picture to set ([Store.tabPicture]); if not, a toast says why.
     * Checked before the set menu opens.
     */
    private fun canSetNow(): Boolean {
        val post = post ?: return false
        if (store.tabPicture(post.source) != null) return true
        val waiting = videoDays.source?.takeIf { post.isVideo }
        toast(
            when {
                waiting != null -> "${waiting.possessive} hasn't loaded yet"
                post.isVideo -> "Video day: wallpaper kept"
                else -> "No picture yet"
            },
        )
        return false
    }

    /**
     * The shown tab's picture on [screens], set by hand ([Store.setByHand]: held, and with turns
     * the clock restarts); on the APOD tab on a video day what the video-day setting says. The
     * button: in APOD mode the screens "Where" names, with the switch "All NASA pictures" on the
     * menu's choice (also ✓'s).
     */
    private fun setWallpaperNow(screens: Set<Screen> = where.screens) {
        val post = post ?: return
        if (!canSetNow()) return
        cropping = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.setByHand(post.source, screens) } }
            result.onFailure { Log.w("APODroid", "Set failed: $it", it) }
            readStatus()
            if (isStale()) show()
            toast(if (result.getOrNull() == true) "Wallpaper set" else "Couldn't set the wallpaper")
        }
    }

    /**
     * A setting changed ([change]): while the daily change is on, the screens it affects follow at
     * once ([Store.settingChanged]: not a set by hand, so no hold and the clock stays; while it's
     * off, only the button sets). [videoOnly] for the video-day settings, which change nothing on
     * an image day.
     */
    private fun applyNow(change: Changed, videoOnly: Boolean = false) {
        if (!daily || (videoOnly && apod?.isVideo != true)) return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.settingChanged(change) } }
            result.onFailure { Log.w("APODroid", "Set failed: $it", it) }
            readStatus()
            if (isStale()) show()
            when {
                result.isFailure -> toast("Couldn't set the wallpaper")
                result.getOrNull() != null -> toast("Wallpaper set")
            }
        }
    }

    /**
     * On a video day another source's picture is fetched first ([load], which also sets it while
     * the daily change is on), the user's own is set at once; with "Skip it" a screen with APOD
     * alone keeps its picture and one with turns passes APOD's turn on at once ([load]). "Use my
     * picture" with nothing picked opens the picker first ([pickFirst]): the choice is made only
     * once a picture is there, so backing out keeps the one before.
     */
    private fun chooseVideoDays(choice: VideoDays, pickFirst: () -> Unit = {}) {
        if (videoDays == choice) return
        if (choice == VideoDays.MINE && !hasFallback) {
            pickFirst()
            return
        }
        videoDays = choice
        store.videoDays = choice
        readStatus()
        when {
            apod?.isVideo != true -> {}
            choice.source != null -> lifecycleScope.launch {
                // Forget the records (and holds) first, off the main thread; load() then fetches and sets the stand-in.
                if (daily) withContext(Dispatchers.IO) { runCatching { store.settingChanged(Changed.VIDEO_DAYS) } }
                load()
            }
            choice == VideoDays.MINE -> applyNow(Changed.VIDEO_DAYS, videoOnly = true)
            // "Skip it": a screen with turns passes APOD's turn on at once (no forget: dueOn keeps a held stand-in).
            daily -> lifecycleScope.launch { load() }
        }
        // The video-day source's tab went: show the one now in its place.
        if (choice.source == null && isStale()) lifecycleScope.launch { show() }
    }

    /**
     * Ticks or unticks [source] in [screen]'s column of the grid (not the last tick) and fetches
     * what's new ([Store.setColumns]: the column's hold ends). While the daily change is on, a
     * tick that changes what a screen shows sets it ([load]); one that only adds to a turning
     * screen waits for its turn. Ticking NASA or Earth Observatory where it wasn't ticked
     * anywhere says they post on weekdays only.
     */
    private fun tick(screen: Screen, source: Source) {
        val column = columns[screen].orEmpty()
        val new = columns + (screen to if (source in column) column - source else Source.entries.filter { it in column || it == source })
        if (new.values.all { it.isEmpty() }) return
        val added = columns.values.none { source in it }
        columns = new
        ticked = Source.entries.filter { s -> new.values.any { s in it } }
        // Off the main thread: the ticks wait for a set the job may be doing.
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { store.setColumns(new) }
            rescheduleJob()
            load()
        }
        if (added && source != Source.APOD && !weekdayNoticeHidden) weekdayDialog = true
    }

    /**
     * The switch "All NASA pictures" ([Store.setAllSources]): the first time on, the grid starts
     * from "Where". Works like a tick: every hold ends, and while the daily change is on what the
     * new mode puts on a screen is set ([load]); the job's period follows the mode.
     */
    private fun switchAllSources(on: Boolean) {
        allSources = on
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { store.setAllSources(on) }
            columns = store.columns
            ticked = store.sources
            rescheduleJob()
            load()
        }
    }

    private fun switchWifiOnly(on: Boolean) {
        wifiOnly = on
        store.wifiOnly = on
        rescheduleJob()
    }

    /** Save every picture: the job's, so nothing is saved now; its next run saves. */
    private fun switchSaveEvery(on: Boolean) {
        saveEvery = on
        store.saveEvery = on
    }

    /** Fill or Fit, both screens: with Fit there's no part to choose, so crop mode closes; the screens follow at once while the daily change is on. */
    private fun chooseFit(on: Boolean) {
        if (fit == on) return
        fit = on
        store.fit = on
        if (on) cropping = false
        applyNow(Changed.FIT)
    }

    private fun closeWeekdayDialog(forever: Boolean) {
        weekdayDialog = false
        if (forever) {
            weekdayNoticeHidden = true
            store.weekdayNoticeHidden = true
        }
    }

    /** The new interval waits for the next turn. */
    private fun chooseInterval(choice: Interval) {
        if (interval == choice) return
        interval = choice
        store.interval = choice
        rescheduleJob()
    }

    /** The job runs as often as a turn of 3 hours needs. */
    private fun rescheduleJob() {
        if (daily && !DailyJob.ensure(this)) toast("Couldn't schedule the daily change")
    }

    private fun savePicture() {
        if (saving) return
        if (saved) {
            toast("Already saved")
            return
        }
        val post = post ?: return
        saving = true
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.save(post) } }
            saving = false
            result.onFailure { Log.w("APODroid", "Save failed: $it", it) }
            if (result.getOrNull() == Saved.BUSY) {
                toast("Still saving")
                return@launch
            }
            if (result.getOrNull() == Saved.STALE) {
                toast("Not saved: a new picture came in")
                load()
                return@launch
            }
            // The shown tab's, which may not be the saved one's any more.
            saved = withContext(Dispatchers.IO) { store.isSaved(shown) }
            toast(
                when (result.getOrNull()) {
                    Saved.ORIGINAL -> "Saved to Pictures/APODroid"
                    Saved.RENDITION -> "Saved a smaller copy: the original didn't load"
                    else -> "Couldn't save the picture"
                },
            )
        }
    }

    /**
     * Copies the picked picture to [Store.fallbackFile]; a failed pick keeps the previous one.
     * With [choose] (picked on choosing "Use my picture") the choice follows once it's there.
     */
    private fun saveFallback(uri: Uri, choose: Boolean = false) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val part = File(filesDir, "fallback.part")
                val ok = runCatching {
                    contentResolver.openInputStream(uri)!!.use { input -> part.outputStream().use { input.copyTo(it) } }
                }.isSuccess && isImage(part) && part.renameTo(store.fallbackFile)
                part.delete()
                ok
            }
            if (!ok) toast("Couldn't use that picture")
            hasFallback = store.fallbackFile.exists()
            when {
                !ok -> {}
                choose -> chooseVideoDays(VideoDays.MINE)
                videoDays == VideoDays.MINE -> applyNow(Changed.MY_PICTURE, videoOnly = true)
            }
        }
    }

    private fun openPage(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            toast("No browser")
        }
    }

    private fun share(post: Post) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "${post.title}\n${post.pageUrl}")
        try {
            startActivity(Intent.createChooser(send, null))
        } catch (_: ActivityNotFoundException) {
            toast("Nothing to share with")
        }
    }

    /** Android 13+ confirms a copy itself. */
    private fun copy(label: String, text: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
        if (Build.VERSION.SDK_INT < 33) toast("Copied")
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    /**
     * One column; from 600 dp wide (tablets, landscape) two panes that scroll on their own: the
     * picture side, which pull to refresh covers, and the settings.
     */
    @Composable
    private fun Page() {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (maxWidth < 600.dp) {
                PullToRefreshBox(isRefreshing = refreshing, onRefresh = ::pullToRefresh) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Tabs()
                        Picture()
                        HorizontalDivider()
                        Settings()
                    }
                }
            } else {
                Row {
                    PullToRefreshBox(isRefreshing = refreshing, onRefresh = ::pullToRefresh, modifier = Modifier.weight(0.55f)) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Tabs()
                            Picture()
                        }
                    }
                    VerticalDivider()
                    Column(Modifier.weight(0.45f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        Settings()
                    }
                }
            }
        }
        if (weekdayDialog) WeekdayDialog()
    }

    /** A segmented row, one tab per source in use, only with two or more. */
    @Composable
    private fun Tabs() {
        val sources = sources
        if (sources.size < 2) return
        val shown = shown
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp).height(IntrinsicSize.Min)) {
            sources.forEachIndexed { i, source ->
                SegmentedButton(
                    selected = source == shown,
                    onClick = { if (source != shown) showTab(source) },
                    shape = SegmentedButtonDefaults.itemShape(i, sources.size),
                    modifier = Modifier.fillMaxHeight(),
                ) { Text(source.shortLabel) }
            }
        }
    }

    @Composable
    private fun Picture() {
        val apod = apod
        val post = post
        val picture = picture
        if (post == null || (picture == null && !post.isVideo)) {
            Column {
                Box(Modifier.fillMaxWidth().aspectRatio(3f / 2f), contentAlignment = Alignment.Center) {
                    when {
                        loading -> CircularProgressIndicator()
                        failed -> Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(problem?.let(::problemText) ?: "Couldn't load today's picture.", textAlign = TextAlign.Center)
                            TextButton(onClick = ::load) { Text("Try again") }
                        }
                    }
                }
                BatteryNotice()
            }
            return
        }
        var expanded by rememberSaveable(post.source, post.date) { mutableStateOf(false) }
        // Whether the two-line preview cuts the explanation off; a short one has nothing to expand.
        var cut by remember(post.explanation) { mutableStateOf(false) }
        var creditCut by remember(post.credit) { mutableStateOf(false) }
        val expandable = cut || creditCut || expanded
        val tabs = sources.size > 1
        val chip = if (tabs) chipText(post) else null
        // Back leaves crop mode without setting; the dragged position stays for the job.
        BackHandler(enabled = cropping) { cropping = false }
        Column {
            // Every box that applies shows: problem, video notice, battery hint.
            val shownProblem = problem?.takeIf { problemKey(it) != problemNoticeKey }
            if (picture == null) VideoTile(post) else Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    bitmap = picture,
                    contentDescription = post.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(picture.width.toFloat() / picture.height)
                        .then(if (cropping) Modifier.cropFrame(picture, post) else Modifier.clickable { openPage(post.pageUrl) }),
                    contentScale = ContentScale.FillWidth,
                )
                // Not over the crop frame.
                if (chip != null && !cropping) {
                    Text(
                        chip,
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                // ✓ sets the chosen part at once; with the switch on it asks which screens, as the set button does.
                // With Fit there's no part to choose.
                var setMenu by remember { mutableStateOf(false) }
                if (!fit) Box(Modifier.align(Alignment.BottomStart)) {
                    IconButton(
                        onClick = {
                            when {
                                !cropping -> cropping = true
                                !allSources -> setWallpaperNow()
                                canSetNow() -> setMenu = true
                            }
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                    ) {
                        Text(
                            if (cropping) "✓" else "⛶",
                            Modifier.clearAndSetSemantics {
                                contentDescription = if (cropping) "Set as wallpaper" else "Choose the wallpaper's part"
                            },
                            color = Color.White,
                            fontSize = 24.sp,
                        )
                    }
                    SetChoices(setMenu && cropping) { setMenu = false }
                }
                Row {
                    ShareButton(post)
                    IconButton(
                        onClick = ::savePicture,
                        modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape),
                    ) {
                        Text(
                            if (saved) "★" else "☆",
                            Modifier.clearAndSetSemantics {
                                contentDescription = when {
                                    saving -> "Saving"
                                    saved -> "Saved"
                                    else -> "Save"
                                }
                            },
                            color = Color.White.copy(alpha = if (saving) 0.4f else 1f),
                            fontSize = 24.sp,
                        )
                    }
                }
            }
            if (shownProblem != null) {
                NoticeBox({ TextButton(onClick = { dismissProblem(shownProblem) }) { Text("Dismiss") } }, problem = true) {
                    Text(problemText(shownProblem), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (apod?.isVideo == true && !videoNoticeHidden && videoNoticeDate != apod.date.toString()) VideoNotice()
            BatteryNotice()
            // With tabs a source line names the source; with one source the caption stays as 0.4.0's.
            val dateLine = post.date.format(dateFormat) + if (post.isVideo) " · Video" else ""
            Column(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { if (expandable) expanded = !expanded },
                        onLongClickLabel = "Copy link",
                        onLongClick = { copy("Link", post.pageUrl) },
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (post.explanation.isEmpty()) 16.dp else 4.dp),
            ) {
                if (tabs) {
                    Text(
                        "${post.source.label} · $dateLine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(post.title, style = MaterialTheme.typography.titleLarge)
                if (!tabs) {
                    Text(dateLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // APOD's only; all of it while the explanation is open.
                post.credit?.let { credit ->
                    Text(
                        credit,
                        Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (!expanded) creditCut = it.hasVisualOverflow },
                    )
                }
            }
            if (post.explanation.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClickLabel = if (!expandable) null else if (expanded) "Hide explanation" else "Show explanation",
                            onClick = { if (expandable) expanded = !expanded },
                            onLongClickLabel = "Copy explanation",
                            onLongClick = { copy("Explanation", post.explanation) },
                        )
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    Text(
                        post.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (!expanded) cut = it.hasVisualOverflow },
                    )
                    if (cut && !expanded) {
                        Text(
                            "more",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }
            }
        }
    }

    /**
     * Video days: no picture (the still frame is often a generic NASA image, or missing), so no
     * ⛶ or ★. Tapping plays the video on the APOD page. The wallpaper is the previous picture or
     * the user's own.
     */
    @Composable
    private fun VideoTile(apod: Post) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .background(Color.Black)
                .clickable { openPage(apod.pageUrl) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "▶",
                Modifier.clearAndSetSemantics { contentDescription = "Play video on the web" },
                color = Color.White,
                fontSize = 48.sp,
            )
            Box(Modifier.align(Alignment.BottomEnd)) { ShareButton(apod) }
        }
    }

    /** ⤴ shares the title and the page link through the system share sheet. */
    @Composable
    private fun ShareButton(post: Post) {
        IconButton(
            onClick = { share(post) },
            modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape),
        ) {
            Text(
                "⤴",
                Modifier.clearAndSetSemantics { contentDescription = "Share" },
                color = Color.White,
                fontSize = 24.sp,
            )
        }
    }

    /**
     * Says why the wallpaper didn't change on a video day and what the user can do; follows the
     * settings. With the switch on (also with turns) what takes APOD's place; with APOD ticked
     * nowhere only that it's a video.
     */
    @Composable
    private fun VideoNotice() {
        // ▶ is only on the APOD tab.
        val watch = if (post?.isVideo == true) " Tap ▶ to watch it on NASA's site." else ""
        val kept = "This APOD is a video, so your wallpaper stays as it was."
        val other = videoDays.source
        val apodTicked = columns.values.any { Source.APOD in it }
        val text = when {
            !daily || (allSources && !apodTicked) -> "This APOD is a video.$watch"
            allSources -> when {
                other != null && standInReady -> "This APOD is a video, so ${other.possessive} takes its place.$watch"
                other != null -> "This APOD is a video. ${other.possessive} takes its place once it has loaded.$watch"
                videoDays == VideoDays.MINE && hasFallback -> "This APOD is a video, so your own picture takes its place.$watch"
                videoDays == VideoDays.MINE -> "This APOD is a video, so it's skipped.$watch Pick a picture below to use it on video days."
                else -> "This APOD is a video, so it's skipped.$watch To change that, choose another picture for video days below."
            }
            other != null && pictureSource(pageScreen) == other -> "This APOD is a video, so ${other.possessive} is the wallpaper.$watch"
            other != null -> "This APOD is a video. ${other.possessive} becomes the wallpaper once it has loaded.$watch"
            videoDays == VideoDays.MINE && hasFallback ->
                "This APOD is a video, so your own picture is the wallpaper.$watch"
            videoDays == VideoDays.MINE -> "$kept$watch Pick a picture below to use it on video days."
            else -> "$kept$watch To change that, choose another picture for video days below."
        }
        NoticeBox({
            TextButton(onClick = { dismissVideoNotice(forever = false) }) { Text("Dismiss") }
            TextButton(onClick = { dismissVideoNotice(forever = true) }) { Text("Don't show again") }
        }) { Text(text, style = MaterialTheme.typography.bodyMedium) }
    }

    /**
     * The box below the picture or tile for problems, the video notice and the battery hint.
     * Orange only for a problem (a failing app must look failing); the others follow the theme.
     */
    @Composable
    private fun NoticeBox(
        buttons: @Composable RowScope.() -> Unit,
        problem: Boolean = false,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val orange = Color(0xFFFF9800)
        val fill = if (problem) orange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        val border = if (problem) orange else MaterialTheme.colorScheme.outline
        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .fillMaxWidth()
                .background(fill, RoundedCornerShape(4.dp))
                .border(1.dp, border, RoundedCornerShape(4.dp))
                .padding(12.dp),
        ) {
            content()
            // TextButton's content padding; the offset lines its text up with the text above.
            Row(Modifier.offset(x = (-12).dp), content = buttons)
        }
    }

    /**
     * Dims what the wallpaper leaves out; dragging moves the frame along the picture's longer
     * side (relative to the screen's shape), the other drags still scroll the page.
     */
    private fun Modifier.cropFrame(picture: ImageBitmap, post: Post): Modifier {
        val full = wallpaperFrame(picture.width, picture.height, screen, 0f)
        val horizontal = full.width() < picture.width
        return drawWithContent {
            drawContent()
            val scale = size.width / picture.width
            val frame = wallpaperFrame(picture.width, picture.height, screen, cropPosition)
            val topLeft = Offset(frame.left * scale, frame.top * scale)
            val frameSize = Size(frame.width() * scale, frame.height() * scale)
            clipRect(topLeft.x, topLeft.y, topLeft.x + frameSize.width, topLeft.y + frameSize.height, ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.6f))
            }
            drawRect(Color.White, topLeft, frameSize, style = Stroke(1.dp.toPx()))
        }.pointerInput(picture, post) {
            // The distance the frame can travel, in pixels on the page.
            val travel = if (horizontal) {
                size.width * (1 - full.width().toFloat() / picture.width)
            } else {
                size.height * (1 - full.height().toFloat() / picture.height)
            }
            if (travel <= 0f) return@pointerInput
            val move = { delta: Float -> cropPosition = (cropPosition + delta / travel).coerceIn(0f, 1f) }
            val end = { store.setCropPosition(post, cropPosition) }
            if (horizontal) {
                detectHorizontalDragGestures(onDragEnd = end, onDragCancel = end) { change, delta -> change.consume(); move(delta) }
            } else {
                detectVerticalDragGestures(onDragEnd = end, onDragCancel = end) { change, delta -> change.consume(); move(delta) }
            }
        }
    }

    /**
     * Wallpaper (the daily switch and its status line; in APOD mode "Where", "Fit", "If APOD is a
     * video" and the set button; with the switch "All NASA pictures" on "Fit"), with that switch
     * on the grid with "If it's a video" under APOD's row, "Change every" and the set menu; then
     * the mode switch.
     */
    @Composable
    private fun Settings() {
        // Two launchers: the result reaches the right one also after a rotation.
        val change = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let(::saveFallback) }
        val pickAndChoose = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let { saveFallback(it, choose = true) } }
        val request = PickVisualMediaRequest(PickVisualMedia.ImageOnly)
        val videoDaysChoice = @Composable { label: String?, enabled: Boolean ->
            VideoDaysChoice(label, enabled, pick = { pickAndChoose.launch(request) }, change = { change.launch(request) })
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading("Wallpaper")
            Row(
                Modifier.fillMaxWidth().toggleable(daily, role = Role.Switch, onValueChange = ::switchDaily),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Change wallpaper automatically", Modifier.weight(1f))
                Switch(checked = daily, onCheckedChange = null)
            }
            if (daily) statusLines().forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (daily) Row(
                Modifier.fillMaxWidth().toggleable(wifiOnly, role = Role.Switch, onValueChange = ::switchWifiOnly),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Wi-Fi only", Modifier.weight(1f))
                Switch(checked = wifiOnly, onCheckedChange = null)
            }
            if (daily) SaveEverySwitch()

            if (!allSources) {
                Heading("Where")
                // One height for all: a label that wraps (a narrow pane, a large font) made its segment taller.
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    val labels = mapOf(Where.HOME to "Home screen", Where.LOCK to "Lock screen", Where.BOTH to "Both")
                    Where.entries.forEachIndexed { i, w ->
                        SegmentedButton(
                            selected = where == w,
                            onClick = {
                                if (where != w) {
                                    where = w; store.where = w
                                    applyNow(Changed.WHERE)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, Where.entries.size),
                            modifier = Modifier.fillMaxHeight(),
                        ) { Text(labels.getValue(w)) }
                    }
                }
                FitChoice()

                Heading("If APOD is a video")
                videoDaysChoice(null, true)

                // Also while the daily change is on: the shown picture may not be the one on the wallpaper.
                Button(
                    onClick = { setWallpaperNow() },
                    enabled = post != null,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Set as wallpaper now") }
            } else {
                FitChoice()
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Grid(videoDaysChoice)
                // Hiding it moves the page only after a tick.
                if (columns.values.any { it.size >= 2 }) {
                    Heading("Change every")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        val labels = mapOf(Interval.DAY to "Day", Interval.H12 to "12 h", Interval.H6 to "6 h", Interval.H3 to "3 h")
                        Interval.entries.forEachIndexed { i, choice ->
                            SegmentedButton(
                                selected = interval == choice,
                                onClick = { chooseInterval(choice) },
                                shape = SegmentedButtonDefaults.itemShape(i, Interval.entries.size),
                                modifier = Modifier.fillMaxHeight(),
                            ) { Text(labels.getValue(choice)) }
                        }
                    }
                }
                SetMenu()
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Row(
                Modifier.fillMaxWidth().toggleable(allSources, role = Role.Switch, onValueChange = ::switchAllSources),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("All NASA pictures", Modifier.weight(1f))
                Switch(checked = allSources, onCheckedChange = null)
            }
        }
    }

    /** "Fit": Fill (the screen's shape cut from the picture; ⛶ chooses which part) or Fit (all of it on black). */
    @Composable
    private fun FitChoice() {
        Heading("Fit")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            listOf(false to "Fill", true to "Fit").forEachIndexed { i, (on, label) ->
                SegmentedButton(
                    selected = fit == on,
                    onClick = { chooseFit(on) },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    modifier = Modifier.fillMaxHeight(),
                ) { Text(label) }
            }
        }
    }

    /**
     * "Pictures from": a row per source (its short name, so one line at any font size), a tick box
     * for the home and the lock screen in columns of one width; "If it's a video" ([videoDaysChoice])
     * under APOD's row, greyed out while APOD has no tick. The last tick can't be removed.
     */
    @Composable
    private fun Grid(videoDaysChoice: @Composable (String?, Boolean) -> Unit) {
        val heads = mapOf(Screen.HOME to "Home", Screen.LOCK to "Lock")
        val headStyle = MaterialTheme.typography.titleSmall
        // 72 dp, or wider when a large font makes a head wider (at font scale 2.0 "Home" broke
        // inside the word): the heads never wrap, and the boxes stay centred under them.
        val measurer = rememberTextMeasurer()
        val widestHead = heads.values.maxOf { measurer.measure(it, headStyle, softWrap = false).size.width }
        val cell = Modifier.width(max(72.dp, with(LocalDensity.current) { widestHead.toDp() } + 8.dp))
        val screenNames = mapOf(Screen.HOME to "home screen", Screen.LOCK to "lock screen")
        val ticks = columns.values.sumOf { it.size }
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.weight(1f)) { Heading("Pictures from") }
            Screen.entries.forEach { screen ->
                Text(
                    heads.getValue(screen),
                    cell,
                    style = headStyle,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                )
            }
        }
        Source.entries.forEach { source ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(source.shortLabel, Modifier.weight(1f))
                Screen.entries.forEach { screen ->
                    val checked = source in columns[screen].orEmpty()
                    Box(cell, contentAlignment = Alignment.Center) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { tick(screen, source) },
                            // The last tick stays.
                            enabled = !(checked && ticks == 1),
                            modifier = Modifier.semantics { contentDescription = "${source.shortLabel} on the ${screenNames.getValue(screen)}" },
                        )
                    }
                }
            }
            if (source == Source.APOD) {
                Box(Modifier.padding(start = 16.dp)) {
                    // Greyed out, not hidden, while APOD has no tick: the page doesn't move.
                    videoDaysChoice("If it's a video", columns.values.any { Source.APOD in it })
                }
            }
        }
    }

    /**
     * "If APOD is a video": a dropdown with the four choices, after [label] when given; while
     * "Use my picture" is chosen "Change picture" beside it. Choosing it with nothing picked
     * opens the picker at once ([pick]).
     */
    @Composable
    private fun VideoDaysChoice(label: String?, enabled: Boolean, pick: () -> Unit, change: () -> Unit) {
        var open by remember { mutableStateOf(false) }
        val disabled = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            if (label != null) {
                Text(label, color = if (enabled) Color.Unspecified else disabled)
            }
            Box {
                val name = videoDays.label
                OutlinedButton(onClick = { open = true }, enabled = enabled) {
                    Text("$name ▾", Modifier.clearAndSetSemantics { contentDescription = "If APOD is a video: $name" })
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    VideoDays.entries.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(choice.label) },
                            onClick = {
                                open = false
                                chooseVideoDays(choice, pickFirst = pick)
                            },
                            // As the segmented buttons mark theirs; a text glyph, no icon library.
                            leadingIcon = { Text(if (choice == videoDays) "✓" else "") },
                        )
                    }
                }
            }
            if (videoDays == VideoDays.MINE) {
                TextButton(onClick = change, enabled = enabled) { Text(if (hasFallback) "Change picture" else "Pick picture") }
            }
        }
    }

    /** With the switch "All NASA pictures" on: the set button asks which screens. */
    @Composable
    private fun SetMenu() {
        var open by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(
                onClick = { if (canSetNow()) open = true },
                enabled = post != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Set as wallpaper ▾", Modifier.clearAndSetSemantics { contentDescription = "Set as wallpaper" }) }
            SetChoices(open) { open = false }
        }
    }

    /** The set menu's choices, Home screen / Lock screen / Both, for the set button and ✓. */
    @Composable
    private fun SetChoices(open: Boolean, close: () -> Unit) {
        DropdownMenu(expanded = open, onDismissRequest = close) {
            val choices = listOf(
                "Home screen" to setOf(Screen.HOME),
                "Lock screen" to setOf(Screen.LOCK),
                "Both" to Screen.entries.toSet(),
            )
            choices.forEach { (text, screens) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { close(); setWallpaperNow(screens) })
            }
        }
    }

    /** "Save every picture" with a small ⓘ beside the label, which says what it costs. */
    @Composable
    private fun SaveEverySwitch() {
        var info by rememberSaveable { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth().toggleable(saveEvery, role = Role.Switch, onValueChange = ::switchSaveEvery),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Not filling: the ⓘ stays beside the label, also when a large font wraps it.
            Text("Save every picture", Modifier.weight(1f, fill = false))
            Text(
                "ⓘ",
                Modifier
                    .clickable(role = Role.Button, onClick = { info = true })
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clearAndSetSemantics { contentDescription = "About saving" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Switch(checked = saveEvery, onCheckedChange = null)
        }
        if (info) {
            AlertDialog(
                onDismissRequest = { info = false },
                confirmButton = { TextButton(onClick = { info = false }) { Text("OK") } },
                text = {
                    Text(
                        "Each new picture is saved full size to Pictures/APODroid. " +
                            "APOD's are 2–10 MB, NASA's up to about 100 MB, Earth Observatory's up to 3 MB: " +
                            "a few hundred MB a month or more.",
                    )
                },
            )
        }
    }

    /** NASA and Earth Observatory post on weekdays only; shown when one is ticked, until hidden for good. */
    @Composable
    private fun WeekdayDialog() {
        AlertDialog(
            onDismissRequest = { closeWeekdayDialog(forever = false) },
            confirmButton = { TextButton(onClick = { closeWeekdayDialog(forever = false) }) { Text("Dismiss") } },
            dismissButton = { TextButton(onClick = { closeWeekdayDialog(forever = true) }) { Text("Don't show again") } },
            text = {
                Text(
                    "NASA Image of the Day and Earth Observatory post on weekdays only. " +
                        "On weekends, Friday's pictures come back.",
                )
            },
        )
    }

    @Composable
    private fun BatteryNotice() {
        if (daily && (batteryLimited || mayHibernate) && !pauseHintHidden) PauseHint()
    }

    /** Names only the settings still missing; hiding it asks first, with what will happen. */
    @Composable
    private fun PauseHint() {
        var confirm by rememberSaveable { mutableStateOf(false) }
        // Android 15+ hides the choice behind a switch whose row has to be tapped; the choice stays
        // greyed out until the switch is on, and a fresh install shows it off (keep-running.md).
        val battery = if (Build.VERSION.SDK_INT >= 35) {
            "App battery usage → tap \"Allow background usage\" → turn it on → Unrestricted"
        } else {
            "App battery usage → Unrestricted"
        }
        // Stock Android 12–14: "Pause app activity if unused"; the test phone (Android 16): "Manage app if unused".
        val unused = "Unused app settings → \"Pause or manage app if unused\": off"
        val (missing, effect) = when {
            batteryLimited && mayHibernate -> listOf(battery, unused) to
                "after about 8 days the wallpaper only changes while the phone charges, and after about 3 months it stops"
            batteryLimited -> listOf(battery) to "after about 8 days the wallpaper only changes while the phone charges"
            else -> listOf(unused) to "after about 3 months the wallpaper stops changing"
        }
        NoticeBox({
            TextButton(onClick = ::openAppInfo) { Text("App info") }
            TextButton(onClick = { confirm = true }) { Text("Don't show again") }
        }) {
            Text("Android pauses apps you don't open. In App info:", style = MaterialTheme.typography.bodyMedium)
            missing.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (confirm) {
            AlertDialog(
                onDismissRequest = { confirm = false },
                confirmButton = { TextButton(onClick = { confirm = false; hidePauseHint() }) { Text("Hide") } },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
                title = { Text("Hide this hint?") },
                text = { Text("If you don't open APODroid, $effect.") },
            )
        }
    }

    @Composable
    private fun Heading(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** The source's name in the video-day choices, the problem box and the source line. */
private val Source.label
    get() = when (this) {
        Source.APOD -> "Astronomy Picture of the Day"
        Source.NASA -> "NASA Image of the Day"
        Source.EARTH -> "Earth Observatory"
    }

/** The source's short name: its tab above the picture, its row in the grid (and its tick boxes' screen-reader labels), the status line. */
private val Source.shortLabel
    get() = when (this) {
        Source.APOD -> "APOD"
        Source.NASA -> "NASA"
        Source.EARTH -> "Earth"
    }

/** The source's picture in a sentence, e.g. "NASA's Image of the Day is the wallpaper". */
private val Source.possessive
    get() = when (this) {
        Source.APOD -> "the APOD"
        Source.NASA -> "NASA's Image of the Day"
        Source.EARTH -> "Earth Observatory's Image of the Day"
    }

/** The video-day choice in its dropdown. */
private val VideoDays.label
    get() = when (this) {
        VideoDays.KEEP -> "Skip it"
        VideoDays.NASA -> Source.NASA.label
        VideoDays.EARTH -> Source.EARTH.label
        VideoDays.MINE -> "Use my picture"
    }

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
private val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
