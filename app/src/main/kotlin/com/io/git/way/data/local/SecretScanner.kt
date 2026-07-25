package com.io.git.way.data.local

/**
 * Secret Detection Engine (PRD "Smart GitHub Upload Protection" §6). Scans a text file's
 * content for likely API keys, tokens, credentials, and connection strings. Only ever run
 * on files that already passed [IgnoreEngine] and look like text under [isScannable]'s
 * size/extension guard, to keep the scan fast and avoid false positives on binaries.
 */
object SecretScanner {

    /** Kept close to the PRD's own regex examples (§6). Label is shown to the user on the
     * matching warning card / file badge — never the matched text itself. */
    private val PATTERNS: List<Pair<String, Regex>> = listOf(
        "Google API key" to Regex("AIza[0-9A-Za-z\\-_]{35}"),
        "GitHub personal access token" to Regex("ghp_[0-9A-Za-z]{36}"),
        "GitHub fine-grained token" to Regex("github_pat_[0-9A-Za-z_]{20,}"),
        "OpenAI API key" to Regex("sk-[0-9A-Za-z]{20,}"),
        "AWS access key" to Regex("AKIA[0-9A-Z]{16}"),
        "Stripe live key" to Regex("sk_live_[0-9A-Za-z]{16,}"),
        "Private key block" to Regex("BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY"),
        "Telegram bot token" to Regex("\\d{9,10}:[0-9A-Za-z_-]{35}"),
        "Discord bot token" to Regex("[MN][A-Za-z\\d]{23}\\.[\\w-]{6}\\.[\\w-]{27,}"),
        "JWT" to Regex("eyJ[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+"),
        "Hardcoded password" to Regex("(?i)password\\s*[=:]\\s*['\"][^'\"\\s]{4,}['\"]"),
        "Hardcoded secret" to Regex("(?i)secret\\s*[=:]\\s*['\"][^'\"\\s]{4,}['\"]"),
        "Hardcoded token" to Regex("(?i)\\btoken\\s*[=:]\\s*['\"][^'\"\\s]{6,}['\"]"),
        "Bearer token" to Regex("(?i)bearer\\s+[0-9A-Za-z_\\-.]{10,}"),
        "Database connection string" to Regex("(?i)(jdbc:|mongodb://|postgres(ql)?://|mysql://)[\\w.-]+:[^\\s'\"]+@[\\w.-]+")
    )

    private const val MAX_SCAN_BYTES = 2 * 1024 * 1024 // 2 MB — skip huge text files

    private val TEXT_EXTENSIONS = setOf(
        "kt", "kts", "java", "py", "js", "ts", "tsx", "jsx", "json", "xml", "yaml", "yml",
        "toml", "properties", "gradle", "env", "txt", "md", "sh", "bat", "cfg", "ini",
        "conf", "config", "html", "css", "sql", "csv", "swift", "dart", "rb", "go", "rs", "php"
    )

    /** Whether [fileName]/[sizeBytes] is even worth reading for a secret scan. */
    fun isScannable(fileName: String, sizeBytes: Long): Boolean {
        if (sizeBytes <= 0 || sizeBytes > MAX_SCAN_BYTES) return false
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in TEXT_EXTENSIONS || ext.isEmpty()
    }

    /** Returns the label of the first secret pattern found in [content], or null if none. */
    fun scan(content: String): String? =
        PATTERNS.firstOrNull { (_, regex) -> regex.containsMatchIn(content) }?.first
}
