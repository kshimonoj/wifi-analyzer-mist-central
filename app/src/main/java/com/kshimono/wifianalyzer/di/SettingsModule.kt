package com.kshimono.wifianalyzer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.kshimono.wifianalyzer.data.settings.SettingsRepository
import com.kshimono.wifianalyzer.data.settings.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        @Provides @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.appDataStore
    }
}
