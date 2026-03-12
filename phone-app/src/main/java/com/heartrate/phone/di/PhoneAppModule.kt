package com.heartrate.phone.di

import com.heartrate.phone.BuildConfig
import com.heartrate.phone.data.PhoneRelayHeartRateRepository
import com.heartrate.phone.network.PhoneWebSocketRelayServer
import com.heartrate.shared.domain.repository.HeartRateRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val phoneAppModule = module {
    single { PhoneWebSocketRelayServer(port = BuildConfig.WS_SERVER_PORT) }
    single<HeartRateRepository> {
        PhoneRelayHeartRateRepository(
            appContext = androidContext(),
            relayServer = get()
        )
    }
}
