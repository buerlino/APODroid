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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.buerlino.apodroid.core.Post
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

    /** Whose check [problem] and [checkedAt] are: APOD's, or on a video day the video-day source's. */
    private var problemSource by mutableStateOf(Source.APOD)
    private var checkedAt by mutableStateOf<Instant?>(null)
    private var wallpaperSetAt by mutableStateOf<Instant?>(null)
    private var wallpaperSource by mutableStateOf(Source.APOD)
    private var wallpaperDate by mutableStateOf<LocalDate?>(null)
    private var wallpaperUrl by mutableStateOf<String?>(null)
    private var saved by mutableStateOf(false)

    /** ★ is downloading the original; taps wait for it. */
    private var saving by mutableStateOf(false)
    private var daily by mutableStateOf(false)
    private var where by mutableStateOf(Where.BOTH)
    private var videoDays by mutableStateOf(VideoDays.KEEP)

    /** The ticked sources ([Store.sources]); with two or more they take turns every [interval]. */
    private var ticked by mutableStateOf(listOf(Source.APOD))
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
        pauseHintHidden = store.pauseHintHidden
        videoNoticeDate = store.videoNoticeDate
        videoNoticeHidden = store.videoNoticeHidden
        problemNoticeKey = store.problemNoticeKey
        where = store.where
        videoDays = store.videoDays
        ticked = store.sources
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
     * One tab per source in use: the ticked ones; with APOD alone, on an APOD video day also the
     * source chosen for video days.
     */
    private val sources
        get() = if (apodAlone) listOfNotNull(Source.APOD, videoDays.source.takeIf { apod?.isVideo == true }) else ticked

    /** The tab shown: the one chosen, else the source on the wallpaper, else the first. */
    private val shown
        get() = sources.let { s -> tab?.takeIf { it in s } ?: wallpaperSource.takeIf { it in s } ?: s.first() }

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
     * What the last check and the last set stored (also the job's), and the stored APOD, which
     * decides the tabs. One problem box: APOD's problem, else on a video day that of the source
     * chosen for video days.
     */
    private fun readStatus() {
        apod = store.apod
        problemSource = sources.firstOrNull { store.problem(it) != null } ?: sources.first()
        problem = store.problem(problemSource)
        checkedAt = store.checkedAt(problemSource)
        wallpaperSetAt = store.wallpaperSetAt
        wallpaperSource = store.wallpaperSource
        wallpaperDate = store.wallpaperDate
        wallpaperUrl = store.wallpaperUrl
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
     * two or more sources are ticked, or that a video day kept it.
     */
    private fun statusText(): String? = when {
        apodAlone && apod?.isVideo == true && when (videoDays) {
            VideoDays.KEEP -> true
            VideoDays.MINE -> !hasFallback
            else -> wallpaperSource != videoDays.source
        } -> "Video day, wallpaper kept"
        else -> wallpaperSetAt?.let {
            val time = if (DateFormat.is24HourFormat(this)) "d MMM, HH:mm" else "d MMM, h:mm\u00A0a"
            "Wallpaper changed " + it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(time, Locale.ENGLISH)) +
                if (wallpaperSource == Source.APOD && apodAlone) "" else " · ${wallpaperSource.label}"
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
     * The shown tab's picture, set by hand ([Store.setByHand]: held, and with turns the clock
     * restarts); on the APOD tab on a video day what the video-day setting says. On the screens
     * "Where" names, also with the switch "All NASA pictures" on until M3's menu.
     */
    private fun setWallpaperNow() {
        val post = post ?: return
        if (store.tabPicture(post.source) == null) {
            val waiting = videoDays.source?.takeIf { post.isVideo }
            toast(
                when {
                    waiting != null -> "${waiting.possessive} hasn't loaded yet"
                    post.isVideo -> "Video day: wallpaper kept"
                    else -> "No picture yet"
                },
            )
            return
        }
        cropping = false
        val screens = where.screens
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
     * the daily change is on), the user's own is set at once, "keep" sets nothing.
     */
    private fun chooseVideoDays(choice: VideoDays) {
        if (videoDays == choice) return
        videoDays = choice
        store.videoDays = choice
        readStatus()
        when {
            apod?.isVideo != true -> {}
            choice.source != null -> load()
            choice == VideoDays.MINE -> applyNow(Changed.VIDEO_DAYS, videoOnly = true)
        }
        // The video-day source's tab went: show the one now in its place.
        if (choice.source == null && isStale()) lifecycleScope.launch { show() }
    }

    /**
     * Ticks or unticks [source] (not the last one) and fetches what's new. While the daily change
     * is on, unticking the source on the wallpaper sets the next one ([load]); a ticked one waits
     * for its turn. Ticking NASA or Earth Observatory says they post on weekdays only.
     */
    private fun tick(source: Source) {
        val new = if (source in ticked) ticked - source else Source.entries.filter { it in ticked || it == source }
        if (new.isEmpty()) return
        val added = source !in ticked
        ticked = new
        // Off the main thread: the ticks wait for a set the job may be doing.
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { store.sources = new }
            rescheduleJob()
            load()
        }
        if (added && source != Source.APOD && !weekdayNoticeHidden) weekdayDialog = true
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

    /** Copies the picked picture to [Store.fallbackFile]; a failed pick keeps the previous one. */
    private fun saveFallback(uri: Uri) {
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
            if (ok && videoDays == VideoDays.MINE) applyNow(Changed.MY_PICTURE, videoOnly = true)
        }
    }

    private fun openPage(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            toast("No browser")
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
                ) { Text(source.tabLabel) }
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
        val expandable = cut || expanded
        val tabs = sources.size > 1
        val onWallpaper = tabs && post.source == wallpaperSource && post.isNoted(wallpaperDate, wallpaperUrl)
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
                if (onWallpaper && !cropping) {
                    Text(
                        "On wallpaper",
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                IconButton(
                    // ✓ sets the chosen part at once.
                    onClick = { if (cropping) setWallpaperNow() else cropping = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
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
            if (shownProblem != null) {
                NoticeBox({ TextButton(onClick = { dismissProblem(shownProblem) }) { Text("Dismiss") } }, problem = true) {
                    Text(problemText(shownProblem), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (apodAlone && apod?.isVideo == true && !videoNoticeHidden && videoNoticeDate != apod.date.toString()) VideoNotice()
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
        }
    }

    /** Says why the wallpaper didn't change on a video day and what the user can do; follows the settings. */
    @Composable
    private fun VideoNotice() {
        // ▶ is only on the APOD tab.
        val watch = if (post?.isVideo == true) " Tap ▶ to watch it on NASA's site." else ""
        val kept = "This APOD is a video, so your wallpaper stays as it was."
        val other = videoDays.source
        val text = when {
            !daily -> "This APOD is a video.$watch"
            other != null && wallpaperSource == other -> "This APOD is a video, so ${other.possessive} is the wallpaper.$watch"
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

    @Composable
    private fun Settings() {
        val pick = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let(::saveFallback) }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading("Wallpaper")
            Row(
                Modifier.fillMaxWidth().toggleable(daily, role = Role.Switch, onValueChange = ::switchDaily),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Change wallpaper automatically", Modifier.weight(1f))
                Switch(checked = daily, onCheckedChange = null)
            }
            if (daily) statusText()?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

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

            // Also while the daily change is on: with tabs and turns the shown picture may not be the one on the wallpaper.
            Button(
                onClick = ::setWallpaperNow,
                enabled = post != null,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Set as wallpaper now") }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Heading("Pictures from")
            EqualGrid(gap = 8.dp) {
                Source.entries.forEach { source ->
                    FilterChip(
                        selected = source in ticked,
                        onClick = { tick(source) },
                        label = { Text(source.tabLabel, Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        // As the segmented buttons mark theirs; a text glyph, no icon library.
                        leadingIcon = if (source in ticked) { { Text("✓") } } else null,
                        // The last one stays.
                        enabled = ticked != listOf(source),
                    )
                }
            }
            if (ticked.size > 1) {
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

            // Always shown, so ticking a second source doesn't move the page. With turns a video APOD
            // just skips its turn: greyed out, the choice kept for when APOD is ticked alone again.
            Heading("If APOD is a video")
            if (!apodAlone) {
                Text(
                    "The next source takes its turn.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Choice("Keep the previous picture", videoDays == VideoDays.KEEP, apodAlone) { chooseVideoDays(VideoDays.KEEP) }
            Choice(Source.NASA.label, videoDays == VideoDays.NASA, apodAlone) { chooseVideoDays(VideoDays.NASA) }
            Choice(Source.EARTH.label, videoDays == VideoDays.EARTH, apodAlone) { chooseVideoDays(VideoDays.EARTH) }
            Choice("Use my picture", videoDays == VideoDays.MINE, apodAlone) { chooseVideoDays(VideoDays.MINE) }
            if (videoDays == VideoDays.MINE) {
                OutlinedButton(
                    onClick = { pick.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                    enabled = apodAlone,
                    modifier = Modifier.padding(start = 48.dp),
                ) { Text(if (hasFallback) "Change picture" else "Pick picture") }
            }
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

    /**
     * [content] in equal widths, as many on a line as the widest fits (all on one, unless the font is
     * large): a label never breaks inside a word.
     */
    @Composable
    private fun EqualGrid(gap: Dp, content: @Composable () -> Unit) {
        Layout(content, Modifier.fillMaxWidth()) { measurables, constraints ->
            val gapPx = gap.roundToPx()
            val widest = measurables.maxOf { it.maxIntrinsicWidth(constraints.maxHeight) }
            val perLine = (measurables.size downTo 1).firstOrNull { widest * it + gapPx * (it - 1) <= constraints.maxWidth } ?: 1
            val width = (constraints.maxWidth - gapPx * (perLine - 1)) / perLine
            val lines = measurables.map { it.measure(Constraints.fixedWidth(width)) }.chunked(perLine)
            layout(constraints.maxWidth, lines.sumOf { line -> line.maxOf { it.height } }) {
                var y = 0
                lines.forEach { line ->
                    line.forEachIndexed { i, placeable -> placeable.place(i * (width + gapPx), y) }
                    y += line.maxOf { it.height }
                }
            }
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

    @Composable
    private fun Choice(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().selectable(selected, enabled = enabled, onClick = onClick, role = Role.RadioButton),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.padding(12.dp))
            // Material's disabled alpha, as the radio button's.
            Text(label, color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        }
    }
}

/** The source's name in the video-day choices, the status line, the problem box and the source line. */
private val Source.label
    get() = when (this) {
        Source.APOD -> "Astronomy Picture of the Day"
        Source.NASA -> "NASA Image of the Day"
        Source.EARTH -> "Earth Observatory"
    }

/** The source's tab above the picture and its chip in the settings. */
private val Source.tabLabel
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

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
private val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
