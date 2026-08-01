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
