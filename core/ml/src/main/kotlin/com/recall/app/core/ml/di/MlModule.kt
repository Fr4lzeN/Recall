package com.recall.app.core.ml.di

import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.ml.MockEmbeddingModel
import com.recall.app.core.ml.ModelProfileSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MlModule {
    @Provides
    @Singleton
    fun provideEmbeddingModel(profileSelector: ModelProfileSelector): EmbeddingModel {
        val profile = profileSelector.selectProfile()
        return MockEmbeddingModel(profile)
    }
}
