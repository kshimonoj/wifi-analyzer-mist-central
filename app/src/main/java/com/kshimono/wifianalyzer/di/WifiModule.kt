package com.kshimono.wifianalyzer.di

import com.kshimono.wifianalyzer.data.wifi.AndroidWifiScanner
import com.kshimono.wifianalyzer.data.wifi.WifiScanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WifiModule {

    @Binds
    @Singleton
    abstract fun bindWifiScanner(impl: AndroidWifiScanner): WifiScanner
}
