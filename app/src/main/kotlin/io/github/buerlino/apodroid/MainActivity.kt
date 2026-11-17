package io.github.buerlino.apodroid

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
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
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import io.github.buerlino.apodroid.core.DEFAULT_PICTURE_AT
import io.github.buerlino.apodroid.core.Maker
import io.github.buerlino.apodroid.core.Part
import io.github.buerlino.apodroid.core.PickerTrip
import io.github.buerlino.apodroid.core.Picture
import io.github.buerlino.apodroid.core.Post
import io.github.buerlino.apodroid.core.Problem
import io.github.buerlino.apodroid.core.Screen
import io.github.buerlino.apodroid.core.Size as Sizing
import io.github.buerlino.apodroid.core.Source
import io.github.buerlino.apodroid.core.SwitchRuns
import io.github.buerlino.apodroid.core.apodColumns
import io.github.buerlino.apodroid.core.darkening
import io.github.buerlino.apodroid.core.equalColumns
import io.github.buerlino.apodroid.core.firstStarted
import io.github.buerlino.apodroid.core.forPost
import io.github.buerlino.apodroid.core.framePosition
import io.github.buerlino.apodroid.core.hasUp
import io.github.buerlino.apodroid.core.livePosition
import io.github.buerlino.apodroid.core.maker
import io.github.buerlino.apodroid.core.missedWhen
import io.github.buerlino.apodroid.core.notedPosition
import io.github.buerlino.apodroid.core.notedSideways
import io.github.buerlino.apodroid.core.orderOf
import io.github.buerlino.apodroid.core.partOf
import io.github.buerlino.apodroid.core.pictureAtOrDefault
import io.github.buerlino.apodroid.core.pictureHours
import io.github.buerlino.apodroid.core.placing
import io.github.buerlino.apodroid.core.screensOf
import io.github.buerlino.apodroid.core.sidewaysHelps
import io.github.buerlino.apodroid.core.sidewaysOf
import io.github.buerlino.apodroid.core.sizeOf
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    /** Their files' upright sizes ([uprightSize], EXIF applied), for ⛶'s pill and the sideways rule (SW3). */
    private var uprightSizes by mutableStateOf(emptyMap<Source, IntSize>())

    /**
     * VS1: the still from APOD's video, decoded, behind ▶ ([VideoTile]); the page only. Let go
     * with the pictures ([onStop]).
     */
    private var videoStill by mutableStateOf<VideoStill?>(null)

    /**
     * The sizes of the pictures [onStop] let go (O12), until [show] decodes them again: the pages
     * keep their height meanwhile (an empty box of the picture's shape), so the page doesn't jump.
     */
    private var stoppedSizes by mutableStateOf(emptyMap<Source, IntSize>())

    /** [onStop] let the pictures go: the next [load] shows them again, also while one runs. */
    private var restore = false

    /** The shown source's post: the boxes and the caption below the pager, ⛶'s part, Set's crop. */
    private val post
        get() = posts[shown]

    /** [show] calls: only the last one started draws (it read the store last). */
    private var showing = 0

    /** The source the user chose (the dropdown, a swipe); null opens on the source on the wallpaper. Survives rotation, not a restart. */
    private var chosenSource by mutableStateOf<Source?>(null)
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

    /** Size Live set and home with a column, APODroid's live wallpaper not on home ([Store.liveMissing], L6): the status line says so. */
    private var liveMissing by mutableStateOf(false)

    /** APOD mode's screen ([Store.pageScreen]): the lock screen's record with "Where" = Lock. */
    private var pageScreen by mutableStateOf(Screen.HOME)

    /** On an APOD video day: the stand-in is stored ([Store.sourcePicture] of APOD), for the video notice. */
    private var standInReady by mutableStateOf(false)

    /** The offered sources whose stored post ★ saved ([Store.isSaved]). */
    private var saved by mutableStateOf(emptySet<Source>())

    /** ★ is downloading the original; taps wait for it. */
    private var saving by mutableStateOf(false)
    private var saveEvery by mutableStateOf(false)

    /** "Largest original" ([Store.saveLimit], MB, 0: no limit): at once, as "Save every picture". */
    private var saveLimit by mutableIntStateOf(20)

    /** "Keep running" ([Store.keepRunning], O11): at once, as "Save every picture" ([switchKeepRunning]). */
    private var keepRunning by mutableStateOf(false)

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

    /** "As soon as it's out" ([Store.soon]): a newer picture goes on within the hour, not only in the window. */
    private var soon by mutableStateOf(false)

    /** "Darken bright pictures" ([Store.darken], BR2): each set darkens a bright picture a little. */
    private var darken by mutableStateOf(true)

    /** "Centre when unsure" ([Store.autoFallback], UI1; SA3b's Advanced switch before): Auto keeps the middle for a small move. */
    private var autoFallback by mutableStateOf(true)

    /** Size ([Store.size], L6, SA3a): Centre, Auto (the frame on the subject), or Live (home's picture moving, drawn by APODroid's live wallpaper). */
    private var size by mutableStateOf(Sizing.AUTO)

    /**
     * The list's order ([Store.order], OR2): all three sources, the top first, sorted by drag.
     * Kept in APOD mode too (as the list), saved only while the list shows.
     */
    private var order by mutableStateOf(Source.entries.toList())

    /** ⛶'s part per source ([Store.parts]); ✓ notes the shown post's. */
    private var parts by mutableStateOf(emptyMap<Source, Part>())

    /** A "my picture" picked and not set yet ([Store.draftFallbackFile]). */
    private var draftFallback by mutableStateOf(false)

    /**
     * Page state a rotation keeps (FP6, 2026-11-15, C8): the open ⓘ dialog (its "About …" name),
     * the post whose explanation is open ("SOURCE:date"), the pause hint's "Hide" question. Not
     * `rememberSaveable`: the one-column and two-pane layouts compose them in different places, so
     * a rotation across 600 dp lost them (and an old state came back at a later rotation).
     */
    private var openInfo by mutableStateOf<String?>(null)
    private var expandedPost by mutableStateOf<String?>(null)
    private var pauseConfirm by mutableStateOf(false)

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

    /** The detector's box asked for "Keep running": Advanced opens and its switch scrolls into view once. */
    private var showKeepRunning by mutableStateOf(false)

    /** The detector (O14): the last miss's change time, whether "Keep running" was on then, the one dismissed. */
    private var missedAt by mutableStateOf<Instant?>(null)
    private var missedKeep by mutableStateOf(false)
    private var missDismissed by mutableStateOf<Instant?>(null)

    private var weekdayNoticeHidden by mutableStateOf(false)

    /** The weekday dialog shows. */
    private var weekdayDialog by mutableStateOf(false)
    private var batteryLimited by mutableStateOf(false)
    private var mayHibernate by mutableStateOf(false)
    private var pauseHintHidden by mutableStateOf(false)

    /** The phone's maker, for the pause hint's battery line and the detector's lines. */
    private val phoneMaker = maker(Build.MANUFACTURER)
    private var videoNoticeDate by mutableStateOf<String?>(null)
    private var videoNoticeHidden by mutableStateOf(false)
    private var problemNoticeKey by mutableStateOf<String?>(null)
    private var cropping by mutableStateOf(false)
    private var cropPosition by mutableFloatStateOf(0.5f)

    /**
     * Whether ⛶'s frame was dragged since it opened (SA3b): ✓ then notes [cropPosition]; else the
     * part keeps its previous position (null, not dragged, so the stored Size keeps deciding).
     * The "Sideways" pill turning the frame clears it (FP1: a drag belongs to its orientation).
     */
    private var cropMoved by mutableStateOf(false)

    /** Crop mode's "Whole picture" toggle (A4): ✓ keeps it in the draft with the part, Back drops it. */
    private var cropWhole by mutableStateOf(false)

    /**
     * Crop mode's "Sideways" toggle (SW2, issue #4): the picture turned 90° clockwise; ✓ saves and
     * sets it, Back drops it. It opens on the value used ([turnsSideways]: the pill's, else Auto's turn).
     */
    private var cropSideways by mutableStateOf(false)

    /**
     * Whether the pill was touched since ⛶ opened (SW3): ✓ then notes its value; untouched (and the
     * frame not dragged), ✓ keeps the part's previous one (null stays Auto's turn, so a later change
     * of Size or of NASA's text applies).
     */
    private var cropSidewaysTouched by mutableStateOf(false)

    /** The clock ⛶'s Live frame follows (L7): moved on by a minute tick while it's open. */
    private var liveNow by mutableStateOf(Instant.now())
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

        /** VS1: a video still is being made ([makeVideoStill]), in any page. */
        var makingStill = false

        /** "Set as wallpaper" is running, in any page (one run at a time in the app). */
        var setting by mutableStateOf(false)

        /** The daily switch's run is going ([switchOn]); Set waits for it. */
        var switching by mutableStateOf(false)

        /**
         * Set opened Android's live wallpaper picker (L6) and waits for its return and the engine
         * ([Store.awaitLive]): a page's load waits too ([load]), so it doesn't set a still picture
         * first over the live wallpaper just picked.
         */
        var picking = false

        /** Completed by the picker's return ([livePicker]'s result, or a page's [onResume] as a fallback). */
        var pickerBack: CompletableDeferred<Unit>? = null

        /**
         * The dialog before Set's picker (2026-11-15, [PickerDialog]): which of Android's answers to
         * tap. Process-wide, so the page a rotation makes of [PickerAsk.page] shows it again.
         */
        var pickerAsk by mutableStateOf<PickerAsk?>(null)

        /** ⛶'s ✓ whose set failed ([setPart]): ✓ with that part again isn't "no change". */
        var failedPart: Part? = null

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

    /**
     * Android's live wallpaper picker (L6), opened by Set ([openLivePicker]): whatever it
     * answers, its return lets Set go on ([pickerBack]). Registered with the page, so a page a
     * rotation made gets the result too.
     */
    private val livePicker = registerForActivityResult(StartActivityForResult()) { pickerBack?.complete(Unit) }

    /**
     * Opens Android's picker for APODroid's live wallpaper, started directly (L1: `resolveActivity`
     * is null without `<queries>` while the start works), else the list of live wallpapers; neither:
     * a toast, and the Set goes on as Fill. Returns whether a picker opened.
     */
    private fun openLivePicker(): Boolean {
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, LiveWallpaper::class.java))
        // Any refusal (no activity, a maker's picker not exported or behind a permission) tries the next.
        val started = firstStarted(
            listOf(direct, Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)),
            { intent, e -> Log.i("APODroid", "Set: couldn't start ${intent.action}: $e") },
        ) { livePicker.launch(it) }
        if (started != null) return true
        toast("This phone can't show live wallpapers.")
        return false
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
        openInfo = savedInstanceState?.getString("openInfo")
        expandedPost = savedInstanceState?.getString("expandedPost")
        pauseConfirm = savedInstanceState?.getBoolean("pauseConfirm") == true
        advancedOpen = savedInstanceState?.getBoolean("advancedOpen") == true
        chosenSource = enumValue<Source>(savedInstanceState?.getString("chosenSource"))
        daily = store.daily
        saveEvery = store.saveEvery
        saveLimit = store.saveLimit
        keepRunning = store.keepRunning
        pauseHintHidden = store.pauseHintHidden
        videoNoticeDate = store.videoNoticeDate
        videoNoticeHidden = store.videoNoticeHidden
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
            enumValue<Where>(savedInstanceState?.getString("lastScreens.${source.name}"))?.screens
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
        // Its page closed for good (Back, Recents) with the picker's dialog open: Cancel, so Set ends.
        if (!isChangingConfigurations) pickerAsk?.takeIf { it.page == id }?.answer?.complete(PickerAnswer.CANCEL)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pageId", id)
        outState.putString("chosenSource", chosenSource?.name)
        outState.putBoolean("weekdayDialog", weekdayDialog)
        outState.putBoolean("setupOpen", setupOpen)
        outState.putString("openInfo", openInfo)
        outState.putString("expandedPost", expandedPost)
        outState.putBoolean("pauseConfirm", pauseConfirm)
        outState.putBoolean("advancedOpen", advancedOpen)
        outState.putBundle("draft", draft().toBundle())
        outState.putBoolean("draftFallback", draftFallback)
        lastScreens.forEach { (source, screens) ->
            outState.putString("lastScreens.${source.name}", Where.of(screens)?.name)
        }
    }

    /** The draft as a [Setup], for Set and the saved state. */
    private fun draft() = Setup(allSources, where, list, interval, videoDays, parts, pictureAt, soon, darken, order, size, autoFallback)

    private fun useDraft(setup: Setup) {
        allSources = setup.allSources
        where = setup.where
        list = setup.list
        interval = setup.interval
        videoDays = setup.videoDays
        parts = setup.parts
        pictureAt = setup.pictureAt
        soon = setup.soon
        darken = setup.darken
        autoFallback = setup.autoFallback
        order = setup.order
        size = setup.size
    }

    /** The draft's columns: APOD mode's from "Where", else the list. A column with two or more takes turns every [interval]. */
    private val columns
        get() = draft().columns

    override fun onResume() {
        super.onResume()
        // Back from Set's picker without its result (started from a page in the background, L6).
        pickerBack?.complete(Unit)
        // The picker's dialog of a page that's gone: Cancel, so Set ends.
        pickerAsk?.takeIf { it.page !in live }?.answer?.complete(PickerAnswer.CANCEL)
        // Hibernation or a force stop deleted the job (scheduling runs it at once) and the alarms.
        if (store.daily && !DailyJob.ensure(this)) Log.w("APODroid", "Couldn't schedule the daily job again")
        // Keep running (O11) back after Task Manager's Stop, a force stop or a refused start: the
        // page in the foreground may start it.
        KeepRunning.ensure(this)
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
     * Lets the decoded pictures go (O12: with Keep running or Live the process stays alive for
     * days, and O7 saw them held in the native heap after Back). The composition holds them while
     * the page is stopped (it doesn't recompose then), so they're recycled, not only dropped; a
     * [show] that runs meanwhile ends without drawing (one going now too). [onResume]'s [load]
     * decodes them again. The draft, crop mode's part and the shown source stay.
     */
    override fun onStop() {
        super.onStop()
        // Always, also with no pictures yet: a [show] past its STARTED check, still decoding,
        // must not draw into the stopped page (its pictures would stay for days).
        showing++
        restore = true
        val still = videoStill?.picture
        if (pictures.isEmpty() && still == null) return
        val dropped = pictures.values + listOfNotNull(still)
        if (pictures.isNotEmpty()) stoppedSizes = pictures.mapValues { (_, picture) -> IntSize(picture.width, picture.height) }
        pictures = emptyMap()
        videoStill = null
        // The composition hears of it before its next frame draws (a recycled picture isn't drawn: [PicturePainter]).
        Snapshot.sendApplyNotifications()
        dropped.forEach { it.asAndroidBitmap().recycle() }
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
            chosenSource?.takeIf { it in s } ?: onScreens.firstOrNull { it in s } ?: s.first()
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
        if (loading || switching || picking) {
            loadAgain = force || loadAgain == true
            // A page a rotation made during the daily switch's run: what's stored meanwhile; a page
            // back from the background (O12): its pictures.
            if (posts.isEmpty() || restore) lifecycleScope.launch {
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
    } || stillMissing()

    /** VS1: APOD's video still was made (the job's notes) and this page doesn't show it yet. */
    private fun stillMissing() = posts[Source.APOD]?.takeIf { it.isVideo && it.videoUrl != null }?.let { post ->
        videoStill?.isFor(post) != true && store.videoStill(post) != null
    } == true

    private suspend fun readSaved() {
        val offered = sources
        saved = withContext(Dispatchers.IO) { offered.filter(store::isSaved).toSet() }
    }

    /**
     * What the last check and the last sets stored (also the job's), the stored setup, and the
     * stored APOD, which decides the pages. The draft's parts are the stored ones: ✓ saves at
     * once, so a page's own (one under a stacked page, or restored after the system killed the
     * app) mustn't bring an older one back with Set.
     */
    private fun readStatus() {
        stored = store.setup
        parts = stored.parts
        hasFallback = store.fallbackFile.exists()
        apod = store.apod
        pageScreen = store.pageScreen
        screens = Screen.entries.associateWith(store::screenState)
        liveMissing = store.liveMissing
        standInReady = apod?.isVideo == true && store.sourcePicture(Source.APOD) != null
        missedAt = store.missedAt
        missedKeep = store.missedKeep
        missDismissed = store.missDismissed
        readProblem()
    }

    /**
     * One problem box, by the stored setup. In APOD mode the first of APOD and its video-day
     * source with a problem (APOD's first). With the switch on the shown source's, else the first
     * ticked source's. A problem dismissed today ([dismissProblem]) gives way to the next one
     * that isn't (FP6 tests' review, 2026-11-15: a dismissed APOD problem hid NASA's).
     */
    private fun readProblem() {
        val candidates = if (stored.allSources) {
            listOf(shown) + stored.columns.ticked()
        } else {
            listOfNotNull(Source.APOD, stored.videoDays.source.takeIf { apod?.isVideo == true })
        }
        val failing = candidates.filter { store.problem(it) != null }
        problemSource = failing.firstOrNull { source -> store.problem(source)?.let { problemKey(it, source) } != store.problemNoticeKey(source) }
            ?: failing.firstOrNull() ?: shown
        problem = store.problem(problemSource)
        problemNoticeKey = store.problemNoticeKey(problemSource)
        checkedAt = store.checkedAt(problemSource)
    }

    /**
     * The cause, short (U0, 2026-11-02), with since when nothing has worked once that's more than a
     * day: "NASA's site: no connection since 27 Oct."; the source's name instead of "NASA's site"
     * when it isn't APOD's or two or more sources are ticked.
     */
    private fun problemText(problem: Problem): String {
        val since = checkedAt?.takeIf { it < Instant.now().minus(1, ChronoUnit.DAYS) }
            ?.let { " since " + it.atZone(zone).format(dayFormat) }.orEmpty()
        val named = !(problemSource == Source.APOD && apodAlone)
        val subject = if (named) problemSource.label else "NASA's site"
        return when (problem) {
            Problem.Network -> "$subject: no connection$since."
            is Problem.Http -> "$subject: error ${problem.code}$since. Trying again later."
            Problem.Storage -> (if (named) "$subject: " else "") + "Couldn't store the picture. Check the free space."
            Problem.SiteChanged -> "$subject has changed${if (since.isEmpty()) "" else " (nothing new$since)"}. An app update may be needed."
        }
    }

    /**
     * Under the daily switch: when the wallpaper last changed, from where when it isn't APOD or
     * two or more sources are ticked, or that a video day kept it ([ScreenState.kept]). In APOD
     * mode one line, for the screen "Where" names ("Changed 2 Nov, 06:12", "Video day, previous
     * kept"; U0, 2026-11-02). With the switch on one line while both screens show the same
     * picture, else one per screen that was set or kept ("Home: 2 Nov, 06:12 · NASA", "Lock:
     * video day, previous kept"). Then, with Size Live and home not live, "Live isn't on…" (L6).
     */
    private fun statusLines(): List<String> = changedLines() + listOfNotNull("Live isn't on. Set as wallpaper turns it on.".takeIf { liveMissing })

    private fun changedLines(): List<String> {
        // [name] null: the one line for the whole wallpaper.
        fun line(name: String?, state: ScreenState, setAt: Instant? = state.setAt): String? = when {
            state.kept -> if (name == null) "Video day, previous kept" else "$name: video day, previous kept"
            else -> setAt?.let {
                val time = if (hours24) "d MMM, HH:mm" else "d MMM, h:mm\u00A0a"
                (if (name == null) "Changed " else "$name: ") + it.atZone(zone).format(DateTimeFormatter.ofPattern(time, Locale.ENGLISH)) +
                    pictureSuffix(state.picture)
            }
        }
        if (!stored.allSources) return listOfNotNull(screens[pageScreen]?.let { line(null, it) })
        val home = screens[Screen.HOME]
        val lock = screens[Screen.LOCK]
        if (home != null && lock != null && home.picture != null && home.picture == lock.picture && home.kept == lock.kept) {
            return listOfNotNull(line(null, home, listOfNotNull(home.setAt, lock.setAt).maxOrNull()))
        }
        return listOfNotNull(home?.let { line("Home", it) }, lock?.let { line("Lock", it) })
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
        // A stopped page decodes nothing (O12): [onResume]'s [load] shows it again.
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
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
        val sizes = withContext(Dispatchers.IO) {
            latest.filter { (_, post) -> !post.isVideo }.mapNotNull { (source, _) ->
                uprightSize(store.imageFile(source))?.let { source to IntSize(it.width, it.height) }
            }.toMap()
        }
        val isSaved = withContext(Dispatchers.IO) { offered.filter(store::isSaved).toSet() }
        // VS1: APOD's video still, decoded once per post; made once when the video is NASA's mp4.
        val video = latest[Source.APOD]?.takeIf { it.isVideo && it.videoUrl != null }
        val still = video?.let { post ->
            videoStill?.takeIf { it.isFor(post) && !it.picture.asAndroidBitmap().isRecycled } ?: withContext(Dispatchers.IO) {
                store.videoStill(post)?.let { BitmapFactory.decodeFile(it.path) }?.let { VideoStill(post.date, post.videoUrl!!, it.asImageBitmap()) }
            }
        }
        // A later show() read the store after this one: it draws.
        if (run != showing) return
        if (video != null && still == null) makeVideoStill(video)
        videoStill = still
        val old = posts[shown]
        val new = latest[shown]
        if (old?.date != new?.date || old?.imageUrl != new?.imageUrl) cropping = false
        posts = latest
        pictures = kept + decoded
        uprightSizes = sizes
        stoppedSizes = emptyMap()
        restore = false
        saved = isSaved
    }

    /**
     * VS1: makes [post]'s video still once, in the process-wide [runs] (as Set's), so a rotation
     * during it doesn't leave its try noted with no page to show it. Its notes reach the open page
     * through [Store.watch]; a page opened later reads the file.
     */
    private fun makeVideoStill(post: Post) {
        if (makingStill || !store.videoStillOwed(post)) return
        makingStill = true
        val store = store
        runs.launch {
            try {
                withContext(Dispatchers.IO) { store.makeVideoStill(post) }
            } finally {
                makingStill = false
            }
        }
    }

    /** The dropdown's choice or the page a swipe settled on. Crop mode closes. */
    private fun showSource(source: Source) {
        chosenSource = source
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
            KeepRunning.stop(this)
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
            KeepRunning.ensure(app)
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

    /** Android's Battery optimisation list ("Don't allow" there is Unrestricted); App info where a phone lacks it. */
    private fun openBatteryOptimisation() {
        try {
            startActivity(Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            openAppInfo()
        }
    }

    /** The detector's box hidden until the next miss. */
    private fun dismissMiss() {
        missDismissed = missedAt
        store.missDismissed = missedAt
    }

    /** The detector's "Keep running": Advanced settings opened at the switch (not turned on). */
    private fun openKeepRunning() {
        advancedOpen = true
        showKeepRunning = true
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
    private fun problemKey(problem: Problem, of: Source = problemSource): String {
        val kind = if (problem is Problem.Http) "${problem.key}:${problem.code}" else problem.key
        val source = if (of == Source.APOD) "" else "${of.name.lowercase()}:"
        return "$source$kind:${LocalDate.now()}"
    }

    private fun dismissProblem(problem: Problem) {
        val key = problemKey(problem)
        problemNoticeKey = key
        store.dismissProblem(problemSource, key)
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
        // Only what the page shows is saved (2026-11-15, FP6 P4 and P17): the hidden mode's settings
        // stay as stored, "Where" while the list shows, the list in APOD mode (the order already is).
        val setup = draft().let { if (it.allSources) it.copy(where = store.where) else it.copy(list = store.list) }
        val offered = sources
        // Only this page's own pick is renamed: another page's draft picture stays its own (B11).
        // And only while "Use my picture" is chosen: an abandoned pick doesn't replace the stored one.
        val pick = draftFallback && setup.videoDays == VideoDays.MINE
        // The list as the store derives it from "Where" (never saved): it follows the new "Where" (CR 3).
        val derivedList = !store.hasList && list == store.list
        val store = store
        val app = applicationContext
        val page = id
        runs.launch {
            // Size Live (L6): Android's picker when home isn't covered, then the wait for the engine,
            // before anything is saved: back from it without a pick, nothing is saved or set and the
            // draft stays as it was (user, 2026-11-15). Whatever fails on the way (L6 fix round 1):
            // Fill, and Set goes on to its save and update and resets `setting`.
            // "Lock screen" picked alone (2026-11-15, FP6 T22): nothing saved either, and the lock
            // screen put back by a run of the stored setup (`:core`'s lostLive replaces the engine there).
            // Home live and the columns equal (SPLIT): asked every time, "Not now" saves as without the picker.
            val opens = withContext(Dispatchers.IO) { runCatching { store.opensPicker(setup) }.getOrDefault(PickerTrip.NONE) }
            if (opens != PickerTrip.NONE) {
                val answer = try {
                    pickLive(store, page, setup, lockToo = opens == PickerTrip.LOCK_TOO)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("APODroid", "Set: the picker failed, Fill: $e", e)
                    Pick.GO
                }
                if (answer != Pick.GO) {
                    Log.i("APODroid", "Set: no pick, nothing saved")
                    if (answer == Pick.LOCK_ONLY) {
                        val repaired = withContext(Dispatchers.IO) { runCatching { store.update(force = false, page = true, show = offered, set = true) } }
                        repaired.exceptionOrNull()?.let { Log.w("APODroid", "Set: the lock screen's repair failed: $it", it) }
                        repaired.getOrNull()?.done?.let { Log.i("APODroid", "Set: the lock screen put back: $it") }
                        live[page]?.run { redrawAfterRun(failed = repaired.getOrNull()?.failure != null || repaired.isFailure) }
                        toast(app, "Live goes on the home screen. Setup not saved.")
                    }
                    setting = false
                    runAllNext()
                    return@launch
                }
            }
            // Not written (a full disk, B20): the sets still run from memory, the toast says so.
            val saving = withContext(Dispatchers.IO) { runCatching { store.saveSetup(setup, pick) } }
            val result = saving.mapCatching { saved ->
                withContext(Dispatchers.IO) {
                    Triple(store.update(force = false, page = true, show = offered, set = true), store.setsSinceSetup(), saved)
                }
            }
            if (store.daily) scheduleJob(app) else DailyJob.cancel(app)
            setting = false
            val updated = result.getOrNull()?.first
            // Also what a load or the job set of this setup before Set's own update got there (CR 5).
            val (wasSet, setFailed) = result.getOrNull()?.second ?: (false to true)
            val saved = result.getOrNull()?.third ?: true
            result.exceptionOrNull()?.let { Log.w("APODroid", "Set failed: $it", it) }
            updated?.done?.let { Log.i("APODroid", "Set: $it") }
            updated?.failure?.let { Log.w("APODroid", "Set: $it", it) }
            live[page]?.run {
                if (pick) draftFallback = store.draftFallbackFile.exists()
                if (derivedList && !store.hasList && list == setup.list) list = store.list
                // Saved: "Setup" closes on its summary. A failure, or a setup not written, leaves it open.
                if (updated != null && !setFailed && saved) setupOpen = false
                redrawAfterRun(failed = updated?.failure != null || result.isFailure)
            }
            toast(
                app,
                when {
                    !saved -> "Couldn't save the setup. Check the free space."
                    updated == null || setFailed -> "Couldn't set the wallpaper"
                    wasSet -> "Wallpaper set"
                    else -> "Setup saved. Nothing to set yet."
                },
            )
            runAllNext()
        }
    }

    /**
     * Set's trip to Android's live wallpaper picker (L6; live-0.7.0.md, Set with Live), before
     * [Store.saveSetup]. First the dialog ([PickerDialog], 2026-11-15): which answer to tap, by the
     * [draft]'s columns ("Home screen and lock screen" on 14+ when they're equal, else "Home
     * screen"); with [lockToo] (home live, SPLIT) it asks "Live on the lock screen too?", and
     * "Not now" is [Pick.GO] without the picker (the lock screen its own still Fill). On OK both
     * screens' wallpaper ids noted, the picker opened from the page that
     * started Set (none left in the foreground, or no picker on the phone: Fill), its return
     * awaited, then up to 3 s for the engine when an id changed ([Store.awaitLive]). A page's load
     * waits meanwhile, the dialog too ([picking]). [Pick.NONE] for a back-out: Cancel (Back, a tap
     * outside, the page closed for good; also in [lockToo]'s dialog, user, 2026-11-15), or no
     * wallpaper changed (a "Home screen" re-pick of a live home that keeps its id too: the dialog
     * asked for both screens then); [Pick.LOCK_ONLY] for "Lock screen" alone (14+: home's id the
     * same, the lock screen's changed, APODroid's engine there): Set then saves and sets nothing.
     * [Pick.GO]: home live, its picture goes to the engine (and the lock screen's still Fill when
     * it differs, Decision 10); no picker, or below 14 a home not live in time: Fill.
     */
    private suspend fun pickLive(store: Store, page: String, draft: Setup, lockToo: Boolean): Pick {
        picking = true
        try {
            val ask = PickerAsk(page, both = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && equalColumns(draft.columns), lockToo = lockToo)
            pickerAsk = ask
            val answer = try {
                ask.answer.await()
            } finally {
                pickerAsk = null
            }
            if (answer == PickerAnswer.NOT_NOW) {
                Log.i("APODroid", "Set: Live on the lock screen not now")
                return Pick.GO
            }
            if (answer != PickerAnswer.OK) {
                Log.i("APODroid", "Set: the picker's dialog cancelled")
                return Pick.NONE
            }
            val ids = withContext(Dispatchers.IO) { store.wallpaperIds() }
            // Only after OK: a page's onResume while the dialog shows would complete it early.
            val back = CompletableDeferred<Unit>()
            pickerBack = back
            // Only from a page in the foreground: a start from the background is blocked without a word.
            val opened = live[page]?.takeIf { it.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }?.openLivePicker() == true
            Log.i("APODroid", if (opened) "Set: Android's live wallpaper picker" else "Set: no picker opened, Fill")
            if (!opened) return Pick.GO
            back.await()
            val lockOnly = withContext(Dispatchers.IO) {
                val now = store.wallpaperIds()
                now[0] == ids[0] && now[1] != ids[1] && store.lockEngine()
            }
            if (lockOnly) {
                Log.i("APODroid", "Live: back from the picker, the lock screen alone")
                return Pick.LOCK_ONLY
            }
            val changed = withContext(Dispatchers.IO) { store.awaitLive(ids) }
            return if (changed) Pick.GO else Pick.NONE
        } finally {
            pickerBack = null
            picking = false
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

    /** "Keep running" (O11): at once, the service started or stopped ([KeepRunning.ensure]). */
    private fun switchKeepRunning(on: Boolean) {
        keepRunning = on
        store.keepRunning = on
        KeepRunning.ensure(this)
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
        val store = store
        val app = applicationContext
        val page = id
        // In [runs], so a rotation or Back during the download keeps its end: the toast and ★
        // (FP6 tests, 2026-11-15, S4a: the page's own scope was cancelled with it).
        runs.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.save(post) } }
            saving = false
            live[page]?.saving = false
            result.onFailure { Log.w("APODroid", "Save failed: $it", it) }
            if (result.getOrNull() == Saved.BUSY) {
                toast(app, "Still saving")
                return@launch
            }
            if (result.getOrNull() == Saved.STALE) {
                toast(app, "Not saved: a new picture came in")
                live[page]?.load()
                return@launch
            }
            // Each page's own, which may not be the saved one's any more.
            live.values.toList().forEach { it.readSaved() }
            toast(
                app,
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

    /**
     * Crop mode closes; the frame's place and the "Whole picture" and "Sideways" toggles are
     * [post]'s part in the draft. An untouched "Sideways" keeps the part's previous value (null,
     * Auto's turn, stays null) unless the frame was dragged (0.9.0: the drag notes its orientation,
     * `:core`'s [notedSideways]), and an undragged frame keeps its position (SA3b: null, the stored
     * Size's frame, stays null).
     */
    private fun keepPart(post: Post) {
        val previous = parts.forPost(post)
        val sideways = notedSideways(cropSidewaysTouched, cropMoved, cropSideways, previous?.sideways)
        val position = notedPosition(cropMoved, cropPosition, previous?.position)
        parts = parts + (post.source to Part(post.date, post.imageUrl, position, cropWhole, sideways))
        cropping = false
    }

    /**
     * Where [post]'s frame stands with [part] and [sideways] (SA3b, `:core`'s [framePosition], as
     * [Store.set]): the part's dragged position, else with the **stored** Size Auto its subject
     * for that orientation ([Store.subject], [Store.autoFallback]), else the middle. Stored, not
     * the draft's: ✓ sets at once by the stored setup.
     */
    private fun frameOf(post: Post, part: Part?, sideways: Boolean): Float {
        val subject = store.subject(post)
        return framePosition(stored.size, part?.position, if (sideways) subject?.sideways else subject?.position, store.autoFallback)
    }

    /**
     * Whether [post]'s picture goes on sideways with a part noting [sideways] (0.9.0, SH2a, `:core`'s
     * [sidewaysOf], as [Store.turnsSideways]): that value (⛶'s pill), else Auto's turn while the
     * **stored** Size is Auto, an APOD landscape picture by its [uprightSizes] whose text has no up
     * ([hasUp]), else upright. Stored, not the draft's: ✓ sets at once by the stored setup (as ⛶
     * with Live, T04).
     */
    private fun turnsSideways(post: Post, sideways: Boolean?): Boolean =
        sidewaysOf(sideways, stored.size, post.source, { uprightSizes[post.source]?.let { it.width to it.height } }) {
            hasUp(post.title, post.alt, post.explanation)
        }

    /**
     * ✓ (U0, 2026-11-02: users expected ✓ to set it): [keepPart], then, when the part differs from
     * the stored one or its last set failed, [Store.savePart] saves it at once (each page's draft
     * takes it at its next [readStatus]) and [Store.setPart] sets the screens showing that post
     * again (a post on no screen: only saved). "Wallpaper set" or "Couldn't set the wallpaper"; no
     * toast when nothing was set. In [runs], as Set's, so a rotation keeps its end. ⛶ is disabled
     * while a Set or the daily switch's run goes: Set's draft, taken before, would write the old
     * part back.
     */
    private fun setPart(post: Post) {
        keepPart(post)
        val part = parts.getValue(post.source)
        val saved = stored.parts.forPost(post)
        val turned = turnsSideways(post, part.sideways)
        val savedTurned = turnsSideways(post, saved?.sideways)
        // SA3b: the frames compared where they stand (an undragged one by Size and the subject).
        if (frameOf(post, part, turned) == frameOf(post, saved, savedTurned) && part.whole == (saved?.whole == true) &&
            turned == savedTurned && part != failedPart
        ) {
            // The same picture on the screen, but a touched pill or a dragged frame notes its value
            // (SW3, SA3b: a drag ending where Auto had it): saved, no set.
            if (part.sideways != saved?.sideways || part.position != saved?.position) {
                store.savePart(post.source, part)
                readStatus()
            }
            return
        }
        store.savePart(post.source, part)
        readStatus()
        val store = store
        val app = applicationContext
        val page = id
        runs.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.setPart(post) } }
            // A failed set: the same part again sets it once more.
            failedPart = if (result.isFailure) part else null
            result.exceptionOrNull()?.let { Log.w("APODroid", "Part: set failed: $it", it) }
            result.getOrNull()?.takeIf { it.isNotEmpty() }?.let { Log.i("APODroid", "Part: ${post.source} ${post.date} set on ${it.joinToString("+")}") }
            live[page]?.run {
                readStatus()
                if (isStale()) show()
            }
            when {
                result.isFailure -> toast(app, "Couldn't set the wallpaper")
                result.getOrNull().orEmpty().isNotEmpty() -> toast(app, "Wallpaper set")
            }
        }
    }

    /**
     * ⛶: crop mode opens on [post]'s draft part, whole picture and sideways: not whole unless ✓
     * noted it for this post; sideways as it goes on ([turnsSideways], SW3); the frame where it
     * goes on ([frameOf], SA3b: the dragged part, else Auto's subject, else the middle).
     */
    private fun startCrop(post: Post) {
        val part = parts.forPost(post)
        cropWhole = part?.whole == true
        cropSideways = turnsSideways(post, part?.sideways)
        cropSidewaysTouched = false
        cropPosition = frameOf(post, part, cropSideways)
        cropMoved = false
        cropping = true
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

    private fun toast(text: String) = toast(this, text)

    /**
     * One column; from 600 dp wide (tablets, landscape) two panes that scroll on their own: the
     * picture side, which pull to refresh covers, and the settings.
     */
    @Composable
    private fun Page() {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            // UI1: while Setup is open, Set as wallpaper stands in a bar pinned at the bottom of the
            // screen (one column) or of the settings pane, over the scrolling content, which ends
            // with a space of the bar's height so its last row scrolls clear.
            var barHeight by remember { mutableIntStateOf(0) }
            val barSpace = with(LocalDensity.current) { barHeight.toDp() }
            if (maxWidth < 600.dp) {
                Box(Modifier.fillMaxSize()) {
                    PullToRefreshBox(isRefreshing = refreshing, onRefresh = ::pullToRefresh) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            PictureSide()
                            HorizontalDivider()
                            Settings()
                            if (setupOpen) Spacer(Modifier.height(barSpace))
                        }
                    }
                    if (setupOpen) SetBar(Modifier.align(Alignment.BottomCenter).onSizeChanged { barHeight = it.height })
                }
            } else {
                Row {
                    PullToRefreshBox(isRefreshing = refreshing, onRefresh = ::pullToRefresh, modifier = Modifier.weight(0.55f)) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            PictureSide()
                        }
                    }
                    VerticalDivider()
                    Box(Modifier.weight(0.45f).fillMaxHeight()) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Settings()
                            if (setupOpen) Spacer(Modifier.height(barSpace))
                        }
                        if (setupOpen) SetBar(Modifier.align(Alignment.BottomCenter).onSizeChanged { barHeight = it.height })
                    }
                }
            }
        }
        if (weekdayDialog) WeekdayDialog()
        pickerAsk?.takeIf { it.page == id }?.let { PickerDialog(it) }
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
                sources.getOrNull(page)?.let { if (it != shown) showSource(it) }
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
                            if (choice != shown) showSource(choice)
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
        // Back from the background (O12): the picture's place until it's decoded again.
        val stopped = stoppedSizes[source]
        if (post != null && picture == null && !post.isVideo && stopped != null) {
            Box(Modifier.fillMaxWidth().aspectRatio(stopped.width.toFloat() / stopped.height))
            return
        }
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
        // ⛶ with Live (L7): the frame where the clock has it, no drag; a minute tick moves it.
        val live = crop && livesOnHome(source)
        if (live) {
            LaunchedEffect(Unit) {
                while (true) {
                    liveNow = Instant.now()
                    delay(60_000 - liveNow.toEpochMilli() % 60_000)
                }
            }
        }
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = remember(picture) { PicturePainter(picture) },
                contentDescription = post.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(picture.width.toFloat() / picture.height)
                    // With "Whole picture" on there's no frame: the page shows it whole.
                    .then(
                        when {
                            crop && cropWhole -> Modifier
                            live -> Modifier.shadeFrame(picture, cropSideways) { livePreviewPosition() }
                            crop -> Modifier.cropFrame(picture, post)
                            else -> Modifier.clickable { openPage(post.pageUrl) }
                        },
                    ),
                contentScale = ContentScale.FillWidth,
            )
            // "Whole picture" on (L7): the screen-shaped preview in the picture's place, as the wallpaper draws it.
            if (crop && cropWhole) WholePreview(source, picture, cropSideways, Modifier.matchParentSize())
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
                val wholeChanged = cropWhole != (stored.parts.forPost(post)?.whole == true)
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
            // Crop mode's "Sideways" (SW2), at the top right: only a landscape picture gains by the turn
            // (by its file's upright size, SW3, the page's decoded picture as a fallback).
            val upright = uprightSizes[source] ?: IntSize(picture.width, picture.height)
            if (crop && sidewaysHelps(upright.width, upright.height)) {
                val sidewaysChanged = cropSideways != turnsSideways(post, stored.parts.forPost(post)?.sideways)
                Text(
                    (if (cropSideways) "✓ " else "") + "Sideways",
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .toggleable(cropSideways, role = Role.Switch) {
                            cropSidewaysTouched = true
                            // FP1: a drag belongs to the orientation it was made in, so a turn
                            // drops this crop mode's drag; the frame goes where ✓ would then put
                            // it, the part's previous position (null: Size and the subject, SA3b;
                            // a stored number keeps it, K26), until it's dragged again.
                            if (it != cropSideways) {
                                cropMoved = false
                                cropPosition = frameOf(post, parts.forPost(post), it)
                            }
                            cropSideways = it
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clearAndSetSemantics {
                            contentDescription = "Sideways"
                            stateDescription = (if (cropSideways) "On" else "Off") + if (sidewaysChanged) ", not set yet" else ""
                        },
                    color = if (sidewaysChanged) MaterialTheme.colorScheme.tertiary else Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            // ✓ saves the chosen part and the whole picture and sets the screens showing this post (U0).
            // Not while a Set or the daily switch's run goes (their draft or setup was read before).
            val partEnabled = !setting && !switching
            IconButton(
                onClick = { if (!crop) startCrop(post) else setPart(post) },
                enabled = partEnabled,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
            ) {
                // No highlight: ✓ saves the part at once, so the draft never holds one not set.
                Text(
                    if (crop) "✓" else "⛶",
                    Modifier.clearAndSetSemantics { contentDescription = if (crop) "Done" else "Choose the wallpaper's part" },
                    color = Color.White.copy(alpha = if (partEnabled) 1f else 0.4f),
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
        if (post == null || (post.source !in pictures && post.source !in stoppedSizes && !post.isVideo)) {
            MissNotice()
            BatteryNotice()
            return
        }
        val postKey = "${post.source}:${post.date}"
        val expanded = expandedPost == postKey
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
            MissNotice()
            BatteryNotice()
            // With two or more sources a source line names it; with one the caption stays as 0.4.0's.
            val dateLine = post.date.format(dateFormat) + if (post.isVideo) " · Video" else ""
            Column(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { if (expandable) expandedPost = if (expanded) null else postKey },
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
                            onClick = { if (expandable) expandedPost = if (expanded) null else postKey },
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
     * ⛶ or ★. Tapping plays the video on the APOD page. What the day does to the wallpaper:
     * [VideoNotice].
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
            // VS1: a still from the video, cropped to the tile, dimmed so ▶ stays the thing to tap.
            videoStill?.takeIf { it.isFor(apod) }?.let { still ->
                Image(
                    painter = remember(still.picture) { PicturePainter(still.picture) },
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.4f)))
            }
            Text(
                "▶",
                Modifier.clearAndSetSemantics { contentDescription = "Play video on the web" },
                color = Color.White,
                fontSize = 48.sp,
            )
        }
    }

    /**
     * Says what a video day does to the wallpaper, short (U0, 2026-11-02), by the stored settings:
     * what takes APOD's place ("…: NASA's Image of the Day instead." once its picture is stored,
     * B17), with the switch on also with turns; with APOD ticked nowhere only that it's a video.
     * The stand-in says "instead" also when the screen has shown it before (RO2: it may repeat).
     * Where to change it ("Change: Setup › If APOD is a video") only when nothing takes APOD's place.
     */
    @Composable
    private fun VideoNotice() {
        val video = "This APOD is a video"
        val change = " Change: Setup › If APOD is a video"
        // APOD mode: the screen keeps its picture; with the switch on a screen with turns passes the turn.
        val skipped = if (stored.allSources) "$video: skipped" else "$video: wallpaper unchanged"
        // What is set, not the draft.
        val other = stored.videoDays.source
        val apodTicked = stored.columns.values.any { Source.APOD in it }
        val mine = stored.videoDays == VideoDays.MINE
        val text = when {
            !daily || (stored.allSources && !apodTicked) -> "$video."
            // B15: "instead", not "is the wallpaper": the wallpaper changes at the next check or change time.
            // B17: by the stand-in's stored picture, not by what the screen shows yet.
            other != null && standInReady -> "$video: ${other.possessive} instead."
            other != null -> "$video: ${other.possessive} instead, once loaded."
            mine && hasFallback -> "$video: your picture instead."
            mine -> "$skipped (no picture chosen).$change"
            else -> "$skipped.$change"
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
            FlowRow(Modifier.offset(x = (-12).dp), content = buttons)
        }
    }

    /**
     * Whether ⛶ shows [source]'s frame as Live moves it (L7): the stored Size Live, Live on home
     * and [source] in the stored home column, since ✓ acts at once on the stored setup (FP6,
     * 2026-11-15, T04: with the draft's it dragged while ✓ handed over to the engine). A source
     * only on the lock screen, or Live not on, crops as Fill (✓ then sets Fill at the part).
     */
    private fun livesOnHome(source: Source) =
        stored.size == Sizing.LIVE && !liveMissing && source in stored.columns[Screen.HOME].orEmpty()

    /** Where the engine's frame is at [liveNow] (`:core`'s [livePosition]) by the stored home column and change times. */
    private fun livePreviewPosition(): Float =
        livePosition(liveNow, ZoneId.systemDefault(), stored.pictureTime, turns = stored.columns[Screen.HOME].orEmpty().size >= 2)

    /**
     * "Whole picture"'s preview (L7, Decision 7): [source]'s stored picture drawn whole on a
     * bitmap of the screen's portrait size by the wallpaper's own code ([wholePicture]: on black
     * or blurred), shown in the screen's shape, centred on black in the picture's place (so the
     * page doesn't move). Drawn off the main thread, again when the page's [picture] changes;
     * a spinner meanwhile, the picture alone when it fails. Darkened as the wallpaper will be
     * (BR3) while the draft's "Darken bright pictures" is on: its top third measured as drawn,
     * `:core`'s [darkening] (the page's picture itself stays as it is). [sideways] (SW2): drawn
     * turned 90° clockwise, as the set will.
     */
    @Composable
    private fun WholePreview(source: Source, picture: ImageBitmap, sideways: Boolean, modifier: Modifier) {
        val file = store.imageFile(source)
        val screen = screen
        val darken = darken
        val preview by produceState<ImageBitmap?>(null, picture, darken, sideways) {
            val drawn = withContext(Dispatchers.IO) {
                runCatching {
                    val whole = wholePicture(file, screen, sideways)
                    val factor = if (darken) darkening(topThirdGrey(whole)) else 1f
                    if (factor < 1f) darkened(whole, factor) else whole
                }
                    .onFailure { Log.w("APODroid", "Part: couldn't draw the whole picture's preview: $it", it) }
                    .getOrNull()
            }
            value = drawn?.asImageBitmap()
            awaitDispose {
                value = null
                drawn?.recycle()
            }
        }
        Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
            preview?.let {
                Image(
                    painter = remember(it) { PicturePainter(it) },
                    contentDescription = null,
                    modifier = Modifier.fillMaxHeight().aspectRatio(screen.width.toFloat() / screen.height),
                )
            } ?: CircularProgressIndicator()
        }
    }

    /**
     * Dims what the wallpaper leaves out of [picture] at the frame's [position] (0–1), framed in
     * white; [sideways] (SW2): the band the turned picture shows ([pictureFrame]), on the page's
     * upright picture.
     */
    private fun Modifier.shadeFrame(picture: ImageBitmap, sideways: Boolean, position: () -> Float): Modifier = drawWithContent {
        drawContent()
        val scale = size.width / picture.width
        val frame = pictureFrame(picture.width, picture.height, screen, position(), sideways)
        val topLeft = Offset(frame.left * scale, frame.top * scale)
        val frameSize = Size(frame.width() * scale, frame.height() * scale)
        clipRect(topLeft.x, topLeft.y, topLeft.x + frameSize.width, topLeft.y + frameSize.height, ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.6f))
        }
        drawRect(Color.White, topLeft, frameSize, style = Stroke(1.dp.toPx()))
    }

    /**
     * Dims what the wallpaper leaves out; dragging moves the frame along the picture's longer
     * side (relative to the screen's shape), the other drags still scroll the page. Sideways
     * (SW2): the band across the picture, dragged up and down; position 0 is the bottom band, so
     * a drag down lowers it.
     */
    private fun Modifier.cropFrame(picture: ImageBitmap, post: Post): Modifier {
        val sideways = cropSideways
        val full = pictureFrame(picture.width, picture.height, screen, 0f, sideways)
        val end = pictureFrame(picture.width, picture.height, screen, 1f, sideways)
        val horizontal = full.width() < picture.width
        // +1 when the frame moves right or down as the position grows, -1 the other way.
        val sign = if ((if (horizontal) end.left - full.left else end.top - full.top) < 0) -1f else 1f
        return shadeFrame(picture, sideways) { cropPosition }.pointerInput(picture, post, sideways) {
            // The distance the frame can travel, in pixels on the page.
            val travel = if (horizontal) {
                size.width * (1 - full.width().toFloat() / picture.width)
            } else {
                size.height * (1 - full.height().toFloat() / picture.height)
            }
            if (travel <= 0f) return@pointerInput
            val move = { delta: Float ->
                cropPosition = (cropPosition + sign * delta / travel).coerceIn(0f, 1f)
                cropMoved = true
            }
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
     * "Where", with the switch on the list ("Pictures from"), then Size [Centre | Auto | Live] (L6, SA3a); while the daily switch is on "New
     * picture at", then, while a column takes turns, "Change every", and "If APOD is a video";
     * "Darken bright pictures", "Wide pictures sideways" (not with Live) and "Centre when unsure"
     * (Auto only; UI1). The set button, always: here while Setup is closed, else pinned at the
     * bottom ([SetBar], UI1). Then, while the daily switch is on, "Advanced settings", closed by
     * default: "Save every picture" and "Keep running" (they act at once). A setting the draft
     * changed and Set hasn't saved shows in `tertiary` ([highlight]).
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
        LaunchedEffect(daily, soon, stored.soon) { if (!daily) soon = stored.soon }
        // UI1: "Centre when unsure" shown only with Auto.
        LaunchedEffect(size, autoFallback, stored.autoFallback) { if (size != Sizing.AUTO) autoFallback = stored.autoFallback }
        val setupChanged = setupChanged()
        // Opens by itself while the draft differs (a rotation, the system killing the app).
        LaunchedEffect(setupChanged) { if (setupChanged) setupOpen = true }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading("Wallpaper")
            Row(
                Modifier.fillMaxWidth().toggleable(daily, role = Role.Switch, onValueChange = ::switchDaily),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Change automatically", Modifier.weight(1f))
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
                // Always shown (L6), as "Where": never put back.
                Heading("Size", size != stored.size)
                Segments(Sizing.entries.map { it to it.label }, size, size != stored.size) { size = it }
                if (daily) {
                    PictureAtChoice()
                    SoonSwitch()
                }
                // Hiding it moves the page only after a tick.
                if (allSources && showsInterval) {
                    val changed = interval != stored.interval
                    ChangeEveryHeading(changed)
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
                // Shown with the daily switch off too: a one-off Set darkens as well.
                DraftSwitch(
                    "Darken bright pictures", darken, stored.darken, about = "About Darken bright pictures",
                    info = "Darkens a bright picture a little, so the clock and widgets stay readable.",
                ) { darken = it }
                // UI1 (was SA3b's Advanced switch): Auto's, part of the draft, shown only with Size Auto.
                if (size == Sizing.AUTO) {
                    DraftSwitch(
                        "Centre when unsure", autoFallback, stored.autoFallback, about = "About Centre when unsure",
                        info = "With Size Auto, a picture whose subject is only slightly off the middle stays centred. " +
                            "Off: Auto places every frame.",
                    ) { autoFallback = it }
                }
            }
            // One button, both modes, the daily switch on or off: it applies the whole draft. While Setup is
            // open it stands in the bar pinned at the bottom instead (UI1, SetBar): one button, never two.
            if (!setupOpen) SetButton(Modifier.fillMaxWidth().padding(top = 8.dp))

            // Empty while the daily switch is off: hidden then.
            if (daily) {
                Header("Advanced settings", advancedOpen, changed = false) { advancedOpen = !advancedOpen }
                if (advancedOpen) {
                    SaveEverySwitch()
                    val keepRow = remember { BringIntoViewRequester() }
                    // At once, not the draft: the stored state is the shown one, so never highlighted.
                    Box(Modifier.bringIntoViewRequester(keepRow)) {
                        DraftSwitch(
                            "Keep running", keepRunning, keepRunning, about = "About keeping running",
                            info = "Keeps APODroid running, so the phone can't stop it between changes. It shows as an active app.",
                            onChange = ::switchKeepRunning,
                        )
                    }
                    // The detector's "Keep running" (O14): once laid out, the switch scrolled into view.
                    if (showKeepRunning) LaunchedEffect(Unit) {
                        withFrameNanos { }
                        keepRow.bringIntoView()
                        showKeepRunning = false
                    }
                }
            }
        }
    }

    /** "Set as wallpaper": enabled unless Set or the daily switch's run goes; it applies the whole draft ([setAsWallpaper]). */
    @Composable
    private fun SetButton(modifier: Modifier) {
        Button(onClick = ::setAsWallpaper, enabled = !setting && !switching, modifier = modifier) { Text("Set as wallpaper") }
    }

    /**
     * The bar pinned at the bottom while Setup is open (UI1): the theme's surface behind
     * [SetButton], a line above it. Inside the page's safe-drawing padding, so above the
     * navigation bar. The only Set button then ([Settings] leaves its own out).
     */
    @Composable
    private fun SetBar(modifier: Modifier) {
        Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
            Column {
                HorizontalDivider()
                SetButton(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
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
        Dropdown("New picture at", pictureHours, pictureAt, ::hourLabel, changed = changed) { pictureAt = it }
    }

    /** "As soon as it's out" (SO2), under "New picture at": part of the draft ([DraftSwitch]). */
    @Composable
    private fun SoonSwitch() = DraftSwitch(
        "As soon as it's out", soon, stored.soon, about = "About As soon as it's out",
        info = "A new picture goes on within the hour after it's out, not only at the change time.",
    ) { soon = it }

    /**
     * A switch of the draft ([label], [checked]), with a small ⓘ beside the label (as "Save every
     * picture"'s; [about] for screen readers) whose dialog says [info], and the highlight while it
     * differs from the [stored] one: "As soon as it's out" (SO2), "Darken bright pictures" (BR2).
     * Also "Keep running" (O11), which acts at once: [stored] is [checked], so no highlight.
     */
    @Composable
    private fun DraftSwitch(
        label: String, checked: Boolean, stored: Boolean, about: String, info: String,
        onChange: (Boolean) -> Unit,
    ) {
        val changed = checked != stored
        val labelColor = highlight(changed)
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(checked, role = Role.Switch, onValueChange = onChange)
                .semantics { if (changed) stateDescription = "${if (checked) "On" else "Off"}, not set yet" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f, fill = false), color = labelColor)
                Text(
                    "ⓘ",
                    Modifier
                        .clickable(role = Role.Button, onClick = { openInfo = about })
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clearAndSetSemantics { contentDescription = about },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val colors = MaterialTheme.colorScheme
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = if (!changed) SwitchDefaults.colors() else SwitchDefaults.colors(
                    checkedTrackColor = colors.tertiary,
                    checkedThumbColor = colors.onTertiary,
                    uncheckedBorderColor = colors.tertiary,
                    uncheckedThumbColor = colors.tertiary,
                ),
            )
        }
        if (openInfo == about) {
            AlertDialog(
                onDismissRequest = { openInfo = null },
                confirmButton = { TextButton(onClick = { openInfo = null }) { Text("OK") } },
                text = { Text(info) },
            )
        }
    }

    /**
     * "Change every" with a small ⓘ beside it (RO2, the user's Open 3, 2026-11-14: the rotation's
     * effect is what confused), whose dialog says it in one sentence with the draft's values.
     */
    @Composable
    private fun ChangeEveryHeading(changed: Boolean) {
        Row(verticalAlignment = Alignment.Bottom) {
            Heading("Change every", changed)
            Text(
                "ⓘ",
                Modifier
                    .clickable(role = Role.Button, onClick = { openInfo = "About Change every" })
                    .padding(horizontal = 8.dp)
                    .clearAndSetSemantics { contentDescription = "About Change every" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (openInfo == "About Change every") {
            AlertDialog(
                onDismissRequest = { openInfo = null },
                confirmButton = { TextButton(onClick = { openInfo = null }) { Text("OK") } },
                text = {
                    Text(
                        "Every ${interval.label} from ${hourLabel(pictureAt)}, each screen switches to its next source in the list; " +
                            "a new order starts with the top one at ${hourLabel(pictureAt)}.",
                    )
                },
            )
        }
    }

    /** An hour as the system's clock shows it: "06:00" or "6:00 AM". */
    private fun hourLabel(hour: Int): String =
        LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern(if (hours24) "HH:mm" else "h:mm\u00A0a", Locale.ENGLISH))

    /** Anything in "Setup" the draft changed: what the shown mode's settings differ in from the stored setup. */
    private fun setupChanged(): Boolean =
        allSources != stored.allSources ||
            (if (allSources) Source.entries.any(::rowChanged) || order != stored.order else where != stored.where) ||
            interval != stored.interval || pictureAt != stored.pictureAt || soon != stored.soon ||
            darken != stored.darken || size != stored.size ||
            autoFallback != stored.autoFallback || videoDaysChanged()

    /** The list's row for [source] puts it on other screens than the stored list. */
    private fun rowChanged(source: Source) = list.screensOf(source) != stored.list.screensOf(source)

    /** The list's row for [source] has another place than in the stored order (OR2): its "≡" in `tertiary`. */
    private fun rowMoved(source: Source) = order.indexOf(source) != stored.order.indexOf(source)

    /** The draft's order with [source] moved [by] places (−1 up, 1 down): the drag and the actions "Move up" / "Move down". */
    private fun move(source: Source, by: Int) {
        val from = order.indexOf(source)
        val to = from + by
        if (from < 0 || to !in order.indices) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Another video-day choice, or a newly picked "my picture" while it's chosen. */
    private fun videoDaysChanged() = videoDays != stored.videoDays || (videoDays == VideoDays.MINE && draftFallback)

    /**
     * "Setup"'s summary while it's closed, of the stored setup, grouped by screen (U0,
     * 2026-11-02): "APOD, NASA · both screens" while both screens have the same sources, "APOD ·
     * home screen" while only one has any, else "Home: APOD, Earth · Lock: NASA, Earth"; while
     * the daily switch is on " · every 6 h from 06:00" when a screen takes turns, else " · 06:00".
     * The sources in the list's order (OR2): "NASA, APOD · both screens". With Size Live and a home column " · Live"
     * at the end (L6).
     */
    private fun setupSummary(): String {
        val home = stored.columns[Screen.HOME].orEmpty()
        val lock = stored.columns[Screen.LOCK].orEmpty()
        fun names(sources: List<Source>) = stored.order.filter { it in sources }.joinToString(", ") { it.shortLabel }
        val sources = when {
            lock.isEmpty() -> "${names(home)} · home screen"
            home.isEmpty() -> "${names(lock)} · lock screen"
            home.toSet() == lock.toSet() -> "${names(home)} · both screens"
            else -> "Home: ${names(home)} · Lock: ${names(lock)}"
        }
        val time = hourLabel(stored.pictureAt)
        val turns = when {
            !daily -> ""
            stored.columns.values.any { it.size >= 2 } -> " · every ${stored.interval.label} from $time"
            else -> " · $time"
        }
        // Size Live (L6): " · Live" at the end.
        // Only with a home column: Live is home's (FP6, 2026-11-15, T31).
        val moving = if (stored.size == Sizing.LIVE && home.isNotEmpty()) " · Live" else ""
        return "$sources$turns$moving"
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
     * A row that opens and closes its section, as a row in Android's Settings (U0, 2026-11-02:
     * the heading-like "Setup ▸" didn't look tappable): the title, [summary] under it while it's
     * closed, "▾" (closed) or "▴" (open) at the right; the whole row tappable with its ripple, the
     * pane's full width, at least 56 dp. A button for screen readers ("expanded", "collapsed").
     * [changed]: the title in `tertiary`, ", not set yet".
     */
    @Composable
    private fun Header(text: String, open: Boolean, changed: Boolean, summary: String? = null, toggle: () -> Unit) {
        Row(
            Modifier
                .bleed(16.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = toggle)
                .semantics { stateDescription = (if (open) "expanded" else "collapsed") + if (changed) ", not set yet" else "" }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.bodyLarge, color = highlight(changed))
                if (!open && summary != null) {
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Roboto draws "▾" small: larger than the text, so it reads as the row's control.
            Text(
                if (open) "▴" else "▾",
                Modifier.padding(start = 16.dp).clearAndSetSemantics { },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 30.sp,
                lineHeight = 30.sp,
            )
        }
    }

    /** Wider by [by] on each side, into the parent's padding: a row whose ripple spans the pane. */
    private fun Modifier.bleed(by: Dp) = layout { measurable, constraints ->
        val extra = if (constraints.hasBoundedWidth) by.roundToPx() * 2 else 0
        val placeable = measurable.measure(constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra))
        layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
    }

    /** A highlighted control's text: `tertiary` while the draft differs from the stored setup, else the default. */
    @Composable
    private fun highlight(changed: Boolean) = if (changed) MaterialTheme.colorScheme.tertiary else Color.Unspecified

    /**
     * A dropdown (C5): an outlined button "06:00 ▾" (a no-break space keeps "▾" with the last word)
     * and a menu of [choices], ✓ before [chosen]; the border and text in `tertiary` while [changed].
     * Screen readers "[label]: 06:00".
     */
    @Composable
    private fun <T> Dropdown(
        label: String,
        choices: Iterable<T>,
        chosen: T,
        name: (T) -> String,
        enabled: Boolean = true,
        changed: Boolean = false,
        choose: (T) -> Unit,
    ) {
        var open by remember { mutableStateOf(false) }
        val tertiary = MaterialTheme.colorScheme.tertiary
        Box {
            OutlinedButton(
                onClick = { open = true },
                enabled = enabled,
                border = if (changed && enabled) BorderStroke(1.dp, tertiary) else ButtonDefaults.outlinedButtonBorder(enabled),
                colors = if (changed) ButtonDefaults.outlinedButtonColors(contentColor = tertiary) else ButtonDefaults.outlinedButtonColors(),
            ) {
                Text(
                    "${name(chosen)}\u00A0▾",
                    Modifier.clearAndSetSemantics {
                        contentDescription = "$label: ${name(chosen)}"
                        if (changed) stateDescription = "not set yet"
                    },
                )
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(name(choice)) },
                        onClick = { open = false; choose(choice) },
                        // A text glyph, no icon library.
                        leadingIcon = { Text(if (choice == chosen) "✓" else "") },
                    )
                }
            }
        }
    }

    /**
     * "Pictures from": a row per source in the list's order ([order], OR2), a drag handle "≡", a
     * tick box with its short name and a dropdown Home screen / Lock screen / Both ([place]). An
     * unticked row's dropdown is greyed out and shows the screens the source had last
     * ([lastScreens]); a tick puts it there. The last ticked row can't be unticked.
     *
     * The drag (OR2): the handle takes a vertical drag (no long press) and consumes it, so the
     * pane doesn't scroll while a row moves (it still scrolls from anywhere else); the row is
     * drawn lifted under the finger and swaps with its neighbour once it has passed half that
     * row's height. Screen readers get "Move up" and "Move down" on the tick box instead; the
     * handle is hidden from them. A moved row's "≡" shows in `tertiary`.
     */
    @Composable
    private fun PictureList() {
        val tickedCount = Source.entries.count { s -> columns.values.any { s in it } }
        Heading("Pictures from")
        // The dragged row, how far it is from its place in the layout, each row's height (onSizeChanged).
        var dragged by remember { mutableStateOf<Source?>(null) }
        var dragOffset by remember { mutableFloatStateOf(0f) }
        val heights = remember { mutableStateMapOf<Source, Int>() }
        val gap = with(LocalDensity.current) { 8.dp.toPx() }
        val colors = MaterialTheme.colorScheme
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            order.forEachIndexed { index, source ->
                // Keyed by the source, so a swap moves the row (and its running drag) instead of restarting it.
                key(source) {
                    val on = columns.screensOf(source)
                    val shown = on.ifEmpty { lastScreens[source] ?: Screen.entries.toSet() }
                    val checked = on.isNotEmpty()
                    val at = Where.of(shown)!!
                    // The row differs from the stored list: its tick box, name and dropdown in tertiary.
                    val changed = rowChanged(source)
                    val moved = rowMoved(source)
                    val lifted = dragged == source
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { heights[source] = it.height }
                            .then(
                                if (lifted) {
                                    Modifier
                                        .zIndex(1f)
                                        .graphicsLayer {
                                            translationY = dragOffset
                                            shadowElevation = 8.dp.toPx()
                                            shape = RoundedCornerShape(8.dp)
                                            clip = true
                                        }
                                        .background(colors.surfaceContainerHigh)
                                } else {
                                    Modifier
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .pointerInput(source, gap) {
                                    detectVerticalDragGestures(
                                        onDragStart = { dragged = source; dragOffset = 0f },
                                        onDragEnd = { dragged = null; dragOffset = 0f },
                                        onDragCancel = { dragged = null; dragOffset = 0f },
                                    ) { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                        val place = order.indexOf(source)
                                        val below = order.getOrNull(place + 1)?.let { heights[it] }
                                        val above = order.getOrNull(place - 1)?.let { heights[it] }
                                        if (below != null && dragOffset > below / 2f) {
                                            dragOffset -= below + gap
                                            move(source, 1)
                                        } else if (above != null && dragOffset < -above / 2f) {
                                            dragOffset += above + gap
                                            move(source, -1)
                                        }
                                    }
                                }
                                .clearAndSetSemantics { },
                            contentAlignment = Alignment.Center,
                        ) {
                            // A text glyph, no icon library.
                            Text("≡", style = MaterialTheme.typography.titleLarge, color = if (moved) colors.tertiary else colors.onSurfaceVariant)
                        }
                        // Wraps the dropdown under the name when both don't fit on one line (a large font, a narrow pane).
                        FlowRow(
                            Modifier.weight(1f),
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
                                ).semantics {
                                    if (changed || moved) stateDescription = "${if (checked) "Checked" else "Not checked"}, not set yet"
                                    customActions = listOfNotNull(
                                        CustomAccessibilityAction("Move up") { move(source, -1); true }.takeIf { index > 0 },
                                        CustomAccessibilityAction("Move down") { move(source, 1); true }.takeIf { index < order.lastIndex },
                                    )
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    enabled = !(checked && tickedCount == 1),
                                    colors = if (changed) CheckboxDefaults.colors(checkedColor = colors.tertiary, uncheckedColor = colors.tertiary) else CheckboxDefaults.colors(),
                                )
                                Text(source.shortLabel, Modifier.padding(start = 8.dp, end = 16.dp), color = highlight(changed), softWrap = false)
                            }
                            Dropdown("${source.shortLabel} on", Where.entries, at, Where::label, enabled = checked, changed = changed) {
                                place(source, it.screens)
                            }
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Dropdown("If APOD is a video", VideoDays.entries, videoDays, VideoDays::label, enabled, videoDaysChanged()) {
                chooseVideoDays(it, pickFirst = pick)
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
        Row(
            Modifier.fillMaxWidth().toggleable(saveEvery, role = Role.Switch, onValueChange = ::switchSaveEvery),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The label and the ⓘ take the width the switch leaves (U0: with a Spacer of the same weight
            // the label got half of it and wrapped in the tablet's pane); not filling, so the ⓘ stays
            // beside the label, also when a large font wraps it.
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text("Save every picture", Modifier.weight(1f, fill = false))
                Text(
                    "ⓘ",
                    Modifier
                        .clickable(role = Role.Button, onClick = { openInfo = "About saving" })
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clearAndSetSemantics { contentDescription = "About saving" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = saveEvery, onCheckedChange = null)
        }
        if (saveEvery) {
            FlowRow(
                Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Largest original")
                Dropdown("Largest original", listOf(10, 20, 50, 100, 0), saveLimit, ::limitLabel) { chooseSaveLimit(it) }
            }
        }
        if (openInfo == "About saving") {
            AlertDialog(
                onDismissRequest = { openInfo = null },
                confirmButton = { TextButton(onClick = { openInfo = null }) { Text("OK") } },
                text = {
                    Text(
                        "Saves each new picture to Pictures/APODroid: full size up to the limit, else screen size. " +
                            "A few hundred MB a month or more.",
                    )
                },
            )
        }
    }

    private fun limitLabel(mb: Int) = if (mb == 0) "No limit" else "$mb MB"

    /**
     * Before Set's picker (2026-11-15; placeholder texts, Android's own labels word for word, the
     * FP6's AOSP picker): which answer to tap, "Home screen", or "Home screen and lock screen"
     * when the one engine shows both ([PickerAsk.both]). OK opens the picker; Cancel, Back or a
     * tap outside saves and sets nothing. With home live ([PickerAsk.lockToo], SPLIT, user,
     * 2026-11-15) it asks "Live on the lock screen too?" and has "Not now" instead of Cancel,
     * which saves the Set as without the picker; Back or a tap outside still saves nothing.
     */
    @Composable
    private fun PickerDialog(ask: PickerAsk) {
        val screens = if (ask.both) "Home screen and lock screen" else "Home screen"
        val question = if (ask.lockToo) "Live on the lock screen too?\n" else ""
        AlertDialog(
            onDismissRequest = { ask.answer.complete(PickerAnswer.CANCEL) },
            confirmButton = { TextButton(onClick = { ask.answer.complete(PickerAnswer.OK) }) { Text("OK") } },
            dismissButton = {
                if (ask.lockToo) {
                    TextButton(onClick = { ask.answer.complete(PickerAnswer.NOT_NOW) }) { Text("Not now") }
                } else {
                    TextButton(onClick = { ask.answer.complete(PickerAnswer.CANCEL) }) { Text("Cancel") }
                }
            },
            text = { Text("${question}Next, in Android's preview:\nSet wallpaper › $screens") },
        )
    }

    /** NASA and Earth Observatory post on weekdays only; shown when one is ticked, until hidden for good. */
    @Composable
    private fun WeekdayDialog() {
        AlertDialog(
            onDismissRequest = { closeWeekdayDialog(forever = false) },
            confirmButton = { TextButton(onClick = { closeWeekdayDialog(forever = false) }) { Text("Dismiss") } },
            dismissButton = { TextButton(onClick = { closeWeekdayDialog(forever = true) }) { Text("Don't show again") } },
            text = {
                Text("NASA Image of the Day and Earth Observatory post on weekdays only.")
            },
        )
    }

    /**
     * The detector's box (O14; on-time-0.7.0.md, The detector), while the daily switch is on, from a
     * miss ([Store.missedAt]: the phone stopped the app and the change came late) until dismissed.
     * With "Keep running" off then: the switch ("Keep running" opens Advanced settings at it); a
     * miss with it off hides once it's on. With it on: the maker's settings (:core's [maker]).
     * Placeholder texts until the user approves them.
     */
    @Composable
    private fun MissNotice() {
        val missed = missedAt ?: return
        if (!daily || missed == missDismissed || !missedKeep && keepRunning) return
        val time = missedWhen(
            missed, Instant.now(), zone,
            missed.atZone(zone).format(DateTimeFormatter.ofPattern(if (hours24) "HH:mm" else "h:mm\u00A0a", Locale.ENGLISH)),
        )
        if (!missedKeep) {
            NoticeBox({
                TextButton(onClick = ::openKeepRunning) { Text("Keep running") }
                TextButton(onClick = ::dismissMiss) { Text("Dismiss") }
            }) { Text("Your phone stopped APODroid: the $time came late.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            NoticeBox({
                TextButton(onClick = ::openAppInfo) { Text("App info") }
                TextButton(onClick = ::dismissMiss) { Text("Dismiss") }
            }) {
                Text("Your phone still stops APODroid: the $time came late.", style = MaterialTheme.typography.bodyMedium)
                stopLines().forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    /**
     * The detector's lines with "Keep running" on: the maker's settings that let an app start and
     * run in the background (on-time-0.7.0.md, What stops the job; dontkillmyapp.com, the makers'
     * guides). Placeholders until checked on their phones. Stock Android and Lenovo have no such
     * setting (O8: none on ZUI): only a force stop ends the changes there.
     */
    private fun stopLines(): List<String> = when (phoneMaker) {
        Maker.XIAOMI -> listOf("App info › Autostart: on", "App info › Battery saver › No restrictions")
        Maker.SAMSUNG -> listOf("Battery › Background usage limits › Never sleeping apps › APODroid")
        Maker.OPPO -> listOf("App info › Battery usage › \"Allow background activity\": on", "App info › Battery usage › \"Allow auto launch\": on")
        Maker.VIVO -> listOf("Battery › High background power consumption › APODroid: on", "App info › Autostart: on")
        Maker.HUAWEI -> listOf(
            "App info › Battery usage details › App launch settings › \"Manage automatically\": off",
            "Auto-launch, Secondary launch, Run in background: on",
        )
        Maker.LENOVO, Maker.STOCK -> listOf("A force stop ends the changes until APODroid opens.")
    }

    @Composable
    private fun BatteryNotice() {
        if (daily && (batteryLimited || mayHibernate) && !pauseHintHidden) PauseHint()
    }

    /** Names only the settings still missing; hiding it asks first, with what will happen. */
    @Composable
    private fun PauseHint() {
        val unused = if (Build.VERSION.SDK_INT >= 33) {
            // Stock Android 13–14: "Pause app activity if unused"; the test phone (Android 16): "Manage app if unused".
            // Android's label word for word (the FP6's Settings: unused_apps_switch_v2 on 15+, unused_apps_switch before).
            "App info › Unused app settings › \"${if (Build.VERSION.SDK_INT >= 35) "Manage app if unused" else "Pause app activity if unused"}\": off"
        } else {
            // Android 12 (API 31–32): "Unused apps" with "Remove permissions and free up space" (B25).
            "App info › Unused apps › \"Remove permissions and free up space\": off"
        }
        val (missing, effect) = when {
            batteryLimited && mayHibernate -> listOf(batteryLine(), unused) to
                "after about 8 days the wallpaper only changes while the phone charges, and after about 3 months it stops"
            batteryLimited -> listOf(batteryLine()) to "after about 8 days the wallpaper only changes while the phone charges"
            else -> listOf(unused) to "after about 3 months the wallpaper stops changing"
        }
        NoticeBox({
            TextButton(onClick = ::openAppInfo) { Text("App info") }
            if (batteryLimited) TextButton(onClick = ::openBatteryOptimisation) { Text("Battery optimisation") }
            TextButton(onClick = { pauseConfirm = true }) { Text("Don't show again") }
        }) {
            Text("Android pauses apps you don't open.", style = MaterialTheme.typography.bodyMedium)
            missing.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (pauseConfirm) {
            AlertDialog(
                onDismissRequest = { pauseConfirm = false },
                confirmButton = { TextButton(onClick = { pauseConfirm = false; hidePauseHint() }) { Text("Hide") } },
                dismissButton = { TextButton(onClick = { pauseConfirm = false }) { Text("Cancel") } },
                title = { Text("Hide this hint?") },
                text = { Text("If you don't open APODroid, $effect.") },
            )
        }
    }

    /**
     * The pause hint's battery line: the phone's own path to what `isIgnoringBatteryOptimizations`
     * reads (:core's [maker]; on-time-0.7.0.md, What stops the job). Placeholders from
     * dontkillmyapp.com and the makers' guides until checked on their phones; Android's own path
     * (Pixel, the FP6, the Lenovo tablet) otherwise.
     */
    private fun batteryLine(): String = when (phoneMaker) {
        // MagicOS has no "Unrestricted" in App info (the friend's Honor X60): the Doze allowlist.
        Maker.HUAWEI -> "Battery optimisation › All apps › APODroid › Don't allow"
        Maker.XIAOMI -> "App info › Battery saver › No restrictions"
        Maker.SAMSUNG -> "App info › Battery › Unrestricted"
        Maker.OPPO -> "App info › Battery usage › \"Allow background activity\": on"
        Maker.VIVO -> "Battery › High background power consumption › APODroid: on"
        Maker.LENOVO, Maker.STOCK ->
            // Android 15+ hides the choice behind a switch whose row has to be tapped; the choice stays
            // greyed out until the switch is on, and a fresh install shows it off (keep-running.md).
            if (Build.VERSION.SDK_INT >= 35) {
                "App info › App battery usage › \"Allow background usage\": on › Unrestricted"
            } else {
                "App info › App battery usage › Unrestricted"
            } + " (on Optimized, a new picture can come hours late)"
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

/** The source's picture in the video notice, e.g. "NASA's Image of the Day instead". */
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

/** Size's segments (L6; SA3a: the user's words, 2026-11-15). */
private val Sizing.label
    get() = when (this) {
        Sizing.CENTRE -> "Centre"
        Sizing.AUTO -> "Auto"
        Sizing.LIVE -> "Live"
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
    putBoolean("soon", soon)
    putBoolean("darken", darken)
    putBoolean("autoFallback", autoFallback)
    putString("size", size.name)
    putString("order", order.joinToString(",") { it.name })
    // SA3b: a position or sideways not noted is "null" (partOf reads it back).
    parts.forEach { (source, part) -> putString("part.${source.name}", part.toText()) }
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
        pictureAt = pictureAtOrDefault(bundle.getInt("pictureAt", DEFAULT_PICTURE_AT)),
        soon = bundle.getBoolean("soon"),
        darken = bundle.getBoolean("darken", true),
        autoFallback = bundle.getBoolean("autoFallback", true),
        size = sizeOf(bundle.getString("size")),
        order = orderOf(bundle.getString("order")),
        parts = Source.entries.mapNotNull { source ->
            bundle.getString("part.${source.name}")?.let { source to partOf(it) }
        }.toMap(),
    )
}.getOrNull()

/** VS1: the decoded still of the video of APOD's post of [date] at [url]. */
private class VideoStill(val date: LocalDate, val url: String, val picture: ImageBitmap) {
    fun isFor(post: Post) = post.date == date && post.videoUrl == url
}

/**
 * Draws [picture] as `Image(bitmap = …)` does, but nothing once it's recycled ([MainActivity.onStop],
 * O12): a frame drawn before the page recomposes mustn't crash.
 */
private class PicturePainter(private val picture: ImageBitmap) : Painter() {
    override val intrinsicSize = Size(picture.width.toFloat(), picture.height.toFloat())

    override fun DrawScope.onDraw() {
        if (picture.asAndroidBitmap().isRecycled) return
        drawImage(picture, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
    }
}

/**
 * Set's question before the picker ([MainActivity.pickerAsk]): the page that asks, whether the
 * answer is both screens, whether it asks for Live on the lock screen too (home live, SPLIT),
 * and the user's answer.
 */
class PickerAsk(val page: String, val both: Boolean, val lockToo: Boolean) {
    val answer = CompletableDeferred<PickerAnswer>()
}

/** The answer to [PickerAsk]: OK, "Not now" (only while it asks for the lock screen too), or Cancel, Back, a tap outside or the page gone. */
enum class PickerAnswer { OK, NOT_NOW, CANCEL }

/** What Set's trip to the picker came to ([MainActivity.pickLive]). */
enum class Pick { NONE, LOCK_ONLY, GO }
