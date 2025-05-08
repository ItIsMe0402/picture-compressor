package com.github.itisme0402.picturecompressor

import android.net.Uri
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
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.util.Locale

class ResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)?.toUri()
        val compressedUri = intent.getStringExtra(EXTRA_COMPRESSED_URI)?.toUri()

        val originalSize = imageUri?.let { getFileSizeFromUri(it) }
        val compressedSize = compressedUri?.let { getFileSizeFromUri(it) }

        setContent {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.format_original_image, originalSize?.let(::formatSize) ?: "???"))

                imageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(stringResource(R.string.format_compressed_image, compressedSize?.let(::formatSize) ?: "???"))

                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(this@ResultActivity)
                            .data(compressedUri)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
        }
    }

    private fun getFileSizeFromUri(uri: Uri): Int {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize.toInt()
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "${BuildConfig.APPLICATION_ID}.image_uri"
        const val EXTRA_COMPRESSED_URI = "${BuildConfig.APPLICATION_ID}.compressed_uri"

        fun formatSize(bytes: Int): String {
            val kb = bytes / 1000.0
            return String.format(Locale.ROOT, "%.1f kB", kb)
        }
    }
}

