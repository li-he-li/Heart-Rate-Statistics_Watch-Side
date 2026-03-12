package com.heartrate.desktop.di

import com.heartrate.desktop.data.DesktopWebSocketHeartRateRepository
import com.heartrate.shared.domain.repository.HeartRateRepository
import org.koin.dsl.module

val desktopAppModule = module {
    single<HeartRateRepository> {
        DesktopWebSocketHeartRateRepository(webSocketClient = get())
    }
}
