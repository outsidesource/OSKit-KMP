package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.coroutines.*

actual fun platformInternalKmpFs(): IInternalKmpFs = WasmInternalKmpFs()

internal class WasmInternalKmpFs() : IInternalKmpFs, IInitializableKmpFs {

    private val fsMixin = WasmKmpFsMixin(
        fsType = KmpFsType.Internal,
        sanitizeRef = { if (it == root) internalRoot.await() else it },
        isInitialized = { context != null },
    )

    private val internalRoot = CompletableDeferred<KmpFsRef>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var context: KmpFsContext? = null

    override val root: KmpFsRef = KmpFsRef("00000000000000000000000000", "", true, KmpFsType.Internal)

    override fun init(context: KmpFsContext) {
        this.context = context
        scope.launch {
            if (!supportsOpfs) {
                internalRoot.completeExceptionally(KmpFsError.NotSupported)
                return@launch
            }

            val rootHandle = navigator.storage.getDirectory().kmpAwaitOutcome().unwrapOrReturn {
                internalRoot.completeExceptionally(KmpFsError.NotInitialized)
                return@launch
            }
            val key = WasmFsHandleRegister.putHandle(rootHandle)
            val ref = KmpFsRef(ref = key, name = rootHandle.name, isDirectory = true, fsType = KmpFsType.Internal)
            internalRoot.complete(ref)
        }
    }

    override suspend fun resolveFile(dir: KmpFsRef, name: String, create: Boolean): Outcome<KmpFsRef, KmpFsError> =
        fsMixin.resolveFile(dir, name, create)

    override suspend fun resolveDirectory(dir: KmpFsRef, name: String, create: Boolean): Outcome<KmpFsRef, KmpFsError> =
        fsMixin.resolveDirectory(dir, name, create)

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> = fsMixin.delete(ref)

    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> =
        fsMixin.list(dir, isRecursive)

    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> = fsMixin.readMetadata(ref)

    override suspend fun exists(ref: KmpFsRef): Boolean = fsMixin.exists(ref)
}
