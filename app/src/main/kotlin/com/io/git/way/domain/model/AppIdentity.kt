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

package com.io.git.way.domain.model

/**
 * A detected app identity — package/applicationId (the primary, always-present signal)
 * plus, when it could be resolved, the human-readable app name (Android's
 * `android:label`, resolved through strings.xml if it's a `@string/...` reference).
 * Produced by [com.io.git.way.data.local.AppIdentityDetector] for either the local
 * project folder or a GitHub repository's file tree, so the two can be compared before
 * any diff/upload runs (PRD "Repository Project Validation & Safe Upload System").
 */
data class AppIdentity(
    val packageName: String,
    val appName: String? = null,
    val sourceFile: String
)
