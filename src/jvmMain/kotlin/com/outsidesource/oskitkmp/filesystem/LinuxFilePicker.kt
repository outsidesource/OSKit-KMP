package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.outcome.Outcome

object LinuxFilePicker : IKmpFsFilePicker {
    override suspend fun pickFile(startingDir: KmpFsRef?, filter: KmpFileFilter?): Outcome<KmpFsRef?, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun pickSaveFile(fileName: String, startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        TODO("Not yet implemented")
    }
}