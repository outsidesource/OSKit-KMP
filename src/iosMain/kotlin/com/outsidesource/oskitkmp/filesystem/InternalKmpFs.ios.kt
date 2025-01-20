package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.Path.Companion.toPath
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun platformInternalKmpFs(): IInternalKmpFs = IosInternalKmpFs()

internal class IosInternalKmpFs() : IInternalKmpFs, IInitializableKmpFs {
    private var context: KmpFsContext? = null

    override val root: KmpFsRef by lazy {
        val path = (NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true).first() as String)
            .toPath()

        KmpFsRef(
            ref = path.pathString,
            name = path.name,
            isDirectory = true,
            type = KmpFsRefType.Internal,
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
    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> = nonJResolveRefFromPath(path)
    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> = nonJsDelete(ref)
    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> =
        nonJsList(dir, isRecursive)
    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> = nonJsReadMetadata(ref)
    override suspend fun exists(ref: KmpFsRef): Boolean = nonJsExists(ref)
}
