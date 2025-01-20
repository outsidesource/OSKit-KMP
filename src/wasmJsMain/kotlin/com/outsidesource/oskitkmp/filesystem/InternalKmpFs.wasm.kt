package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.outcome.Outcome

actual fun platformInternalKmpFs(): IInternalKmpFs = WasmInternalKmpFs()

internal class WasmInternalKmpFs() : IInternalKmpFs, IInitializableKmpFs {
    private var context: KmpFsContext? = null

    override val root: KmpFsRef
        get() = TODO("Not yet implemented")

    override fun init(context: KmpFsContext) {
        this.context = context
    }

    override suspend fun resolveFile(
        dir: KmpFsRef,
        fileName: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun list(
        dir: KmpFsRef,
        isRecursive: Boolean,
    ): Outcome<List<KmpFsRef>, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun exists(ref: KmpFsRef): Boolean {
        TODO("Not yet implemented")
    }
}
