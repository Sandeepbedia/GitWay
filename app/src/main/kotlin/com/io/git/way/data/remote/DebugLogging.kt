package com.io.git.way.data.remote

import okhttp3.Interceptor

// Default no-op for builds that don't provide a debug implementation.
// The debug variant can provide a real implementation in src/debug.
fun debugLoggingInterceptor(): Interceptor? = null
