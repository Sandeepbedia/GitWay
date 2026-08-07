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

package com.io.git.way.data.local

import android.content.Context

/**
 * Persists exact file paths the person has manually marked "don't track this for
 * removal" from the Removed section — the escape hatch for any repo-decoration file
 * [com.io.git.way.domain.RepositoryScaffoldFiles]'s built-in heuristic doesn't already
 * catch. Plain (unencrypted) SharedPreferences: these are just file paths, not secrets.
 */
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
