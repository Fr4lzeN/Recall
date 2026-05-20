package com.recall.app.di

import android.content.Context
import com.recall.app.core.database.dao.VectorPostingDao
import com.recall.app.core.database.dao.VectorSegmentDao
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.vector.PersistableVectorIndex
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.vector.segment.SegmentManifest
import com.recall.app.core.vector.segment.VectorPostingStore
import com.recall.app.core.vector.segmented.SegmentedVectorIndex
import com.recall.app.data.vector.RoomSegmentManifest
import com.recall.app.data.vector.RoomVectorPostingStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object VectorModule {
    @Provides
    @Singleton
    fun provideSegmentManifest(segmentDao: VectorSegmentDao): SegmentManifest {
        return RoomSegmentManifest(segmentDao)
    }

    @Provides
    @Singleton
    fun provideVectorPostingStore(postingDao: VectorPostingDao): VectorPostingStore {
        return RoomVectorPostingStore(postingDao)
    }

    @Provides
    @Singleton
    fun provideVectorIndex(
        @ApplicationContext context: Context,
        embeddingModel: EmbeddingModel,
        manifest: SegmentManifest,
        postingStore: VectorPostingStore,
    ): VectorIndex {
        val segmentsDir = File(context.filesDir, "vector_segments")
        return runBlocking {
            SegmentedVectorIndex.open(
                dimensions = embeddingModel.dimensions,
                segmentsDir = segmentsDir,
                manifest = manifest,
                postingStore = postingStore,
            )
        }
    }

    @Provides
    @Singleton
    fun providePersistableVectorIndex(vectorIndex: VectorIndex): PersistableVectorIndex {
        return vectorIndex as PersistableVectorIndex
    }
}
