package com.outsidesource.oskitkmp.bloc

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val defaultBlocEffectDispatcher: CoroutineDispatcher = Dispatchers.Main
