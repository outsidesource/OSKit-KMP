package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.outcome.Outcome
import java.io.File

actual fun platformInternalKmpFs(): IInternalKmpFs = JvmInternalKmpFs()

internal class JvmInternalKmpFs() : IInternalKmpFs, IInitializableKmpFs {

    private val fsMixin = NonJsKmpFsMixin(fsType = KmpFsType.Internal, isInitialized = { context != null })
    private var context: KmpFsContext? = null

    override val root: KmpFsRef by lazy {
        val context = context ?: throw KmpFsError.NotInitialized
        val rootDir = File(FsUtil.appDirPath(context.appName))
        if (!rootDir.exists()) {
            if (!rootDir.mkdirs()) throw KmpFsError.NotInitialized
        }

        KmpFsRef(
            ref = rootDir.absolutePath,
            name = rootDir.name,
            isDirectory = true,
            fsType = KmpFsType.Internal,
        )
    }

    override fun init(context: KmpFsContext) {
        this.context = context
    }

    override suspend fun resolveFile(dir: KmpFsRef, fileName: String, create: Boolean): Outcome<KmpFsRef, KmpFsError> =
        fsMixin.resolveFile(dir, fileName, create)

    override suspend fun resolveDirectory(dir: KmpFsRef, name: String, create: Boolean): Outcome<KmpFsRef, KmpFsError> =
        fsMixin.resolveDirectory(dir, name, create)

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> = fsMixin.delete(ref)

    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> =
        fsMixin.list(dir, isRecursive)

    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> = fsMixin.readMetadata(ref)

    override suspend fun exists(ref: KmpFsRef): Boolean = fsMixin.exists(ref)
}
