package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlin.io.path.name
import kotlin.io.path.pathString

actual fun platformInternalKmpFs(): IInternalKmpFs = AndroidInternalKmpFs()

internal class AndroidInternalKmpFs() : IInternalKmpFs, IInitializableKmpFs {

    private val fsMixin = NonJsKmpFsMixin(fsType = KmpFsType.Internal, isInitialized = { context != null })
    private var context: KmpFsContext? = null

    override val root: KmpFsRef by lazy {
        val context = context?.applicationContext ?: throw KmpFsError.NotInitialized
        val path = context.filesDir.toPath()

        KmpFsRef(
            ref = path.pathString,
            name = path.name,
            isDirectory = true,
            fsType = KmpFsType.Internal,
        )
    }

    override fun init(context: KmpFsContext) {
        this.context = context
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
