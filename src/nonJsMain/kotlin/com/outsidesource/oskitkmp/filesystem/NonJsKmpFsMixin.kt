package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

internal class NonJsKmpFsMixin(
    private val fsType: KmpFsType,
    private val isInitialized: () -> Boolean,
) : IKmpFs {

    override suspend fun resolveFile(
        dir: KmpFsRef,
        fileName: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        return try {
            if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
            if (dir.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)
            if (!dir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)

            val path = joinPathSegments(dir.ref, fileName).toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(KmpFsError.RefNotFound)
            if (exists && FileSystem.SYSTEM.metadata(path).isDirectory) {
                return Outcome.Error(KmpFsError.RefExistsAsDirectory)
            }
            if (!exists && create) FileSystem.SYSTEM.sink(path, mustCreate = true)

            val ref = KmpFsRef(
                ref = path.pathString,
                name = fileName,
                isDirectory = false,
                fsType = this@NonJsKmpFsMixin.fsType,
            )
            return Outcome.Ok(ref)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        return try {
            if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
            if (dir.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)
            if (!dir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)

            val path = joinPathSegments(dir.ref, name).toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(KmpFsError.RefNotFound)
            if (exists && !FileSystem.SYSTEM.metadata(path).isDirectory) {
                return Outcome.Error(KmpFsError.RefExistsAsFile)
            }
            if (!exists && create) FileSystem.SYSTEM.createDirectory(path, mustCreate = true)

            val ref = KmpFsRef(
                ref = path.pathString,
                name = name,
                isDirectory = true,
                fsType = this@NonJsKmpFsMixin.fsType,
            )
            return Outcome.Ok(ref)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> {
        return try {
            if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
            if (ref.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)

            FileSystem.SYSTEM.deleteRecursively(ref.ref.toPath())
            return Outcome.Ok(Unit)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun list(
        dir: KmpFsRef,
        isRecursive: Boolean,
    ): Outcome<List<KmpFsRef>, KmpFsError> {
        return try {
            if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
            if (dir.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)
            if (!dir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)

            val path = dir.ref.toPath()

            val refs = if (isRecursive) {
                FileSystem.SYSTEM.listRecursively(path).toList()
            } else {
                FileSystem.SYSTEM.list(path)
            }.mapNotNull {
                val metadata = FileSystem.SYSTEM.metadataOrNull(it) ?: return@mapNotNull null

                KmpFsRef(
                    ref = it.pathString,
                    name = it.name,
                    isDirectory = metadata.isDirectory,
                    fsType = this@NonJsKmpFsMixin.fsType,
                )
            }

            Outcome.Ok(refs)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> {
        return try {
            if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
            if (ref.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)

            val path = ref.ref.toPath()
            val metadata = FileSystem.SYSTEM.metadata(path)
            val size = metadata.size ?: return Outcome.Error(KmpFsError.Unknown(Unit))
            Outcome.Ok(KmpFileMetadata(size = size))
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun exists(ref: KmpFsRef): Boolean {
        return try {
            if (!isInitialized()) return false
            if (ref.fsType != fsType) return false

            FileSystem.SYSTEM.exists(ref.ref.toPath())
        } catch (_: Throwable) {
            false
        }
    }
}
