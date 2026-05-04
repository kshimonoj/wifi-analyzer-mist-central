package com.kshimono.wifianalyzer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kshimono.wifianalyzer.data.db.entities.ApLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(aps: List<ApLocationEntity>)

    @Query("DELETE FROM ap_locations WHERE floorMapId = :floorMapId")
    suspend fun deleteByFloorMapId(floorMapId: Long)

    @Query("SELECT * FROM ap_locations WHERE floorMapId = :floorMapId ORDER BY apName ASC")
    fun getByFloorMapId(floorMapId: Long): Flow<List<ApLocationEntity>>
}
