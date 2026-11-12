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

object KeystoreSanitizer {

    private val SANITIZABLE_NAMES = setOf("key.properties", "secrets.properties", "credentials.json", "config.secret")

    private val SENSITIVE_LINE_REGEX = Regex(
        "(?im)^([ \\t]*)([\\w.]*(?:password|secret|token|apikey|api_key)[\\w.]*)([ \\t]*[:=][ \\t]*)(.+)$"
    )

    fun isSanitizable(fileName: String): Boolean =
        fileName in SANITIZABLE_NAMES || fileName == ".env" || fileName.startsWith(".env.")

    fun sanitize(content: String): String? {
        var changed = false
        val result = SENSITIVE_LINE_REGEX.replace(content) { match ->
            changed = true
            val indent = match.groupValues[1]
            val key = match.groupValues[2]
            val sep = match.groupValues[3]
            val placeholder = "YOUR_" + key.uppercase().replace(Regex("[^A-Z0-9]"), "_").trim('_') + "_HERE"
            "$indent$key$sep$placeholder"
        }
        return if (changed) result else null
    }
}
