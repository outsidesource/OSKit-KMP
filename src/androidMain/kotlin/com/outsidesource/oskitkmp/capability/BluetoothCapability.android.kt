package com.outsidesource.oskitkmp.capability

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.firstOrNull

internal class BluetoothKmpCapability(
    private val flags: Array<BluetoothCapabilityFlags>,
) : AndroidKmpCapability() {

    private var bluetoothAdapter: BluetoothAdapter? = null

    override val permissions = run {
        if (Build.VERSION.SDK_INT <= 30) return@run emptyList()

        flags.map {
            when (it) {
                BluetoothCapabilityFlags.Scan -> Manifest.permission.BLUETOOTH_SCAN
                BluetoothCapabilityFlags.Connect -> Manifest.permission.BLUETOOTH_CONNECT
                BluetoothCapabilityFlags.Advertise -> Manifest.permission.BLUETOOTH_ADVERTISE
            }
        }
    }.toTypedArray()

    override val serviceChangeActions = listOf(BluetoothAdapter.ACTION_STATE_CHANGED)

    override val hasPermissions: Boolean = permissions.isNotEmpty()
    override val hasEnablableService: Boolean = true
    override val supportsRequestEnable: Boolean = true
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenServiceSettingsScreen: Boolean = true

    override fun onInit() {
        val activity = context?.activity ?: return
        bluetoothAdapter = ContextCompat.getSystemService(activity, BluetoothManager::class.java)?.adapter
    }

    override fun checkHardwareSupport(): Boolean {
        val packageManager = context?.activity?.packageManager ?: return false
        val bluetoothClassicAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        val bleAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        return bleAvailable && bluetoothClassicAvailable
    }

    override fun queryServiceStatus(): CapabilityStatus {
        if (bluetoothAdapter?.isEnabled == false) return CapabilityStatus.NotEnabled
        return CapabilityStatus.Ready
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> {
        try {
            context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            enableResultLauncher?.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            enableResultFlow.firstOrNull()
            return Outcome.Ok(queryStatus())
        } catch (_: Throwable) {
            return Outcome.Error(Unit)
        }
    }

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> {
        try {
            val activity = context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            val openEnableSettings = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            activity.startActivity(openEnableSettings)
            return Outcome.Ok(Unit)
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }
}
