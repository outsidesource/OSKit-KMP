package com.outsidesource.oskit_kmp.bloc

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val defaultBlocEffectDispatcher: CoroutineDispatcher = Dispatchers.Main