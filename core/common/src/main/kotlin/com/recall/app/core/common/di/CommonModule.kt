package com.recall.app.core.common.di

import com.recall.app.core.common.DefaultRecallDispatchers
import com.recall.app.core.common.RecallDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {
    @Provides
    @Singleton
    fun provideRecallDispatchers(): RecallDispatchers = DefaultRecallDispatchers()
}
