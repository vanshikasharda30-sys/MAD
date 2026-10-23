package com.example.clinicai.config

import com.example.clinicai.BuildConfig

/**
 * Central API configuration with separate development and production base URLs.
 *
 * - DEVELOPMENT: `http://10.0.2.2:3000/` — the host machine's `npm run dev` server, reachable from
 *   the Android emulator. Only selectable in debug builds (`-Pclinicai.env=dev`).
 * - PRODUCTION: the deployed Vercel app, which serves the `/api` routes on the same origin as the website
 *   (verified against the live deployment). Always HTTPS.
 */
object ApiConfig {

    enum class Environment(val key: String) {
        DEVELOPMENT("dev"),
        PRODUCTION("prod");

        companion object {
            fun fromKey(key: String): Environment =
                entries.firstOrNull { it.key == key } ?: PRODUCTION
        }
    }

    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    val environment: Environment = Environment.fromKey(BuildConfig.API_ENVIRONMENT)

    val baseUrl: String = resolveBaseUrl(
        environment = environment,
        devUrl = BuildConfig.DEV_API_BASE_URL,
        prodUrl = BuildConfig.PROD_API_BASE_URL,
    )

    /** Resolves the base URL for [environment] and refuses unsafe production configurations. */
    fun resolveBaseUrl(environment: Environment, devUrl: String, prodUrl: String): String {
        val url = when (environment) {
            Environment.DEVELOPMENT -> devUrl
            Environment.PRODUCTION -> prodUrl
        }
        require(url.endsWith("/")) { "API base URL must end with '/': $url" }
        if (environment == Environment.PRODUCTION) {
            require(url.startsWith("https://")) { "Production API must use HTTPS: $url" }
            require(!url.contains("10.0.2.2") && !url.contains("localhost")) {
                "Production API must not point at an emulator/localhost address: $url"
            }
        }
        return url
    }

    val isProduction: Boolean get() = environment == Environment.PRODUCTION
}
