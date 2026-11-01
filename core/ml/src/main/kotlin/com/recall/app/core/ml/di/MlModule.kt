package com.recall.app.core.ml.di

import android.content.Context
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.ml.MockEmbeddingModel
import com.recall.app.core.ml.ModelProfile
import com.recall.app.core.ml.ModelAssetResolver
import com.recall.app.core.ml.ModelProfileSelector
import com.recall.app.core.ml.TFLiteEmbeddingModel
import com.recall.app.core.ml.UnavailableEmbeddingModel
import kotlinx.coroutines.CancellationException
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import java.io.IOException

@Module
@InstallIn(SingletonComponent::class)
object MlModule {
    @Provides
    @Singleton
    fun provideEmbeddingModel(
        @ApplicationContext context: Context,
        profileSelector: ModelProfileSelector,
        @Named("useRealModel") useRealModel: Boolean,
    ): EmbeddingModel {
        val profile = profileSelector.selectProfile()
        return provideEmbeddingModel(context, profile, useRealModel)
    }

    internal fun provideEmbeddingModel(
        context: Context,
        profile: ModelProfile,
        useRealModel: Boolean = true,
    ): EmbeddingModel {
        if (!useRealModel) {
            return MockEmbeddingModel(profile)
        }
        return try {
            val resolved = ModelAssetResolver.resolve(profile) { fileName ->
                try {
                    context.assets.open(fileName).use { }
                    true
                } catch (_: IOException) {
                    false
                }
            }
            TFLiteEmbeddingModel(context, resolved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UnavailableEmbeddingModel(profile, e)
        } catch (e: LinkageError) {
            UnavailableEmbeddingModel(profile, IllegalStateException(e.message, e))
        }
    }
}
