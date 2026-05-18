package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun internalOpenAppSettingsScreen(
    context: KmpCapabilityContext?,
): Outcome<Unit, Any> = withContext(Dispatchers.Main) {
    Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
}
