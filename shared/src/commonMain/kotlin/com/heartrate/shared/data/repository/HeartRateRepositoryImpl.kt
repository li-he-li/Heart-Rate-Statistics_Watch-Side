package com.heartrate.shared.data.repository

import com.heartrate.shared.data.model.HeartRateData
import com.heartrate.shared.domain.repository.HeartRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Expect declaration for platform-specific repository implementation.
 * Each platform provides its own actual implementation.
 */
expect class HeartRateRepositoryImpl() : HeartRateRepository
