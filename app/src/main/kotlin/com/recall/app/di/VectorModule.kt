package com.recall.app.di

import android.content.Context
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.vector.persistent.PersistentVectorIndex
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VectorModule {
    @Provides
    @Singleton
    fun provideVectorIndex(
        @ApplicationContext context: Context,
        embeddingModel: EmbeddingModel,
    ): VectorIndex {
        val indexDir = File(context.filesDir, "vector_index")
        indexDir.mkdirs()
        val indexFile = File(indexDir, "hnsw.idx")
        return PersistentVectorIndex(
            dimensions = embeddingModel.dimensions,
            indexFile = indexFile,
        )
    }
}
