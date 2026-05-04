package com.kshimono.wifianalyzer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kshimono.wifianalyzer.data.db.dao.ApLocationDao
import com.kshimono.wifianalyzer.data.db.dao.ArubaApDao
import com.kshimono.wifianalyzer.data.db.dao.FloorMapDao
import com.kshimono.wifianalyzer.data.db.dao.MistApDao
import com.kshimono.wifianalyzer.data.db.dao.SnapshotDao
import com.kshimono.wifianalyzer.data.db.entities.ApLocationEntity
import com.kshimono.wifianalyzer.data.db.entities.ArubaApEntity
import com.kshimono.wifianalyzer.data.db.entities.FloorMapEntity
import com.kshimono.wifianalyzer.data.db.entities.MistApEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotObservationEntity

@Database(
    entities     = [
        SnapshotEntity::class,
        SnapshotObservationEntity::class,
        MistApEntity::class,
        ArubaApEntity::class,
        FloorMapEntity::class,
        ApLocationEntity::class,
    ],
    version      = 9,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    abstract fun mistApDao(): MistApDao
    abstract fun arubaApDao(): ArubaApDao
    abstract fun floorMapDao(): FloorMapDao
    abstract fun apLocationDao(): ApLocationDao

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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE snapshots ADD COLUMN gpsAccuracy REAL"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `floor_maps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `siteId` TEXT,
                        `floorId` TEXT,
                        `imageUri` TEXT NOT NULL,
                        `widthPx` INTEGER NOT NULL,
                        `heightPx` INTEGER NOT NULL,
                        `widthM` REAL,
                        `heightM` REAL,
                        `scalePixelsPerMeter` REAL,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                database.execSQL("ALTER TABLE snapshots ADD COLUMN floorMapId INTEGER")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN mapX REAL")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN mapY REAL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE floor_maps ADD COLUMN lengthM REAL")
                database.execSQL("ALTER TABLE floor_maps ADD COLUMN breadthM REAL")
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `ap_locations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `floorMapId` INTEGER NOT NULL,
                        `apName` TEXT NOT NULL,
                        `macAddress` TEXT,
                        `model` TEXT,
                        `source` TEXT NOT NULL,
                        `mapX` REAL NOT NULL,
                        `mapY` REAL NOT NULL,
                        `status` TEXT,
                        `radiosJson` TEXT,
                        `lastSynced` INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE snapshots ADD COLUMN connectedSsid TEXT")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN connectedBssid TEXT")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN connectedApName TEXT")
            }
        }
    }
}
