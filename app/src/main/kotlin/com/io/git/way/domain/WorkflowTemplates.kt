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

package com.io.git.way.domain

/** One selectable GitHub Actions workflow the user can choose to add to a repository
 * that doesn't have any CI configured yet — shown with its full YAML so the user can
 * read exactly what it does before adding it (PRD: "CI Workflow Suggestions", user
 * manually opts in per template, nothing is ever added automatically). */
data class WorkflowTemplate(
    val id: String,
    val title: String,
    val description: String,
    /** Path inside the repo this template is written to, e.g. ".github/workflows/android-ci.yml". */
    val path: String,
    val yaml: String
)

object WorkflowTemplates {

    val androidCi = WorkflowTemplate(
        id = "android-ci",
        title = "Android CI (build & test)",
        description = "Runs a debug build and unit tests on every push and pull request to main.",
        path = ".github/workflows/android-ci.yml",
        yaml = """
            name: Android CI

            on:
              push:
                branches: [ "main" ]
              pull_request:
                branches: [ "main" ]

            jobs:
              build:
                runs-on: ubuntu-latest

                steps:
                  - uses: actions/checkout@v4

                  - name: Set up JDK 17
                    uses: actions/setup-java@v4
                    with:
                      java-version: '17'
                      distribution: 'temurin'

                  - name: Grant execute permission for gradlew
                    run: chmod +x gradlew

                  - name: Build with Gradle
                    run: ./gradlew assembleDebug --stacktrace

                  - name: Run unit tests
                    run: ./gradlew testDebugUnitTest --stacktrace
        """.trimIndent()
    )

    val gradleWrapperValidation = WorkflowTemplate(
        id = "gradle-wrapper-validation",
        title = "Gradle Wrapper Validation",
        description = "Verifies gradlew's checksum on every push, so a tampered wrapper script " +
            "gets caught before it can run in CI or on a contributor's machine.",
        path = ".github/workflows/gradle-wrapper-validation.yml",
        yaml = """
            name: Validate Gradle Wrapper

            on: [push, pull_request]

            jobs:
              validation:
                runs-on: ubuntu-latest
                steps:
                  - uses: actions/checkout@v4
                  - uses: gradle/actions/wrapper-validation@v4
        """.trimIndent()
    )

    val all = listOf(androidCi, gradleWrapperValidation)

    /** True if the repo tree (any path -> sha map from [com.io.git.way.domain.repository.GitHubRepository.getRepositoryTree])
     * has no workflow files at all under `.github/workflows/`. */
    fun hasNoWorkflows(remotePaths: Set<String>): Boolean =
        remotePaths.none { it.startsWith(".github/workflows/") }
}
