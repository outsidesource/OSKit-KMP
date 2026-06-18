package com.outsidesource.oskitkmp.capability

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.outsidesource.oskitkmp.outcome.Outcome

internal class LocationKmpCapability(
    private val flags: Array<LocationCapabilityFlags>,
) : AndroidKmpCapability() {

    private var locationManager: LocationManager? = null

    override val permissions = flags
        .filter {
            if (Build.VERSION.SDK_INT < 29 && it == LocationCapabilityFlags.BackgroundLocation) return@filter false
            if (Build.VERSION.SDK_INT > 30 && it == LocationCapabilityFlags.BluetoothAccess) return@filter false
            true
        }
        .map {
            when (it) {
                LocationCapabilityFlags.BluetoothAccess -> Manifest.permission.ACCESS_FINE_LOCATION
                LocationCapabilityFlags.CoarseLocation -> Manifest.permission.ACCESS_COARSE_LOCATION
                LocationCapabilityFlags.FineLocation -> Manifest.permission.ACCESS_FINE_LOCATION
                LocationCapabilityFlags.BackgroundLocation -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }
        }.toTypedArray()

    private val isEnabledRequiredForFlags = when (flags.size) {
        1 -> if (flags[0] == LocationCapabilityFlags.BluetoothAccess) Build.VERSION.SDK_INT <= 30 else true
        else -> true
    }

    override val serviceChangeActions = listOf(LocationManager.MODE_CHANGED_ACTION)

    override val hasPermissions: Boolean = permissions.isNotEmpty()
    override val hasEnablableService: Boolean = true
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenServiceSettingsScreen: Boolean = true

    override fun onInit() {
        val activity = context?.activity ?: return
        locationManager = ContextCompat.getSystemService(activity, LocationManager::class.java)
    }

    override fun checkHardwareSupport(): Boolean =
        context?.activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_LOCATION) ?: false

    override fun queryServiceStatus(): CapabilityStatus {
        if (isEnabledRequiredForFlags && locationManager?.isLocationEnabled == false) return CapabilityStatus.NotEnabled
        return CapabilityStatus.Ready
    }

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> {
        try {
            val activity = context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            val openEnableSettings = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivity(openEnableSettings)
            return Outcome.Ok(Unit)
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }
}
