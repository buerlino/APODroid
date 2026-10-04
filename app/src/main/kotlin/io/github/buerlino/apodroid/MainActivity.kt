package io.github.buerlino.apodroid

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.buerlino.apodroid.core.Apod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var store: Store

    private var apod by mutableStateOf<Apod?>(null)
    private var picture by mutableStateOf<ImageBitmap?>(null)
    private var loading by mutableStateOf(false)
    private var failed by mutableStateOf(false)
    private var saved by mutableStateOf(false)
    private var daily by mutableStateOf(false)
    private var where by mutableStateOf(Where.BOTH)
    private var videoDays by mutableStateOf(VideoDays.KEEP)
    private var hasFallback by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        daily = DailyJob.isScheduled(this)
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
        load()
    }

    /**
     * Shows the stored APOD (also when the daily job stored a new one while the page was in the
     * background), then fetches the newest one unless the stored one is today's.
     */
    private fun load() {
        if (loading) return
        loading = true
        failed = false
        lifecycleScope.launch {
            if (picture == null || store.apod != apod) show()
            if (!store.isCurrent) {
                val changed = withContext(Dispatchers.IO) { runCatching { store.refresh() } }
                changed.onSuccess { if (it || picture == null) show() else apod = store.apod }
                changed.onFailure { Log.w("APODroid", "Refresh failed: $it", it) }
                failed = changed.isFailure && picture == null
            }
            saved = withContext(Dispatchers.IO) { store.isSaved }
            loading = false
        }
    }

    private suspend fun show() {
        apod = store.apod
        picture = withContext(Dispatchers.IO) {
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
        val screen = resources.displayMetrics.widthPixels
        var sample = 1
        while (width / (sample * 2) >= screen || pixels / (sample * sample) > 8_000_000) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun switchDaily(on: Boolean) {
        if (on && !DailyJob.schedule(this)) {
            toast("Couldn't schedule the daily change")
            return
        }
        if (!on) DailyJob.cancel(this)
        daily = on
    }

    private fun setWallpaperNow() {
        val file = store.wallpaperFile()
        if (file == null) {
            toast("Video today: wallpaper kept")
            return
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.setWallpaper(file) } }
            toast(if (result.isSuccess) "Wallpaper set" else "Couldn't set the wallpaper")
        }
    }

    private fun savePicture() {
        if (saved) {
            toast("Already saved")
            return
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.save() } }
            result.onFailure { Log.w("APODroid", "Save failed: $it", it) }
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
        }
    }

    private fun openPage(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            toast("No browser")
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    @Composable
    private fun Page() {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
        ) {
            Picture()
            HorizontalDivider()
            Settings()
        }
    }

    @Composable
    private fun Picture() {
        val apod = apod
        val picture = picture
        if (apod == null || picture == null) {
            Box(Modifier.fillMaxWidth().aspectRatio(3f / 2f), contentAlignment = Alignment.Center) {
                when {
                    loading -> CircularProgressIndicator()
                    failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load today's picture.")
                        TextButton(onClick = ::load) { Text("Try again") }
                    }
                }
            }
            return
        }
        var expanded by rememberSaveable(apod.date) { mutableStateOf(false) }
        val hasExplanation = apod.explanation.isNotEmpty()
        Column {
            Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    bitmap = picture,
                    contentDescription = apod.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(picture.width.toFloat() / picture.height)
                        .clickable { openPage(apod.pageUrl) },
                    contentScale = ContentScale.FillWidth,
                )
                // Saving a video day's still frame isn't worth it: often it's a generic NASA image.
                if (!apod.isVideo) {
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
            }
            Row(
                Modifier
                    .combinedClickable(
                        onClick = { if (hasExplanation) expanded = !expanded },
                        onLongClick = { openPage(apod.pageUrl) },
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
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
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

            Heading("Wallpaper on")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val labels = mapOf(Where.HOME to "Home screen", Where.LOCK to "Lock screen", Where.BOTH to "Both")
                Where.entries.forEachIndexed { i, w ->
                    SegmentedButton(
                        selected = where == w,
                        onClick = { where = w; store.where = w },
                        shape = SegmentedButtonDefaults.itemShape(i, Where.entries.size),
                    ) { Text(labels.getValue(w)) }
                }
            }

            Heading("On video days")
            Choice("Keep the previous picture", videoDays == VideoDays.KEEP) {
                videoDays = VideoDays.KEEP; store.videoDays = VideoDays.KEEP
            }
            Choice("Use my picture", videoDays == VideoDays.MINE) {
                videoDays = VideoDays.MINE; store.videoDays = VideoDays.MINE
            }
            if (videoDays == VideoDays.MINE) {
                OutlinedButton(
                    onClick = { pick.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.padding(start = 48.dp),
                ) { Text(if (hasFallback) "Change picture" else "Pick picture") }
            }

            Button(
                onClick = ::setWallpaperNow,
                enabled = apod != null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Set as wallpaper now") }
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
