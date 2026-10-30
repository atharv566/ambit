package com.example.cpvts.data

import com.example.cpvts.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val AppJson = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

/** One-shot bodies also stop HTTP follow-ups (including Retry-After: 0 on a 503). */
class NeverRetryWrites : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val body = request.body
        val outgoing = if (request.method == "POST" && body != null) request.newBuilder().method(request.method,
            object : RequestBody() {
                override fun contentType() = body.contentType()
                override fun contentLength() = body.contentLength()
                override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
                override fun isOneShot() = true
            }).build() else request
        return chain.proceed(outgoing)
    }
}
fun parkingHttpClient(debugLog: ((String) -> Unit)? = null): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS)
    .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
    .addInterceptor(NeverRetryWrites())
    .apply { if (debugLog != null) addInterceptor { chain ->
        // No URL, vehicle number, body, headers, credentials or raw exception is logged.
        val response = chain.proceed(chain.request())
        debugLog("${chain.request().method} HTTP ${response.code}")
        response
    } }.build()

object Network {
    fun repository(): ParkingRepository {
        // This is the only runtime reference to the build-configured base URL.
        val baseUrl = BuildConfig.API_BASE_URL
        val configured = !java.net.URI(baseUrl).host.endsWith(".invalid")
        val logger: ((String) -> Unit)? = if (BuildConfig.DEBUG) ({ android.util.Log.d("CPVTS-HTTP", it); Unit }) else null
        val api = Retrofit.Builder().baseUrl(baseUrl).client(parkingHttpClient(logger))
            .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType())).build().create(ParkingApi::class.java)
        return ApiParkingRepository(api, configured)
    }
}
