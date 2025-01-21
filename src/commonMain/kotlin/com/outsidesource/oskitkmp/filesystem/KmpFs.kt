package com.outsidesource.oskitkmp.filesystem

import okio.Path

expect class KmpFsContext

/**
 * Provides limited multiplatform (iOS, Android, and Desktop) filesystem interactions for content outside of
 * application sandboxes in iOS and Android. All files/directories created are user accessible from outside the application.
 *
 * In order to access any file a user must call [pickDirectory] or [pickFile]. Use [pickDirectory] to gain permissions to a
 * root directories. The user may then take any action within that directories.
 *
 * In order to rename files, use [moveFile] command.
 *
 * [resolveFile] and [resolveDirectory] will return a file or directory reference if it exists with an optional
 * parameter to create.
 *
 * WASM:
 * Due to browser constraints, [KmpFs] on WASM only supports a subset of functionality available in other targets.
 *  * Directory existence checks will always return true
 *  * All `startingDirectory` parameters are ignored
 *  * Chrome and derivatives support all other functionality
 *  * Firefox and Safari have the following limitations:
 *      * Only file picking and reading is supported
 *      * Persisting KmpFileRefs does not work
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
 */
object KmpFs {
    val Internal: IInternalKmpFs = platformInternalKmpFs()
    val External: IExternalKmpFs = platformExternalKmpFs()

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
 * [extension] defines the file extension used i.e. "txt", "png", "jpg"
 * [mimeType] defined the mimetype used i.e. "text/plain", "image/png", "image/jpeg"
 */
data class KmpFileMimetype(
    val extension: String,
    val mimeType: String,
)

data class KmpFileMetadata(
    val size: Long,
)

sealed class KmpFsError(override val message: String) : Throwable(message) {
    data object InvalidRef : KmpFsError("KmpFsRef is invalid")
    data object RefIsDirectoryReadWriteError : KmpFsError("Cannot read/write from ref. It is a directory")
    data object NotInitialized : KmpFsError("KmpFs has not been initialized")
    data object OpenError : KmpFsError("KmpFs could not open the specified ref")
    data object CreateError : KmpFsError("KmpFs could not create the specified ref")
    data object DeleteError : KmpFsError("KmpFs could not delete the specified ref")
    data object NotFoundError : KmpFsError("KmpFs could not find the specified ref")
    data object MetadataError : KmpFsError("KmpFs could not fetch metadata for the specified ref")
    data object MoveError : KmpFsError("KmpFs could not move the specified ref")
    data object CopyError : KmpFsError("KmpFs could not copy the specified ref")
    data object RefNotPicked : KmpFsError("Ref not picked or saved")
    data object DirectoryListError : KmpFsError("KmpFs could not list directory contents for the specified directory")
    data object NotSupported : KmpFsError("KmpFs does not support this operation on this platform")
    data object Eof : KmpFsError("End of File")
    data object WriteError : KmpFsError("Write error")
    data class Unknown(val error: Any) : KmpFsError(error.toString())
}

private val pathSeparatorChars = Path.DIRECTORY_SEPARATOR.toCharArray()

// Makes sure there is a path separator when joining a directory and file path. Some platforms (linux) may not
// include the trailing / when selecting a directory
internal fun joinPathSegments(dir: String, name: String): String =
    dir.trimEnd(*pathSeparatorChars) + Path.DIRECTORY_SEPARATOR + name.trimStart(*pathSeparatorChars)
