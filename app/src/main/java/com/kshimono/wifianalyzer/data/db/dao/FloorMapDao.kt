package com.kshimono.wifianalyzer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kshimono.wifianalyzer.data.db.entities.FloorMapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FloorMapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(map: FloorMapEntity): Long

    @Query("SELECT * FROM floor_maps ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FloorMapEntity>>

    @Query("SELECT * FROM floor_maps WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FloorMapEntity?

    @Query("DELETE FROM floor_maps WHERE id = :id")
    suspend fun delete(id: Long)
}
