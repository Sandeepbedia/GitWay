/*
 * GitWay — an Android client for GitHub.
 *
 * This file is part of GitWay. GitWay is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * GitWay is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GitWay. If not, see <https://www.gnu.org/licenses/>.
 */
package com.io.git.way.data.remote

import okhttp3.Interceptor

/**
 * Release build only. Deliberately returns null instead of adding an
 * HttpLoggingInterceptor — that class comes from a debugImplementation-only
 * dependency, so this variant of the file never references it at all. See
 * the real implementation in app/src/debug/.../DebugLogging.kt.
 */
internal fun debugLoggingInterceptor(): Interceptor? = null
