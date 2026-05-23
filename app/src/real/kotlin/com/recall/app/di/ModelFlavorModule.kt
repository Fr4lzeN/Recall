package com.recall.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object ModelFlavorModule {
    @Provides
    @Named("useRealModel")
    fun provideUseRealModel(): Boolean = true
}
