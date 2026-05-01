package com.kshimono.wifianalyzer.di

import com.kshimono.wifianalyzer.data.mist.MistRepository
import com.kshimono.wifianalyzer.data.mist.MistRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MistModule {

    @Binds @Singleton
    abstract fun bindMistRepository(impl: MistRepositoryImpl): MistRepository
}
