package com.kshimono.wifianalyzer.di

import com.kshimono.wifianalyzer.data.aruba.ArubaRepository
import com.kshimono.wifianalyzer.data.aruba.ArubaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ArubaModule {

    @Binds @Singleton
    abstract fun bindArubaRepository(impl: ArubaRepositoryImpl): ArubaRepository
}
