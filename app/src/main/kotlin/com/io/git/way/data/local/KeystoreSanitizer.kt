package com.io.git.way.data.local

/**
 * Redacts real secret values out of simple `key=value` / `key: value` credential files
 * instead of hard-blocking them outright — so the file's *shape* (property names,
 * structure) still reaches GitHub for other developers to fill in, without the actual
 * password/token ever leaving the device. Only plain text credential files are touched
 * (key.properties, secrets.properties, credentials.json, .env*) — binary keystores
 * (.jks/.keystore/.p12/.pem/.cer) can't be safely redacted and stay hard-blocked.
 */
object KeystoreSanitizer {

    private val SANITIZABLE_NAMES = setOf("key.properties", "secrets.properties", "credentials.json", "config.secret")

    private val SENSITIVE_LINE_REGEX = Regex(
        "(?im)^([ \\t]*)([\\w.]*(?:password|secret|token|apikey|api_key)[\\w.]*)([ \\t]*[:=][ \\t]*)(.+)$"
    )

    fun isSanitizable(fileName: String): Boolean =
        fileName in SANITIZABLE_NAMES || fileName == ".env" || fileName.startsWith(".env.")

    /** Returns the redacted content, or null if nothing recognizable was found to redact
     * (in which case the caller should keep treating the file as hard-blocked). */
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
