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

/** Pulls minSdk/targetSdk/compileSdk out of a module's build.gradle(.kts) text — used
 * only to populate the Explorer's Android Project Intelligence card (PRD §16), never
 * for anything that gates uploads. Matches both the Kotlin DSL (`minSdk = 26`) and the
 * older Groovy DSL (`minSdkVersion 26`) forms. */
object AndroidProjectInspector {
    private val MIN_SDK_REGEX = Regex("""minSdk(?:Version)?\s*=?\s*(\d+)""")
    private val TARGET_SDK_REGEX = Regex("""targetSdk(?:Version)?\s*=?\s*(\d+)""")
    private val COMPILE_SDK_REGEX = Regex("""compileSdk(?:Version)?\s*=?\s*(\d+)""")

    data class SdkVersions(val minSdk: String?, val targetSdk: String?, val compileSdk: String?)

    fun inspect(gradleText: String): SdkVersions = SdkVersions(
        minSdk = MIN_SDK_REGEX.find(gradleText)?.groupValues?.get(1),
        targetSdk = TARGET_SDK_REGEX.find(gradleText)?.groupValues?.get(1),
        compileSdk = COMPILE_SDK_REGEX.find(gradleText)?.groupValues?.get(1)
    )
}
