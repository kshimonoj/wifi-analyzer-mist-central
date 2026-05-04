package com.kshimono.wifianalyzer.di

import com.kshimono.wifianalyzer.data.floormap.FloorMapRepository
import com.kshimono.wifianalyzer.data.floormap.FloorMapRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FloorMapModule {

    @Binds @Singleton
    abstract fun bindFloorMapRepository(impl: FloorMapRepositoryImpl): FloorMapRepository
}
