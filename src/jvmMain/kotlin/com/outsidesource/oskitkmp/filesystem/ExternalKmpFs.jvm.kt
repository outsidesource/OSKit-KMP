package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.Platform
import com.outsidesource.oskitkmp.lib.current
import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileSystem
import okio.Path.Companion.toPath

actual fun platformExternalKmpFs(): IExternalKmpFs = JvmExternalKmpFs()

internal class JvmExternalKmpFs : IExternalKmpFs, IInitializableKmpFs {
    private val fsMixin = NonJsKmpFsMixin(fsType = KmpFsType.External, isInitialized = { context != null })
    private var context: KmpFsContext? = null

    private val picker: IKmpFsFilePicker? = when (Platform.current) {
        Platform.MacOS -> MacOsFilePicker
        Platform.Windows -> WindowsFilePicker
        Platform.Linux -> LinuxFilePicker
        else -> null
    }

    override fun init(context: KmpFsContext) {
        this@JvmExternalKmpFs.context = context
    }

    override suspend fun pickFile(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (picker == null) return Outcome.Error(KmpFsError.NotSupported)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        return try {
            return picker.pickFile(startingDir, filter)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (picker == null) return Outcome.Error(KmpFsError.NotSupported)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        return try {
            return picker.pickFiles(startingDir, filter)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (picker == null) return Outcome.Error(KmpFsError.NotSupported)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        return try {
            return picker.pickDirectory(startingDir)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (picker == null) return Outcome.Error(KmpFsError.NotSupported)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        return try {
            return picker.pickSaveFile(fileName, startingDir)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun saveFile(
        bytes: ByteArray,
        fileName: String,
    ): Outcome<Unit, KmpFsError> = nonJsSaveFile(bytes, fileName)

    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)

        return try {
            val localPath = path.toPath()
            val exists = FileSystem.SYSTEM.exists(localPath)

            if (!exists) return Outcome.Error(KmpFsError.RefNotFound)
            val metadata = FileSystem.SYSTEM.metadata(localPath)

            val ref = KmpFsRef(
                ref = localPath.pathString,
                name = localPath.name,
                isDirectory = metadata.isDirectory,
                fsType = KmpFsType.External,
            )
            return Outcome.Ok(ref)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
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
