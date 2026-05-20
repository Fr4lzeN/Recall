package com.recall.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.recall.app.core.database.RecallDatabase
import com.recall.app.core.database.dao.AppSettingDao
import com.recall.app.core.database.dao.IndexingJobDao
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.database.dao.ModelProfileDao
import com.recall.app.core.database.dao.VectorPostingDao
import com.recall.app.core.database.dao.VectorSegmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RecallDatabase {
        return Room.databaseBuilder(
            context,
            RecallDatabase::class.java,
            "recall.db",
        ).build()
    }

    @Provides
    fun provideMediaItemDao(db: RecallDatabase): MediaItemDao = db.mediaItemDao()

    @Provides
    fun provideIndexingJobDao(db: RecallDatabase): IndexingJobDao = db.indexingJobDao()

    @Provides
    fun provideVectorSegmentDao(db: RecallDatabase): VectorSegmentDao = db.vectorSegmentDao()

    @Provides
    fun provideVectorPostingDao(db: RecallDatabase): VectorPostingDao = db.vectorPostingDao()

    @Provides
    fun provideAppSettingDao(db: RecallDatabase): AppSettingDao = db.appSettingDao()

    @Provides
    fun provideModelProfileDao(db: RecallDatabase): ModelProfileDao = db.modelProfileDao()
}
