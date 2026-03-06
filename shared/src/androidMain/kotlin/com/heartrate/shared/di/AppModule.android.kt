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
 * Android-specific Koin DI module with actual repository implementation
 */
val androidSharedModule = module {

    // Repository - use Android actual implementation
    single<HeartRateRepository> { HeartRateRepositoryImpl() }

    // Use Cases
    singleOf(::ObserveHeartRate)
    singleOf(::GetBatteryLevel)

    // ViewModels (created as factory to allow multiple instances)
    factoryOf(::HeartRateViewModel)
}

/**
 * Get all Koin modules for Android apps (Wear OS, Phone)
 */
actual fun getAppModules(): List<Module> = listOf(androidSharedModule)
