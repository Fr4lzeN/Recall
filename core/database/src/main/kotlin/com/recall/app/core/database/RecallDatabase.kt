package com.recall.app.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.recall.app.core.database.converter.Converters
import com.recall.app.core.database.dao.AppSettingDao
import com.recall.app.core.database.dao.IndexingJobDao
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.database.dao.ModelProfileDao
import com.recall.app.core.database.dao.VectorPostingDao
import com.recall.app.core.database.dao.VectorSegmentDao
import com.recall.app.core.database.entity.AppSettingEntity
import com.recall.app.core.database.entity.IndexingJobEntity
import com.recall.app.core.database.entity.MediaItemEntity
import com.recall.app.core.database.entity.ModelProfileEntity
import com.recall.app.core.database.entity.VectorPostingEntity
import com.recall.app.core.database.entity.VectorSegmentEntity

@Database(
    entities = [
        MediaItemEntity::class,
        IndexingJobEntity::class,
        VectorSegmentEntity::class,
        VectorPostingEntity::class,
        AppSettingEntity::class,
        ModelProfileEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class RecallDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun indexingJobDao(): IndexingJobDao
    abstract fun vectorSegmentDao(): VectorSegmentDao
    abstract fun vectorPostingDao(): VectorPostingDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun modelProfileDao(): ModelProfileDao
}
