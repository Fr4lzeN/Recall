package com.recall.app.core.media.keyframe

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.net.Uri
import com.recall.app.core.common.RecallDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyframeExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: RecallDispatchers,
) {
    suspend fun extractKeyframes(
        videoUri: Uri,
        maxFrames: Int = 3,
    ): List<Bitmap> = withContext(dispatchers.io) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val duration = retriever.extractMetadata(METADATA_KEY_DURATION)?.toLongOrNull()
                ?: return@withContext emptyList()

            val timestamps = calculateTimestamps(duration, maxFrames)
            timestamps.mapNotNull { timestampMs ->
                retriever.getFrameAtTime(timestampMs * 1000, OPTION_CLOSEST_SYNC)
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            retriever.release()
        }
    }

    private fun calculateTimestamps(durationMs: Long, count: Int): List<Long> {
        if (durationMs <= 0 || count <= 0) return emptyList()
        val interval = durationMs / (count + 1)
        return (1..count).map { it * interval }
    }
}
