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
