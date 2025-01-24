package com.outsidesource.oskitkmp.filesystem

import okio.Path

expect class KmpFsContext

/**
 * [KmpFs] Provides limited multiplatform (iOS, Android, Desktop, and Web Browser) filesystem interactions both
 * inside and outside application sandboxes in iOS and Android via a mostly unified API. [KmpFs] is split into two
 * primary APIs: [IInternalKmpFs] and [IExternalKmpFs] which both implement [IKmpFs].
 *
 * Internal:
 * The Internal API deals with files/directories inside the application sandbox. Files/directories made in Internal
 * are not meant for end-users to see. On some platforms (i.e. Android they may even be encrypted).
 *
 * External:
 * The External API deals with files/directories outside the application sandbox. Files/directories made in External
 * are visible and accessible to end-users. In order to access any file/directory, a user must call [pickDirectory]
 * or [pickFile]. Use [pickDirectory] to gain permissions to directories and any of their descendants.
 * The user may then take any action within that directory.
 *
 * [KmpFsRef]:
 * All interactions originate from a lightweight reference to a file/directory location called Ref. Refs do not contain
 * any file data or file handles themselves, only a reference to a location. Refs also contain basic information like
 * file/directory name and if it is a file or directory. Once a ref is obtained (via [pickFile], [pickDirectory] on
 * External or [root] on Internal), further operations may be performed.
 *
 * Refs can only be created by [KmpFs].
 *
 * Refs are safely persistable via [KmpFsRef.toPersistableString] or [KmpFsRef.toPersistableBytes]. Refs can then be
 * hydrated via [KmpFsRef.fromPersistableString] or [KmpFsRef.fromPersistableBytes].
 * However, since refs only point to a location, there is no guarantee a ref will point to an actual file/directory
 * if the file/directory has been deleted, moved, or renamed.
 *
 * WASM:
 * Due to browser constraints, [KmpFs] on WASM only supports a subset of functionality available in other targets.
 *  External Limitations:
 *  * All `startingDirectory` parameters are ignored
 *  * Chrome and derivatives support all other functionality
 *  * Firefox and Safari have the following limitations:
 *      * Only file picking and reading is supported
 *      * No support for persisting External KmpFileRefs
 *
 *  Internal Limitations:
 *  * All `startingDirectory` parameters are ignored
 *
 * Desktop/JVM:
 * Using KmpFileHandler for the desktop/JVM target will require consumers to include LWJGL's tinyfd library in their classpath
 * ```
 * val lwjglVersion = "3.3.3"
 * val lwjglNatives = Pair(
 * 	System.getProperty("os.name")!!,
 * 	System.getProperty("os.arch")!!
 * ).let { (name, arch) ->
 * 	when {
 * 		arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
 * 			if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
 * 				"natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
 * 			else if (arch.startsWith("ppc"))
 * 				"natives-linux-ppc64le"
 * 			else if (arch.startsWith("riscv"))
 * 				"natives-linux-riscv64"
 * 			else
 * 				"natives-linux"
 * 		arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) }     ->
 * 			"natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
 * 		arrayOf("Windows").any { name.startsWith(it) }                ->
 * 			if (arch.contains("64"))
 * 				"natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
 * 			else
 * 				"natives-windows-x86"
 * 		else                                                                            ->
 * 			throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
 * 	}
 * }
 *
 * dependencies {
 *     runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
 *     runtimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:$lwjglNatives")
 * }
 * ```
 *
 * It may also be necessary to add the `jdk.unsupported` module for some linux builds:
 * ```
 * compose.desktop {
 *    nativeDistributions {
 *        modules("jdk.unsupported")
 *    }
 * }
 * ```
 *
 * Usage:
 * [KmpFs] must be initialized by every platform in order to use it.
 * ```
 * KmpFs.init(KmpFsContext())
 *
 * // Internal Example
 * val ref = KmpFs.Internal.resolveFile(KmpFs.Internal.root, "test.txt").unwrapOrReturn { return it }
 * val sink = ref.sink().unwrapOrReturn { return it }
 * sink.use {
 *     it.writeUtf8("Hello World!")
 * }
 *
 * // External Example
 * val ref = KmpFs.External.pickSaveFile("test.txt").unwrapOrReturn { return it } ?: return // If null, no file was picked
 * val sink = ref.sink().unwrapOrReturn { return it }
 * sink.use {
 *     it.writeUtf8("Hello World!")
 * }
 * ```
 */
object KmpFs {
    val Internal: IInternalKmpFs = platformInternalKmpFs()
    val External: IExternalKmpFs = platformExternalKmpFs()

    /**
     * Initializes KmpFs. This must be called before any other interactions.
     */
    fun init(context: KmpFsContext) {
        (Internal as? IInitializableKmpFs)?.init(context)
        (External as? IInitializableKmpFs)?.init(context)
    }
}

internal expect fun platformExternalKmpFs(): IExternalKmpFs
internal expect fun platformInternalKmpFs(): IInternalKmpFs

internal interface IInitializableKmpFs {
    fun init(context: KmpFsContext)
}

typealias KmpFileFilter = List<KmpFileMimetype>

/**
 * Defines a common MimeType interface
 * @property extension defines the file extension used i.e. "txt", "png", "jpg"
 * @property mimeType defined the mimetype used i.e. "text/plain", "image/png", "image/jpeg"
 */
data class KmpFileMimetype(
    val extension: String,
    val mimeType: String,
)

/**
 * [KmpFileMetadata] defines metadata for files. Only size is supported for now.
 */
data class KmpFileMetadata(
    val size: Long,
)

sealed class KmpFsError(override val message: String) : Throwable(message) {
    data object NotInitialized : KmpFsError("KmpFs has not been initialized")
    data object InvalidRef : KmpFsError("KmpFsRef is invalid")
    data object ReadWriteOnDirectory : KmpFsError("Cannot read from or write to a directory")
    data object RefIsNotDirectory : KmpFsError("The provided ref must be a directory.")
    data object RefFsType : KmpFsError("Must use internal ref with IInternalKmpFs and external ref with IExternalKmpFs")
    data object RefNotFound : KmpFsError("Ref not found")
    data object RefNotCreated : KmpFsError("Unable to create ref")
    data object RefExistsAsFile : KmpFsError("File with the same name exists")
    data object RefExistsAsDirectory : KmpFsError("Directory with the same name exists")
    data object RefNotPicked : KmpFsError("Ref not picked or saved")
    data object NotSupported : KmpFsError("KmpFs does not support this operation on this platform")
    data object Eof : KmpFsError("End of File")
    data class Unknown(val error: Any) : KmpFsError(error.toString())
}

private val pathSeparatorChars = Path.DIRECTORY_SEPARATOR.toCharArray()

// Makes sure there is a path separator when joining a directory and file path. Some platforms (linux) may not
// include the trailing / when selecting a directory
internal fun joinPathSegments(dir: String, name: String): String =
    dir.trimEnd(pathSeparatorChars[0]) + Path.DIRECTORY_SEPARATOR + name.trimStart(pathSeparatorChars[0])
