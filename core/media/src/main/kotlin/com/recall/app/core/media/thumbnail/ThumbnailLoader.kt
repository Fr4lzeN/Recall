package com.recall.app.core.media.thumbnail

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.recall.app.core.common.RecallDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: RecallDispatchers,
) {
    suspend fun loadThumbnail(
        contentUri: Uri,
        width: Int = 256,
        height: Int = 256,
    ): Bitmap? = withContext(dispatchers.io) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(contentUri, Size(width, height), null)
        } else {
            loadLegacyThumbnail(contentUri)
        }
    }

    @Suppress("DEPRECATION")
    private fun loadLegacyThumbnail(contentUri: Uri): Bitmap? {
        val mediaId = ContentUris.parseId(contentUri)
        val isVideo = context.contentResolver.getType(contentUri)?.startsWith("video/") == true

        return if (isVideo) {
            MediaStore.Video.Thumbnails.getThumbnail(
                context.contentResolver,
                mediaId,
                MediaStore.Video.Thumbnails.MINI_KIND,
                null,
            )
        } else {
            MediaStore.Images.Thumbnails.getThumbnail(
                context.contentResolver,
                mediaId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null,
            )
        }
    }
}
