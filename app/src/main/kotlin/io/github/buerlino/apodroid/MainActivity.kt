package io.github.buerlino.apodroid

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.buerlino.apodroid.core.Picture
import io.github.buerlino.apodroid.core.Post
import io.github.buerlino.apodroid.core.Screen
import io.github.buerlino.apodroid.core.Source
import io.github.buerlino.apodroid.core.SwitchRuns
import io.github.buerlino.apodroid.core.apodColumns
import io.github.buerlino.apodroid.core.placing
import io.github.buerlino.apodroid.core.screensOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var store: Store

    /** The stored APOD: whether today is a video day. */
    private var apod by mutableStateOf<Post?>(null)

    /**
     * Each offered source's stored post ([show]): every page of the pager draws its own, so a
     * neighbour shows while dragging.
     */
    private var posts by mutableStateOf(emptyMap<Source, Post>())

    /** Their pictures, decoded for the screen ([decodeForScreen]); none on video days. At most three. */
    private var pictures by mutableStateOf(emptyMap<Source, ImageBitmap>())

    /** The shown source's post: the boxes and the caption below the pager, ⛶'s part, Set's crop. */
    private val post
        get() = posts[shown]

    /** [show] calls: only the last one started draws (it read the store last). */
    private var showing = 0

    /** The source the user chose (the dropdown, a swipe); null opens on the source on the wallpaper. Survives rotation, not a restart. */
    private var tab by mutableStateOf<Source?>(null)
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

    /** What each screen shows, from its record ([Store.screenState]): the chip, the opening page, the status lines. */
    private var screens by mutableStateOf(emptyMap<Screen, ScreenState>())

    /** APOD mode's screen ([Store.pageScreen]): the lock screen's record with "Where" = Lock. */
    private var pageScreen by mutableStateOf(Screen.HOME)

    /** On an APOD video day: the stand-in is stored ([Store.tabPicture] of APOD), for the video notice with the switch on. */
    private var standInReady by mutableStateOf(false)

    /** The offered sources whose stored post ★ saved ([Store.isSaved]). */
    private var saved by mutableStateOf(emptySet<Source>())

    /** ★ is downloading the original; taps wait for it. */
    private var saving by mutableStateOf(false)
    private var saveEvery by mutableStateOf(false)

    /** "Largest original" ([Store.saveLimit], MB, 0: no limit): at once, as "Save every picture". */
    private var saveLimit by mutableIntStateOf(20)

    /** The daily switch ([Store.daily]): not part of the draft, it acts at once ([switchDaily]). */
    private var daily by mutableStateOf(false)

    // The draft: what the settings show and change, until Set saves it all ([Store.saveSetup]).
    // Kept across a rotation and the system killing the app (onSaveInstanceState), not a restart.
    private var where by mutableStateOf(Where.BOTH)
    private var videoDays by mutableStateOf(VideoDays.KEEP)

    /** The switch "More NASA pictures": the list instead of "Where", all three sources offered. */
    private var allSources by mutableStateOf(false)

    /** The list's ticks per screen ([Store.list]), kept in APOD mode too. */
    private var list by mutableStateOf(emptyMap<Screen, List<Source>>())

    /** The screens each source had last in the list ([Store.lastScreens]): an unticked row's dropdown. */
    private var lastScreens by mutableStateOf(emptyMap<Source, Set<Screen>>())
    private var interval by mutableStateOf(Interval.DAY)

    /** "New picture at" ([Store.pictureAt], an hour 0–23): the change times start there. */
    private var pictureAt by mutableIntStateOf(DEFAULT_PICTURE_AT)

    /** ⛶'s part per source ([Store.parts]); ✓ notes the shown post's. */
    private var parts by mutableStateOf(emptyMap<Source, Part>())

    /** A "my picture" picked and not set yet ([Store.draftFallbackFile]). */
    private var draftFallback by mutableStateOf(false)

    /**
     * The setup last set ([Store.setup]), which the job follows: what reports what is set reads
     * it (the status lines, the chip, the video notice, the problem box, the battery hint, the
     * opening page), the controls the draft.
     */
    private var stored by mutableStateOf(Setup(false, Where.BOTH, emptyMap(), Interval.DAY, VideoDays.KEEP, emptyMap()))

    /** "My picture" as set ([Store.fallbackFile]). */
    private var hasFallback by mutableStateOf(false)

    /** "Setup" is open: by hand, by itself while the draft differs, closed after a Set that worked (S5). */
    private var setupOpen by mutableStateOf(false)

    /** "Advanced settings" is open; closed by default. */
    private var advancedOpen by mutableStateOf(false)

    private var weekdayNoticeHidden by mutableStateOf(false)

    /** The weekday dialog shows. */
    private var weekdayDialog by mutableStateOf(false)
    private var batteryLimited by mutableStateOf(false)
    private var mayHibernate by mutableStateOf(false)
    private var pauseHintHidden by mutableStateOf(false)
    private var videoNoticeDate by mutableStateOf<String?>(null)
    private var videoNoticeHidden by mutableStateOf(false)
    private var problemNoticeKey by mutableStateOf<String?>(null)
    private var cropping by mutableStateOf(false)
    private var cropPosition by mutableFloatStateOf(0.5f)

    /** Crop mode's "Whole picture" toggle (A4): ✓ keeps it in the draft with the part, Back drops it. */
    private var cropWhole by mutableStateOf(false)
    private val screen by lazy { store.screen }

    private companion object {
        /**
         * The pages alive in the process, `onCreate` to `onDestroy` (main thread only). A start
         * while another app's screen is on top of a page stacks a second one; its start keeps
         * the draft's picture while the first is alive.
         */
        var pages = 0

        /**
         * The pages alive by their [id] (main thread only). A rotation keeps a page's id (the
         * saved state), so a run's end reaches the page that started it or the one a rotation
         * made of it; another page (one stacked over another app's screen) has its own.
         */
        val live = mutableMapOf<String, MainActivity>()

        /**
         * Set's and the daily switch's runs ([setAsWallpaper], [switchOn]): process-wide, so a
         * rotation or Back during one doesn't cancel its end (B13: the wallpaper was set, but the
         * job wasn't scheduled afresh, no toast, no log line).
         */
        val runs = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        /** "Set as wallpaper" is running, in any page (one run at a time in the app). */
        var setting by mutableStateOf(false)

        /** The daily switch's run is going ([switchOn]); Set waits for it. */
        var switching by mutableStateOf(false)

        /**
         * The daily switch went on while a load, a Set or a switch-on ran: [switchOn] runs after it
         * ([runNext]), unless the switch-on run went on to set (B6: quick taps set once).
         */
        val switchRuns = SwitchRuns()

        /** A toast also when no page is left (Back during a run). */
        fun toast(context: Context, text: String) = Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()

        /**
         * After a set of the stored setup (Set, the daily switch on): arms the alarms for the next
         * change time or check and the fallback job ([DailyJob.ensure]; a fresh fallback runs at
         * once, so only after the set, or it would set first). A run going isn't stopped.
         */
        fun scheduleJob(context: Context) {
            if (!DailyJob.ensure(context)) toast(context, "Couldn't schedule the daily change")
        }

        /** After a run: each page alive runs what waited for it ([runNext]). */
        fun runAllNext() = live.values.toList().forEach { it.runNext() }
    }

    /** This page's id in [live], kept by a rotation. */
    private lateinit var id: String

    /**
     * The system's time zone and 12/24 h for the status line and the problem box, as state: a
     * changed zone alone recomposed nothing, so the times stayed in the old zone until the
     * process restarted (B13). Read on resume and, while resumed, when either changes.
     */
    private var zone by mutableStateOf(ZoneId.systemDefault())
    private var hours24 by mutableStateOf(false)
    private val clockChanged = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = readClock()
    }

    private fun readClock() {
        zone = ZoneId.systemDefault()
        hours24 = DateFormat.is24HourFormat(this)
    }

    /** While resumed: follows a set or a new post by the job ([follow]). */
    private var watcher: OnSharedPreferenceChangeListener? = null
    private var following: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        id = savedInstanceState?.getString("pageId") ?: UUID.randomUUID().toString()
        live[id] = this
        weekdayDialog = savedInstanceState?.getBoolean("weekdayDialog") == true
        setupOpen = savedInstanceState?.getBoolean("setupOpen") == true
        advancedOpen = savedInstanceState?.getBoolean("advancedOpen") == true
        tab = savedInstanceState?.getString("tab")?.let { name -> Source.entries.firstOrNull { it.name == name } }
        daily = store.daily
        saveEvery = store.saveEvery
        saveLimit = store.saveLimit
        pauseHintHidden = store.pauseHintHidden
        videoNoticeDate = store.videoNoticeDate
        videoNoticeHidden = store.videoNoticeHidden
        problemNoticeKey = store.problemNoticeKey
        weekdayNoticeHidden = store.weekdayNoticeHidden
        stored = store.setup
        hasFallback = store.fallbackFile.exists()
        val draft = savedInstanceState?.getBundle("draft")?.let(::readDraft)
        // A start without a saved draft drops the draft's picture too, unless another page is
        // alive (one under another app's screen, B8): it may still use the picture.
        if (draft == null && pages == 0) store.draftFallbackFile.delete()
        // A star save or the job's killed with the process left its temp file.
        if (pages == 0) lifecycleScope.launch(Dispatchers.IO) { store.dropSaveLeftovers() }
        pages++
        useDraft(draft ?: stored)
        draftFallback = draft != null && savedInstanceState.getBoolean("draftFallback") && store.draftFallbackFile.exists()
        lastScreens = Source.entries.associateWith { source ->
            savedInstanceState?.getString("lastScreens.${source.name}")?.let { name -> Where.entries.firstOrNull { it.name == name }?.screens }
                ?: store.lastScreens(source)
        }
        enableEdgeToEdge()
        setContent {
            val colors = if (Build.VERSION.SDK_INT >= 31) dynamicDarkColorScheme(this) else darkColorScheme()
            MaterialTheme(colorScheme = colors) {
                Surface(Modifier.fillMaxSize()) { Page() }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pages--
        if (live[id] === this) live.remove(id)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pageId", id)
        outState.putString("tab", tab?.name)
        outState.putBoolean("weekdayDialog", weekdayDialog)
        outState.putBoolean("setupOpen", setupOpen)
        outState.putBoolean("advancedOpen", advancedOpen)
        outState.putBundle("draft", draft().toBundle())
        outState.putBoolean("draftFallback", draftFallback)
        lastScreens.forEach { (source, screens) ->
            outState.putString("lastScreens.${source.name}", Where.entries.firstOrNull { it.screens == screens }?.name)
        }
    }

    /** The draft as a [Setup], for Set and the saved state. */
    private fun draft() = Setup(allSources, where, list, interval, videoDays, parts, pictureAt)

    private fun useDraft(setup: Setup) {
        allSources = setup.allSources
        where = setup.where
        list = setup.list
        interval = setup.interval
        videoDays = setup.videoDays
        parts = setup.parts
        pictureAt = setup.pictureAt
    }

    /** The draft's columns: APOD mode's from "Where", else the list. A column with two or more takes turns every [interval]. */
    private val columns
        get() = draft().columns

    override fun onResume() {
        super.onResume()
        // Hibernation or a force stop deleted the job (scheduling runs it at once) and the alarms.
        if (store.daily && !DailyJob.ensure(this)) Log.w("APODroid", "Couldn't schedule the daily job again")
        checkPausing()
        readClock()
        // System broadcasts only: no export flag needed.
        registerReceiver(clockChanged, IntentFilter(Intent.ACTION_TIMEZONE_CHANGED).apply { addAction(Intent.ACTION_TIME_CHANGED) })
        load()
        watcher = store.watch(::follow)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(clockChanged)
        watcher?.let(store::stopWatching)
        watcher = null
    }

    /**
     * Redraws after the job set the wallpaper or stored a post, or a save ended, while the page
     * is open. A [load], a Set or the daily switch's run going redraws after it anyway, so this
     * waits for them (B14: during a Set the page drew the home screen's new line beside the lock
     * screen's old one). Called while one runs, it runs once more after it ([followAgain]): the
     * running one read the prefs before it suspended (CR 4).
     */
    private fun follow() {
        if (loading || switching || setting) return
        if (following?.isActive == true) {
            followAgain = true
            return
        }
        following = lifecycleScope.launch {
            do {
                followAgain = false
                readStatus()
                if (isStale()) show() else readSaved()
            } while (followAgain && !loading && !switching && !setting)
        }
    }

    /** [follow] was called while one ran: it reads the prefs once more. */
    private var followAgain = false

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

    /** APOD is ticked alone in the stored setup: the status line and the problem box don't name it. */
    private val apodAlone
        get() = stored.columns.ticked() == listOf(Source.APOD)

    /**
     * The pager's pages, the sources the page offers (and fetches), by the draft: with the switch
     * "More NASA pictures" on all three; in APOD mode APOD, on an APOD video day also the source
     * chosen for video days.
     */
    private val sources
        get() = if (allSources) Source.entries else listOfNotNull(Source.APOD, videoDays.source.takeIf { apod?.isVideo == true })

    /** The source of the picture on [screen]; none for the user's own picture or a screen never set. */
    private fun pictureSource(screen: Screen) = (screens[screen]?.picture as? Picture.Of)?.source

    /**
     * The source shown, the pager's settled page: the one chosen, else the source of the picture
     * on the wallpaper (a stand-in's own page), else the first. With the switch on (stored) the
     * home screen's picture, else the lock screen's; in APOD mode the one on the screen "Where"
     * names ([pageScreen]).
     */
    private val shown
        get() = sources.let { s ->
            val onScreens = if (stored.allSources) Screen.entries.mapNotNull(::pictureSource) else listOfNotNull(pictureSource(pageScreen))
            tab?.takeIf { it in s } ?: onScreens.firstOrNull { it in s } ?: s.first()
        }

    /**
     * Shows the offered sources' stored posts (also when the daily job stored a new one while the page
     * was in the background), then runs [Store.update]: fetches the newest of each source in use
     * unless the stored one is today's or was checked within the hour (always with [force], pull
     * to refresh) and sets the wallpaper while the daily change is on. Called while one runs, it
     * runs once more after, since that one may have read the settings before they changed; also
     * while the daily switch's run goes ([runNext]).
     */
    private fun load(force: Boolean = false) {
        if (loading || switching) {
            loadAgain = force || loadAgain == true
            // A page a rotation made during the daily switch's run: what's stored meanwhile.
            if (posts.isEmpty()) lifecycleScope.launch {
                readStatus()
                show()
            }
            return
        }
        loading = true
        failed = false
        lifecycleScope.launch {
            readStatus()
            if (isStale()) show()
            val offered = sources
            val result = withContext(Dispatchers.IO) { runCatching { store.update(force, page = true, show = offered) } }
            // Also when this update found nothing new: another one (the job's) may have.
            readStatus()
            if (isStale()) show()
            val failure = result.exceptionOrNull() ?: result.getOrNull()?.failure
            result.getOrNull()?.done?.let { Log.i("APODroid", "Page: $it") }
            failure?.let { Log.w("APODroid", "Update failed: $it", it) }
            failed = failure != null
            readSaved()
            loading = false
            // A pull meanwhile keeps its spinner until its own fetch ends.
            if (loadAgain != true) refreshing = false
            runNext()
        }
    }

    /**
     * After a load, a Set or the daily switch's run: once none runs, the daily switch's run that
     * waited ([switchRuns]) goes first, then a load that waited ([loadAgain]).
     */
    private fun runNext() {
        if (loading || setting || switching) return
        if (switchRuns.take()) {
            if (store.daily) {
                switchOn()
                return
            }
        }
        loadAgain?.let {
            loadAgain = null
            load(it)
        }
    }

    /** The page doesn't show an offered source's stored post (or has no picture for it yet). */
    private fun isStale() = sources.any { source ->
        val post = posts[source]
        store.post(source) != post || (post != null && !post.isVideo && source !in pictures)
    }

    private suspend fun readSaved() {
        val offered = sources
        saved = withContext(Dispatchers.IO) { offered.filter(store::isSaved).toSet() }
    }

    /**
     * What the last check and the last sets stored (also the job's), the stored setup, and the
     * stored APOD, which decides the pages.
     */
    private fun readStatus() {
        stored = store.setup
        hasFallback = store.fallbackFile.exists()
        apod = store.apod
        pageScreen = store.pageScreen
        screens = Screen.entries.associateWith(store::screenState)
        standInReady = apod?.isVideo == true && store.tabPicture(Source.APOD) != null
        readProblem()
    }

    /**
     * One problem box, by the stored setup. In APOD mode the first of APOD and its video-day
     * source with a problem (APOD's first). With the switch on the shown source's, else the first
     * ticked source's.
     */
    private fun readProblem() {
        val candidates = if (stored.allSources) {
            listOf(shown) + stored.columns.ticked()
        } else {
            listOfNotNull(Source.APOD, stored.videoDays.source.takeIf { apod?.isVideo == true })
        }
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
            ?.let { " since " + it.atZone(zone).format(dayFormat) }
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
                val time = if (hours24) "d MMM, HH:mm" else "d MMM, h:mm\u00A0a"
                "$name changed " + it.atZone(zone).format(DateTimeFormatter.ofPattern(time, Locale.ENGLISH)) +
                    pictureSuffix(state.picture)
            }
        }
        if (!stored.allSources) return listOfNotNull(screens[pageScreen]?.let { line("Wallpaper", it) })
        val home = screens[Screen.HOME]
        val lock = screens[Screen.LOCK]
        if (home != null && lock != null && home.picture != null && home.picture == lock.picture && home.kept == lock.kept) {
            return listOfNotNull(line("Wallpaper", home, listOfNotNull(home.setAt, lock.setAt).maxOrNull()))
        }
        return listOfNotNull(home?.let { line("Home screen", it) }, lock?.let { line("Lock screen", it) })
    }

    /** " · NASA" on the status line, none for APOD's with APOD ticked alone; the user's own " · My picture" in both modes. */
    private fun pictureSuffix(picture: Picture?): String = when (picture) {
        is Picture.Of -> if (picture.source == Source.APOD && apodAlone) "" else " · ${picture.source.shortLabel}"
        Picture.Own -> " · My picture"
        null -> ""
    }

    /**
     * The chip on [post]'s picture: the screens whose record shows it, by the picture's source (a
     * stand-in's on its own page; the user's own picture has none). With the switch on "On home
     * screen", "On lock screen" or "On wallpaper" (both); in APOD mode "On wallpaper" for the
     * screen "Where" names.
     */
    private fun chipText(post: Post): String? {
        val on = (if (stored.allSources) Screen.entries else listOf(pageScreen)).filter { screen ->
            (screens[screen]?.picture as? Picture.Of)?.let { it.source == post.source && post.isNoted(it.date, it.url) } == true
        }
        return when {
            on.isEmpty() -> null
            !stored.allSources || on.size == 2 -> "On wallpaper"
            on.single() == Screen.HOME -> "On home screen"
            else -> "On lock screen"
        }
    }

    private fun pullToRefresh() {
        refreshing = true
        load(force = true)
    }

    /**
     * Shows each offered source's stored post, for the pager's pages; a picture already decoded
     * for the same date and picture stays. Another date or picture of the shown source closes
     * crop mode; a changed title or explanation for the same date doesn't.
     */
    private suspend fun show() {
        val run = ++showing
        val offered = sources
        val latest = offered.mapNotNull { source -> store.post(source)?.let { source to it } }.toMap()
        // Video days have no picture (an entry stored by 0.3.1 may still have a still frame).
        val kept = latest.mapNotNull { (source, post) ->
            val old = posts[source]
            pictures[source]?.takeIf { !post.isVideo && old?.date == post.date && old.imageUrl == post.imageUrl }?.let { source to it }
        }.toMap()
        val decoded = withContext(Dispatchers.IO) {
            latest.filter { (source, post) -> !post.isVideo && source !in kept }.mapNotNull { (source, _) ->
                store.imageFile(source).takeIf { it.exists() }?.let { decodeForScreen(it) }?.let { source to it.asImageBitmap() }
            }.toMap()
        }
        val isSaved = withContext(Dispatchers.IO) { offered.filter(store::isSaved).toSet() }
        // A later show() read the store after this one: it draws.
        if (run != showing) return
        val old = posts[shown]
        val new = latest[shown]
        if (old?.date != new?.date || old?.imageUrl != new?.imageUrl) cropping = false
        posts = latest
        pictures = kept + decoded
        saved = isSaved
    }

    /** The dropdown's choice or the page a swipe settled on. Crop mode closes. */
    private fun showTab(source: Source) {
        tab = source
        cropping = false
        // With the switch on the problem box follows the shown source.
        readProblem()
        if (isStale()) lifecycleScope.launch { show() }
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

    /**
     * The daily switch, at once (not part of the draft; S4f2): [Store.switchDaily] writes it. Off
     * cancels the jobs and the alarms. On starts the stored setup afresh ([Store.startAfresh]:
     * both records' pictures forgotten, the change times kept), sets it ([Store.update] with set:
     * the stored setup, not the draft, which stays a draft), then arms the alarms and the
     * fallback job ([scheduleJob]), after the set as Set does. "Wallpaper set" or
     * "Couldn't set the wallpaper"; no toast when nothing was set (the status line says it).
     * Turned on while a load, a Set or its own run goes: one run after it ([runNext]), so its set
     * and its toast aren't taken by the other; none after its own run when that one went on to
     * set (B6: on, off, on in quick taps set once), only when it gave up (the switch was off at
     * its check). Off drops a waiting run, and a running one sets nothing more ([switchOn]).
     */
    private fun switchDaily(on: Boolean) {
        daily = on
        store.switchDaily(on)
        if (!on) {
            switchRuns.off()
            DailyJob.cancel(this)
            return
        }
        switchRuns.on()
        runNext()
    }

    /**
     * The daily switch's run ([switchDaily]). Off is off: abandoned while the stored switch is off
     * before [Store.startAfresh], and [Store.update] without `set` sets only while it is still on.
     */
    private fun switchOn() {
        switching = true
        val offered = sources
        val store = store
        val app = applicationContext
        val page = id
        runs.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (!store.daily) return@runCatching null
                    store.startAfresh()
                    store.update(force = false, page = true, show = offered)
                }
            }
            val updated = result.getOrNull()
            // It set (or tried) the stored setup, the switch on: an on meanwhile owes no second set.
            // Right here, before anything that may suspend (show), so an off and on after it owe a
            // run that runNext starts (B6 fix round 1: else they were dropped and no job scheduled).
            switchRuns.ended(checkedOn = updated?.wentToSet == true)
            // Switched off again meanwhile: off is off.
            if (store.daily) scheduleJob(app)
            result.exceptionOrNull()?.let { Log.w("APODroid", "Switch on failed: $it", it) }
            updated?.done?.let { Log.i("APODroid", "Switch on: $it") }
            updated?.failure?.let { Log.w("APODroid", "Switch on: $it", it) }
            live[page]?.redrawAfterRun(failed = updated?.failure != null || result.isFailure)
            switching = false
            when {
                result.isFailure || updated?.setFailed == true -> toast(app, "Couldn't set the wallpaper")
                updated?.done != null -> toast(app, "Wallpaper set")
            }
            runAllNext()
        }
    }

    /** A run's end on its page ([live]): what it stored and set. */
    private suspend fun redrawAfterRun(failed: Boolean) {
        this.failed = failed
        readStatus()
        if (isStale()) show()
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
     * "Set as wallpaper": saves the draft ([Store.saveSetup]: both records' pictures forgotten,
     * the change times kept), sets each screen what it says ([Store.update] with set, also with
     * the daily change off: a one-off set), then arms the alarms and the fallback job or cancels
     * them as the stored daily switch says ([scheduleJob], after the set; the switch isn't part
     * of the draft). Disabled while the
     * daily switch's run goes; the switch turned on meanwhile runs after it ([runNext]).
     */
    private fun setAsWallpaper() {
        if (setting || switching) return
        setting = true
        // Crop mode still open: the dragged part counts, as after ✓.
        if (cropping) post?.let { keepPart(it) }
        val setup = draft()
        val offered = sources
        // Only this page's own pick is renamed: another page's draft picture stays its own (B11).
        val pick = draftFallback
        // The list as the store derives it from "Where" (never saved): it follows the new "Where" (CR 3).
        val derivedList = !store.hasList && list == store.list
        val store = store
        val app = applicationContext
        val page = id
        runs.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    store.saveSetup(setup, pick)
                    store.update(force = false, page = true, show = offered, set = true) to store.setsSinceSetup()
                }
            }
            if (store.daily) scheduleJob(app) else DailyJob.cancel(app)
            setting = false
            val updated = result.getOrNull()?.first
            // Also what a load or the job set of this setup before Set's own update got there (CR 5).
            val (wasSet, setFailed) = result.getOrNull()?.second ?: (false to true)
            result.exceptionOrNull()?.let { Log.w("APODroid", "Set failed: $it", it) }
            updated?.done?.let { Log.i("APODroid", "Set: $it") }
            updated?.failure?.let { Log.w("APODroid", "Set: $it", it) }
            live[page]?.run {
                if (pick) draftFallback = store.draftFallbackFile.exists()
                if (derivedList && !store.hasList && list == setup.list) list = store.list
                // Saved: "Setup" closes on its summary. A failure leaves it open.
                if (updated != null && !setFailed) setupOpen = false
                redrawAfterRun(failed = updated?.failure != null || result.isFailure)
            }
            toast(
                app,
                when {
                    updated == null || setFailed -> "Couldn't set the wallpaper"
                    wasSet -> "Wallpaper set"
                    else -> "Setup saved. Nothing to set yet."
                },
            )
            runAllNext()
        }
    }

    /**
     * The draft's video-day choice. On a video day the stand-in's page follows it, and another
     * source's picture is fetched for it ([load]). "Use my picture" with nothing picked (neither
     * set nor in the draft) opens the picker first ([pickFirst]): the choice is made only once a
     * picture is there, so backing out keeps the one before. Only a draft: nothing is set until
     * Set, which with "Skip it" leaves the screen's picture alone (the status line says "kept").
     */
    private fun chooseVideoDays(choice: VideoDays, pickFirst: () -> Unit = {}) {
        if (videoDays == choice) return
        if (choice == VideoDays.MINE && !hasFallback && !draftFallback) {
            pickFirst()
            return
        }
        videoDays = choice
        if (apod?.isVideo == true) load()
    }

    /**
     * "If APOD is a video" hidden (the daily switch off): the draft's choice goes back to the
     * stored one and a newly picked "my picture" is dropped from the draft (its file stays, for a
     * page that may own it). On a video day the stand-in's page follows ([load]).
     */
    private fun dropVideoDays() {
        draftFallback = false
        if (videoDays == stored.videoDays) return
        videoDays = stored.videoDays
        if (apod?.isVideo == true) load()
    }

    /**
     * The draft's list: [source] on exactly [screens] (a tick, a new dropdown choice; none: an
     * untick, not of the last ticked source). Ticking NASA or Earth Observatory where it wasn't
     * ticked anywhere says they post on weekdays only.
     */
    private fun place(source: Source, screens: Set<Screen>) {
        val new = list.placing(source, screens)
        if (new == list || new.values.all { it.isEmpty() }) return
        val added = list.values.none { source in it }
        lastScreens = lastScreens + (source to screens.ifEmpty { list.screensOf(source) })
        list = new
        if (added && source != Source.APOD && !weekdayNoticeHidden) weekdayDialog = true
    }

    /**
     * The draft's switch "More NASA pictures": "Where" and the list swap, so "Setup" opens, and
     * the page offers (and fetches, [load]) the new mode's sources. The first time on, the list
     * starts from the draft's "Where".
     */
    private fun switchAllSources(on: Boolean) {
        allSources = on
        setupOpen = true
        if (on && !store.hasList && list == store.list) list = apodColumns(where.screens)
        load()
    }

    /** Save every picture: the job's, so nothing is saved now; its next run saves. */
    private fun switchSaveEvery(on: Boolean) {
        saveEvery = on
        store.saveEvery = on
    }

    /** "Largest original": the job's next save uses it. */
    private fun chooseSaveLimit(mb: Int) {
        saveLimit = mb
        store.saveLimit = mb
    }

    private fun closeWeekdayDialog(forever: Boolean) {
        weekdayDialog = false
        if (forever) {
            weekdayNoticeHidden = true
            store.weekdayNoticeHidden = true
        }
    }

    private fun savePicture(post: Post) {
        if (saving) return
        if (post.source in saved) {
            toast("Already saved")
            return
        }
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
            // Each page's own, which may not be the saved one's any more.
            readSaved()
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
     * Copies the picked picture to [Store.draftFallbackFile] (Set renames it into place); a failed
     * pick keeps the previous one. With [choose] (picked on choosing "Use my picture") the choice
     * follows once it's there. In [runs], so a rotation during the copy keeps its end: the page a
     * rotation made of this one gets the pick and the choice (CR 2: both were lost, the file there).
     */
    private fun saveFallback(uri: Uri, choose: Boolean = false) {
        val app = applicationContext
        val target = store.draftFallbackFile
        val page = id
        runs.launch {
            val ok = withContext(Dispatchers.IO) {
                val part = File(app.filesDir, "fallback.part")
                val ok = runCatching {
                    app.contentResolver.openInputStream(uri)!!.use { input -> part.outputStream().use { input.copyTo(it) } }
                }.isSuccess && isImage(part) && part.renameTo(target)
                part.delete()
                ok
            }
            if (!ok) {
                toast(app, "Couldn't use that picture")
                return@launch
            }
            live[page]?.run {
                draftFallback = true
                if (choose) chooseVideoDays(VideoDays.MINE)
            }
        }
    }

    /** ✓: crop mode closes; the frame's place and the "Whole picture" toggle are [post]'s part in the draft. */
    private fun keepPart(post: Post) {
        parts = parts + (post.source to Part(post.date, post.imageUrl, cropPosition, cropWhole))
        cropping = false
    }

    /** ⛶: crop mode opens on [post]'s draft part and whole picture. */
    private fun startCrop(post: Post) {
        cropPosition = draftPosition(post)
        cropWhole = draftWhole(post)
        cropping = true
    }

    /** ⛶'s part of [post] in the draft: the middle unless ✓ noted one for this post. */
    private fun draftPosition(post: Post): Float = position(parts, post)

    /** ⛶'s stored part of [post] ([Store.parts]), for the highlight. */
    private fun storedPosition(post: Post): Float = position(stored.parts, post)

    private fun position(parts: Map<Source, Part>, post: Post): Float =
        parts[post.source]?.takeIf { post.isNoted(it.date, it.url) }?.position ?: 0.5f

    /** Whether the draft shows [post] whole (A4): only if ✓ noted it for this post. */
    private fun draftWhole(post: Post): Boolean = whole(parts, post)

    /** Whether [post] is stored whole ([Store.parts]), for the highlight. */
    private fun storedWhole(post: Post): Boolean = whole(stored.parts, post)

    private fun whole(parts: Map<Source, Part>, post: Post): Boolean =
        parts[post.source]?.takeIf { post.isNoted(it.date, it.url) }?.whole ?: false

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

    private fun toast(text: String) = toast(this, text)

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
                        PictureSide()
                        HorizontalDivider()
                        Settings()
                    }
                }
            } else {
                Row {
                    PullToRefreshBox(isRefreshing = refreshing, onRefresh = ::pullToRefresh, modifier = Modifier.weight(0.55f)) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            PictureSide()
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

    /**
     * The picture side: the shown source's title and picture in a pager over the sources the page
     * offers, so a swipe shows the previous or the next one (title and picture slide, stopping at
     * the ends); in crop mode a drag moves the frame instead. The boxes and the caption below show
     * the page that settled, the shown source.
     */
    @Composable
    private fun PictureSide() {
        val sources = sources
        val index = sources.indexOf(shown)
        // Afresh when the offered sources change, on the shown one.
        val pager = remember(sources) { PagerState(index.coerceAtLeast(0)) { sources.size } }
        // The page a swipe settled on is the shown source.
        LaunchedEffect(pager) {
            snapshotFlow { pager.settledPage }.collect { page ->
                sources.getOrNull(page)?.let { if (it != shown) showTab(it) }
            }
        }
        // The shown source changed otherwise (the dropdown, the picture on the wallpaper): the pager follows.
        LaunchedEffect(pager, index) {
            if (index >= 0 && pager.currentPage != index) pager.animateScrollToPage(index)
        }
        // Back leaves crop mode without keeping the part; ⛶ opens on the draft's again.
        BackHandler(enabled = cropping) { cropping = false }
        // A pager of its own for each list: when the offered sources shrink (the mode switch off
        // in the draft) the old pager's layout would otherwise ask the new, shorter list for a
        // page it no longer has (IndexOutOfBounds, seen on the tablet in S6). The key and the
        // pages read the list defensively too.
        key(sources) {
            HorizontalPager(
                pager,
                verticalAlignment = Alignment.Top,
                userScrollEnabled = !cropping,
                key = { sources.getOrNull(it)?.name ?: it },
            ) { page ->
                sources.getOrNull(page)?.let { source ->
                    Column {
                        SourceTitle(source, sources)
                        PicturePage(source)
                    }
                }
            }
        }
        Caption()
    }

    /**
     * The source's short name, large; with two or more sources "▾" and a menu of them, ✓ before
     * the shown one.
     */
    @Composable
    private fun SourceTitle(source: Source, sources: List<Source>) {
        val menu = sources.size > 1
        var open by remember { mutableStateOf(false) }
        // The tap target at least 48 dp tall.
        Box(Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp)) {
            Text(
                if (menu) "${source.shortLabel}\u00A0▾" else source.shortLabel,
                Modifier
                    .then(if (menu) Modifier.clickable(role = Role.DropdownList) { open = true } else Modifier)
                    .clearAndSetSemantics { contentDescription = "Picture from: ${source.label}" }
                    .padding(8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                sources.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice.shortLabel) },
                        onClick = {
                            open = false
                            if (choice != shown) showTab(choice)
                        },
                        leadingIcon = { Text(if (choice == source) "✓" else "") },
                    )
                }
            }
        }
    }

    /**
     * One page of the pager: [source]'s stored picture with the chip, ⛶ and ★, or the ▶ tile on a
     * video day; a spinner while there's none yet, or why it failed.
     */
    @Composable
    private fun PicturePage(source: Source) {
        val post = posts[source]
        val picture = pictures[source]
        if (post == null || (picture == null && !post.isVideo)) {
            Box(Modifier.fillMaxWidth().aspectRatio(3f / 2f), contentAlignment = Alignment.Center) {
                when {
                    loading -> CircularProgressIndicator()
                    failed && source == shown -> Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(problem?.let(::problemText) ?: "Couldn't load today's picture.", textAlign = TextAlign.Center)
                        TextButton(onClick = ::load) { Text("Try again") }
                    }
                }
            }
            return
        }
        if (picture == null) {
            VideoTile(post)
            return
        }
        val chip = if (sources.size > 1) chipText(post) else null
        // Only the shown page crops; the pager doesn't move meanwhile.
        val crop = cropping && source == shown
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                bitmap = picture,
                contentDescription = post.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(picture.width.toFloat() / picture.height)
                    // With "Whole picture" on there's no frame: the page shows it whole.
                    .then(
                        when {
                            crop && cropWhole -> Modifier
                            crop -> Modifier.cropFrame(picture, post)
                            else -> Modifier.clickable { openPage(post.pageUrl) }
                        },
                    ),
                contentScale = ContentScale.FillWidth,
            )
            // Not over the crop frame.
            if (chip != null && !crop) {
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
            // Crop mode's toggle (A4), where the chip is otherwise: the whole picture instead of a part.
            if (crop) {
                val wholeChanged = cropWhole != storedWhole(post)
                Text(
                    (if (cropWhole) "✓ " else "") + "Whole picture",
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .toggleable(cropWhole, role = Role.Switch) { cropWhole = it }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clearAndSetSemantics {
                            contentDescription = "Whole picture"
                            stateDescription = (if (cropWhole) "On" else "Off") + if (wholeChanged) ", not set yet" else ""
                        },
                    color = if (wholeChanged) MaterialTheme.colorScheme.tertiary else Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            // ✓ keeps the chosen part and the whole picture in the draft (Set stores them).
            IconButton(
                onClick = { if (!crop) startCrop(post) else keepPart(post) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
            ) {
                // The draft's part or whole picture differs from the stored one: in tertiary until Set.
                val partChanged = draftPosition(post) != storedPosition(post) || draftWhole(post) != storedWhole(post)
                Text(
                    if (crop) "✓" else "⛶",
                    Modifier.clearAndSetSemantics {
                        contentDescription = if (crop) "Done" else "Choose the wallpaper's part"
                        if (partChanged) stateDescription = "not set yet"
                    },
                    color = if (partChanged) MaterialTheme.colorScheme.tertiary else Color.White,
                    fontSize = 24.sp,
                )
            }
            IconButton(
                onClick = { savePicture(post) },
                modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape),
            ) {
                val isSaved = source in saved
                Text(
                    if (isSaved) "★" else "☆",
                    Modifier.clearAndSetSemantics {
                        contentDescription = when {
                            saving -> "Saving"
                            isSaved -> "Saved"
                            else -> "Save"
                        }
                    },
                    color = Color.White.copy(alpha = if (saving) 0.4f else 1f),
                    fontSize = 24.sp,
                )
            }
        }
    }

    /**
     * Below the pager, for the shown source only (so the height doesn't jump while dragging):
     * every box that applies (problem, video notice, battery hint), then the caption. With none
     * stored yet only the battery hint.
     */
    @Composable
    private fun Caption() {
        val apod = apod
        val post = post
        if (post == null || (post.source !in pictures && !post.isVideo)) {
            BatteryNotice()
            return
        }
        var expanded by rememberSaveable(post.source, post.date) { mutableStateOf(false) }
        // Whether the two-line preview cuts the explanation off; a short one has nothing to expand.
        var cut by remember(post.explanation) { mutableStateOf(false) }
        var creditCut by remember(post.credit) { mutableStateOf(false) }
        val expandable = cut || creditCut || expanded
        val named = sources.size > 1
        Column {
            val shownProblem = problem?.takeIf { problemKey(it) != problemNoticeKey }
            if (shownProblem != null) {
                NoticeBox({ TextButton(onClick = { dismissProblem(shownProblem) }) { Text("Dismiss") } }, problem = true) {
                    Text(problemText(shownProblem), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (apod?.isVideo == true && !videoNoticeHidden && videoNoticeDate != apod.date.toString()) VideoNotice()
            BatteryNotice()
            // With two or more sources a source line names it; with one the caption stays as 0.4.0's.
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
                if (named) {
                    Text(
                        "${post.source.label} · $dateLine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(post.title, style = MaterialTheme.typography.titleLarge)
                if (!named) {
                    Text(dateLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // APOD's only; one line, all of it while the explanation is open.
                post.credit?.let { credit ->
                    Text(
                        credit,
                        Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
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
        }
    }

    /**
     * Says why the wallpaper didn't change on a video day and what the user can do; follows the
     * settings. With the switch on (also with turns) what takes APOD's place; with APOD ticked
     * nowhere only that it's a video.
     */
    @Composable
    private fun VideoNotice() {
        // ▶ is only on APOD's page.
        val watch = if (post?.isVideo == true) " Tap ▶ to watch it on NASA's site." else ""
        val kept = "This APOD is a video, so your wallpaper stays as it was."
        // What is set, not the draft.
        val other = stored.videoDays.source
        val apodTicked = stored.columns.values.any { Source.APOD in it }
        val mine = stored.videoDays == VideoDays.MINE
        val text = when {
            !daily || (stored.allSources && !apodTicked) -> "This APOD is a video.$watch"
            stored.allSources -> when {
                other != null && standInReady -> "This APOD is a video, so ${other.possessive} takes its place.$watch"
                other != null -> "This APOD is a video. ${other.possessive} takes its place once it has loaded.$watch"
                mine && hasFallback -> "This APOD is a video, so your own picture takes its place.$watch"
                mine -> "This APOD is a video, so it's skipped.$watch Pick a picture below to use it on video days."
                else -> "This APOD is a video, so it's skipped.$watch To change that, choose another picture for video days below."
            }
            // B15: "takes its place", not "is the wallpaper": the wallpaper changes at the next check or change time.
            other != null && pictureSource(pageScreen) == other -> "This APOD is a video, so ${other.possessive} takes its place.$watch"
            other != null -> "This APOD is a video. ${other.possessive} takes its place once it has loaded.$watch"
            mine && hasFallback -> "This APOD is a video, so your own picture takes its place.$watch"
            mine -> "$kept$watch Pick a picture below to use it on video days."
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
            if (horizontal) {
                detectHorizontalDragGestures { change, delta -> change.consume(); move(delta) }
            } else {
                detectVerticalDragGestures { change, delta -> change.consume(); move(delta) }
            }
        }
    }

    /**
     * Wallpaper: the daily switch (at once, S4f2), under it the status lines while it's on. Then
     * "Setup" (S5), a header that opens and closes the draft's settings (closed: the summary of
     * what is stored): the switch "More NASA pictures" at its top (J3), then in APOD mode
     * "Where", with the switch on the list ("Pictures from"); while the daily switch is on "New
     * picture at", then, while a column takes turns, "Change every", and "If APOD is a video".
     * The set button, always. Then, while the daily switch is on, "Advanced settings", closed by
     * default: "Save every picture" (it acts at once). A setting the draft changed and Set
     * hasn't saved shows in `tertiary` ([highlight]).
     */
    @Composable
    private fun Settings() {
        // Two launchers: the result reaches the right one also after a rotation.
        val change = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let(::saveFallback) }
        val pickAndChoose = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let { saveFallback(it, choose = true) } }
        val request = PickVisualMediaRequest(PickVisualMedia.ImageOnly)
        // A hidden setting goes back to the stored one (B14, CR 7), so Set saves only what the page
        // shows and "Setup" highlights only that: "Change every" without turns or with the daily
        // switch off, "If APOD is a video" (and a newly picked "my picture") with it off.
        val showsInterval = daily && columns.values.any { it.size >= 2 }
        LaunchedEffect(showsInterval, interval, stored.interval) { if (!showsInterval) interval = stored.interval }
        LaunchedEffect(daily, videoDays, draftFallback, stored.videoDays) { if (!daily) dropVideoDays() }
        LaunchedEffect(daily, pictureAt, stored.pictureAt) { if (!daily) pictureAt = stored.pictureAt }
        val setupChanged = setupChanged()
        // Opens by itself while the draft differs (a rotation, the system killing the app).
        LaunchedEffect(setupChanged) { if (setupChanged) setupOpen = true }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading("Wallpaper")
            Row(
                Modifier.fillMaxWidth().toggleable(daily, role = Role.Switch, onValueChange = ::switchDaily),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Change wallpaper automatically", Modifier.weight(1f))
                Switch(checked = daily, onCheckedChange = null)
            }
            if (daily) {
                statusLines().forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Header("Setup", setupOpen, setupChanged, summary = setupSummary()) { setupOpen = !setupOpen }
            if (setupOpen) {
                ModeSwitch()
                if (!allSources) {
                    Heading("Where", where != stored.where)
                    Segments(Where.entries.map { it to it.label }, where, where != stored.where) { where = it }
                } else {
                    PictureList()
                }
                if (daily) PictureAtChoice()
                // Hiding it moves the page only after a tick.
                if (allSources && showsInterval) {
                    val changed = interval != stored.interval
                    Heading("Change every", changed)
                    Segments(Interval.entries.map { it to it.label }, interval, changed) { interval = it }
                }
                if (daily) {
                    Heading("If APOD is a video", videoDaysChanged())
                    // With the list: greyed out, not hidden, while APOD has no tick, so the page doesn't move.
                    VideoDaysChoice(
                        enabled = !allSources || columns.values.any { Source.APOD in it },
                        pick = { pickAndChoose.launch(request) },
                        change = { change.launch(request) },
                    )
                }
            }
            // One button, both modes, the daily switch on or off: it applies the whole draft.
            Button(
                onClick = ::setAsWallpaper,
                enabled = !setting && !switching,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Set as wallpaper") }

            // Empty while the daily switch is off: hidden then.
            if (daily) {
                Header("Advanced settings", advancedOpen, changed = false) { advancedOpen = !advancedOpen }
                if (advancedOpen) SaveEverySwitch()
            }
        }
    }

    /** The switch "More NASA pictures", at the top of "Setup": part of the draft ([switchAllSources]). */
    @Composable
    private fun ModeSwitch() {
        val modeChanged = allSources != stored.allSources
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(allSources, role = Role.Switch, onValueChange = ::switchAllSources)
                .semantics { if (modeChanged) stateDescription = "${if (allSources) "On" else "Off"}, not set yet" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("More NASA pictures", Modifier.weight(1f), color = highlight(modeChanged))
            val colors = MaterialTheme.colorScheme
            Switch(
                checked = allSources,
                onCheckedChange = null,
                colors = if (!modeChanged) SwitchDefaults.colors() else SwitchDefaults.colors(
                    checkedTrackColor = colors.tertiary,
                    checkedThumbColor = colors.onTertiary,
                    uncheckedBorderColor = colors.tertiary,
                    uncheckedThumbColor = colors.tertiary,
                ),
            )
        }
    }

    /** "New picture at" (J3): a dropdown of the hours, as "Largest original"; 12/24 h as the system says. */
    @Composable
    private fun PictureAtChoice() {
        val changed = pictureAt != stored.pictureAt
        Heading("New picture at", changed)
        Box {
            var open by remember { mutableStateOf(false) }
            val name = hourLabel(pictureAt)
            DropdownButton("$name\u00A0▾", "New picture at: $name", enabled = true, changed = changed) { open = true }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                (0..23).forEach { hour ->
                    DropdownMenuItem(
                        text = { Text(hourLabel(hour)) },
                        onClick = { open = false; pictureAt = hour },
                        leadingIcon = { Text(if (hour == pictureAt) "✓" else "") },
                    )
                }
            }
        }
    }

    /** An hour as the system's clock shows it: "06:00" or "6:00 AM". */
    private fun hourLabel(hour: Int): String =
        LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern(if (hours24) "HH:mm" else "h:mm\u00A0a", Locale.ENGLISH))

    /** Anything in "Setup" the draft changed: what the shown mode's settings differ in from the stored setup. */
    private fun setupChanged(): Boolean =
        allSources != stored.allSources ||
            (if (allSources) Source.entries.any(::rowChanged) else where != stored.where) ||
            interval != stored.interval || pictureAt != stored.pictureAt || videoDaysChanged()

    /** The list's row for [source] puts it on other screens than the stored list. */
    private fun rowChanged(source: Source) = list.screensOf(source) != stored.list.screensOf(source)

    /** Another video-day choice, or a newly picked "my picture" while it's chosen. */
    private fun videoDaysChanged() = videoDays != stored.videoDays || (videoDays == VideoDays.MINE && draftFallback)

    /**
     * "Setup"'s summary while it's closed, of the stored setup: "APOD on both screens" (APOD
     * mode), else each ticked source "NASA on home screen", joined by ", "; while the daily
     * switch is on " · every 6 h from 06:00" when a screen takes turns, else " · at 06:00".
     */
    private fun setupSummary(): String {
        fun on(screens: Set<Screen>) = when (Where.entries.first { it.screens == screens }) {
            Where.HOME -> "home screen"
            Where.LOCK -> "lock screen"
            Where.BOTH -> "both screens"
        }
        val sources = if (!stored.allSources) {
            "APOD on ${on(stored.where.screens)}"
        } else {
            stored.list.ticked().joinToString(", ") { "${it.shortLabel} on ${on(stored.list.screensOf(it))}" }
        }
        val time = hourLabel(stored.pictureAt)
        val turns = when {
            !daily -> ""
            stored.columns.values.any { it.size >= 2 } -> " · every ${stored.interval.label} from $time"
            else -> " · at $time"
        }
        return "$sources$turns"
    }

    /**
     * A segmented row, one height for all (a label that wraps, a narrow pane, a large font, made
     * its segment taller). [changed]: the draft's choice isn't stored, so it shows in `tertiary`.
     */
    @Composable
    private fun <T> Segments(choices: List<Pair<T, String>>, chosen: T, changed: Boolean, choose: (T) -> Unit) {
        val colors = MaterialTheme.colorScheme
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            choices.forEachIndexed { i, (value, label) ->
                SegmentedButton(
                    selected = chosen == value,
                    onClick = { choose(value) },
                    shape = SegmentedButtonDefaults.itemShape(i, choices.size),
                    modifier = Modifier.fillMaxHeight().semantics {
                        if (changed && chosen == value) stateDescription = "Selected, not set yet"
                    },
                    colors = if (!changed) SegmentedButtonDefaults.colors() else SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.tertiaryContainer,
                        activeContentColor = colors.onTertiaryContainer,
                        activeBorderColor = colors.tertiary,
                        inactiveBorderColor = colors.tertiary,
                    ),
                ) { Text(label) }
            }
        }
    }

    /**
     * A header that opens and closes its section ("Setup ▸", "Setup ▾"), a button for screen
     * readers ("expanded", "collapsed"); [summary] under it while it's closed. [changed]: in `tertiary`.
     */
    @Composable
    private fun Header(text: String, open: Boolean, changed: Boolean, summary: String? = null, toggle: () -> Unit) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = toggle)
                .semantics { stateDescription = (if (open) "expanded" else "collapsed") + if (changed) ", not set yet" else "" }
                .padding(top = 8.dp, bottom = 4.dp),
        ) {
            Text(
                "$text\u00A0${if (open) "▾" else "▸"}",
                Modifier.clearAndSetSemantics { contentDescription = text },
                style = MaterialTheme.typography.titleSmall,
                color = if (changed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            if (!open && summary != null) {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    /** A highlighted control's text: `tertiary` while the draft differs from the stored setup, else the default. */
    @Composable
    private fun highlight(changed: Boolean) = if (changed) MaterialTheme.colorScheme.tertiary else Color.Unspecified

    /** An outlined dropdown's border and text, in `tertiary` while [changed]. */
    @Composable
    private fun DropdownButton(text: String, description: String, enabled: Boolean, changed: Boolean, onClick: () -> Unit) {
        val tertiary = MaterialTheme.colorScheme.tertiary
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            border = if (changed && enabled) BorderStroke(1.dp, tertiary) else ButtonDefaults.outlinedButtonBorder(enabled),
            colors = if (changed) ButtonDefaults.outlinedButtonColors(contentColor = tertiary) else ButtonDefaults.outlinedButtonColors(),
        ) {
            Text(
                text,
                Modifier.clearAndSetSemantics {
                    contentDescription = description
                    if (changed) stateDescription = "not set yet"
                },
            )
        }
    }

    /**
     * "Pictures from": a row per source, a tick box with its short name and a dropdown Home screen
     * / Lock screen / Both ([place]). An unticked row's dropdown is greyed out and shows the
     * screens the source had last ([lastScreens]); a tick puts it there. The last ticked row
     * can't be unticked.
     */
    @Composable
    private fun PictureList() {
        val tickedCount = Source.entries.count { s -> columns.values.any { s in it } }
        Heading("Pictures from")
        Source.entries.forEach { source ->
            val on = columns.screensOf(source)
            val shown = on.ifEmpty { lastScreens[source] ?: Screen.entries.toSet() }
            val checked = on.isNotEmpty()
            val name = Where.entries.first { it.screens == shown }.label
            // The row differs from the stored list: its tick box, name and dropdown in tertiary.
            val changed = rowChanged(source)
            val tertiary = MaterialTheme.colorScheme.tertiary
            // Wraps the dropdown under the name when both don't fit on one line (a large font, a narrow pane).
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.toggleable(
                        checked,
                        // The last tick stays.
                        enabled = !(checked && tickedCount == 1),
                        role = Role.Checkbox,
                        onValueChange = { place(source, if (it) shown else emptySet()) },
                    ).semantics { if (changed) stateDescription = "${if (checked) "Checked" else "Not checked"}, not set yet" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        enabled = !(checked && tickedCount == 1),
                        colors = if (changed) CheckboxDefaults.colors(checkedColor = tertiary, uncheckedColor = tertiary) else CheckboxDefaults.colors(),
                    )
                    Text(source.shortLabel, Modifier.padding(start = 8.dp, end = 16.dp), color = highlight(changed), softWrap = false)
                }
                Box {
                    var open by remember { mutableStateOf(false) }
                    DropdownButton("$name\u00A0▾", "${source.shortLabel} on: $name", enabled = checked, changed = changed) { open = true }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        Where.entries.forEach { w ->
                            DropdownMenuItem(
                                text = { Text(w.label) },
                                onClick = { open = false; place(source, w.screens) },
                                leadingIcon = { Text(if (w.screens == shown) "✓" else "") },
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * "If APOD is a video": a dropdown with the four choices; while
     * "Use my picture" is chosen "Change picture" beside it. Choosing it with nothing picked
     * opens the picker at once ([pick]).
     */
    @Composable
    private fun VideoDaysChoice(enabled: Boolean, pick: () -> Unit, change: () -> Unit) {
        var open by remember { mutableStateOf(false) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Box {
                val name = videoDays.label
                DropdownButton("$name\u00A0▾", "If APOD is a video: $name", enabled, videoDaysChanged()) { open = true }
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
                TextButton(onClick = change, enabled = enabled) { Text(if (hasFallback || draftFallback) "Change picture" else "Pick picture") }
            }
        }
    }

    /**
     * "Save every picture" with a small ⓘ beside the label, which says what it costs; while it's
     * on, "Largest original" under it (F6): a dropdown of the limit for the job's originals.
     */
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
        if (saveEvery) {
            FlowRow(
                Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Largest original")
                Box {
                    var open by remember { mutableStateOf(false) }
                    val name = limitLabel(saveLimit)
                    DropdownButton("$name\u00A0▾", "Largest original: $name", enabled = true, changed = false) { open = true }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        listOf(10, 20, 50, 100, 0).forEach { mb ->
                            DropdownMenuItem(
                                text = { Text(limitLabel(mb)) },
                                onClick = { open = false; chooseSaveLimit(mb) },
                                leadingIcon = { Text(if (mb == saveLimit) "✓" else "") },
                            )
                        }
                    }
                }
            }
        }
        if (info) {
            AlertDialog(
                onDismissRequest = { info = false },
                confirmButton = { TextButton(onClick = { info = false }) { Text("OK") } },
                text = {
                    Text(
                        "Each new picture is saved to Pictures/APODroid: full size up to the largest original, " +
                            "else a screen-sized copy. A few hundred MB a month or more.",
                    )
                },
            )
        }
    }

    private fun limitLabel(mb: Int) = if (mb == 0) "No limit" else "$mb MB"

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
        } + " (on Optimized, a new picture can come hours late)"
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
    private fun Heading(text: String, changed: Boolean = false) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = if (changed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
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

/** The source's short name: the title above the picture and its menu, its row in the list (and its tick box and dropdown labels), the status line. */
private val Source.shortLabel
    get() = when (this) {
        Source.APOD -> "APOD"
        Source.NASA -> "NASA"
        Source.EARTH -> "Earth"
    }

/** The source's picture in a sentence, e.g. "NASA's Image of the Day takes its place". */
private val Source.possessive
    get() = when (this) {
        Source.APOD -> "the APOD"
        Source.NASA -> "NASA's Image of the Day"
        Source.EARTH -> "Earth Observatory's Image of the Day"
    }

/** "Where"'s segments and the list's dropdowns. */
private val Where.label
    get() = when (this) {
        Where.HOME -> "Home screen"
        Where.LOCK -> "Lock screen"
        Where.BOTH -> "Both"
    }

/** "Change every"'s segments and the Setup summary. */
private val Interval.label
    get() = when (this) {
        Interval.DAY -> "24 h"
        Interval.H12 -> "12 h"
        Interval.H6 -> "6 h"
        Interval.H3 -> "3 h"
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

/** The sources ticked on any screen, in order. */
private fun Map<Screen, List<Source>>.ticked(): List<Source> = Source.entries.filter { s -> values.any { s in it } }

/** The draft in the saved state (a rotation, the system killing the app). */
private fun Setup.toBundle() = Bundle().apply {
    putBoolean("allSources", allSources)
    putString("where", where.name)
    Screen.entries.forEach { screen -> putString("list.${screen.name}", list[screen].orEmpty().joinToString(",") { it.name }) }
    putString("interval", interval.name)
    putString("videoDays", videoDays.name)
    putInt("pictureAt", pictureAt)
    parts.forEach { (source, part) -> putString("part.${source.name}", "${part.date}|${part.position}|${part.whole}|${part.url.orEmpty()}") }
}

/** [toBundle]'s draft, or null when it can't be read. */
private fun readDraft(bundle: Bundle): Setup? = runCatching {
    Setup(
        allSources = bundle.getBoolean("allSources"),
        where = Where.valueOf(bundle.getString("where")!!),
        list = Screen.entries.associateWith { screen ->
            val names = bundle.getString("list.${screen.name}").orEmpty().split(',')
            Source.entries.filter { it.name in names }
        },
        interval = Interval.valueOf(bundle.getString("interval")!!),
        videoDays = VideoDays.valueOf(bundle.getString("videoDays")!!),
        pictureAt = bundle.getInt("pictureAt", DEFAULT_PICTURE_AT).takeIf { it in 0..23 } ?: DEFAULT_PICTURE_AT,
        parts = Source.entries.mapNotNull { source ->
            bundle.getString("part.${source.name}")?.split('|', limit = 4)?.let { (date, position, whole, url) ->
                source to Part(LocalDate.parse(date), url.ifEmpty { null }, position.toFloat(), whole.toBooleanStrict())
            }
        }.toMap(),
    )
}.getOrNull()
