package com.kshimono.wifianalyzer.di

import android.content.Context
import androidx.room.Room
import com.kshimono.wifianalyzer.data.db.AppDatabase
import com.kshimono.wifianalyzer.data.db.SnapshotRepository
import com.kshimono.wifianalyzer.data.db.SnapshotRepositoryImpl
import com.kshimono.wifianalyzer.data.db.dao.ApLocationDao
import com.kshimono.wifianalyzer.data.db.dao.ArubaApDao
import com.kshimono.wifianalyzer.data.db.dao.FloorMapDao
import com.kshimono.wifianalyzer.data.db.dao.MistApDao
import com.kshimono.wifianalyzer.data.db.dao.SnapshotDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds @Singleton
    abstract fun bindSnapshotRepository(impl: SnapshotRepositoryImpl): SnapshotRepository

    companion object {
        @Provides @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "wifi_analyzer.db")
                .addMigrations(
                    AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
                    AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5,
                    AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
                    AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9,
                )
                .build()

        @Provides @Singleton
        fun provideSnapshotDao(db: AppDatabase): SnapshotDao = db.snapshotDao()

        @Provides @Singleton
        fun provideMistApDao(db: AppDatabase): MistApDao = db.mistApDao()

        @Provides @Singleton
        fun provideArubaApDao(db: AppDatabase): ArubaApDao = db.arubaApDao()

        @Provides @Singleton
        fun provideFloorMapDao(db: AppDatabase): FloorMapDao = db.floorMapDao()

        @Provides @Singleton
        fun provideApLocationDao(db: AppDatabase): ApLocationDao = db.apLocationDao()
    }
}
