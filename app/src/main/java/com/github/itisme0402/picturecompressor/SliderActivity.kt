package com.github.itisme0402.picturecompressor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

class SliderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = this
        val imageUri = intent.getStringExtra("image_uri")?.toUri()
        setContent {
            val coroutineScope = rememberCoroutineScope()

            var quality by rememberSaveable { mutableFloatStateOf(100f) }
            var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var previewBytes by remember { mutableStateOf<ByteArray?>(null) }
            var previewBitmap by remember {
                mutableStateOf<ImageBitmap?>(null)
            }
            LaunchedEffect(previewBytes) {
                previewBytes?.let { bytes ->
                    previewBitmap = withContext(Dispatchers.Default) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                    }
                }
            }
            var originalSize by remember { mutableIntStateOf(0) }

            // Load original bitmap once
            LaunchedEffect(imageUri) {
                imageUri?.let {
                    contentResolver.openInputStream(it)?.use { input ->
                        val bytes = input.readBytes()
                        originalSize = bytes.size
                        originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            }

            // Update preview when quality or bitmap changes
            LaunchedEffect(quality, originalBitmap) {
                originalBitmap?.let { bitmap ->
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality.toInt(), stream)
                    previewBytes = stream.toByteArray()
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.format_compression_quality, quality.toInt()))

                Slider(
                    value = quality,
                    onValueChange = { quality = it },
                    valueRange = 1f..100f,
                    steps = 98
                )

                Spacer(modifier = Modifier.height(16.dp))

                val compressedSize = previewBytes?.size
                if (originalSize > 0 && compressedSize != null) {
                    val originalSizeKb = originalSize / 1000.0
                    val compressedSizeKb = compressedSize / 1000.0
                    val sizeDelta = (compressedSize - originalSize) / 1000.0
                    val percentageDelta = (compressedSize.toDouble() / originalSize * 100).roundToInt() - 100
                    val text = String.format(
                        Locale.getDefault(),
                        "%.1f kB → %.1f kB (%+.1f kB, %+d%%)",
                        originalSizeKb,
                        compressedSizeKb,
                        sizeDelta,
                        percentageDelta
                    )
                    Text(text)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(stringResource(R.string.format_compressed_preview))
                previewBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = {
                    originalBitmap?.let { bitmap ->
                        val compressedFolder = File(cacheDir, "compressed")
                        compressedFolder.mkdirs()
                        val compressedFile = File(compressedFolder, "compressed.jpg")
                        compressedFile.delete()
                        coroutineScope.launch(Dispatchers.IO) {
                            compressedFile.delete()
                            compressedFile.outputStream().use {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.toInt(), it)
                            }
                        }

                        val compressedUri = FileProvider.getUriForFile(
                            context,
                            BuildConfig.FILE_PROVIDER_AUTHORITY,
                            compressedFile
                        )

                        val intent = Intent(context, ResultActivity::class.java).apply {
                            putExtra(ResultActivity.EXTRA_IMAGE_URI, imageUri.toString())
                            putExtra(ResultActivity.EXTRA_COMPRESSED_URI, compressedUri.toString())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(intent)
                    }
                }) {
                    Text(stringResource(R.string.text_continue))
                }
            }
        }
    }
}
