package com.recall.app.di

import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.vector.linear.LinearScanIndex
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VectorModule {
    @Provides
    @Singleton
    fun provideVectorIndex(embeddingModel: EmbeddingModel): VectorIndex {
        return LinearScanIndex(embeddingModel.dimensions)
    }
}
