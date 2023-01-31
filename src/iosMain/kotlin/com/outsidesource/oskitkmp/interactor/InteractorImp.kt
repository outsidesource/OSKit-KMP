package com.outsidesource.oskitkmp.interactor

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val defaultInteractorDispatcher: CoroutineDispatcher = Dispatchers.Main
