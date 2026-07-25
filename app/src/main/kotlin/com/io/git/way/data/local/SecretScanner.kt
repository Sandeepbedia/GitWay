package com.io.git.way.data.local

/**
 * Secret Detection Engine (PRD "Smart GitHub Upload Protection" §6).
 * Scans text files for likely API keys, tokens, credentials,
 * and connection strings.
 */
object SecretScanner {

    private val PATTERNS: List<Pair<String, Regex>> = listOf(
        "Google API key" to Regex("AIza[0-9A-Za-z\\-_]{35}"),

        "GitHub personal access token" to Regex(
            "ghp_[0-9A-Za-z]{36}"
        ),

        "GitHub fine-grained token" to Regex(
            "github_pat_[0-9A-Za-z_]{20,}"
        ),

        "OpenAI API key" to Regex(
            "sk-[0-9A-Za-z]{20,}"
        ),

        "AWS access key" to Regex(
            "AKIA[0-9A-Z]{16}"
        ),

        "Stripe live key" to Regex(
            "sk_live_[0-9A-Za-z]{16,}"
        ),

        "Private key block" to Regex(
            "BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY"
        ),

        "Telegram bot token" to Regex(
            "\\d{9,10}:[0-9A-Za-z_-]{35}"
        ),

        "Discord bot token" to Regex(
            "[MN][A-Za-z\\d]{23}\\.[\\w-]{6}\\.[\\w-]{27,}"
        ),

        "JWT" to Regex(
            "eyJ[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+"
        ),

        "Hardcoded password" to Regex(
            "(?i)password\\s*[=:]\\s*['\"][^'\"\\s]{4,}['\"]"
        ),

        "Hardcoded secret" to Regex(
            "(?i)secret\\s*[=:]\\s*['\"][^'\"\\s]{4,}['\"]"
        ),

        "Hardcoded token" to Regex(
            "(?i)\\btoken\\s*[=:]\\s*['\"][^'\"\\s]{6,}['\"]"
        ),

        "Bearer token" to Regex(
            "(?i)bearer\\s+[0-9A-Za-z_\\-.]{10,}"
        ),

        /**
         * Improved database detection:
         * Only triggers when URL contains username/password.
         * Avoids false detection on regex examples like:
         * jdbc:
         * mongodb://
         */
        "Database connection string" to Regex(
            "(?i)(jdbc:|mongodb://|postgres(ql)?://|mysql://)" +
                    "[^\\s\"']+:[^\\s\"']+@[^\\s\"']+"
        )
    )


    private const val MAX_SCAN_BYTES = 2 * 1024 * 1024 // 2 MB


    private val TEXT_EXTENSIONS = setOf(
        "kt", "kts",
        "java",
        "py",
        "js",
        "ts",
        "tsx",
        "jsx",
        "json",
        "xml",
        "yaml",
        "yml",
        "toml",
        "properties",
        "gradle",
        "env",
        "txt",
        "md",
        "sh",
        "bat",
        "cfg",
        "ini",
        "conf",
        "config",
        "html",
        "css",
        "sql",
        "csv",
        "swift",
        "dart",
        "rb",
        "go",
        "rs",
        "php"
    )


    /**
     * Checks whether file should be scanned.
     */
    fun isScannable(
        fileName: String,
        sizeBytes: Long
    ): Boolean {

        if (sizeBytes <= 0 || sizeBytes > MAX_SCAN_BYTES) {
            return false
        }

        val ext = fileName
            .substringAfterLast('.', "")
            .lowercase()

        return ext in TEXT_EXTENSIONS || ext.isEmpty()
    }


    /**
     * Returns detected secret label.
     * Does not return actual secret value.
     */
    fun scan(content: String): String? {

        // Prevent scanner detecting its own regex definitions
        if (content.contains("object SecretScanner")) {
            return null
        }

        return PATTERNS.firstOrNull { (_, regex) ->
            regex.containsMatchIn(content)
        }?.first
    }
}