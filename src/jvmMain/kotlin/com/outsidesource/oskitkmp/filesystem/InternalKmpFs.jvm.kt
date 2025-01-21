package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.outcome.Outcome
import java.io.File

actual fun platformInternalKmpFs(): IInternalKmpFs = JvmInternalKmpFs()

internal class JvmInternalKmpFs() : IInternalKmpFs, IInitializableKmpFs {
    private var context: KmpFsContext? = null

    override val root: KmpFsRef by lazy {
        val context = context ?: throw KmpFsError.NotInitialized
        val rootDir = File(FileUtil.appDirPath(context.appName))
        if (!rootDir.exists()) {
            if (!rootDir.mkdirs()) throw KmpFsError.CreateError
        }

        KmpFsRef(
            ref = rootDir.absolutePath,
            name = rootDir.name,
            isDirectory = true,
            type = KmpFsType.Internal,
        )
    }

    override fun init(context: KmpFsContext) {
        this.context = context
    }

    override suspend fun resolveFile(dir: KmpFsRef, fileName: String, create: Boolean): Outcome<KmpFsRef, KmpFsError> =
        nonJsResolveFile(dir, fileName, create)
    override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> = nonJsResolveDirectory(dir, name, create)
    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> = nonJsDelete(ref)
    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> =
        nonJsList(dir, isRecursive)
    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> = nonJsReadMetadata(ref)
    override suspend fun exists(ref: KmpFsRef): Boolean = nonJsExists(ref)
}
