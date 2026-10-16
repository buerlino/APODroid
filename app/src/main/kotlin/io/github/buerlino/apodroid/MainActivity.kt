package io.github.buerlino.apodroid

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.buerlino.apodroid.core.Apod
import kotlinx.coroutines.Dispatchers
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

    private var apod by mutableStateOf<Apod?>(null)
    private var picture by mutableStateOf<ImageBitmap?>(null)
    private var loading by mutableStateOf(false)
    private var refreshing by mutableStateOf(false)
    private var failed by mutableStateOf(false)
    private var problem by mutableStateOf<Problem?>(null)
    private var checkedAt by mutableStateOf<Instant?>(null)
    private var wallpaperSetAt by mutableStateOf<Instant?>(null)
    private var saved by mutableStateOf(false)
    private var daily by mutableStateOf(false)
    private var where by mutableStateOf(Where.BOTH)
    private var videoDays by mutableStateOf(VideoDays.KEEP)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        daily = store.daily
        pauseHintHidden = store.pauseHintHidden
        videoNoticeDate = store.videoNoticeDate
        videoNoticeHidden = store.videoNoticeHidden
        problemNoticeKey = store.problemNoticeKey
        where = store.where
        videoDays = store.videoDays
        hasFallback = store.fallbackFile.exists()
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { Page() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Hibernation or a force stop deleted the job; scheduling runs it at once.
        if (daily && !DailyJob.isScheduled(this) && !DailyJob.schedule(this)) {
            Log.w("APODroid", "Couldn't schedule the daily job again")
        }
        checkPausing()
        load()
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

    /**
     * Shows the stored APOD (also when the daily job stored a new one while the page was in the
     * background), then runs [Store.update]: fetches the newest one unless the stored one is
     * today's (always with [force], pull to refresh) and sets it while the daily change is on.
     */
    private fun load(force: Boolean = false) {
        if (loading) return
        loading = true
        failed = false
        lifecycleScope.launch {
            if (picture == null || store.apod != apod) show()
            readStatus()
            val result = withContext(Dispatchers.IO) { runCatching { store.update(force) } }
            // Also when this update found nothing new: another one (the job's) may have.
            if (picture == null || store.apod != apod) show()
            readStatus()
            result.onSuccess { if (it != null) Log.i("APODroid", "Page: $it") }
            result.onFailure { Log.w("APODroid", "Update failed: $it", it) }
            failed = result.isFailure && picture == null && apod?.isVideo != true
            saved = withContext(Dispatchers.IO) { store.isSaved }
            loading = false
            refreshing = false
        }
    }

    /** What the last check and the last set stored (also the job's). */
    private fun readStatus() {
        problem = store.problem
        checkedAt = store.checkedAt
        wallpaperSetAt = store.wallpaperSetAt
    }

    /** The cause in a sentence, with since when nothing has worked once that's more than a day. */
    private fun problemText(problem: Problem): String {
        val since = checkedAt?.takeIf { it < Instant.now().minus(1, ChronoUnit.DAYS) }
            ?.let { " since " + it.atZone(ZoneId.systemDefault()).format(dayFormat) }
        return when (problem) {
            Problem.Network -> "Couldn't reach NASA's site${since.orEmpty()}. Check the connection."
            is Problem.Http -> if (since == null) {
                "NASA's site answered with an error (${problem.code}). The app tries again later."
            } else {
                "NASA's site has answered with an error (${problem.code})$since. The app tries again later."
            }
            Problem.SiteChanged -> "NASA's site has changed${since?.let { "; nothing new$it" }.orEmpty()}. " +
                "An app update may be needed."
        }
    }

    /** Under the daily switch: when the wallpaper last changed, or that a video day kept it. */
    private fun statusText(): String? = when {
        apod?.isVideo == true && !(videoDays == VideoDays.MINE && hasFallback) -> "Video day, wallpaper kept"
        else -> wallpaperSetAt?.let {
            val time = if (DateFormat.is24HourFormat(this)) "d MMM, HH:mm" else "d MMM, h:mm a"
            "Wallpaper changed " + it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(time, Locale.ENGLISH))
        }
    }

    private fun pullToRefresh() {
        refreshing = true
        load(force = true)
    }

    /** A new date closes crop mode; a changed title or explanation for the same date doesn't. */
    private suspend fun show() {
        val stored = store.apod
        if (stored?.date != apod?.date) {
            cropping = false
            cropPosition = store.cropPosition(stored?.date)
        }
        apod = stored
        // Video days have no picture (an entry stored by 0.3.1 may still have a still frame).
        picture = if (apod?.isVideo == true) null else withContext(Dispatchers.IO) {
            store.imageFile.takeIf { it.exists() }?.let { decodeForScreen(it) }?.asImageBitmap()
        }
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
        if (on) {
            store.forgetWallpaper()
            load()
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
        }
        return "$kind:${LocalDate.now()}"
    }

    private fun dismissProblem(problem: Problem) {
        val key = problemKey(problem)
        problemNoticeKey = key
        store.problemNoticeKey = key
    }

    private fun setWallpaperNow() {
        val file = store.wallpaperFile()
        if (file == null) {
            toast("Video day: wallpaper kept")
            return
        }
        cropping = false
        set(file)
    }

    private fun set(file: File) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.setWallpaper(file) } }
            result.onFailure { Log.w("APODroid", "Set failed: $it", it) }
            readStatus()
            toast(if (result.isSuccess) "Wallpaper set" else "Couldn't set the wallpaper")
        }
    }

    /**
     * A setting changed: while the daily change is on, the wallpaper follows at once (the button
     * is only there while it's off). [videoOnly] for the video-day settings, which change nothing
     * on an image day.
     */
    private fun applyNow(videoOnly: Boolean = false) {
        if (!daily || (videoOnly && apod?.isVideo != true)) return
        store.wallpaperFile()?.let(::set)
    }

    private fun savePicture() {
        if (saved) {
            toast("Already saved")
            return
        }
        val date = apod?.date ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.save(date) } }
            result.onFailure { Log.w("APODroid", "Save failed: $it", it) }
            if (result.getOrNull() == false) {
                toast("Not saved: a new picture came in")
                load()
                return@launch
            }
            saved = result.isSuccess
            toast(if (result.isSuccess) "Saved to Pictures/APODroid" else "Couldn't save the picture")
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
            if (ok && videoDays == VideoDays.MINE) applyNow(videoOnly = true)
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

    @Composable
    private fun Page() {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = ::pullToRefresh,
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Picture()
                HorizontalDivider()
                Settings()
            }
        }
    }

    @Composable
    private fun Picture() {
        val apod = apod
        val picture = picture
        if (apod == null || (picture == null && !apod.isVideo)) {
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
        var expanded by rememberSaveable(apod.date) { mutableStateOf(false) }
        val hasExplanation = apod.explanation.isNotEmpty()
        // Back leaves crop mode without setting; the dragged position stays for the job.
        BackHandler(enabled = cropping) { cropping = false }
        Column {
            // Every box that applies shows: problem, video notice, battery hint.
            val shownProblem = problem?.takeIf { problemKey(it) != problemNoticeKey }
            if (picture == null) VideoTile(apod) else Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    bitmap = picture,
                    contentDescription = apod.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(picture.width.toFloat() / picture.height)
                        .then(if (cropping) Modifier.cropFrame(picture, apod.date) else Modifier.clickable { openPage(apod.pageUrl) }),
                    contentScale = ContentScale.FillWidth,
                )
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
                        Modifier.clearAndSetSemantics { contentDescription = if (saved) "Saved" else "Save" },
                        color = Color.White,
                        fontSize = 24.sp,
                    )
                }
            }
            if (shownProblem != null) {
                NoticeBox({ TextButton(onClick = { dismissProblem(shownProblem) }) { Text("Dismiss") } }) {
                    Text(problemText(shownProblem), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (picture == null && !videoNoticeHidden && videoNoticeDate != apod.date.toString()) VideoNotice()
            BatteryNotice()
            Row(
                Modifier
                    .combinedClickable(
                        onClick = { if (hasExplanation) expanded = !expanded },
                        onLongClickLabel = "Copy link",
                        onLongClick = { copy("Link", apod.pageUrl) },
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(apod.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        apod.date.format(dateFormat) + if (apod.isVideo) " · Video" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hasExplanation) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Text(
                            if (expanded) "▴" else "▾",
                            Modifier.clearAndSetSemantics {
                                contentDescription = if (expanded) "Hide explanation" else "Show explanation"
                            },
                            fontSize = 24.sp,
                        )
                    }
                }
            }
            if (expanded) {
                Text(
                    apod.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .combinedClickable(
                            onClickLabel = "Hide explanation",
                            onClick = { expanded = false },
                            onLongClickLabel = "Copy explanation",
                            onLongClick = { copy("Explanation", apod.explanation) },
                        )
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            }
        }
    }

    /**
     * Video days: no picture (the still frame is often a generic NASA image, or missing), so no
     * ⛶ or ★. Tapping plays the video on the APOD page. The wallpaper is the previous picture or
     * the user's own.
     */
    @Composable
    private fun VideoTile(apod: Apod) {
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
        val watch = "Tap ▶ to watch it on NASA's site."
        val kept = "This APOD is a video, so your wallpaper stays as it was."
        val text = when {
            !daily -> "This APOD is a video. $watch"
            videoDays == VideoDays.MINE && hasFallback ->
                "This APOD is a video, so your own picture is the wallpaper. $watch"
            videoDays == VideoDays.MINE -> "$kept $watch Pick a picture below to use it on video days."
            else -> "$kept $watch To use your own picture on video days, choose \"Use my picture\" below."
        }
        NoticeBox({
            TextButton(onClick = { dismissVideoNotice(forever = false) }) { Text("Dismiss") }
            TextButton(onClick = { dismissVideoNotice(forever = true) }) { Text("Don't show again") }
        }) { Text(text, style = MaterialTheme.typography.bodyMedium) }
    }

    /** The orange box below the picture or tile, for problems, the video notice and the battery hint. */
    @Composable
    private fun NoticeBox(buttons: @Composable RowScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
        val orange = Color(0xFFFF9800)
        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .fillMaxWidth()
                .background(orange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .border(1.dp, orange, RoundedCornerShape(4.dp))
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
    private fun Modifier.cropFrame(picture: ImageBitmap, date: LocalDate): Modifier {
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
        }.pointerInput(picture, date) {
            // The distance the frame can travel, in pixels on the page.
            val travel = if (horizontal) {
                size.width * (1 - full.width().toFloat() / picture.width)
            } else {
                size.height * (1 - full.height().toFloat() / picture.height)
            }
            if (travel <= 0f) return@pointerInput
            val move = { delta: Float -> cropPosition = (cropPosition + delta / travel).coerceIn(0f, 1f) }
            val end = { store.setCropPosition(date, cropPosition) }
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
            Row(
                Modifier.fillMaxWidth().toggleable(daily, role = Role.Switch, onValueChange = ::switchDaily),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Change wallpaper daily", Modifier.weight(1f))
                Switch(checked = daily, onCheckedChange = null)
            }
            if (daily) statusText()?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Heading("Wallpaper on")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val labels = mapOf(Where.HOME to "Home screen", Where.LOCK to "Lock screen", Where.BOTH to "Both")
                Where.entries.forEachIndexed { i, w ->
                    SegmentedButton(
                        selected = where == w,
                        onClick = {
                            if (where != w) {
                                where = w; store.where = w
                                applyNow()
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, Where.entries.size),
                    ) { Text(labels.getValue(w)) }
                }
            }

            Heading("On video days")
            Choice("Keep the previous picture", videoDays == VideoDays.KEEP) {
                videoDays = VideoDays.KEEP; store.videoDays = VideoDays.KEEP
            }
            Choice("Use my picture", videoDays == VideoDays.MINE) {
                if (videoDays != VideoDays.MINE) {
                    videoDays = VideoDays.MINE; store.videoDays = VideoDays.MINE
                    applyNow(videoOnly = true)
                }
            }
            if (videoDays == VideoDays.MINE) {
                OutlinedButton(
                    onClick = { pick.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.padding(start = 48.dp),
                ) { Text(if (hasFallback) "Change picture" else "Pick picture") }
            }

            // While the daily change is on, the app keeps the wallpaper current.
            if (!daily) {
                Button(
                    onClick = ::setWallpaperNow,
                    enabled = apod != null,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) { Text("Set as wallpaper now") }
            }
        }
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

    @Composable
    private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().selectable(selected, onClick = onClick, role = Role.RadioButton),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
            Text(label)
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
private val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
