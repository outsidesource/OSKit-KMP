package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.ObjCJna.cls
import com.outsidesource.oskitkmp.lib.ObjCJna.invokeLong
import com.outsidesource.oskitkmp.lib.ObjCJna.invokePtr
import com.outsidesource.oskitkmp.lib.ObjCJna.invokeVoid
import com.outsidesource.oskitkmp.lib.ObjCJna.nsStringArrayFromUtf8Array
import com.outsidesource.oskitkmp.lib.ObjCJna.nsStringFromUtf8
import com.outsidesource.oskitkmp.lib.ObjCJna.nsStringToUtf8
import com.outsidesource.oskitkmp.lib.ObjCJna.nsUrlFileURLFromPath
import com.outsidesource.oskitkmp.lib.ObjCJna.runOnMainThread
import com.outsidesource.oskitkmp.lib.ObjCJna.sel
import com.outsidesource.oskitkmp.lib.ObjCJna.withAutoReleasePool
import com.outsidesource.oskitkmp.outcome.Outcome
import com.sun.jna.Pointer

object MacOsFilePicker : IKmpFsFilePicker {
    private val NSOpenPanel = cls("NSOpenPanel")
    private val NSSavePanel = cls("NSSavePanel")

    private val URLs = sel("URLs")
    private val URL = sel("URL")
    private val openPanel = sel("openPanel")
    private val savePanel = sel("savePanel")
    private val runModal = sel("runModal")
    private val setAllowsMultipleSelection = sel("setAllowsMultipleSelection:")
    private val setCanChooseDirectories = sel("setCanChooseDirectories:")
    private val setCanChooseFiles = sel("setCanChooseFiles:")
    private val setAllowedFileTypes = sel("setAllowedFileTypes:")
    private val setDirectoryURL = sel("setDirectoryURL:")
    private val count = sel("count")
    private val objectAtIndex = sel("objectAtIndex:")
    private val path = sel("path")
    private val setNameFieldStringValue = sel("setNameFieldStringValue:")

    override suspend fun pickFile(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, KmpFsError> = withAutoReleasePool {
        runOnMainThread {
            val panel = NSOpenPanel.invokePtr(openPanel)!!
            panel.invokeVoid(setAllowsMultipleSelection, 0)
            panel.invokeVoid(setCanChooseFiles, 1)
            setFilters(panel, filter)
            setStartDir(panel, startingDir)

            val result = panel.invokeLong(runModal)
            val ref = if (result != 1L) null else collectURLs(panel, isDirectory = false).firstOrNull()
            Outcome.Ok(ref)
        }
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> = withAutoReleasePool {
        runOnMainThread {
            val panel = NSOpenPanel.invokePtr(openPanel)!!
            panel.invokeVoid(setAllowsMultipleSelection, 1)
            panel.invokeVoid(setCanChooseFiles, 1)
            setFilters(panel, filter)
            setStartDir(panel, startingDir)

            val result = panel.invokeLong(runModal)
            val refs = if (result != 1L) null else collectURLs(panel, isDirectory = false)
            Outcome.Ok(refs)
        }
    }

    override suspend fun pickDirectory(
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> = withAutoReleasePool {
        runOnMainThread {
            val panel = NSOpenPanel.invokePtr(openPanel)!!
            panel.invokeVoid(setAllowsMultipleSelection, 0)
            panel.invokeVoid(setCanChooseDirectories, 1)
            panel.invokeVoid(setCanChooseFiles, 0)
            setStartDir(panel, startingDir)

            val ref =
                if (panel.invokeLong(runModal) != 1L) null else collectURLs(panel, isDirectory = true).firstOrNull()
            Outcome.Ok(ref)
        }
    }

    override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> = withAutoReleasePool {
        runOnMainThread {
            val panel = NSSavePanel.invokePtr(savePanel)!!
            panel.invokeVoid(setNameFieldStringValue, nsStringFromUtf8(fileName))

            val ref = if (panel.invokeLong(runModal) != 1L) null else collectSingleURL(panel, isDirectory = false)
            Outcome.Ok(ref)
        }
    }

    private fun setFilters(panel: Pointer, filters: KmpFileFilter?) {
        val normalized = filters?.map { it.extension } ?: emptyList()
        if (normalized.isNotEmpty()) panel.invokeVoid(setAllowedFileTypes, nsStringArrayFromUtf8Array(normalized))
    }

    private fun setStartDir(panel: Pointer, start: KmpFsRef?) {
        if (start == null) return
        panel.invokeVoid(setDirectoryURL, nsUrlFileURLFromPath(start.ref))
    }

    private fun collectSingleURL(panel: Pointer, isDirectory: Boolean): KmpFsRef? {
        val url = panel.invokePtr(URL) ?: return null
        return nsUrlToKmpFsRef(url, isDirectory)
    }

    private fun collectURLs(panel: Pointer, isDirectory: Boolean): List<KmpFsRef> {
        val urls = panel.invokePtr(URLs)!!
        val count = urls.invokeLong(count).toInt()

        return (0 until count).mapNotNull { i ->
            val url = urls.invokePtr(objectAtIndex, i) ?: return@mapNotNull null
            nsUrlToKmpFsRef(url, isDirectory)
        }
    }

    private fun nsUrlToKmpFsRef(nsUrl: Pointer, isDirectory: Boolean): KmpFsRef? {
        val nsPath = nsUrl.invokePtr(path) ?: return null
        val path = nsStringToUtf8(nsPath)

        return KmpFsRef(
            ref = path,
            isDirectory = isDirectory,
            name = path.split("/").lastOrNull() ?: "",
            fsType = KmpFsType.External,
        )
    }
}
