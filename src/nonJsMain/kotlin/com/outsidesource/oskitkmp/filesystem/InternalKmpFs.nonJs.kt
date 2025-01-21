package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM


internal fun IInternalKmpFs.nonJsResolveFile(
    dir: KmpFsRef,
    fileName: String,
    create: Boolean,
): Outcome<KmpFsRef, KmpFsError> {
    return try {
        val path = joinPathSegments(dir.ref, fileName).toPath()
        val exists = FileSystem.SYSTEM.exists(path)

        if (!exists && !create) return Outcome.Error(KmpFsError.FileNotFoundError)
        if (create) FileSystem.SYSTEM.sink(path, mustCreate = !exists)

        val ref = KmpFsRef(
            ref = path.pathString,
            name = fileName,
            isDirectory = false,
            type = KmpFsRefType.Internal,
        )
        return Outcome.Ok(ref)
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

internal fun IInternalKmpFs.nonJsResolveDirectory(
    dir: KmpFsRef,
    name: String,
    create: Boolean,
): Outcome<KmpFsRef, KmpFsError> {
    return try {
        val path = joinPathSegments(dir.ref, name).toPath()
        val exists = FileSystem.SYSTEM.exists(path)

        if (!exists && !create) return Outcome.Error(KmpFsError.FileNotFoundError)
        if (create) FileSystem.SYSTEM.createDirectory(path, mustCreate = !exists)

        val ref = KmpFsRef(
            ref = path.pathString,
            name = name,
            isDirectory = true,
            type = KmpFsRefType.Internal,
        )
        return Outcome.Ok(ref)
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

internal fun IInternalKmpFs.nonJResolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> {
    return try {
        val localPath = path.toPath()
        val exists = FileSystem.SYSTEM.exists(localPath)

        if (!exists) return Outcome.Error(KmpFsError.FileNotFoundError)
        val metadata = FileSystem.SYSTEM.metadata(localPath)

        val ref = KmpFsRef(
            ref = localPath.pathString,
            name = localPath.name,
            isDirectory = metadata.isDirectory,
            type = KmpFsRefType.Internal,
        )
        return Outcome.Ok(ref)
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

internal fun IInternalKmpFs.nonJsDelete(ref: KmpFsRef): Outcome<Unit, KmpFsError> {
    return try {
        FileSystem.SYSTEM.deleteRecursively(ref.ref.toPath())
        return Outcome.Ok(Unit)
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

internal fun IInternalKmpFs.nonJsList(
    dir: KmpFsRef,
    isRecursive: Boolean,
): Outcome<List<KmpFsRef>, KmpFsError> {
    return try {
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
                type = KmpFsRefType.Internal,
            )
        }

        Outcome.Ok(refs)
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

internal fun IInternalKmpFs.nonJsReadMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> {
    return try {
        val path = ref.ref.toPath()
        val metadata = FileSystem.SYSTEM.metadata(path)
        val size = metadata.size ?: return Outcome.Error(KmpFsError.FileMetadataError)
        Outcome.Ok(KmpFileMetadata(size = size))
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

internal fun IInternalKmpFs.nonJsExists(ref: KmpFsRef): Boolean {
    return try {
        FileSystem.SYSTEM.exists(ref.ref.toPath())
    } catch (_: Throwable) {
        false
    }
}