package com.kshimono.wifianalyzer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kshimono.wifianalyzer.data.db.dao.ArubaApDao
import com.kshimono.wifianalyzer.data.db.dao.MistApDao
import com.kshimono.wifianalyzer.data.db.dao.SnapshotDao
import com.kshimono.wifianalyzer.data.db.entities.ArubaApEntity
import com.kshimono.wifianalyzer.data.db.entities.MistApEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotObservationEntity

@Database(
    entities     = [SnapshotEntity::class, SnapshotObservationEntity::class, MistApEntity::class, ArubaApEntity::class],
    version      = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    abstract fun mistApDao(): MistApDao
    abstract fun arubaApDao(): ArubaApDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `mist_aps` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `mac` TEXT NOT NULL,
                        `macPrefix` TEXT NOT NULL,
                        `model` TEXT,
                        `serial` TEXT,
                        `siteId` TEXT NOT NULL,
                        `siteName` TEXT,
                        `orgId` TEXT NOT NULL,
                        `lastSynced` INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE mist_aps ADD COLUMN radioPrefixes TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `aruba_aps` (
                        `bssid` TEXT NOT NULL PRIMARY KEY,
                        `deviceName` TEXT NOT NULL,
                        `wlanName` TEXT NOT NULL,
                        `siteName` TEXT,
                        `serialNumber` TEXT,
                        `radioMacAddress` TEXT,
                        `lastSynced` INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE snapshot_observations ADD COLUMN arubaApName TEXT"
                )
            }
        }
    }
}
