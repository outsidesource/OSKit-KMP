package com.outsidesource.oskitkmp.bloc

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val defaultBlocDispatcher: CoroutineDispatcher = Dispatchers.Main
