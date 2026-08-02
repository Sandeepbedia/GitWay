package com.io.git.way.data.remote

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

// Real debug-only implementation that depends on the logging interceptor
// library. This file is compiled only into the debug variant (src/debug).
fun debugLoggingInterceptor(): Interceptor? {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    return logging
}
