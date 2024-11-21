package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome

class WASMKMPStorage(private val appName: String) : IKMPStorage {
    override fun openNode(nodeName: String): Outcome<IKMPStorageNode, Exception> = try {
        Outcome.Error(Exception("Not Supported"))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
