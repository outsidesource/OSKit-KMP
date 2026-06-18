package com.outsidesource.oskitkmp.capability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shared Android implementation of [IKmpCapability]. Concrete capabilities provide the small set of
 * variation points (required permissions, hardware check, optional enablable-service check, and any
 * system broadcasts that should trigger a status re-query); everything else — permission requesting,
 * lifecycle/broadcast-driven status updates, and the app-settings screen — is implemented here.
 */
internal abstract class AndroidKmpCapability : IInitializableKmpCapability, IKmpCapability {

    protected var context: KmpCapabilityContext? = null
    protected val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var permissionResultLauncher: ActivityResultLauncher<Array<String>>? = null
    protected var enableResultLauncher: ActivityResultLauncher<Intent>? = null
    private val permissionsResultFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    protected val enableResultFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var hardwareSupportsCapability: Boolean = false
    private var hasRequestedPermissions: Boolean = false

    /** The runtime permissions required by this capability. May be empty. */
    protected abstract val permissions: Array<String>

    /** System broadcast actions that should trigger a status re-query (e.g. service enabled/disabled). */
    protected open val serviceChangeActions: List<String> = emptyList()

    // Each capability must declare its full support surface explicitly — no inherited defaults.
    abstract override val hasPermissions: Boolean
    abstract override val hasEnablableService: Boolean
    abstract override val supportsRequestEnable: Boolean
    abstract override val supportsOpenAppSettingsScreen: Boolean
    abstract override val supportsOpenServiceSettingsScreen: Boolean

    override val status: Flow<CapabilityStatus> = callbackFlow {
        val activity = context?.activity ?: return@callbackFlow

        launch {
            activity.lifecycle.currentStateFlow.collect {
                when (it) {
                    Lifecycle.State.RESUMED -> send(queryStatus())
                    else -> {}
                }
            }
        }

        val receivers = serviceChangeActions.map { action ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    launch { send(queryStatus()) }
                }
            }
            ContextCompat.registerReceiver(activity, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
            receiver
        }

        send(queryStatus())

        awaitClose {
            receivers.forEach { activity.unregisterReceiver(it) }
        }
    }.distinctUntilChanged()

    override fun init(context: KmpCapabilityContext) {
        this.context = context
        hardwareSupportsCapability = checkHardwareSupport()

        permissionResultLauncher = context.activity
            .registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                scope.launch { permissionsResultFlow.emit(Unit) }
            }

        enableResultLauncher = context.activity
            .registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                scope.launch { enableResultFlow.emit(Unit) }
            }

        onInit()
    }

    override suspend fun queryStatus(): CapabilityStatus {
        val activity = context?.activity ?: return CapabilityStatus.Unknown

        if (!hardwareSupportsCapability) return CapabilityStatus.Unsupported()

        val hasAuthorization = permissions
            .all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }

        if (!hasAuthorization) {
            val reason = if (hasRequestedPermissions) {
                NoPermissionReason.DeniedPermanently
            } else {
                NoPermissionReason.NotRequested
            }
            return CapabilityStatus.NoPermission(reason)
        }

        return queryServiceStatus()
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        try {
            context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            withContext(Dispatchers.Main) { permissionResultLauncher?.launch(permissions) }
            permissionsResultFlow.firstOrNull()
            hasRequestedPermissions = true
            return Outcome.Ok(queryStatus())
        } catch (e: Exception) {
            return Outcome.Error(Unit)
        }
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> = internalOpenAppSettingsScreen(context)

    /** Determines whether the underlying hardware/platform supports this capability. Called during [init]. */
    protected abstract fun checkHardwareSupport(): Boolean

    /** Hook for acquiring system services after [context] is set. */
    protected open fun onInit() {}

    /**
     * Status check performed once permissions are granted, e.g. whether an enablable service is on.
     * Defaults to [CapabilityStatus.Ready] for permission-only capabilities.
     */
    protected open fun queryServiceStatus(): CapabilityStatus = CapabilityStatus.Ready
}
