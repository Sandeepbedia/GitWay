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
package com.io.git.way.ui.common

import androidx.annotation.RawRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.io.git.way.R
import com.io.git.way.ui.theme.GlassBlobBlue
import com.io.git.way.ui.theme.GlassBlobPink
import com.io.git.way.ui.theme.GlassBlobPurple
import com.io.git.way.ui.theme.GlassBlobTeal

/**
 * File-manager style extension -> icon/colour lookup.
 *
 * The dedicated drawings in `res/raw` — one per language/tool, the same set a
 * desktop IDE's project tree uses — are ported wholesale from the PhoneIDE/LSP
 * file-tree icon pack, so Git Way's Project Explorer (PRD "Repository Explorer")
 * and the local Folder Selection screen both recognise the same ~180 file types
 * instead of falling back to a handful of tinted Material glyphs. Lookup is by
 * exact filename first (`Dockerfile`, `.gitignore`, `gradlew`) and only then by
 * extension, since the interesting files at a project root are mostly the ones
 * with no extension at all.
 */
object FileTypeIcons {

    private val codeExtensions = setOf(
        "kt", "kts", "java", "py", "js", "ts", "tsx", "jsx", "c", "cpp", "h", "hpp",
        "cs", "go", "rs", "rb", "php", "swift", "sh", "gradle", "dart", "lua"
    )
    private val dataExtensions = setOf("json", "xml", "yaml", "yml", "toml", "csv", "sql", "proto")

    /** Every raster/vector image format Git Way recognises for icon/colour, the "Images"
     * type filter, and the file-metadata label — one list so those three never drift out
     * of sync with each other again. */
    private val imageExtensions = setOf(
        // Common raster formats
        "png", "jpg", "jpeg", "jpe", "jfif", "webp", "bmp", "dib",
        // Vector
        "svg", "svgz",
        // Apple / HEIF family
        "heic", "heif",
        // Next-gen web formats
        "avif",
        // TIFF
        "tif", "tiff",
        // Icons/cursors
        "ico", "icns", "cur",
        // Legacy/rare bitmap formats still seen in older Android/asset repos
        "pbm", "pgm", "ppm", "pnm", "xbm", "xpm", "tga",
        // RAW camera formats (occasionally checked into design/asset folders)
        "raw", "cr2", "nef", "arw", "dng", "orf", "rw2"
    )
    private val gifExtensions = setOf("gif")
    private val videoExtensions = setOf("mp4", "mov", "mkv", "avi", "webm", "3gp", "m4v")
    private val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "opus")
    private val docExtensions = setOf("md", "txt", "rst")
    private val pdfExtensions = setOf("pdf")
    private val configExtensions = setOf("properties", "cfg", "ini", "conf", "env", "lock", "pro")

    /** Single source of truth for "is this file an image" — used by the icon/colour/label
     * lookups below AND by [com.io.git.way.ui.common.GitWaySessionViewModel]'s Images type
     * filter, so a format recognised here is never missed by the filter (or vice versa). */
    fun isImage(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in imageExtensions || ext in gifExtensions
    }

    fun iconFor(fileName: String): ImageVector = when (fileName.substringAfterLast('.', "").lowercase()) {
        in codeExtensions -> Icons.Filled.Code
        in dataExtensions -> Icons.Filled.DataObject
        in imageExtensions -> Icons.Filled.Image
        in gifExtensions -> Icons.Filled.Gif
        in videoExtensions -> Icons.Filled.MovieFilter
        in audioExtensions -> Icons.Filled.MusicNote
        in docExtensions -> Icons.Filled.TextSnippet
        in pdfExtensions -> Icons.Filled.PictureAsPdf
        in configExtensions -> Icons.Filled.Settings
        "" -> Icons.Filled.InsertDriveFile
        else -> Icons.Filled.Description
    }

    fun colorFor(fileName: String): Color = when (fileName.substringAfterLast('.', "").lowercase()) {
        in codeExtensions -> GlassBlobBlue
        in dataExtensions -> GlassBlobPurple
        in imageExtensions, in gifExtensions -> GlassBlobPink
        in videoExtensions -> GlassBlobPink
        in audioExtensions -> GlassBlobTeal
        in pdfExtensions -> GlassBlobPink
        else -> GlassBlobTeal
    }

    /** Human readable "12.4 KB" / "3.2 MB" style size, for the file-manager list. */
    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.1f GB", gb)
    }

    /** Human "AndroidManifest.xml -> XML Document" style label (PRD §6/§7). */
    fun typeLabel(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower == ".gitignore" -> "Git Ignore File"
            lower == ".gitattributes" -> "Git Attributes File"
            lower.endsWith(".gradle.kts") -> "Kotlin Script"
            lower.endsWith(".gradle") -> "Gradle Script"
            lower.endsWith(".pro") -> "ProGuard Rules"
            lower.endsWith(".kt") -> "Kotlin File"
            lower.endsWith(".kts") -> "Kotlin Script"
            lower.endsWith(".java") -> "Java File"
            lower.endsWith(".xml") -> "XML Document"
            lower.endsWith(".json") -> "JSON Document"
            lower.endsWith(".md") || lower.endsWith(".markdown") -> "Markdown Document"
            lower.endsWith(".yml") || lower.endsWith(".yaml") -> "YAML Document"
            lower.endsWith(".properties") -> "Properties File"
            lower.endsWith(".toml") -> "TOML Document"
            lower.endsWith(".zip") || lower.endsWith(".jar") || lower.endsWith(".aar") -> "Archive"
            lower.endsWith(".pdf") -> "PDF Document"
            fileName.substringAfterLast('.', "") in imageExtensions -> "Image"
            fileName.substringAfterLast('.', "") in gifExtensions -> "GIF Image"
            fileName.substringAfterLast('.', "") in videoExtensions -> "Video"
            fileName.substringAfterLast('.', "") in audioExtensions -> "Audio"
            fileName.substringAfterLast('.', "").isEmpty() -> "File"
            else -> "${fileName.substringAfterLast('.').uppercase()} File"
        }
    }

    /** Short badge text ("XML", "KTS", "PRO") shown next to a file row. */
    fun badgeFor(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".gradle.kts") -> "KTS"
            lower == ".gitignore" -> "GIT"
            else -> fileName.substringAfterLast('.', "").uppercase().take(5)
        }
    }

    /**
     * Wired custom SVG icon (res/raw) for [fileName], if a dedicated drawing exists —
     * the full PhoneIDE/LSP icon pack (Kotlin, Java, Gradle, every mainstream language
     * and web/tooling config, LICENSE, Docker, lockfiles, dotfiles, …). Returns null
     * when nothing was drawn specifically for that file, so callers fall back to the
     * generic Material icon from [iconFor]/[colorFor].
     */
    @RawRes
    fun iconResFor(fileName: String): Int? {
        val lower = fileName.lowercase()
        byName[lower]?.let { return it }

        // Compound and prefixed names have to be tested before the last-dot
        // extension: `foo.gradle.kts` is a Gradle script, not a plain Kotlin
        // script, `Card.blade.php` is a Laravel view rather than plain PHP, and
        // `.env.local` is still an env file.
        when {
            lower.endsWith(".gradle.kts") -> return R.raw.gradle
            lower.endsWith(".blade.php") -> return R.raw.laravel
            lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> return R.raw.gz
            lower.startsWith(".env.") -> return R.raw.env
            // tsconfig.app.json, tsconfig.node.json, tsconfig.build.json …
            lower.startsWith("tsconfig.") && lower.endsWith(".json") -> return R.raw.tsconfig
            lower.startsWith("docker-compose") || lower.startsWith("compose.") -> return R.raw.dockerfile
        }

        byExtension[lower.substringAfterLast('.', "")]?.let { return it }

        // Anything still unmatched that begins with a dot is some tool's config,
        // so it gets the generic dotfile drawing rather than no icon at all. A
        // project root is mostly dotfiles, and "a config file I don't know" is
        // more information than "a file".
        return if (lower.startsWith(".")) R.raw.dotfile else null
    }

    /** Directory icon: a distinct drawing for the repository root, and open vs
     * closed for everything else, so nesting is readable at a glance. */
    @RawRes
    fun folderRes(isRoot: Boolean, isExpanded: Boolean): Int = when {
        isRoot -> R.raw.folder_root
        isExpanded -> R.raw.folder_open
        else -> R.raw.folder_closed
    }

    // ── lookup tables (ported from the PhoneIDE/LSP file-tree icon pack) ──────

    /** Exact filenames — everything a real project actually contains at its root,
     * where a plain extension lookup is useless (`gradlew`, `Makefile`, `LICENSE`,
     * `Dockerfile`, `.gitignore`). */
    private val byName: Map<String, Int> = buildMap {
        // Android / Gradle
        put("gradlew", R.raw.gradlew)
        put("gradlew.bat", R.raw.gradlew)
        put("gradle.properties", R.raw.properties)
        put("local.properties", R.raw.properties)
        put("settings.gradle", R.raw.gradle)
        put("build.gradle", R.raw.gradle)
        put("gradle-wrapper.properties", R.raw.gradle)
        put("proguard-rules.pro", R.raw.pro)
        put("androidmanifest.xml", R.raw.android)
        put("cmakelists.txt", R.raw.cmake)
        put("makefile", R.raw.makefile)
        put("pom.xml", R.raw.maven)
        put("mvnw", R.raw.maven)
        put("mvnw.cmd", R.raw.maven)
        // Docker
        put("dockerfile", R.raw.dockerfile)
        put(".dockerignore", R.raw.dockerfile)
        // Flutter / Dart
        put("pubspec.yaml", R.raw.flutter)
        put("pubspec.yml", R.raw.flutter)
        put("pubspec.lock", R.raw.lock)
        // Lockfiles that carry their tool's mark instead of a padlock
        put("package.json", R.raw.npm)
        put("package-lock.json", R.raw.npm)
        put("yarn.lock", R.raw.yarn)
        put("bun.lockb", R.raw.bun)
        put("bunfig.toml", R.raw.bun)
        put("deno.json", R.raw.deno)
        put("deno.jsonc", R.raw.deno)
        put("deno.lock", R.raw.deno)
        put(".nvmrc", R.raw.nodejs)
        put(".node-version", R.raw.nodejs)
        put("gradle.lockfile", R.raw.lock)
        put("cargo.lock", R.raw.lock)
        put("cargo.toml", R.raw.rs)
        put("go.mod", R.raw.go)
        put("go.sum", R.raw.go)
        put("composer.json", R.raw.composer)
        put("composer.lock", R.raw.composer)
        // Git
        put(".gitignore", R.raw.git)
        put(".gitattributes", R.raw.git)
        put(".gitmodules", R.raw.git)
        put(".gitconfig", R.raw.git)
        put(".gitkeep", R.raw.git)
        put(".gitlab-ci.yml", R.raw.gitlab)
        put("jenkinsfile", R.raw.jenkins)
        // JS/TS tooling
        put("tsconfig.json", R.raw.tsconfig)
        put("jsconfig.json", R.raw.jsconfig)
        put(".npmignore", R.raw.npm)
        put(".npmrc", R.raw.npm)
        put(".yarnrc", R.raw.yarn)
        put(".yarnrc.yml", R.raw.yarn)
        put(".eslintrc", R.raw.eslintrc)
        put(".eslintrc.js", R.raw.eslintrc)
        put(".eslintrc.json", R.raw.eslintrc)
        put("eslint.config.js", R.raw.eslintrc)
        put(".prettierrc", R.raw.prettierrc)
        put(".prettierrc.json", R.raw.prettierrc)
        put(".babelrc", R.raw.babel)
        put(".babelrc.json", R.raw.babel)
        put("angular.json", R.raw.angular)
        put("nest-cli.json", R.raw.nestjs)
        put("firebase.json", R.raw.firebase)
        put(".firebaserc", R.raw.firebase)
        put("firestore.rules", R.raw.firebase)
        put("vercel.json", R.raw.vercel)
        put("now.json", R.raw.vercel)
        // Python
        put("requirements.txt", R.raw.pythonconfig)
        put("pipfile", R.raw.pythonconfig)
        put("pipfile.lock", R.raw.pythonconfig)
        put("setup.py", R.raw.pythonconfig)
        put("setup.cfg", R.raw.pythonconfig)
        put("pyproject.toml", R.raw.poetry)
        put("poetry.lock", R.raw.poetry)
        put("pytest.ini", R.raw.pytest)
        put("conftest.py", R.raw.pytest)
        put("manage.py", R.raw.django)
        // PHP
        put("artisan", R.raw.laravel)
        // Infra
        put("nginx.conf", R.raw.nginx)
        // Editors, shells and coding agents whose dotfiles are in every tree
        put("opencode.json", R.raw.opencode)
        put("opencode.jsonc", R.raw.opencode)
        put("claude.md", R.raw.anthropic)
        put("gemini.md", R.raw.gemini)
        put(".cursorrules", R.raw.cursor)
        put(".cursorignore", R.raw.cursor)
        put(".bashrc", R.raw.bash)
        put(".bash_profile", R.raw.bash)
        put(".zshrc", R.raw.zsh)
        put(".zshenv", R.raw.zsh)
        put(".zprofile", R.raw.zsh)
        put(".vimrc", R.raw.vim)
        put(".profile", R.raw.sh)
        put(".editorconfig", R.raw.editorconfig)
        put(".env", R.raw.env)
        put(".env.example", R.raw.env_example)
        put("env.example", R.raw.env_example)
        put("project.godot", R.raw.godot)
        // LICENSE files ship with and without an extension, and both spellings.
        listOf("license", "licence").forEach { base ->
            listOf("", ".md", ".txt").forEach { suffix -> put(base + suffix, R.raw.license) }
        }
        // Config files whose stem, not extension, identifies the tool. Every one
        // of these ships under several extensions depending on whether the project
        // is ESM, CJS or TypeScript, so the stems are expanded rather than listed
        // by hand — 6 lines instead of 60, and a new variant cannot be forgotten
        // for one tool but remembered for another.
        mapOf(
            "vite.config" to R.raw.vite,
            "vitest.config" to R.raw.vite,
            "webpack.config" to R.raw.webpack,
            "rollup.config" to R.raw.rollup,
            "babel.config" to R.raw.babel,
            "jest.config" to R.raw.jest,
            "karma.conf" to R.raw.karma,
            "cypress.config" to R.raw.cypress,
            "playwright.config" to R.raw.playwright,
            "tailwind.config" to R.raw.tailwind,
            "postcss.config" to R.raw.postcss,
            "next.config" to R.raw.nextjs,
            "nuxt.config" to R.raw.nuxt,
            "svelte.config" to R.raw.svelte,
            "astro.config" to R.raw.astro
        ).forEach { (stem, res) ->
            listOf("js", "cjs", "mjs", "ts", "mts", "json").forEach { ext -> put("$stem.$ext", res) }
        }
        // Both spellings of an API description, in all three serialisations.
        listOf("openapi", "swagger").forEach { stem ->
            listOf("yaml", "yml", "json").forEach { ext -> put("$stem.$ext", R.raw.openapi) }
        }
    }

    /** Extension → drawing. Lowercase keys, no leading dot. */
    private val byExtension: Map<String, Int> = buildMap {
        // JVM / Android
        put("kt", R.raw.kt)
        put("kts", R.raw.kts)
        put("java", R.raw.java)
        put("class", R.raw.javaclass)
        put("jar", R.raw.jar)
        put("aar", R.raw.aar)
        put("aab", R.raw.aab)
        put("apk", R.raw.apk)
        put("gradle", R.raw.gradle)
        put("groovy", R.raw.groovy)
        put("pro", R.raw.pro)
        put("properties", R.raw.properties)
        put("so", R.raw.so)
        put("dll", R.raw.dll)
        // Markup / data
        put("xml", R.raw.xml)
        put("json", R.raw.json)
        put("jsonc", R.raw.jsonc)
        put("yaml", R.raw.yaml)
        put("yml", R.raw.yml)
        put("toml", R.raw.toml)
        put("ini", R.raw.ini)
        put("cfg", R.raw.ini)
        put("conf", R.raw.config)
        put("config", R.raw.config)
        put("csv", R.raw.csv)
        put("sql", R.raw.sql)
        put("db", R.raw.db)
        put("sqlite", R.raw.db)
        put("avsc", R.raw.avsc)
        put("proto", R.raw.proto)
        put("graphql", R.raw.graphql)
        put("gql", R.raw.graphql)
        put("http", R.raw.http)
        put("rest", R.raw.rest)
        put("diff", R.raw.diff)
        put("patch", R.raw.diff)
        // Languages
        put("c", R.raw.c)
        put("h", R.raw.h)
        put("cpp", R.raw.cpp)
        put("cc", R.raw.cpp)
        put("cxx", R.raw.cpp)
        put("hpp", R.raw.hpp)
        put("hh", R.raw.hpp)
        put("cs", R.raw.cs)
        put("fs", R.raw.fs)
        put("fsx", R.raw.fs)
        put("go", R.raw.go)
        put("rs", R.raw.rs)
        put("rb", R.raw.rb)
        put("php", R.raw.php)
        put("pl", R.raw.pl)
        put("pm", R.raw.pl)
        put("lua", R.raw.lua)
        put("r", R.raw.r)
        put("ex", R.raw.ex)
        put("exs", R.raw.ex)
        put("erl", R.raw.erlang)
        put("hrl", R.raw.erlang)
        put("scala", R.raw.scala)
        put("sc", R.raw.scala)
        put("clj", R.raw.clojure)
        put("edn", R.raw.clojure)
        put("cljs", R.raw.clojurescript)
        put("cljc", R.raw.clojurescript)
        put("hs", R.raw.haskell)
        put("lhs", R.raw.haskell)
        put("ml", R.raw.ocaml)
        put("mli", R.raw.ocaml)
        put("elm", R.raw.elm)
        put("nim", R.raw.nim)
        put("nims", R.raw.nim)
        put("zig", R.raw.zig)
        put("cr", R.raw.crystal)
        put("jl", R.raw.julia)
        put("vala", R.raw.vala)
        put("vapi", R.raw.vala)
        put("f", R.raw.fortran)
        put("for", R.raw.fortran)
        put("f90", R.raw.fortran)
        put("f95", R.raw.fortran)
        put("f03", R.raw.fortran)
        put("m", R.raw.objc)
        put("mm", R.raw.objc)
        put("awk", R.raw.awk)
        put("hx", R.raw.haxe)
        put("hxml", R.raw.haxe)
        put("purs", R.raw.purescript)
        put("sol", R.raw.solidity)
        put("apl", R.raw.apl)
        put("ll", R.raw.llvm)
        put("swift", R.raw.swift)
        put("dart", R.raw.dart)
        put("py", R.raw.py)
        put("pyw", R.raw.py)
        put("pyi", R.raw.py)
        put("ipynb", R.raw.ipynb)
        put("ino", R.raw.arduino)
        put("pde", R.raw.arduino)
        put("unity", R.raw.unity)
        put("unitypackage", R.raw.unity)
        put("gd", R.raw.godot)
        put("tscn", R.raw.godot)
        put("tres", R.raw.godot)
        // Web
        put("js", R.raw.js)
        put("mjs", R.raw.js)
        put("cjs", R.raw.js)
        put("jsx", R.raw.jsx)
        put("ts", R.raw.ts)
        put("mts", R.raw.ts)
        put("cts", R.raw.ts)
        put("tsx", R.raw.tsx)
        put("vue", R.raw.vue)
        put("svelte", R.raw.svelte)
        put("astro", R.raw.astro)
        put("coffee", R.raw.coffee)
        put("hbs", R.raw.handlebars)
        put("handlebars", R.raw.handlebars)
        put("css", R.raw.css)
        put("scss", R.raw.scss)
        put("sass", R.raw.scss)
        put("less", R.raw.less)
        put("styl", R.raw.stylus)
        put("html", R.raw.html)
        put("htm", R.raw.html)
        put("wasm", R.raw.wasm)
        put("cmake", R.raw.cmake)
        put("mk", R.raw.makefile)
        put("tf", R.raw.terraform)
        put("tfvars", R.raw.terraform)
        put("tfstate", R.raw.terraform)
        // Shells
        put("sh", R.raw.sh)
        put("bash", R.raw.bash)
        put("zsh", R.raw.zsh)
        put("vim", R.raw.vim)
        put("bat", R.raw.bat)
        put("cmd", R.raw.cmd)
        put("ps1", R.raw.ps1)
        // Docs
        put("md", R.raw.md)
        put("markdown", R.raw.md)
        put("txt", R.raw.txt)
        put("log", R.raw.log)
        put("tex", R.raw.tex)
        put("lock", R.raw.lock)
        // Archives
        put("zip", R.raw.zip)
        put("tar", R.raw.tar)
        put("gz", R.raw.gz)
        put("7z", R.raw.sevenz)
        // Images
        put("png", R.raw.png)
        put("jpg", R.raw.jpg)
        put("jpeg", R.raw.jpeg)
        put("gif", R.raw.gif)
        put("bmp", R.raw.bmp)
        put("webp", R.raw.webp)
        put("ico", R.raw.ico)
        put("svg", R.raw.svg)
    }
}
