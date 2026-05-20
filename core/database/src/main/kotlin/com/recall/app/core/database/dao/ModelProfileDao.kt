package com.recall.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recall.app.core.database.entity.ModelProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelProfileDao {
    @Query("SELECT * FROM model_profiles")
    fun observeAll(): Flow<List<ModelProfileEntity>>

    @Query("SELECT * FROM model_profiles WHERE is_active = 1 LIMIT 1")
    suspend fun getActive(): ModelProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<ModelProfileEntity>)

    @Query("UPDATE model_profiles SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE model_profiles SET is_active = 1 WHERE id = :id")
    suspend fun activate(id: Long)
}
