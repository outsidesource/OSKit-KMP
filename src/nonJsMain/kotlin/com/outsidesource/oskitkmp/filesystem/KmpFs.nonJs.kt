package com.outsidesource.oskitkmp.filesystem

actual suspend fun onKmpFileRefPersisted(ref: KmpFsRef) {}
actual suspend fun internalClearPersistedDataCache(ref: KmpFsRef?) {}
