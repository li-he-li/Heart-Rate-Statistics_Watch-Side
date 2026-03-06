package com.heartrate.shared.di

import com.heartrate.shared.data.repository.HeartRateRepositoryImpl
import com.heartrate.shared.domain.repository.HeartRateRepository
import com.heartrate.shared.domain.usecase.GetBatteryLevel
import com.heartrate.shared.domain.usecase.ObserveHeartRate
import com.heartrate.shared.presentation.viewmodel.HeartRateViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Desktop-specific Koin DI module with actual repository implementation
 */
val desktopSharedModule = module {

    // Repository - use Desktop actual implementation
    single<HeartRateRepository> { HeartRateRepositoryImpl() }

    // Use Cases
    singleOf(::ObserveHeartRate)
    singleOf(::GetBatteryLevel)

    // ViewModels (created as factory to allow multiple instances)
    factoryOf(::HeartRateViewModel)
}

/**
 * Get all Koin modules for Desktop app
 */
actual fun getAppModules(): List<Module> = listOf(desktopSharedModule)
