package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow

class LocationCapability(private val flags: Array<LocationCapabilityFlags>) : IInitializableCapability, ICapability {
    override fun init(context: CapabilityContext) {
        TODO("Not yet implemented")
    }

    override val status: CapabilityStatus
        get() = TODO("Not yet implemented")
    override val statusFlow: Flow<CapabilityStatus>
        get() = TODO("Not yet implemented")
    override val hasPermissions: Boolean
        get() = TODO("Not yet implemented")
    override val hasEnablableService: Boolean
        get() = TODO("Not yet implemented")
    override val supportsRequestEnable: Boolean
        get() = TODO("Not yet implemented")
    override val supportsOpenAppSettingsScreen: Boolean
        get() = TODO("Not yet implemented")
    override val supportsOpenEnableSettingsScreen: Boolean
        get() = TODO("Not yet implemented")

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        TODO("Not yet implemented")
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> {
        TODO("Not yet implemented")
    }

    override suspend fun openEnableSettingsScreen(): Outcome<Unit, Any> {
        TODO("Not yet implemented")
    }

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> {
        TODO("Not yet implemented")
    }

}