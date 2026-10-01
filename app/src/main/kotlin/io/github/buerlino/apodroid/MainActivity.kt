package io.github.buerlino.apodroid

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.github.buerlino.apodroid.core.Apod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var store: Store

    private var apod by mutableStateOf<Apod?>(null)
    private var picture by mutableStateOf<ImageBitmap?>(null)
    private var loading by mutableStateOf(false)
    private var failed by mutableStateOf(false)
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

    /** Shows the stored APOD, then fetches the newest one unless the stored one is today's. */
    private fun load() {
        if (loading) return
        loading = true
        failed = false
        lifecycleScope.launch {
            if (picture == null) show()
            if (!store.isCurrent) {
                val changed = withContext(Dispatchers.IO) { runCatching { store.refresh() } }
                changed.onSuccess { if (it || picture == null) show() else apod = store.apod }
                changed.onFailure { Log.w("APODroid", "Refresh failed: $it", it) }
                failed = changed.isFailure && picture == null
            }
            loading = false
        }
    }

    private suspend fun show() {
        apod = store.apod
        picture = withContext(Dispatchers.IO) {
            store.imageFile.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }?.asImageBitmap()
        }
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

    private fun saveFallback(uri: Uri) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)!!.use { input ->
                        store.fallbackFile.outputStream().use { input.copyTo(it) }
                    }
                }.isSuccess && isImage(store.fallbackFile)
            }
            if (!ok) {
                store.fallbackFile.delete()
                toast("Couldn't use that picture")
            }
            hasFallback = ok
        }
    }

    private fun openPage(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
        Column(Modifier.clickable { openPage(apod.pageUrl) }) {
            Image(
                bitmap = picture,
                contentDescription = apod.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(picture.width.toFloat() / picture.height),
                contentScale = ContentScale.FillWidth,
            )
            Column(Modifier.padding(16.dp)) {
                Text(apod.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    apod.date.format(dateFormat) + if (apod.isVideo) " · Video" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
