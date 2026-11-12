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
package com.io.git.way.data.local

import android.content.Context

class IgnoreListManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    fun getAll(): Set<String> = prefs.getStringSet(KEY_PATHS, emptySet()).orEmpty()

    fun add(path: String) {
        prefs.edit().putStringSet(KEY_PATHS, getAll() + path).apply()
    }

    fun remove(path: String) {
        prefs.edit().putStringSet(KEY_PATHS, getAll() - path).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_PATHS).apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "gitway_ignore_list"
        const val KEY_PATHS = "ignored_paths"
    }
}
