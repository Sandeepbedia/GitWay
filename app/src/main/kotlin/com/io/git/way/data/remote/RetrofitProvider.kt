package com.io.git.way.data.remote

import com.io.git.way.data.local.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the Retrofit client used to talk to the GitHub API.
 * The auth interceptor reads the current token on every request so a freshly
 * saved/cleared token takes effect immediately, without rebuilding the client.
 */
object RetrofitProvider {

    private const val BASE_URL = "https://api.github.com/"

    fun create(tokenManager: TokenManager): GitHubApiService {
        val authInterceptor = Interceptor { chain ->
            val token = tokenManager.getToken()
            val requestBuilder = chain.request().newBuilder()
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
            if (!token.isNullOrBlank()) {
                // Never logged: logging interceptor below is set to BODY level only for
                // non-sensitive debugging and headers are not printed by BASIC level.
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        // encodeDefaults = true is critical: kotlinx.serialization's Json defaults to
        // OMITTING properties that still hold their default value. TreeEntryInput's
        // mode/type and CreateBlobRequest's encoding are always left at their defaults
        // in this app, so without this flag they were silently stripped from every
        // request body — GitHub then rejected trees with "Must supply a valid
        // tree.mode" (422), and worse, blobs were silently uploaded as UTF-8 text
        // instead of base64, corrupting any binary file (images, APKs, etc.).
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubApiService::class.java)
    }
}
