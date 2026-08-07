package app.rocat.core.common.network

import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Central HTTP client holder, mirroring mihon's `NetworkHelper`.
 *
 * Owns a single [OkHttpClient] (plus a per-request disposable client used by the
 * scripting engine so that scripts cannot leak connections into the main pool).
 */
class NetworkHelper(
    cacheDir: File,
    userAgent: String = "RoCat/0.1",
) {
    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .cache(Cache(File(cacheDir, "http_cache"), 5L * 1024 * 1024))
        .addInterceptor(UserAgentInterceptor(userAgent))
        .build()

    val client: OkHttpClient
        get() = baseClient

    /**
     * A short-lived client with aggressive timeouts, used by script executions so a
     * misbehaving script cannot hang the app or poison the shared connection pool.
     */
    fun newScriptClient(): OkHttpClient = baseClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
}

private class UserAgentInterceptor(private val userAgent: String) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(request)
    }
}
