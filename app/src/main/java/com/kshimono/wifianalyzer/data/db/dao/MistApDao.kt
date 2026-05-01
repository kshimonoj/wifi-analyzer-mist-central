package com.kshimono.wifianalyzer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kshimono.wifianalyzer.data.db.entities.MistApEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MistApDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(aps: List<MistApEntity>)

    @Query("DELETE FROM mist_aps WHERE orgId = :orgId")
    suspend fun deleteByOrgId(orgId: String)

    @Query("SELECT * FROM mist_aps WHERE macPrefix = :prefix LIMIT 1")
    suspend fun findByMacPrefix(prefix: String): MistApEntity?

    @Query("SELECT * FROM mist_aps WHERE radioPrefixes LIKE '%' || :prefix || '%' LIMIT 1")
    suspend fun findByRadioPrefix(prefix: String): MistApEntity?

    @Query("SELECT * FROM mist_aps ORDER BY name ASC")
    fun getAll(): Flow<List<MistApEntity>>

    @Query("SELECT COUNT(*) FROM mist_aps")
    suspend fun count(): Int
}
