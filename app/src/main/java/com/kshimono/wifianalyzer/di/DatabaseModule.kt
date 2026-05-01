package com.kshimono.wifianalyzer.di

import android.content.Context
import androidx.room.Room
import com.kshimono.wifianalyzer.data.db.AppDatabase
import com.kshimono.wifianalyzer.data.db.SnapshotRepository
import com.kshimono.wifianalyzer.data.db.SnapshotRepositoryImpl
import com.kshimono.wifianalyzer.data.db.dao.ArubaApDao
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
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .build()

        @Provides @Singleton
        fun provideSnapshotDao(db: AppDatabase): SnapshotDao = db.snapshotDao()

        @Provides @Singleton
        fun provideMistApDao(db: AppDatabase): MistApDao = db.mistApDao()

        @Provides @Singleton
        fun provideArubaApDao(db: AppDatabase): ArubaApDao = db.arubaApDao()
    }
}
