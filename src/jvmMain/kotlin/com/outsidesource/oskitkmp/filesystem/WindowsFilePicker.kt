package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.filesystem.ShTypes.FILEOPENDIALOGOPTIONS.Companion.FOS_ALLOWMULTISELECT
import com.outsidesource.oskitkmp.filesystem.ShTypes.FILEOPENDIALOGOPTIONS.Companion.FOS_PICKFOLDERS
import com.outsidesource.oskitkmp.filesystem.ShTypes.SIGDN.Companion.SIGDN_DESKTOPABSOLUTEPARSING
import com.outsidesource.oskitkmp.filesystem.ShTypes.SIGDN.Companion.SIGDN_FILESYSPATH
import com.outsidesource.oskitkmp.outcome.Outcome
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.COM.COMUtils
import com.sun.jna.platform.win32.COM.COMUtils.FAILED
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.Ole32.COINIT_APARTMENTTHREADED
import com.sun.jna.platform.win32.WinError.*
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.W32APIOptions
import java.awt.Window

/**
 * Windows File Picker adapted from https://github.com/vinceglb/FileKit
 */
class WindowsFilePicker(
    private val context: () -> KmpFsContext?,
) : IKmpFsFilePicker {

    override suspend fun pickFile(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, KmpFsError> = withDialog(PickerType.File) { dialog ->
        dialog.setStartingDirectory(startingDir)
        dialog.setFilters(filter)
        val refs = dialog.show(context()?.window) { it.getResult(SIGDN_FILESYSPATH, isDirectory = false) }
        return Outcome.Ok(refs)
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> = withDialog(PickerType.File) { dialog ->
        dialog.setStartingDirectory(startingDir)
        dialog.setFilters(filter)
        dialog.setFlag(FOS_ALLOWMULTISELECT)
        val refs = dialog.show(context()?.window) { it.getResults() }
        Outcome.Ok(refs)
    }

    override suspend fun pickDirectory(
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> = withDialog(PickerType.File) { dialog ->
        dialog.setStartingDirectory(startingDir)
        dialog.setFlag(FOS_PICKFOLDERS)
        val refs = dialog.show(context()?.window) { it.getResult(SIGDN_DESKTOPABSOLUTEPARSING, isDirectory = true) }
        return Outcome.Ok(refs)
    }

    override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> = withDialog(PickerType.Save) { dialog ->
        startingDir?.let { dialog.setStartingDirectory(it) }
        dialog.SetFileName(WString(fileName))
        val extension = fileName.split(".").lastOrNull()
        extension?.let { dialog.SetDefaultExtension(WString(it)) }
        extension?.let { dialog.setFilters(listOf(KmpFileMimetype(extension = extension, mimeType = ""))) }
        val refs = dialog.show(context()?.window) { it.getResult(SIGDN_FILESYSPATH, isDirectory = false) }
        return Outcome.Ok(refs)
    }

    private inline fun <reified T : FileDialog, R> withDialog(type: PickerType<T>, block: (T) -> R): R {
        var dialog: T? = null

        return try {
            Ole32.INSTANCE.CoInitializeEx(null, COINIT_APARTMENTTHREADED or Ole32.COINIT_DISABLE_OLE1DDE)
            val isInit = COMUtils.comIsInitialized()
            if (!isInit) throw RuntimeException("COM init failed")

            val dialogPointer = PointerByReference()
            dialog = Ole32.INSTANCE.CoCreateInstance(type.clsid, null, WTypes.CLSCTX_ALL, type.iid, dialogPointer)
                .let { type.build(dialogPointer.value) }

            block(dialog)
        } finally {
            dialog?.Release()
            Ole32.INSTANCE.CoUninitialize()
        }
    }

    private fun FileDialog.setStartingDirectory(startingDir: KmpFsRef?) {
        if (startingDir == null) return
        val pbrFolder = PointerByReference()
        val resultFolder = Shell32.INSTANCE.SHCreateItemFromParsingName(
            WString(startingDir.ref),
            null,
            Guid.REFIID(ShellItem.IID_ISHELLITEM),
            pbrFolder,
        )

        val fileNotFoundException = Win32Exception(ERROR_FILE_NOT_FOUND)
        if (resultFolder == fileNotFoundException.hr) return

        val invalidDriveException = Win32Exception(ERROR_INVALID_DRIVE)
        if (resultFolder == invalidDriveException.hr) return
        if (FAILED(resultFolder)) throw RuntimeException("SHCreateItemFromParsingName failed")

        val folder = ShellItem(pbrFolder.value)
        this.SetFolder(folder.pointer)

        folder.Release()
    }

    private fun FileDialog.setFilters(filter: KmpFileFilter?) {
        if (filter == null) return
        val filterString = filter.joinToString(";") { "*.${it.extension}" }

        val filterSpec = ShTypes.COMDLG_FILTERSPEC()
        filterSpec.pszName = WString("Files ($filterString)")
        filterSpec.pszSpec = WString(filterString)

        SetFileTypes(1, arrayOf(filterSpec))
    }

    private fun FileDialog.setFlag(flag: Int) {
        val ref = IntByReference()
        this.GetOptions(ref)

        this.SetOptions(ref.value or flag)
    }

    private fun <FD : FileDialog, T> FD.show(
        parentWindow: Window?,
        block: (FD) -> T,
    ): T? {
        val openDialogResult = this.Show(parentWindow.toHwnd())
        val userCanceledException = Win32Exception(ERROR_CANCELLED)
        if (openDialogResult == userCanceledException.hr) return null
        if (FAILED(openDialogResult)) throw RuntimeException("Show failed")

        return block(this)
    }

    private fun FileDialog.getResult(sigdnName: Long, isDirectory: Boolean): KmpFsRef {
        var item: ShellItem? = null
        var pbrDisplayName: PointerByReference? = null

        try {
            val pbrItem = PointerByReference()
            this.GetResult(pbrItem)

            item = ShellItem(pbrItem.value)

            pbrDisplayName = PointerByReference()
            item.GetDisplayName(sigdnName, pbrDisplayName)

            val path = pbrDisplayName.value.getWideString(0)
            return KmpFsRef(
                ref = path,
                name = path.split("\\").lastOrNull() ?: "",
                isDirectory = isDirectory,
                fsType = KmpFsType.External,
            )
        } finally {
            pbrDisplayName?.let { Ole32.INSTANCE.CoTaskMemFree(it.value) }
            item?.Release()
        }
    }

    private fun FileOpenDialog.getResults(): List<KmpFsRef> {
        var itemArray: ShellItemArray? = null

        try {
            val pbrItemArray = PointerByReference()
            this.GetResults(pbrItemArray)

            itemArray = ShellItemArray(pbrItemArray.value)

            val countRef = IntByReference()
            itemArray.GetCount(countRef)

            val refs = mutableListOf<KmpFsRef>()
            for (i in 0 until countRef.value) {
                val pbrItem = PointerByReference()
                itemArray.GetItemAt(i, pbrItem)

                val item = ShellItem(pbrItem.value)

                val pbrDisplayName = PointerByReference()
                item.GetDisplayName(SIGDN_FILESYSPATH, pbrDisplayName)

                val path = pbrDisplayName.value.getWideString(0)
                val ref = KmpFsRef(
                    ref = path,
                    name = path.split("\\").lastOrNull() ?: "",
                    isDirectory = false,
                    fsType = KmpFsType.External,
                )
                refs.add(ref)

                pbrDisplayName.let { Ole32.INSTANCE.CoTaskMemFree(it.value) }
                item.Release()
            }

            return refs
        } finally {
            itemArray?.Release()
        }
    }

    private fun Window?.toHwnd(): WinDef.HWND? {
        return when (this) {
            null -> null
            else -> Native.getWindowPointer(this).let { WinDef.HWND(it) }
        }
    }
}

private sealed class PickerType<T : FileDialog>(
    val clsid: GuidFixed.CLSID,
    val iid: GuidFixed.IID,
    val build: (pointer: Pointer) -> T,
) {
    data object File : PickerType<FileOpenDialog>(
        iid = GuidFixed.IID("{d57c7288-d4ad-4768-be02-9d969532d960}"),
        clsid = GuidFixed.CLSID("{DC1C5A9C-E88A-4dde-A5A1-60F82A20AEF7}"),
        build = { FileOpenDialog(it) },
    )
    data object Save : PickerType<FileSaveDialog>(
        iid = GuidFixed.IID("{84bccd23-5fde-4cdb-aea4-af64b83d78ab}"),
        clsid = GuidFixed.CLSID("{C0B4E2F3-BA21-4773-8DBA-335EC946EB8B}"),
        build = { FileSaveDialog(it) },
    )
}

// https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nn-shobjidl_core-ifileopendialog
private class FileOpenDialog(pointer: Pointer?) : FileDialog(pointer) {
    fun GetResults(ppenum: PointerByReference?): WinNT.HRESULT =
        _invokeNativeObject(27, arrayOf(pointer, ppenum), WinNT.HRESULT::class.java) as WinNT.HRESULT
}

// https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nn-shobjidl_core-ifilesavedialog
private class FileSaveDialog(pointer: Pointer?) : FileDialog(pointer)

// https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nn-shobjidl_core-ifiledialog
private open class FileDialog(pointer: Pointer?) : ModalWindow(pointer) {
    fun SetFileTypes(FileTypes: Int, rgFilterSpec: Array<ShTypes.COMDLG_FILTERSPEC?>?): WinNT.HRESULT =
        _invokeNativeObject(4, arrayOf(pointer, FileTypes, rgFilterSpec), WinNT.HRESULT::class.java) as WinNT.HRESULT

    fun SetOptions(fos: Int): WinNT.HRESULT =
        _invokeNativeObject(9, arrayOf(this.pointer, fos), WinNT.HRESULT::class.java) as WinNT.HRESULT

    fun GetOptions(pfos: IntByReference?): WinNT.HRESULT =
        _invokeNativeObject(10, arrayOf(this.pointer, pfos), WinNT.HRESULT::class.java) as WinNT.HRESULT

    fun SetFolder(psi: Pointer?): WinNT.HRESULT =
        _invokeNativeObject(12, arrayOf<Any?>(this.pointer, psi), WinNT.HRESULT::class.java) as WinNT.HRESULT

    fun SetFileName(pszName: WString?): WinNT.HRESULT =
        _invokeNativeObject(15, arrayOf(this.pointer, pszName), WinNT.HRESULT::class.java) as WinNT.HRESULT

    fun GetResult(ppsi: PointerByReference?): WinNT.HRESULT =
        _invokeNativeObject(20, arrayOf(this.pointer, ppsi), WinNT.HRESULT::class.java) as WinNT.HRESULT

    fun SetDefaultExtension(pszDefaultExtension: WString?): WinNT.HRESULT =
        _invokeNativeObject(22, arrayOf(this.pointer, pszDefaultExtension), WinNT.HRESULT::class.java) as WinNT.HRESULT
}

// https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nn-shobjidl_core-imodalwindow
private open class ModalWindow(pointer: Pointer?) : Unknown(pointer) {
    fun Show(hwndOwner: WinDef.HWND?): WinNT.HRESULT =
        _invokeNativeObject(3, arrayOf(pointer, hwndOwner), WinNT.HRESULT::class.java) as WinNT.HRESULT
}

private interface Shell32 : com.sun.jna.platform.win32.Shell32 {
    fun SHCreateItemFromParsingName(
        pszPath: WString?,
        pbc: Pointer?,
        riid: Guid.REFIID?,
        ppv: PointerByReference?,
    ): WinNT.HRESULT

    companion object {
        val INSTANCE: Shell32 = Native.load("shell32", Shell32::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }
}

private interface ShTypes {
    @Structure.FieldOrder("pszName", "pszSpec")
    class COMDLG_FILTERSPEC : Structure() {
        @JvmField
        var pszName: WString? = null

        @JvmField
        var pszSpec: WString? = null

        override fun getFieldOrder(): List<String> = listOf("pszName", "pszSpec")
    }

    interface FILEOPENDIALOGOPTIONS {
        companion object {
            const val FOS_PICKFOLDERS: Int = 0x20
            const val FOS_ALLOWMULTISELECT: Int = 0x200
        }
    }

    interface SIGDN {
        companion object {
            var SIGDN_DESKTOPABSOLUTEPARSING: Long = 0x80028000
            var SIGDN_FILESYSPATH: Long = 0x80058000
        }
    }
}

private class ShellItemArray(pointer: Pointer?) : Unknown(pointer) {
    fun GetCount(pdwNumItems: IntByReference?): WinNT.HRESULT =
        _invokeNativeObject(7, arrayOf(this.pointer, pdwNumItems), WinNT.HRESULT::class.java) as WinNT.HRESULT

    fun GetItemAt(dwIndex: Int, ppsi: PointerByReference?): WinNT.HRESULT =
        _invokeNativeObject(8, arrayOf(this.pointer, dwIndex, ppsi), WinNT.HRESULT::class.java) as WinNT.HRESULT
}

private class ShellItem(pointer: Pointer?) : Unknown(pointer) {

    companion object {
        val IID_ISHELLITEM: GuidFixed.IID = GuidFixed.IID("{43826d1e-e718-42ee-bc55-a1e261c37bfe}") // Guid.IID
    }

    fun GetDisplayName(
        sigdnName: Long,
        ppszName: PointerByReference?,
    ): WinNT.HRESULT {
        return _invokeNativeObject(
            5,
            arrayOf(this.pointer, sigdnName, ppszName),
            WinNT.HRESULT::class.java,
        ) as WinNT.HRESULT
    }
}

/**
 * Using Guid.CLSID or Guid.IID when using proguard and obfuscate crashes the application.
 *
 * This is due to annotation @FieldOrder not being applied to the fields in the class.
 *
 * This is a workaround to use the fixed CLSID and IID.
 * Declaring getFieldOrder() in the class works as expected.
 */
private object GuidFixed {
    class CLSID(guid: String) : Guid.CLSID(guid) {
        override fun getFieldOrder(): List<String> = listOf("Data1", "Data2", "Data3", "Data4")
    }

    class IID(iid: String) : Guid.IID(iid) {
        override fun getFieldOrder(): List<String> = listOf("Data1", "Data2", "Data3", "Data4")
    }
}
