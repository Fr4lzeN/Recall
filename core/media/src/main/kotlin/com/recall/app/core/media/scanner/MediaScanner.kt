package com.recall.app.core.media.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.recall.app.core.common.RecallDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: RecallDispatchers,
) {
    suspend fun scanAll(): List<ScannedMediaItem> = withContext(dispatchers.io) {
        scanImages() + scanVideos()
    }

    suspend fun scanSince(lastScanTimestamp: Long): List<ScannedMediaItem> = withContext(dispatchers.io) {
        val selection = "${MediaStore.MediaColumns.DATE_ADDED} >= ?"
        val selectionArgs = arrayOf(lastScanTimestamp.toString())
        scanImages(selection, selectionArgs) + scanVideos(selection, selectionArgs)
    }

    suspend fun scanFolders(): List<MediaFolder> = withContext(dispatchers.io) {
        val buckets = linkedMapOf<String, BucketAccumulator>()
        accumulateBuckets(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, buckets)
        accumulateBuckets(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, buckets)
        buckets.values
            .map { acc ->
                MediaFolder(
                    bucketId = acc.bucketId,
                    displayName = acc.displayName,
                    path = acc.path,
                    itemCount = acc.count,
                    coverUris = acc.coverUris.toList(),
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }

    suspend fun getMediaIdsInBuckets(bucketIds: Set<String>): List<Long> = withContext(dispatchers.io) {
        if (bucketIds.isEmpty()) return@withContext emptyList()
        val ids = mutableSetOf<Long>()
        collectMediaIdsInBuckets(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, bucketIds, ids)
        collectMediaIdsInBuckets(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, bucketIds, ids)
        ids.toList()
    }

    private fun scanImages(
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ): List<ScannedMediaItem> {
        val projection = IMAGE_PROJECTION
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        return queryMedia(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection = projection,
            selection = selection,
            selectionArgs = selectionArgs,
            sortOrder = sortOrder,
            isVideo = false,
        )
    }

    private fun scanVideos(
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ): List<ScannedMediaItem> {
        val projection = VIDEO_PROJECTION
        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        return queryMedia(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection = projection,
            selection = selection,
            selectionArgs = selectionArgs,
            sortOrder = sortOrder,
            isVideo = true,
        )
    }

    private fun queryMedia(
        collection: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String,
        isVideo: Boolean,
    ): List<ScannedMediaItem> {
        val items = mutableListOf<ScannedMediaItem>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val durationCol = if (isVideo) {
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(displayNameCol) ?: ""
                val dateTaken = if (cursor.isNull(dateTakenCol)) null else cursor.getLong(dateTakenCol)
                val dateAdded = cursor.getLong(dateAddedCol)
                val mimeType = cursor.getString(mimeTypeCol) ?: ""
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val size = cursor.getLong(sizeCol)
                val bucketId = cursor.getString(bucketIdCol) ?: ""
                val duration = if (isVideo && durationCol >= 0 && !cursor.isNull(durationCol)) {
                    cursor.getLong(durationCol)
                } else {
                    null
                }

                items.add(
                    ScannedMediaItem(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = displayName,
                        dateTaken = dateTaken,
                        dateAdded = dateAdded,
                        mimeType = mimeType,
                        width = width,
                        height = height,
                        size = size,
                        duration = duration,
                        bucketId = bucketId,
                    ),
                )
            }
        }
        return items
    }

    private fun accumulateBuckets(
        collection: Uri,
        buckets: MutableMap<String, BucketAccumulator>,
    ) {
        context.contentResolver.query(
            collection,
            FOLDER_PROJECTION,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC",
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val relativePathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdCol) ?: continue
                val bucketName = cursor.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: bucketId
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id).toString()
                val path = resolveFolderPath(cursor, relativePathCol, dataCol, bucketName)

                val acc = buckets.getOrPut(bucketId) {
                    BucketAccumulator(
                        bucketId = bucketId,
                        displayName = bucketName,
                        path = path,
                    )
                }
                acc.count++
                if (acc.coverUris.size < 4) {
                    acc.coverUris.add(uri)
                }
                if (acc.displayName.isBlank()) {
                    acc.displayName = bucketName
                }
                if (acc.path.isBlank() || acc.path == acc.displayName) {
                    acc.path = path
                }
            }
        }
    }

    private fun collectMediaIdsInBuckets(
        collection: Uri,
        bucketIds: Set<String>,
        ids: MutableSet<Long>,
    ) {
        val selection = "${MediaStore.MediaColumns.BUCKET_ID} IN (${bucketIds.joinToString(",") { "?" }})"
        val selectionArgs = bucketIds.toTypedArray()
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(idCol))
            }
        }
    }

    private fun resolveFolderPath(
        cursor: android.database.Cursor,
        relativePathCol: Int,
        dataCol: Int,
        fallback: String,
    ): String {
        if (relativePathCol >= 0 && !cursor.isNull(relativePathCol)) {
            return cursor.getString(relativePathCol).trimEnd('/')
        }
        if (dataCol >= 0 && !cursor.isNull(dataCol)) {
            return cursor.getString(dataCol).substringBeforeLast('/')
        }
        return fallback
    }

    private class BucketAccumulator(
        val bucketId: String,
        var displayName: String,
        var path: String,
        var count: Int = 0,
        val coverUris: MutableList<String> = mutableListOf(),
    )

    private companion object {
        private val IMAGE_PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_ID,
        )

        private val VIDEO_PROJECTION = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
        )

        private val FOLDER_PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATA,
        )
    }
}
