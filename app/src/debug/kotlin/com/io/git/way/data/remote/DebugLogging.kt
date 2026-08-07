/*
 * Git Way
 * Copyright (C) 2026 Sandeep Bedia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.io.git.way.data.remote

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Debug build only. Backed by `debugImplementation(libs.squareup.okhttp.logging.interceptor)`
 * in app/build.gradle.kts, so this file — and the whole logging-interceptor
 * artifact — never gets compiled into or shipped in release; see the matching
 * no-op in app/src/release/.../DebugLogging.kt (same package + function
 * signature, resolved per-variant by Gradle's source sets).
 */
internal fun debugLoggingInterceptor(): Interceptor =
    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
