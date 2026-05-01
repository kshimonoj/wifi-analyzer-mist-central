package com.kshimono.wifianalyzer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kshimono.wifianalyzer.data.db.entities.ArubaApEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArubaApDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(aps: List<ArubaApEntity>)

    @Query("DELETE FROM aruba_aps")
    suspend fun deleteAll()

    @Query("SELECT * FROM aruba_aps WHERE bssid = :bssid LIMIT 1")
    suspend fun findByBssid(bssid: String): ArubaApEntity?

    @Query("SELECT * FROM aruba_aps ORDER BY deviceName ASC")
    fun getAll(): Flow<List<ArubaApEntity>>

    @Query("SELECT COUNT(*) FROM aruba_aps")
    suspend fun count(): Int
}
