package com.recall.app.core.ml.di

import android.content.Context
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.ml.MockEmbeddingModel
import com.recall.app.core.ml.ModelProfile
import com.recall.app.core.ml.ModelProfileSelector
import com.recall.app.core.ml.TFLiteEmbeddingModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MlModule {
    @Provides
    @Singleton
    fun provideEmbeddingModel(
        @ApplicationContext context: Context,
        profileSelector: ModelProfileSelector,
    ): EmbeddingModel {
        val profile = profileSelector.selectProfile()
        return provideEmbeddingModel(context, profile)
    }

    internal fun provideEmbeddingModel(context: Context, profile: ModelProfile): EmbeddingModel {
        return try {
            context.assets.open(profile.modelFileName).close()
            TFLiteEmbeddingModel(context, profile)
        } catch (_: Exception) {
            MockEmbeddingModel(profile)
        }
    }
}
