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
package com.io.git.way.domain.model

/**
 * Android Project Intelligence (PRD "Repository Explorer" §16): what Git Way could
 * detect about the repository from its build files, shown as a summary card at the
 * root of the Explorer. Every field besides [packageName] is best-effort — a project
 * using version catalogs or a non-standard build setup may leave some of them null.
 */
data class AndroidProjectInfo(
    val packageName: String,
    val minSdk: String? = null,
    val targetSdk: String? = null,
    val compileSdk: String? = null,
    val language: String = "Kotlin",
    val buildSystem: String = "Gradle"
)
