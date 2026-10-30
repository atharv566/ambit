# Complete project source
Every distributable text file follows, using its complete project-relative path. This generated listing excludes itself. The Gradle wrapper JAR is distributed as a binary and identified by SHA-256 below. Local settings, caches, signing keys and build products are excluded.

## .gitignore

````
.gradle/
.kotlin/
**/build/
local.properties
.idea/
*.iml
*.jks
*.keystore
.env*

````

## app/build.gradle.kts

````
import java.util.Properties
import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
// AGP 9 supplies Kotlin support; do not also apply kotlin-android.
val local = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
fun apiUrl(property: String, placeholder: String, release: Boolean): String {
    val value = providers.gradleProperty(property).orNull ?: local.getProperty(property) ?: placeholder
    val uri = URI(value)
    require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null && uri.path.endsWith("/api/v1/")) { "$property must be a URL ending in /api/v1/ without credentials, query or fragment." }
    require(uri.scheme == "https" || (!release && uri.scheme == "http")) { "$property requires HTTPS for release." }
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
android {
    namespace = "com.example.cpvts"
    buildToolsVersion = "37.0.0"
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig {
        applicationId = "com.example.cpvts"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE_URL", apiUrl("CPVTS_DEV_API_BASE_URL", "https://cpvts-dev-api-host.invalid/api/v1/", false))
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", apiUrl("CPVTS_PROD_API_BASE_URL", "https://cpvts-production-api-host.invalid/api/v1/", true))
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests.isReturnDefaultValues = true; unitTests.isIncludeAndroidResources = false }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
}

````

## app/src/debug/AndroidManifest.xml

````
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">
    <application android:usesCleartextTraffic="true" android:networkSecurityConfig="@xml/debug_network_security_config" tools:replace="android:usesCleartextTraffic"/>
</manifest>

````

## app/src/debug/res/xml/debug_network_security_config.xml

````
<?xml version="1.0" encoding="utf-8"?>
<network-security-config><base-config cleartextTrafficPermitted="true"><trust-anchors><certificates src="system"/></trust-anchors></base-config></network-security-config>

````

## app/src/main/AndroidManifest.xml

````
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <!-- This app registers no dynamic broadcast receivers. Keep the permission list limited to the two network permissions. -->
    <permission android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" tools:node="remove" />
    <uses-permission android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" tools:node="remove" />
    <application android:name=".CpvtsApplication" android:label="@string/app_name"
        android:icon="@drawable/ic_parking" android:theme="@style/Theme.CPVTS"
        android:allowBackup="false" android:supportsRtl="true" android:usesCleartextTraffic="false">
        <activity android:name=".MainActivity" android:exported="true" android:windowSoftInputMode="adjustResize">
            <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
        </activity>
    </application>
</manifest>

````

## app/src/main/java/com/example/cpvts/CpvtsApplication.kt

````
package com.example.cpvts
import android.app.Application
import com.example.cpvts.data.Network
class CpvtsApplication : Application() { val repository by lazy { Network.repository() } }

````

## app/src/main/java/com/example/cpvts/data/Models.kt

````
package com.example.cpvts.data

import kotlinx.serialization.Serializable

@Serializable data class Envelope<T>(val data: T, val meta: Meta? = null)
@Serializable data class Meta(val mode: String)
@Serializable data class Health(val status: String, val mode: String)
@Serializable data class Calculation(val freeHours: Int, val billableHours: Int, val ratePerHour: Int)
@Serializable data class ParkingRecord(
    val id: String, val vehicleNumber: String, val vehicleType: String, val ownerCategory: String,
    val slotId: String, val entryHour: Int, val exitHour: Int? = null,
    val checkedInAt: String, val checkedOutAt: String? = null, val status: String,
    val duration: Int? = null, val fee: Int? = null, val calculation: Calculation? = null
)
@Serializable data class Slot(val id: String, val vehicleType: String, val ordinal: Int, val status: String, val record: ParkingRecord? = null)
@Serializable data class Zone(val vehicleType: String, val total: Int, val occupied: Int, val vacant: Int)
@Serializable data class Summary(val vehiclesParked: Int, val completedCheckouts: Int, val feesAssessed: Int)
@Serializable data class Snapshot(val date: String, val timezone: String, val slots: List<Slot>, val records: List<ParkingRecord>, val summary: Summary, val zones: List<Zone> = emptyList())
@Serializable data class CheckInRequest(val vehicleNumber: String, val vehicleType: String, val ownerCategory: String, val entryHour: Int)
@Serializable data class CheckOutRequest(val exitHour: Int, val vehicleNumber: String? = null, val slotId: String? = null)
@Serializable data class FieldIssue(val field: String, val message: String)
@Serializable data class ServerError(val code: String, val message: String, val details: List<FieldIssue> = emptyList())
@Serializable data class ErrorEnvelope(val error: ServerError)

enum class ErrorKind { Validation, FullZone, Duplicate, NotFound, Database, Timeout, Offline, UnknownOutcome, Configuration, Unexpected }
data class UiError(val kind: ErrorKind, val serverMessage: String? = null, val fields: Map<String, String> = emptyMap())
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: UiError) : ApiResult<Nothing>
}

````

## app/src/main/java/com/example/cpvts/data/Network.kt

````
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

````

## app/src/main/java/com/example/cpvts/data/ParkingApi.kt

````
package com.example.cpvts.data

import retrofit2.Response
import retrofit2.http.*

interface ParkingApi {
    @GET("health") suspend fun health(): Response<Envelope<Health>>
    @GET("dashboard") suspend fun dashboard(): Response<Envelope<Snapshot>>
    @GET("slots") suspend fun slots(): Response<Envelope<List<Slot>>>
    @POST("check-ins") suspend fun checkIn(@Body request: CheckInRequest): Response<Envelope<ParkingRecord>>
    @POST("check-outs") suspend fun checkOut(@Body request: CheckOutRequest): Response<Envelope<ParkingRecord>>
    @GET("vehicles/{vehicleNumber}") suspend fun find(@Path("vehicleNumber") vehicleNumber: String): Response<Envelope<ParkingRecord>>
    @GET("summary") suspend fun summary(@Query("date") date: String? = null): Response<Envelope<Snapshot>>
}

````

## app/src/main/java/com/example/cpvts/data/ParkingRepository.kt

````
package com.example.cpvts.data

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Locale

interface ParkingRepository {
    suspend fun dashboard(): ApiResult<Snapshot>
    suspend fun checkIn(request: CheckInRequest): ApiResult<ParkingRecord>
    suspend fun checkOut(request: CheckOutRequest): ApiResult<ParkingRecord>
    suspend fun find(number: String): ApiResult<ParkingRecord>
    suspend fun summary(date: String): ApiResult<Snapshot>
}
class ApiParkingRepository(private val api: ParkingApi, private val configured: Boolean = true, private val json: Json = AppJson) : ParkingRepository {
    override suspend fun dashboard() = execute { api.dashboard() }
    override suspend fun checkIn(request: CheckInRequest) = execute(write = true) { api.checkIn(request.copy(vehicleNumber = normalizeVehicle(request.vehicleNumber))) }
    override suspend fun checkOut(request: CheckOutRequest) = execute(write = true) { api.checkOut(request.copy(vehicleNumber = request.vehicleNumber?.let(::normalizeVehicle), slotId = request.slotId?.trim()?.uppercase(Locale.ROOT))) }
    override suspend fun find(number: String) = execute { api.find(normalizeVehicle(number)) }
    override suspend fun summary(date: String) = execute { api.summary(date) }
    private suspend fun <T> execute(write: Boolean = false, call: suspend () -> Response<Envelope<T>>): ApiResult<T> {
        if (!configured) return ApiResult.Failure(UiError(ErrorKind.Configuration))
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) ApiResult.Success(body.data)
            else {
                val server = response.errorBody()?.use { runCatching { json.decodeFromString<ErrorEnvelope>(it.string()).error }.getOrNull() }
                val kind = when (server?.code) {
                    "PARKING_FULL" -> ErrorKind.FullZone
                    "DUPLICATE_VEHICLE" -> ErrorKind.Duplicate
                    "VEHICLE_NOT_FOUND" -> ErrorKind.NotFound
                    "VALIDATION_ERROR", "EXIT_BEFORE_ENTRY", "INVALID_JSON" -> ErrorKind.Validation
                    "DATABASE_NOT_CONFIGURED" -> ErrorKind.Configuration
                    // A gateway/database failure can arrive after a write commits.
                    "DATABASE_UNAVAILABLE" -> if (write) ErrorKind.UnknownOutcome else ErrorKind.Database
                    else -> if (write && (response.isSuccessful || response.code() >= 500 || response.code() in listOf(408, 429))) ErrorKind.UnknownOutcome else ErrorKind.Unexpected
                }
                ApiResult.Failure(UiError(kind, server?.message, server?.details?.associate { it.field to it.message } ?: emptyMap()))
            }
        } catch (e: CancellationException) { throw e }
        catch (e: SocketTimeoutException) { ApiResult.Failure(UiError(if (write) ErrorKind.UnknownOutcome else ErrorKind.Timeout)) }
        catch (e: IOException) { ApiResult.Failure(UiError(if (write) ErrorKind.UnknownOutcome else ErrorKind.Offline)) }
        catch (e: SerializationException) { ApiResult.Failure(UiError(if (write) ErrorKind.UnknownOutcome else ErrorKind.Unexpected)) }
    }
}
fun normalizeVehicle(value: String) = value.trim().uppercase(Locale.ROOT).replace(" ", "").replace("-", "")
fun validVehicle(value: String) = normalizeVehicle(value).matches(Regex("[A-Z0-9]{2,20}"))
fun validHour(value: String) = value.toIntOrNull()?.takeIf { it in 0..23 }

````

## app/src/main/java/com/example/cpvts/MainActivity.kt

````
package com.example.cpvts

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpvts.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        val repository = (application as CpvtsApplication).repository
        setContent {
            val vm: ParkingViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = ParkingViewModel(repository, extras.createSavedStateHandle()) as T
            })
            CpvtsTheme { CpvtsApp(vm) }
        }
    }
}

````

## app/src/main/java/com/example/cpvts/ui/ParkingViewModel.kt

````
package com.example.cpvts.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpvts.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

val CampusZone: ZoneId = ZoneId.of("Asia/Kolkata")
@Serializable data class FormState(
    val entryVehicle: String = "", val vehicleType: String = "two_wheeler", val owner: String = "Student",
    val entryHour: String = ZonedDateTime.now(CampusZone).hour.toString(),
    val exitIdentifier: String = "", val exitBySlot: Boolean = false,
    val exitHour: String = ZonedDateTime.now(CampusZone).hour.toString(),
    val searchVehicle: String = "", val summaryDate: String = LocalDate.now(CampusZone).toString(),
    val checkInResult: ParkingRecord? = null, val receipt: ParkingRecord? = null, val found: ParkingRecord? = null,
    val checkInUncertain: Boolean = false, val checkOutUncertain: Boolean = false
)
data class LoadState<T>(val value: T? = null, val loading: Boolean = false, val error: UiError? = null)
data class ParkingUiState(
    val forms: FormState, val dashboard: LoadState<Snapshot> = LoadState(), val summary: LoadState<Snapshot> = LoadState(),
    val checkIn: LoadState<Unit> = LoadState(), val checkOut: LoadState<Unit> = LoadState(), val find: LoadState<Unit> = LoadState()
)
class ParkingViewModel(private val repository: ParkingRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val restored = saved.get<String>("forms")?.let { runCatching { AppJson.decodeFromString<FormState>(it) }.getOrNull() } ?: FormState()
    private val _state = MutableStateFlow(ParkingUiState(restored,
        checkIn = LoadState(error = if (restored.checkInUncertain) UiError(ErrorKind.UnknownOutcome) else null),
        checkOut = LoadState(error = if (restored.checkOutUncertain) UiError(ErrorKind.UnknownOutcome) else null)))
    val state: StateFlow<ParkingUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var summaryJob: Job? = null
    init { refreshDashboard(); refreshSummary() }
    private fun forms(change: (FormState) -> FormState) {
        val next = change(_state.value.forms)
        saved["forms"] = AppJson.encodeToString(next)
        _state.update { it.copy(forms = next) }
    }
    fun edit(field: String, value: String) {
        when (field) {
            "searchVehicle" -> { searchJob?.cancel(); _state.update { it.copy(find = LoadState()) } }
            "summaryDate" -> { summaryJob?.cancel(); _state.update { it.copy(summary = LoadState()) } }
        }
        forms { f -> when (field) {
            "entryVehicle" -> f.copy(entryVehicle = value.uppercase(Locale.ROOT).take(40), checkInResult = null)
            "vehicleType" -> f.copy(vehicleType = value, checkInResult = null)
            "owner" -> f.copy(owner = value, checkInResult = null)
            "entryHour" -> f.copy(entryHour = value.take(2), checkInResult = null)
            "exitIdentifier" -> f.copy(exitIdentifier = value.uppercase(Locale.ROOT).take(40), receipt = null)
            "exitHour" -> f.copy(exitHour = value.take(2), receipt = null)
            "searchVehicle" -> f.copy(searchVehicle = value.uppercase(Locale.ROOT).take(40), found = null)
            "summaryDate" -> f.copy(summaryDate = value.take(10))
            else -> f
        } }
    }
    fun exitMode(bySlot: Boolean) { forms { it.copy(exitBySlot = bySlot, exitIdentifier = "", receipt = null) } }
    fun stageCheckout(number: String) { forms { it.copy(exitBySlot = false, exitIdentifier = number, receipt = null) } }
    fun acknowledgeOutcome(checkIn: Boolean) {
        forms { if (checkIn) it.copy(checkInUncertain = false) else it.copy(checkOutUncertain = false) }
        _state.update { if (checkIn) it.copy(checkIn = LoadState()) else it.copy(checkOut = LoadState()) }
    }
    fun refreshDashboard() {
        if (_state.value.dashboard.loading) return
        _state.update { it.copy(dashboard = it.dashboard.copy(loading = true, error = null)) }
        viewModelScope.launch { when (val r = repository.dashboard()) {
            is ApiResult.Success -> _state.update { it.copy(dashboard = LoadState(value = r.value)) }
            is ApiResult.Failure -> _state.update { it.copy(dashboard = it.dashboard.copy(loading = false, error = r.error)) }
        } }
    }
    fun refreshSummary() {
        val date = _state.value.forms.summaryDate
        if (runCatching { LocalDate.parse(date) }.isFailure) {
            _state.update { it.copy(summary = LoadState(error = UiError(ErrorKind.Validation, fields = mapOf("date" to "@date")))) }; return
        }
        summaryJob?.cancel()
        _state.update { it.copy(summary = LoadState(loading = true)) }
        summaryJob = viewModelScope.launch { when (val r = repository.summary(date)) {
            is ApiResult.Success -> _state.update { it.copy(summary = LoadState(value = r.value)) }
            is ApiResult.Failure -> _state.update { it.copy(summary = LoadState(error = r.error)) }
        } }
    }
    fun checkIn() {
        val f = _state.value.forms
        if (_state.value.checkIn.loading || f.checkInUncertain) return
        val fields = buildMap {
            if (!validVehicle(f.entryVehicle)) put("vehicleNumber", "@vehicle")
            if (validHour(f.entryHour) == null) put("entryHour", "@hour")
        }
        if (fields.isNotEmpty()) { _state.update { it.copy(checkIn = LoadState(error = UiError(ErrorKind.Validation, fields = fields))) }; return }
        forms { it.copy(entryVehicle = it.entryVehicle.trim(), checkInResult = null, checkInUncertain = true) }
        _state.update { it.copy(checkIn = LoadState(loading = true)) }
        viewModelScope.launch {
            when (val r = repository.checkIn(CheckInRequest(f.entryVehicle, f.vehicleType, f.owner, validHour(f.entryHour)!!))) {
                is ApiResult.Success -> { forms { it.copy(checkInResult = r.value, checkInUncertain = false) }; _state.update { it.copy(checkIn = LoadState(value = Unit)) }; refreshDashboard(); refreshSummary() }
                is ApiResult.Failure -> { forms { it.copy(checkInUncertain = r.error.kind == ErrorKind.UnknownOutcome) }; _state.update { it.copy(checkIn = LoadState(error = r.error)) } }
            }
        }
    }
    fun checkOut() {
        val f = _state.value.forms
        if (_state.value.checkOut.loading || f.checkOutUncertain) return
        val fields = buildMap {
            if (f.exitBySlot && !f.exitIdentifier.trim().uppercase(Locale.ROOT).matches(Regex("[TF]-\\d{2,}"))) put("slotId", "@slot")
            if (!f.exitBySlot && !validVehicle(f.exitIdentifier)) put("vehicleNumber", "@vehicle")
            if (validHour(f.exitHour) == null) put("exitHour", "@hour")
        }
        if (fields.isNotEmpty()) { _state.update { it.copy(checkOut = LoadState(error = UiError(ErrorKind.Validation, fields = fields))) }; return }
        forms { it.copy(exitIdentifier = it.exitIdentifier.trim(), receipt = null, checkOutUncertain = true) }
        _state.update { it.copy(checkOut = LoadState(loading = true)) }
        val request = CheckOutRequest(validHour(f.exitHour)!!, vehicleNumber = if (!f.exitBySlot) f.exitIdentifier else null, slotId = if (f.exitBySlot) f.exitIdentifier else null)
        viewModelScope.launch { when (val r = repository.checkOut(request)) {
            is ApiResult.Success -> { forms { it.copy(receipt = r.value, checkOutUncertain = false) }; _state.update { it.copy(checkOut = LoadState(value = Unit)) }; refreshDashboard(); refreshSummary() }
            is ApiResult.Failure -> { forms { it.copy(checkOutUncertain = r.error.kind == ErrorKind.UnknownOutcome) }; _state.update { it.copy(checkOut = LoadState(error = r.error)) } }
        } }
    }
    fun find() {
        val number = _state.value.forms.searchVehicle
        if (!validVehicle(number)) { _state.update { it.copy(find = LoadState(error = UiError(ErrorKind.Validation, fields = mapOf("vehicleNumber" to "@vehicle")))) }; return }
        searchJob?.cancel()
        forms { it.copy(searchVehicle = it.searchVehicle.trim(), found = null) }
        _state.update { it.copy(find = LoadState(loading = true)) }
        searchJob = viewModelScope.launch { when (val r = repository.find(number)) {
            is ApiResult.Success -> { forms { it.copy(found = r.value) }; _state.update { it.copy(find = LoadState(value = Unit)) } }
            is ApiResult.Failure -> _state.update { it.copy(find = LoadState(error = r.error)) }
        } }
    }
}

````

## app/src/main/java/com/example/cpvts/ui/Screens.kt

````
package com.example.cpvts.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.cpvts.R
import com.example.cpvts.data.*
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private data class Destination(val route: String, @param:StringRes val title: Int, @param:StringRes val short: Int, val icon: ImageVector)
private val destinations = listOf(
    Destination("dashboard", R.string.dashboard, R.string.nav_home, Icons.Default.Home),
    Destination("check-in", R.string.check_in, R.string.nav_in, Icons.Default.Add),
    Destination("check-out", R.string.check_out, R.string.nav_out, Icons.AutoMirrored.Filled.ExitToApp),
    Destination("finder", R.string.find_vehicle, R.string.nav_find, Icons.Default.Search),
    Destination("summary", R.string.daily_summary, R.string.nav_today, Icons.Default.DateRange)
)
@Composable fun CpvtsApp(vm: ParkingViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "dashboard"
    fun go(target: String) { nav.navigate(target) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }
    val keyboard = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(modifier = Modifier.fillMaxSize().imePadding(), containerColor = Palette.Canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = { if (!keyboard) NavigationBar(containerColor = Palette.Beige, tonalElevation = 0.dp) {
            destinations.forEach { d -> val label = stringResource(d.title)
                NavigationBarItem(selected = route == d.route, onClick = { go(d.route) },
                    icon = { Icon(d.icon, contentDescription = label) },
                    label = if (LocalDensity.current.fontScale <= 1.3f) ({ Text(stringResource(d.short)) }) else null,
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Palette.Ink, selectedTextColor = Palette.Ink, indicatorColor = Palette.AmberTint, unselectedIconColor = Palette.Muted, unselectedTextColor = Palette.Muted))
            }
        } }
    ) { padding -> Box(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        NavHost(nav, startDestination = "dashboard", modifier = Modifier.widthIn(max = 900.dp).fillMaxSize()) {
            composable("dashboard") { DashboardScreen(state.dashboard, vm::refreshDashboard) }
            composable("check-in") { CheckInScreen(state, vm, { go("finder") }, { go("dashboard") }) }
            composable("check-out") { CheckOutScreen(state, vm, { go("finder") }, { go("dashboard") }) }
            composable("finder") { FinderScreen(state, vm) { number -> vm.stageCheckout(number); go("check-out") } }
            composable("summary") { SummaryScreen(state, vm) }
        }
    } }
}
@Composable private fun Screen(@StringRes title: Int, @StringRes subtitle: Int, footer: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(stringResource(R.string.brand_subtitle), style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
            Text(stringResource(title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            Text(stringResource(subtitle), style = MaterialTheme.typography.bodyLarge, color = Palette.Muted)
            content()
            Spacer(Modifier.height(8.dp))
        }
        if (footer != null) Surface(color = Palette.Canvas) { Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) { footer() } }
    }
}
@Composable private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Palette.White), border = BorderStroke(1.dp, Palette.Border), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable private fun Primary(@StringRes label: Int, busy: Boolean = false, enabled: Boolean = true, action: () -> Unit) {
    val focus = LocalFocusManager.current
    Button(onClick = { focus.clearFocus(); action() }, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        if (busy) { CircularProgressIndicator(Modifier.size(20.dp), color = Palette.Ink, strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
        Text(stringResource(if (busy) R.string.working else label), style = MaterialTheme.typography.labelLarge)
    }
}
@Composable private fun Secondary(@StringRes label: Int, enabled: Boolean = true, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = Palette.BlueTint, contentColor = Palette.Ink)) { Text(stringResource(label)) }
}
@Composable private fun Loading() { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
    CircularProgressIndicator(Modifier.size(24.dp), color = Palette.Green, strokeWidth = 2.dp); Text(stringResource(R.string.loading))
} }
@Composable private fun Status(text: String) { Surface(color = Palette.GreenTint, shape = MaterialTheme.shapes.small, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Check, null, tint = Palette.Green); Text(text, color = Palette.Green) } } }
@Composable private fun ErrorPanel(error: UiError?, retry: (() -> Unit)? = null, finder: (() -> Unit)? = null, dashboard: (() -> Unit)? = null, acknowledge: (() -> Unit)? = null) {
    if (error == null) return
    val label = when(error.kind) {
        ErrorKind.Validation -> R.string.error_validation; ErrorKind.FullZone -> R.string.error_full; ErrorKind.Duplicate -> R.string.error_duplicate
        ErrorKind.NotFound -> R.string.error_not_found; ErrorKind.Database -> R.string.error_database; ErrorKind.Timeout -> R.string.error_timeout
        ErrorKind.Offline -> R.string.error_offline; ErrorKind.UnknownOutcome -> R.string.error_unknown; ErrorKind.Configuration -> R.string.error_config; ErrorKind.Unexpected -> R.string.error_unexpected
    }
    Surface(color = Palette.AmberTint, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, Palette.Amber), modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Info, null); Text(stringResource(label)) }
            error.serverMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            // These fields have fixed choices rather than editable text fields.
            error.fields.filterKeys { it !in setOf("vehicleNumber", "slotId", "entryHour", "exitHour", "date") }.values.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (error.kind == ErrorKind.UnknownOutcome) {
                finder?.let { Secondary(R.string.verify_finder, action = it) }
                dashboard?.let { Secondary(R.string.verify_dashboard, action = it) }
                acknowledge?.let { OutlinedButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.verified_outcome)) } }
            } else if (retry != null) Secondary(R.string.retry, action = retry)
        }
    }
}
@Composable private fun fieldText(value: String?): String? = when (value) {
    "@vehicle" -> stringResource(R.string.invalid_vehicle); "@hour" -> stringResource(R.string.invalid_hour)
    "@slot" -> stringResource(R.string.invalid_slot); "@date" -> stringResource(R.string.invalid_date); else -> value
}
@Composable private fun Field(@StringRes label: Int, value: String, change: (String) -> Unit, issue: String? = null, numeric: Boolean = false, @StringRes hint: Int? = null, @StringRes placeholder: Int? = null, enabled: Boolean = true) {
    val error = fieldText(issue)
    val focus = LocalFocusManager.current
    OutlinedTextField(value, change, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(label)) }, enabled = enabled, singleLine = true,
        placeholder = placeholder?.let { { Text(stringResource(it)) } }, isError = error != null,
        supportingText = if (error != null) ({ Text(error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }) else hint?.let { { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(capitalization = if(numeric) KeyboardCapitalization.None else KeyboardCapitalization.Characters, keyboardType = if(numeric) KeyboardType.Number else KeyboardType.Text, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.Muted, unfocusedBorderColor = Palette.Border, errorBorderColor = Palette.Ink, focusedLabelColor = Palette.Ink))
}
@Composable private fun Choices(@StringRes title: Int, options: List<Pair<String, Int>>, selected: String, enabled: Boolean, select: (String) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        options.forEach { (value, label) -> Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected == value, enabled = enabled, role = Role.RadioButton, onClick = { select(value) }).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected == value, onClick = null, enabled = enabled, colors = RadioButtonDefaults.colors(selectedColor = Palette.Ink, unselectedColor = Palette.Muted))
            Spacer(Modifier.width(12.dp)); Text(stringResource(label), modifier = Modifier.weight(1f))
        } }
    }
}
private fun money(value: Int) = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply { currency = Currency.getInstance("INR"); maximumFractionDigits = 0 }.format(value)
private fun hour(value: Int) = "%02d:00".format(Locale.ROOT, value)
private fun dateLabel(value: String) = runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.forLanguageTag("en-IN"))) }.getOrDefault(value)
@Composable private fun vehicleLabel(value: String) = stringResource(if (value == "two_wheeler") R.string.two_wheeler else R.string.four_wheeler)
@Composable private fun Detail(@StringRes label: Int, value: String, emphasis: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(stringResource(label), color = Palette.Muted, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = if(emphasis) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge, fontWeight = if(emphasis) FontWeight.SemiBold else FontWeight.Medium)
    }
}
@Composable private fun DashboardScreen(load: LoadState<Snapshot>, refresh: () -> Unit) {
    Screen(R.string.dashboard, R.string.dashboard_intro) {
        Secondary(R.string.refresh, !load.loading, refresh)
        if (load.loading) Loading()
        ErrorPanel(load.error, refresh)
        load.value?.let { data ->
            if (load.error != null) Text(stringResource(R.string.last_known), color = Palette.Muted)
            data.zones.forEach { zone -> Panel {
                Text(stringResource(if(zone.vehicleType == "two_wheeler") R.string.two_zone else R.string.four_zone), style = MaterialTheme.typography.titleLarge)
                Status("${zone.vacant} ${stringResource(R.string.available)}")
                Detail(R.string.total_spaces, zone.total.toString()); Detail(R.string.occupied, zone.occupied.toString())
            } }
            Panel { Detail(R.string.vehicles_today, data.summary.vehiclesParked.toString(), true); Detail(R.string.fees_assessed, money(data.summary.feesAssessed), true); Text(stringResource(R.string.not_payment), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted) }
            Text(stringResource(R.string.currently_parked), style = MaterialTheme.typography.titleLarge)
            val active = data.records.filter { it.status == "Parked" }
            if (active.isEmpty()) Panel { Text(stringResource(R.string.empty_campus), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.empty_campus_detail), color = Palette.Muted) }
            active.forEach { RecordCard(it) }
        }
    }
}
@Composable private fun CheckInScreen(s: ParkingUiState, vm: ParkingViewModel, finder: () -> Unit, dashboard: () -> Unit) {
    val f = s.forms; val busy = s.checkIn.loading
    Screen(R.string.check_in, R.string.entry_intro, footer = { Primary(R.string.check_in_vehicle, busy, !f.checkInUncertain, vm::checkIn) }) {
        ErrorPanel(s.checkIn.error, finder = finder, dashboard = dashboard, acknowledge = { vm.acknowledgeOutcome(true) })
        f.checkInResult?.let { Panel(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { Status(stringResource(R.string.entry_confirmed)); Text(stringResource(R.string.entry_result, it.vehicleNumber, it.slotId, hour(it.entryHour))) } }
        Panel {
            Field(R.string.vehicle_number, f.entryVehicle, { vm.edit("entryVehicle", it) }, s.checkIn.error?.fields?.get("vehicleNumber"), hint = R.string.vehicle_hint, placeholder = R.string.vehicle_example, enabled = !busy)
            Choices(R.string.vehicle_type, listOf("two_wheeler" to R.string.two_wheeler, "four_wheeler" to R.string.four_wheeler), f.vehicleType, !busy) { vm.edit("vehicleType", it) }
            Choices(R.string.owner_category, listOf("Staff" to R.string.staff, "Student" to R.string.student, "Visitor" to R.string.visitor), f.owner, !busy) { vm.edit("owner", it) }
            Field(R.string.entry_hour, f.entryHour, { vm.edit("entryHour", it) }, s.checkIn.error?.fields?.get("entryHour"), numeric = true, hint = R.string.hour_hint, enabled = !busy)
            Text(stringResource(R.string.first_available), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        }
        Rates()
    }
}
@Composable private fun CheckOutScreen(s: ParkingUiState, vm: ParkingViewModel, finder: () -> Unit, dashboard: () -> Unit) {
    val f = s.forms; val busy = s.checkOut.loading
    Screen(R.string.check_out, R.string.exit_intro, footer = { Primary(R.string.create_receipt, busy, !f.checkOutUncertain, vm::checkOut) }) {
        ErrorPanel(s.checkOut.error, finder = finder, dashboard = dashboard, acknowledge = { vm.acknowledgeOutcome(false) })
        Panel {
            Choices(R.string.lookup_by, listOf("vehicle" to R.string.vehicle_number, "slot" to R.string.slot_id), if(f.exitBySlot) "slot" else "vehicle", !busy) { vm.exitMode(it == "slot") }
            Field(if(f.exitBySlot) R.string.slot_id else R.string.vehicle_number, f.exitIdentifier, { vm.edit("exitIdentifier", it) }, s.checkOut.error?.fields?.get(if(f.exitBySlot) "slotId" else "vehicleNumber"), placeholder = if(f.exitBySlot) R.string.slot_example else R.string.vehicle_example, enabled = !busy)
            Field(R.string.exit_hour, f.exitHour, { vm.edit("exitHour", it) }, s.checkOut.error?.fields?.get("exitHour"), numeric = true, hint = R.string.hour_hint, enabled = !busy)
            Text(stringResource(R.string.no_overnight), color = Palette.Muted, style = MaterialTheme.typography.bodyMedium)
        }
        f.receipt?.let { Receipt(it) } ?: Panel { Text(stringResource(R.string.receipt_empty), color = Palette.Muted) }
    }
}
@Composable private fun Receipt(record: ParkingRecord) {
    Panel(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Status(stringResource(R.string.checkout_confirmed))
        Text(stringResource(R.string.receipt), style = MaterialTheme.typography.headlineSmall)
        Detail(R.string.receipt_number, record.id); Detail(R.string.vehicle_number, record.vehicleNumber)
        Detail(R.string.vehicle_type, vehicleLabel(record.vehicleType)); Detail(R.string.owner_category, record.ownerCategory)
        Detail(R.string.assigned_space, record.slotId); Detail(R.string.entry_hour, hour(record.entryHour))
        record.exitHour?.let { Detail(R.string.exit_hour, hour(it)) }
        record.duration?.let { Detail(R.string.duration, stringResource(R.string.hours_value, it)) }
        HorizontalDivider(color = Palette.Border)
        record.calculation?.let { Detail(R.string.free_hours, stringResource(R.string.hours_value, it.freeHours)); Detail(R.string.billable_hours, stringResource(R.string.hours_value, it.billableHours)); Detail(R.string.hourly_rate, money(it.ratePerHour)) }
        record.fee?.let { Surface(color = Palette.AmberTint, shape = MaterialTheme.shapes.small) { Box(Modifier.fillMaxWidth().padding(16.dp)) { Detail(R.string.amount_due, money(it), true) } } }
        Text(stringResource(R.string.space_released, record.slotId), color = Palette.Green)
        Text(stringResource(R.string.not_payment), color = Palette.Muted, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable private fun RecordCard(r: ParkingRecord, checkout: ((String) -> Unit)? = null) {
    Panel { Status(r.status); Text(r.vehicleNumber, style = MaterialTheme.typography.titleLarge); Detail(R.string.assigned_space, r.slotId, true); Detail(R.string.entry_hour, hour(r.entryHour)); Detail(R.string.owner_category, r.ownerCategory)
        checkout?.let { Secondary(R.string.check_out) { it(r.vehicleNumber) } }
    }
}
@Composable private fun FinderScreen(s: ParkingUiState, vm: ParkingViewModel, checkout: (String) -> Unit) {
    Screen(R.string.find_vehicle, R.string.finder_intro, footer = { Primary(R.string.find_action, s.find.loading, action = vm::find) }) {
        Field(R.string.vehicle_number, s.forms.searchVehicle, { vm.edit("searchVehicle", it) }, s.find.error?.fields?.get("vehicleNumber"), placeholder = R.string.vehicle_example)
        ErrorPanel(s.find.error, vm::find)
        s.forms.found?.let { Text(stringResource(R.string.recorded_hint), color = Palette.Muted); RecordCard(it, checkout) }
        if (s.forms.found == null && s.find.error == null && !s.find.loading) Panel { Text(stringResource(R.string.finder_empty), color = Palette.Muted) }
    }
}
@Composable private fun SummaryScreen(s: ParkingUiState, vm: ParkingViewModel) {
    Screen(R.string.daily_summary, R.string.summary_intro) {
        Field(R.string.reporting_date, s.forms.summaryDate, { vm.edit("summaryDate", it) }, s.summary.error?.fields?.get("date"), hint = R.string.date_hint)
        Secondary(R.string.load_summary, !s.summary.loading, vm::refreshSummary)
        if (s.summary.loading) Loading()
        ErrorPanel(s.summary.error, vm::refreshSummary)
        s.summary.value?.let { data ->
            Text(stringResource(R.string.summary_day, dateLabel(data.date)), style = MaterialTheme.typography.titleMedium)
            Panel { Detail(R.string.vehicles_today, data.summary.vehiclesParked.toString(), true); Detail(R.string.completed_checkouts, data.summary.completedCheckouts.toString(), true); Detail(R.string.fees_assessed, money(data.summary.feesAssessed), true) }
            Text(stringResource(R.string.completed_sessions), style = MaterialTheme.typography.titleLarge)
            val completed = data.records.filter { it.checkedOutAt?.let { time -> runCatching { Instant.parse(time).atZone(CampusZone).toLocalDate().toString() == data.date }.getOrDefault(false) } == true }
            if (completed.isEmpty()) Panel { Text(stringResource(R.string.no_sessions), color = Palette.Muted) }
            completed.forEach { r -> Panel { Text(r.vehicleNumber, style = MaterialTheme.typography.titleMedium); Detail(R.string.assigned_space, r.slotId); r.fee?.let { Detail(R.string.fees_assessed, money(it)) } } }
            Text(stringResource(R.string.not_payment), color = Palette.Muted)
        }
    }
}
@Composable private fun Rates() { Panel { Text(stringResource(R.string.rates_title), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.rates_detail), color = Palette.Muted) } }

````

## app/src/main/java/com/example/cpvts/ui/Theme.kt

````
package com.example.cpvts.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Palette {
    val Canvas = Color(0xFFF9F7F2); val Beige = Color(0xFFF4EFE6)
    val White = Color(0xFFFFFFFF); val Border = Color(0xFFE6DFD5)
    val Ink = Color(0xFF231F20); val Muted = Color(0xFF736B63)
    val Amber = Color(0xFFFFB300); val AmberTint = Color(0xFFFFF8E1)
    val Blue = Color(0xFF1E88E5); val BlueTint = Color(0xFFE3F2FD)
    val Green = Color(0xFF2E7D32); val GreenTint = Color(0xFFE8F5E9)
}
private val colors = lightColorScheme(
    primary = Palette.Amber, onPrimary = Palette.Ink, primaryContainer = Palette.AmberTint, onPrimaryContainer = Palette.Ink,
    secondary = Palette.Blue, onSecondary = Palette.Ink, secondaryContainer = Palette.BlueTint, onSecondaryContainer = Palette.Ink,
    tertiary = Palette.Green, onTertiary = Palette.White, tertiaryContainer = Palette.GreenTint, onTertiaryContainer = Palette.Green,
    background = Palette.Canvas, onBackground = Palette.Ink, surface = Palette.White, onSurface = Palette.Ink,
    surfaceVariant = Palette.Beige, onSurfaceVariant = Palette.Muted, outline = Palette.Muted, outlineVariant = Palette.Border,
    error = Palette.Ink, onError = Palette.White, errorContainer = Palette.AmberTint, onErrorContainer = Palette.Ink,
    inverseSurface = Palette.Ink, inverseOnSurface = Palette.White, inversePrimary = Palette.Amber,
    surfaceTint = Palette.White, scrim = Palette.Ink,
    surfaceDim = Palette.Beige, surfaceBright = Palette.White,
    surfaceContainerLowest = Palette.White, surfaceContainerLow = Palette.Canvas, surfaceContainer = Palette.Beige,
    surfaceContainerHigh = Palette.Beige, surfaceContainerHighest = Palette.Beige
)
@Composable fun CpvtsTheme(content: @Composable () -> Unit) {
    // User's explicit light-only palette overrides the generic brief's dark-mode request.
    MaterialTheme(colorScheme = colors, typography = Typography(), shapes = Shapes(
        small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp)
    ), content = content)
}

````

## app/src/main/res/drawable/ic_parking.xml

````
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="48dp" android:height="48dp" android:viewportWidth="48" android:viewportHeight="48">
    <path android:fillColor="#FFB300" android:pathData="M0,0h48v48h-48z"/>
    <path android:fillColor="#231F20" android:pathData="M16,36V12h10a8,8 0,0 1,0 16h-4v8zM22,22h4a2,2 0,0 0,0 -4h-4z"/>
</vector>

````

## app/src/main/res/values/strings.xml

````
<resources>
    <string name="app_name">CPVTS</string>
    <string name="brand_subtitle">CAMPUS PARKING</string>
    <string name="dashboard">Dashboard</string><string name="check_in">Check-in</string><string name="check_out">Check-out</string><string name="find_vehicle">Find vehicle</string><string name="daily_summary">Daily summary</string>
    <string name="nav_home">Home</string><string name="nav_in">In</string><string name="nav_out">Out</string><string name="nav_find">Find</string><string name="nav_today">Today</string>
    <string name="dashboard_intro">Every space, accounted for.</string><string name="entry_intro">Register an arrival. The server assigns the first available space.</string><string name="exit_intro">Close a parking session and receive its fee breakdown.</string><string name="finder_intro">Locate a vehicle currently parked on campus.</string><string name="summary_intro">Arrivals, departures and assessed fees.</string>
    <string name="refresh">Refresh</string><string name="retry">Try again</string><string name="loading">Loading parking data…</string><string name="working">Submitting…</string>
    <string name="total_spaces">Total spaces</string><string name="occupied">Occupied</string><string name="available">Available</string><string name="vehicles_today">Vehicles parked</string><string name="completed_checkouts">Completed check-outs</string><string name="fees_assessed">Fees assessed</string><string name="two_wheeler">Two-wheeler</string><string name="four_wheeler">Four-wheeler</string><string name="two_zone">Two-wheeler zone</string><string name="four_zone">Four-wheeler zone</string>
    <string name="currently_parked">Currently parked</string><string name="empty_campus">Ready for the next arrival</string><string name="empty_campus_detail">Checked-in vehicles appear here with their assigned spaces.</string><string name="last_known">Last known figures. Refresh to confirm the current status.</string>
    <string name="vehicle_number">Vehicle number</string><string name="vehicle_example">e.g. KA-22-AB-1234</string><string name="vehicle_type">Vehicle type</string><string name="owner_category">Owner category</string><string name="staff">Staff</string><string name="student">Student</string><string name="visitor">Visitor</string><string name="entry_hour">Entry hour</string><string name="exit_hour">Exit hour</string><string name="hour_hint">Whole hours, 0–23 (IST)</string><string name="vehicle_hint">Letters, numbers, spaces and hyphens accepted.</string>
    <string name="check_in_vehicle">Check in vehicle</string><string name="create_receipt">Check out and create receipt</string><string name="first_available">The first available space in the selected zone is assigned automatically.</string><string name="entry_confirmed">Check-in confirmed</string><string name="checkout_confirmed">Check-out confirmed</string><string name="entry_result">%1$s · Space %2$s · Entry %3$s</string>
    <string name="lookup_by">Look up by</string><string name="slot_id">Space ID</string><string name="slot_example">e.g. T-01</string><string name="no_overnight">Exit hour cannot be earlier than entry hour. Overnight rollover is not supported.</string>
    <string name="receipt">Parking receipt</string><string name="receipt_empty">Your receipt will appear here after a confirmed check-out.</string><string name="receipt_number">Receipt reference</string><string name="assigned_space">Assigned space</string><string name="status">Status</string><string name="duration">Duration</string><string name="hours_value">%1$d hours</string><string name="free_hours">Free hours</string><string name="billable_hours">Billable hours</string><string name="hourly_rate">Rate per hour</string><string name="amount_due">Total amount due</string><string name="space_released">The server released space %1$s at this check-out.</string><string name="not_payment">Fee assessed. This receipt does not confirm payment.</string>
    <string name="find_action">Find vehicle</string><string name="finder_empty">Enter a full vehicle number to see its space, entry hour and status.</string><string name="recorded_status">Last confirmed status</string><string name="recorded_hint">This is the last server result. Refresh before acting on it.</string>
    <string name="reporting_date">Reporting date</string><string name="date_hint">YYYY-MM-DD · India Standard Time</string><string name="load_summary">Load summary</string><string name="completed_sessions">Completed parking sessions</string><string name="no_sessions">No completed check-outs for this date.</string><string name="summary_day">Reporting day: %1$s (IST)</string>
    <string name="error_validation">Check the highlighted fields and the server message.</string><string name="error_full">Parking full in the selected vehicle zone.</string><string name="error_duplicate">This vehicle is already parked. Use Vehicle Finder to check its space.</string><string name="error_not_found">No active parking session matches this vehicle or space.</string><string name="error_database">The parking database is unavailable. Try refreshing.</string><string name="error_timeout">The request timed out. Check your connection and try again.</string><string name="error_offline">Cannot reach the parking API. Check your connection and server address.</string><string name="error_unknown">The result of this submission is unknown. It may have completed. Check Vehicle Finder or the Dashboard before submitting again.</string><string name="error_config">Set the CPVTS API address in the project configuration, then rebuild the app. A Supabase URL is not the CPVTS API address.</string><string name="error_unexpected">The server returned an unexpected response. Refresh before continuing.</string>
    <string name="verify_finder">Open Vehicle Finder</string><string name="verify_dashboard">Open Dashboard</string><string name="verified_outcome">I checked the current status</string>
    <string name="invalid_vehicle">Use 2–20 letters and numbers, with optional spaces or hyphens.</string><string name="invalid_hour">Enter an integer from 0 through 23.</string><string name="invalid_slot">Enter a space ID such as T-01 or F-01.</string><string name="invalid_date">Enter a valid date in YYYY-MM-DD format.</string>
    <string name="rates_title">Parking rates</string><string name="rates_detail">Staff: free. Students: first 2 hours free, then ₹10/hour for two-wheelers or ₹20/hour for four-wheelers. Visitors: ₹20/hour or ₹40/hour respectively. Final fees come from the server.</string>
</resources>

````

## app/src/main/res/values/styles.xml

````
<resources><style name="Theme.CPVTS" parent="android:style/Theme.Material.Light.NoActionBar">
    <item name="android:fontFamily">sans</item><item name="android:windowLightStatusBar">true</item>
    <item name="android:windowLightNavigationBar">true</item><item name="android:windowActionModeOverlay">true</item>
    <item name="android:colorAccent">#FFB300</item><item name="android:windowBackground">#F9F7F2</item>
    <item name="android:forceDarkAllowed">false</item>
</style></resources>

````

## app/src/test/java/com/example/cpvts/HttpContractTest.kt

````
package com.example.cpvts

import com.example.cpvts.data.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class HttpContractTest {
    private lateinit var server: MockWebServer
    private lateinit var repo: ParkingRepository
    private lateinit var api: ParkingApi
    @Before fun setup() {
        server = MockWebServer(); server.start()
        api = Retrofit.Builder().baseUrl(server.url("/api/v1/")).client(parkingHttpClient())
            .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType())).build().create(ParkingApi::class.java)
        repo = ApiParkingRepository(api)
    }
    @After fun close() { server.shutdown() }
    private fun enqueueRecord() { server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(AppJson.encodeToString(Envelope(receipt)))) }
    @Test fun checkoutJsonOmitsUnusedIdentifierAndSendsNoAuth() = runTest {
        enqueueRecord()
        repo.checkOut(CheckOutRequest(12, slotId = "T-01"))
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("/api/v1/check-outs", request.path)
        val fields = AppJson.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(setOf("exitHour", "slotId"), fields.keys)
        assertEquals("T-01", fields["slotId"]!!.jsonPrimitive.content)
        assertNull(request.getHeader("Authorization"))
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
    }
    @Test fun checkInFieldsAndPathMatchContract() = runTest {
        enqueueRecord()
        repo.checkIn(CheckInRequest("ka-01-ab1234", "two_wheeler", "Student", 9))
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("/api/v1/check-ins", request.path)
        assertEquals(AppJson.parseToJsonElement("""{"vehicleNumber":"KA01AB1234","vehicleType":"two_wheeler","ownerCategory":"Student","entryHour":9}"""), AppJson.parseToJsonElement(request.body.readUtf8()))
    }
    @Test fun allReadPathsMatchContract() = runTest {
        server.enqueue(MockResponse().setBody(AppJson.encodeToString(Envelope(snapshot))))
        repo.dashboard(); assertEquals("/api/v1/dashboard", server.takeRequest().path)
        server.enqueue(MockResponse().setBody(AppJson.encodeToString(Envelope(snapshot))))
        repo.summary("2026-10-09"); assertEquals("/api/v1/summary?date=2026-10-09", server.takeRequest().path)
        enqueueRecord(); repo.find("ka-01-ab1234"); assertEquals("/api/v1/vehicles/KA01AB1234", server.takeRequest().path)
        server.enqueue(MockResponse().setBody("""{"data":[],"meta":{"mode":"local"}}"""))
        api.slots(); assertEquals("/api/v1/slots", server.takeRequest().path)
        server.enqueue(MockResponse().setBody("""{"data":{"status":"ready","mode":"local"}}"""))
        api.health(); assertEquals("/api/v1/health", server.takeRequest().path)
    }
    @Test fun retryAfterZeroDoesNotRepeatWrite() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "0").setBody("{}"))
        enqueueRecord()
        val result = repo.checkIn(CheckInRequest("KA01AB1234", "two_wheeler", "Student", 9))
        assertEquals(ErrorKind.UnknownOutcome, (result as ApiResult.Failure).error.kind)
        assertEquals(1, server.requestCount)
    }
    @Test fun redirectsDoNotResubmitWrite() = runTest {
        server.enqueue(MockResponse().setResponseCode(307).setHeader("Location", server.url("/other")))
        enqueueRecord()
        assertTrue(repo.checkOut(CheckOutRequest(12, slotId = "T-01")) is ApiResult.Failure)
        assertEquals(1, server.requestCount)
    }
    @Test fun malformedSuccessfulWriteIsUnknownOutcome() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        assertEquals(ErrorKind.UnknownOutcome, (repo.checkOut(CheckOutRequest(12, slotId = "T-01")) as ApiResult.Failure).error.kind)
    }
}

````

## app/src/test/java/com/example/cpvts/ParkingViewModelTest.kt

````
package com.example.cpvts

import androidx.lifecycle.SavedStateHandle
import com.example.cpvts.data.*
import com.example.cpvts.ui.ParkingViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

internal class FakeRepository : ParkingRepository {
    var entryCalls = 0
    var exitCalls = 0
    var lastExit: CheckOutRequest? = null
    var entryResult: ApiResult<ParkingRecord> = ApiResult.Success(parked)
    var exitResult: ApiResult<ParkingRecord> = ApiResult.Success(receipt)
    var pending: CompletableDeferred<ApiResult<ParkingRecord>>? = null
    override suspend fun dashboard() = ApiResult.Success(snapshot)
    override suspend fun summary(date: String) = ApiResult.Success(snapshot.copy(date = date))
    override suspend fun find(number: String) = ApiResult.Success(parked)
    override suspend fun checkIn(request: CheckInRequest): ApiResult<ParkingRecord> { entryCalls++; return pending?.await() ?: entryResult }
    override suspend fun checkOut(request: CheckOutRequest): ApiResult<ParkingRecord> { exitCalls++; lastExit = request; return exitResult }
}
@OptIn(ExperimentalCoroutinesApi::class)
class ParkingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun validEntry(vm: ParkingViewModel) { vm.edit("entryVehicle", " ka01ab1234 "); vm.edit("entryHour", "9") }
    @Test fun invalidFormsNeverReachRepository() = runTest {
        val repo = FakeRepository(); val vm = ParkingViewModel(repo, SavedStateHandle())
        vm.edit("entryVehicle", "?"); vm.edit("entryHour", "24"); vm.checkIn()
        vm.edit("exitIdentifier", ""); vm.edit("exitHour", "-1"); vm.checkOut(); advanceUntilIdle()
        assertEquals(0, repo.entryCalls); assertEquals(0, repo.exitCalls)
        assertEquals(setOf("vehicleNumber", "entryHour"), vm.state.value.checkIn.error!!.fields.keys)
        assertEquals(ErrorKind.Validation, vm.state.value.checkOut.error!!.kind)
    }
    @Test fun duplicateTapsSendOneRequestAndSuccessSurvivesRecreation() = runTest {
        val repo = FakeRepository().apply { pending = CompletableDeferred() }
        val saved = SavedStateHandle(); val vm = ParkingViewModel(repo, saved); validEntry(vm)
        vm.checkIn(); vm.checkIn(); runCurrent()
        assertEquals(1, repo.entryCalls); assertTrue(vm.state.value.checkIn.loading)
        repo.pending!!.complete(ApiResult.Success(parked)); advanceUntilIdle()
        assertEquals(parked, vm.state.value.forms.checkInResult)
        val restored = ParkingViewModel(repo, SavedStateHandle(mapOf("forms" to saved.get<String>("forms"))))
        advanceUntilIdle()
        assertEquals(parked, restored.state.value.forms.checkInResult)
        assertEquals("KA01AB1234", restored.state.value.forms.entryVehicle)
        assertEquals(1, repo.entryCalls)
    }
    @Test fun uncertainWriteRequiresExplicitAcknowledgment() = runTest {
        val repo = FakeRepository().apply { entryResult = ApiResult.Failure(UiError(ErrorKind.UnknownOutcome)) }
        val vm = ParkingViewModel(repo, SavedStateHandle()); validEntry(vm)
        vm.checkIn(); advanceUntilIdle(); vm.checkIn(); advanceUntilIdle()
        assertEquals(1, repo.entryCalls); assertTrue(vm.state.value.forms.checkInUncertain)
        vm.acknowledgeOutcome(true); vm.checkIn(); advanceUntilIdle(); assertEquals(2, repo.entryCalls)
    }
    @Test fun interruptedWriteRestoresAsUnknownAndIsNotReplayed() = runTest {
        val repo = FakeRepository().apply { pending = CompletableDeferred() }
        val saved = SavedStateHandle(); val vm = ParkingViewModel(repo, saved); validEntry(vm); vm.checkIn(); runCurrent()
        val restored = ParkingViewModel(repo, SavedStateHandle(mapOf("forms" to saved.get<String>("forms"))))
        restored.checkIn(); runCurrent()
        assertFalse(restored.state.value.checkIn.loading)
        assertEquals(ErrorKind.UnknownOutcome, restored.state.value.checkIn.error!!.kind)
        assertEquals(1, repo.entryCalls)
        repo.pending!!.complete(ApiResult.Failure(UiError(ErrorKind.UnknownOutcome))); advanceUntilIdle()
    }
    @Test fun slotCheckoutUsesExactlyOneIdentifierAndServerReceipt() = runTest {
        val repo = FakeRepository(); val saved = SavedStateHandle(); val vm = ParkingViewModel(repo, saved)
        vm.exitMode(true); vm.edit("exitIdentifier", "t-01"); vm.edit("exitHour", "12"); vm.checkOut(); advanceUntilIdle()
        assertEquals(CheckOutRequest(12, slotId = "T-01"), repo.lastExit)
        assertEquals(receipt, vm.state.value.forms.receipt)
        val restored = ParkingViewModel(repo, SavedStateHandle(mapOf("forms" to saved.get<String>("forms"))))
        advanceUntilIdle(); assertEquals(receipt, restored.state.value.forms.receipt); assertEquals(1, repo.exitCalls)
    }
    @Test fun fullZoneIsShownWithoutLosingInput() = runTest {
        val repo = FakeRepository().apply { entryResult = ApiResult.Failure(UiError(ErrorKind.FullZone)) }
        val vm = ParkingViewModel(repo, SavedStateHandle()); validEntry(vm); vm.checkIn(); advanceUntilIdle()
        assertEquals(ErrorKind.FullZone, vm.state.value.checkIn.error!!.kind)
        assertEquals("KA01AB1234", vm.state.value.forms.entryVehicle)
        assertFalse(vm.state.value.forms.checkInUncertain); assertNull(vm.state.value.forms.checkInResult)
    }
}

````

## app/src/test/java/com/example/cpvts/RepositoryTest.kt

````
package com.example.cpvts

import com.example.cpvts.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

internal val parked = ParkingRecord("record-1", "KA01AB1234", "two_wheeler", "Student", "T-01", 9,
    checkedInAt = "2026-10-09T03:30:00.000Z", status = "Parked")
internal val receipt = parked.copy(exitHour = 12, checkedOutAt = "2026-10-09T06:30:00.000Z", status = "Completed",
    duration = 3, fee = 10, calculation = Calculation(2, 1, 10))
internal val snapshot = Snapshot("2026-10-09", "Asia/Kolkata", emptyList(), listOf(parked), Summary(1, 0, 0),
    listOf(Zone("two_wheeler", 10, 1, 9), Zone("four_wheeler", 5, 0, 5)))

internal class FakeApi : ParkingApi {
    var response: Response<Envelope<ParkingRecord>> = Response.success(Envelope(parked))
    var failure: Exception? = null
    var calls = 0
    var entry: CheckInRequest? = null
    var exit: CheckOutRequest? = null
    var query: String? = null
    private fun result(): Response<Envelope<ParkingRecord>> { calls++; failure?.let { throw it }; return response }
    override suspend fun health() = Response.success(Envelope(Health("ready", "local")))
    override suspend fun dashboard() = Response.success(Envelope(snapshot))
    override suspend fun slots() = Response.success(Envelope(emptyList<Slot>()))
    override suspend fun checkIn(request: CheckInRequest): Response<Envelope<ParkingRecord>> { entry = request; return result() }
    override suspend fun checkOut(request: CheckOutRequest): Response<Envelope<ParkingRecord>> { exit = request; return result() }
    override suspend fun find(vehicleNumber: String): Response<Envelope<ParkingRecord>> { query = vehicleNumber; return result() }
    override suspend fun summary(date: String?) = Response.success(Envelope(snapshot))
    fun error(status: Int, code: String, details: List<FieldIssue> = emptyList()) {
        response = Response.error(status, AppJson.encodeToString(ErrorEnvelope(ServerError(code, "Server explanation", details))).toResponseBody("application/json".toMediaType()))
    }
}
class RepositoryTest {
    private val input = CheckInRequest(" ka-01 ab1234 ", "two_wheeler", "Student", 9)
    @Test fun checkInReturnsServerSlotAndNormalizesVehicle() = runTest {
        val api = FakeApi()
        val result = ApiParkingRepository(api).checkIn(input) as ApiResult.Success
        assertEquals(parked, result.value)
        assertEquals("KA01AB1234", api.entry!!.vehicleNumber)
        assertEquals(1, api.calls)
    }
    @Test fun fullZoneIsTypedAndNeverRetried() = runTest {
        val api = FakeApi().apply { error(409, "PARKING_FULL") }
        assertEquals(ErrorKind.FullZone, (ApiParkingRepository(api).checkIn(input) as ApiResult.Failure).error.kind)
        assertEquals(1, api.calls)
    }
    @Test fun duplicateIsTyped() = runTest {
        val api = FakeApi().apply { error(409, "DUPLICATE_VEHICLE") }
        assertEquals(ErrorKind.Duplicate, (ApiParkingRepository(api).checkIn(input) as ApiResult.Failure).error.kind)
    }
    @Test fun checkoutPreservesServerFeeAndOnlyOneIdentifier() = runTest {
        val api = FakeApi().apply { response = Response.success(Envelope(receipt)) }
        assertEquals(receipt, (ApiParkingRepository(api).checkOut(CheckOutRequest(12, slotId = " t-01 ")) as ApiResult.Success).value)
        assertEquals(CheckOutRequest(12, slotId = "T-01"), api.exit)
    }
    @Test fun notFoundAndServerValidationArePreserved() = runTest {
        val api = FakeApi().apply { error(404, "VEHICLE_NOT_FOUND") }
        val repo = ApiParkingRepository(api)
        assertEquals(ErrorKind.NotFound, (repo.find(" ka-01 ab1234 ") as ApiResult.Failure).error.kind)
        assertEquals("KA01AB1234", api.query)
        api.error(422, "EXIT_BEFORE_ENTRY", listOf(FieldIssue("exitHour", "Must follow entry hour")))
        val error = (repo.checkOut(CheckOutRequest(8, vehicleNumber = "KA01AB1234")) as ApiResult.Failure).error
        assertEquals(ErrorKind.Validation, error.kind)
        assertEquals("Server explanation", error.serverMessage)
        assertEquals("Must follow entry hour", error.fields["exitHour"])
    }
    @Test fun failedReadsAreOfflineButFailedWritesHaveUnknownOutcome() = runTest {
        val api = FakeApi().apply { failure = IOException("sensitive internal detail") }
        val repo = ApiParkingRepository(api)
        assertEquals(UiError(ErrorKind.Offline), (repo.find("KA01AB1234") as ApiResult.Failure).error)
        assertEquals(UiError(ErrorKind.UnknownOutcome), (repo.checkIn(input) as ApiResult.Failure).error)
        assertEquals(2, api.calls)
    }
    @Test fun readTimeoutAndWriteTimeoutAreDifferent() = runTest {
        val api = FakeApi().apply { failure = SocketTimeoutException() }
        val repo = ApiParkingRepository(api)
        assertEquals(ErrorKind.Timeout, (repo.find("KA01AB1234") as ApiResult.Failure).error.kind)
        assertEquals(ErrorKind.UnknownOutcome, (repo.checkOut(CheckOutRequest(12, slotId = "T-01")) as ApiResult.Failure).error.kind)
    }
    @Test fun unconfiguredClientNeverCallsApi() = runTest {
        val api = FakeApi()
        assertEquals(ErrorKind.Configuration, (ApiParkingRepository(api, false).checkIn(input) as ApiResult.Failure).error.kind)
        assertEquals(0, api.calls)
    }
    @Test fun cancellationPropagates() = runTest {
        val api = FakeApi().apply { failure = CancellationException() }
        try { ApiParkingRepository(api).find("KA01AB1234"); fail("Cancellation must propagate") } catch (_: CancellationException) { }
    }
}

````

## build.gradle.kts

````
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

````

## docs/api-contract.md

````
# CPVTS REST API v1

Base URL: `http://127.0.0.1:5173/api/v1` locally. Android devices must use the Node server's reachable LAN address, not the device's loopback. Supabase database access (Session pooler or optional Data API) is a private implementation detail; mobile and CLI clients consume this Node API. All responses and request bodies are JSON. POST requests require `Content-Type: application/json`. No authentication protocol is defined for this local/trusted-network version. Do not embed a database password or Supabase server key in any client.

## Conventions

- Success envelope: `{"data": ...}`. Dashboard/summary also return `"meta":{"mode":"local"|"supabase"}`.
- Errors: `{"error":{"code":"CODE","message":"Readable description","details":[{"field":"entryHour","message":"..."}]}}`. `details` is present for field validation errors only.
- Vehicle type: `two_wheeler` or `four_wheeler`. Owner category: `Staff`, `Student`, `Visitor` (case-sensitive).
- Hours: JSON integers 0–23, never strings. No overnight rollover.
- Vehicle numbers: normalized uppercase with spaces/hyphens removed, 2–20 ASCII letters/numbers. Lookups use the same normalization.
- Slot IDs: `T-01`…`T-10` and `F-01`…`F-05`, case-insensitive input. Clients cannot choose a slot at check-in.
- Fees: integer Indian rupees, assessed rather than paid. Timestamps: ISO 8601 with timezone. Dates: valid `YYYY-MM-DD`, interpreted in `Asia/Kolkata`.
- POST bodies reject unknown fields. Request body limit: 16 KB. Reads may be safely retried. After an ambiguous mutation timeout, read back state before retrying.

## Endpoints

| Method | Path                       | Result                                                               |
| ------ | -------------------------- | -------------------------------------------------------------------- |
| GET    | `/health`                  | Checks database RPC availability; `{status:"ready",mode:"local"}`    |
| GET    | `/dashboard`               | Current slot grid, active/day records, date, timezone, daily summary |
| GET    | `/slots`                   | Array of every slot and its active record, if occupied               |
| POST   | `/check-ins`               | Creates a session; HTTP 201 and the parking record                   |
| POST   | `/check-outs`              | Completes a session; HTTP 200 and its receipt/record                 |
| GET    | `/vehicles/:vehicleNumber` | Active record for the encoded vehicle number, or 404                 |
| GET    | `/summary?date=2026-10-09` | Same snapshot shape for selected day; defaults to today in IST       |

### Check-in

```json
{
  "vehicleNumber": "KA-22-XY-9876",
  "vehicleType": "two_wheeler",
  "ownerCategory": "Student",
  "entryHour": 9
}
```

HTTP 201:

```json
{
  "data": {
    "id": "00000000-0000-4000-8000-000000000001",
    "vehicleNumber": "KA22XY9876",
    "vehicleType": "two_wheeler",
    "ownerCategory": "Student",
    "slotId": "T-01",
    "entryHour": 9,
    "exitHour": null,
    "checkedInAt": "2026-10-09T03:30:00+00:00",
    "checkedOutAt": null,
    "status": "Parked",
    "duration": null,
    "fee": null,
    "calculation": null
  }
}
```

The first available slot in the matching zone is assigned transactionally. Duplicate active vehicles return 409 `DUPLICATE_VEHICLE`; a full matching zone returns 409 `PARKING_FULL`. Completed vehicles may enter again.

### Check-out

Provide **exactly one** identifier:

```json
{ "slotId": "T-01", "exitHour": 13 }
```

or `{"vehicleNumber":"KA-22-XY-9876","exitHour":13}`. Both/neither is a 400 error, even if they identify the same vehicle.

HTTP 200:

```json
{
  "data": {
    "id": "00000000-0000-4000-8000-000000000001",
    "vehicleNumber": "KA22XY9876",
    "vehicleType": "two_wheeler",
    "ownerCategory": "Student",
    "slotId": "T-01",
    "entryHour": 9,
    "exitHour": 13,
    "checkedInAt": "2026-10-09T03:30:00+00:00",
    "checkedOutAt": "2026-10-09T07:30:00+00:00",
    "status": "Completed",
    "duration": 4,
    "fee": 20,
    "calculation": { "freeHours": 2, "billableHours": 2, "ratePerHour": 10 }
  }
}
```

The slot becomes available only on successful completion. Same-hour exits have zero duration. Staff: ₹0. Student: first two hours free, then ₹10 (two-wheeler) / ₹20 (four-wheeler). Visitor: ₹20 / ₹40 for every hour. Repeated checkout returns 404; it does not create a second receipt or fee.

### Dashboard, slots and summary

Example abbreviated snapshot (the real response contains all 15 slots):

```json
{
  "data": {
    "date": "2026-10-09",
    "timezone": "Asia/Kolkata",
    "slots": [
      {
        "id": "T-01",
        "vehicleType": "two_wheeler",
        "ordinal": 1,
        "status": "Available",
        "record": null
      }
    ],
    "records": [],
    "summary": {
      "vehiclesParked": 0,
      "completedCheckouts": 0,
      "feesAssessed": 0
    }
  },
  "meta": { "mode": "local" }
}
```

For an occupied slot, `record` is the active parking record. A record has all fields shown above, with checkout/fee/calculation fields null while active. `slots` always reflects **current** occupancy, including for historical summaries. `records` includes all active records and records whose entry or exit date matches the reporting day. Filter records by the desired timestamp for day-specific tables. Summary counts already have the correct date filtering.

The snapshot also includes `zones`, omitted from the abbreviated example above: `[{"vehicleType":"two_wheeler","total":10,"occupied":0,"vacant":10},{"vehicleType":"four_wheeler","total":5,"occupied":0,"vacant":5}]`. These authoritative current zone counts match the slot grid.

`GET /slots` returns just the `slots` array in the `data` envelope. `GET /vehicles/KA-22-XY-9876` returns the active record in `data`, with slot ID, entry hour and status. It returns 404 after checkout or for an unknown vehicle; it does not search historical records.

## Errors

| HTTP | Code                      | Meaning                                                       |
| ---- | ------------------------- | ------------------------------------------------------------- |
| 400  | `VALIDATION_ERROR`        | Invalid enum, date, number, identifier, hour or body shape    |
| 400  | `INVALID_JSON`            | Malformed JSON request body                                   |
| 403  | `ORIGIN_NOT_ALLOWED`      | Browser mutation originated outside this app                  |
| 404  | `VEHICLE_NOT_FOUND`       | No matching active session                                    |
| 404  | `NOT_FOUND`               | Unknown API route                                             |
| 409  | `DUPLICATE_VEHICLE`       | Vehicle already has an active session                         |
| 409  | `PARKING_FULL`            | Matching vehicle zone is full                                 |
| 413  | `REQUEST_TOO_LARGE`       | Payload exceeds 16 KB                                         |
| 422  | `EXIT_BEFORE_ENTRY`       | Exit precedes entry; record remains occupied                  |
| 503  | `DATABASE_NOT_CONFIGURED` | Database URL or optional Data API server configuration missing/invalid |
| 503  | `DATABASE_UNAVAILABLE`    | Database/network/schema error; refresh before retrying writes |

Example: `{"error":{"code":"EXIT_BEFORE_ENTRY","message":"Exit hour cannot be earlier than entry hour. Overnight parking is not supported."}}`.

````

## docs/verification.md

````
# Verification and integration checklist

## Executed checks

Verification was performed on Windows with JDK 17, Gradle 9.6.0, AGP 9.4.1, SDK Platform 37.2 and Build Tools 37.0.0.

- Gradle wrapper generation and distribution checksum verification: passed.
- Kotlin application compilation (`compileDebugKotlin`) and generated Java compilation: passed.
- Debug manifest merge: passed. The app requests only `INTERNET` and `ACCESS_NETWORK_STATE`; AndroidX's unused dynamic-receiver permission is explicitly removed because the app registers no dynamic broadcast receivers.
- Debug signing validation: passed using an isolated local Android preferences directory. No signing key is distributed.
- `assembleDebug`: **not completed**. Both the AGP-bundled AAPT2 and SDK Build Tools AAPT2 crash during resource linking. A standalone minimal manifest, containing no app resources, also reproduces native exit `-1073741819` (`0xC0000005`) against both SDK 37.2 and the installed SDK 36. This is a tool/runtime failure observed in this environment; it does not establish that APK assembly works on another machine. No APK is supplied or claimed to be verified.
- The compiled app classes were exercised by an isolated JVM JUnit harness: **21 tests passed** (`RepositoryTest`, `HttpContractTest`, `ParkingViewModelTest`). The normal Gradle unit-test task could not complete because AGP schedules resource processing before its test-class bundling task, and that resource step is the AAPT2 failure above.
- Lint analysis reached the app and reports the existing untracked `local.properties` Windows path formatting error plus Android API warnings for platform theme attributes; the distributable project excludes `local.properties`. Dependency-update notices are informational. Lint did not produce a clean final report because resource processing crashed.

Validation redirected Gradle/Android caches and Java temporary files into the workspace. Kotlin compilation used `-Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false` because the sandbox's compiler-daemon temporary files were not reliable across processes. These environment-specific flags are not forced on the distributed project. Build commands in the README use the normal Gradle workflow.

The Android contract was compared with `docs/api-contract.md` in the existing CPVTS web project and the user's copy under `D:\ambit\cpvts`. The migration in that copy has the same SHA-256 as the migration already applied to Supabase earlier in this work: `598b26dc6dbfcb3e990a4aa62ac1f3f6c420be5971faa91d46e7f222cf06d4cf`. No schema change is required for this Android client.

## Tests included

| Suite | Coverage |
| --- | --- |
| `RepositoryTest` | Confirmed check-in/checkout values; normalized vehicle and slot; full zone; duplicate; not found; server field validation; read vs write network failures/timeouts; unconfigured API; cancellation |
| `HttpContractTest` | Retrofit paths and request JSON; unused checkout identifier omitted; JSON content type; no auth header; all read routes; no POST retry after 503/Retry-After; no redirect replay; malformed success body |
| `ParkingViewModelTest` | Invalid forms block calls; double taps submit once; input/result/receipt restoration; no replay of interrupted writes; explicit verification after unknown result; slot identifier exclusivity; full-zone input preservation |

The tests use isolated fake/mock data and do not create records in Supabase. Tests that reconstruct SavedStateHandle verify the application's restoration logic; they do not replace on-device Android process recreation testing.

## Device and API checks still to perform

Run these after configuring an API URL reachable from the target device. Use a test backend/database or coordinate test vehicle identifiers with the parking operator.

1. Launch with the API available; compare both zone counts with the web dashboard. Refresh while the web app checks a vehicle in/out and confirm shared state.
2. Check in a student two-wheeler; verify the returned assigned space matches the web record. Try the same active vehicle again and verify the duplicate response. In an isolated test dataset, fill a zone and verify the full-zone response.
3. Check out using the vehicle number, then a different test session using space ID. Verify server duration, free hours, rate and fee, and the released space on the web dashboard. Try exit before entry, missing vehicle and out-of-range hour.
4. Search for a parked vehicle, then for its number after checkout. Verify active/not-found behavior and the prefilled check-out shortcut.
5. Compare the summary with the web app for today and another day, including sessions whose entry and checkout fall on different IST calendar dates. Fees must be assessed values, not payment confirmations.
6. Disable connectivity for a read. Restore it and use the retry action. Interrupt a write in a controlled test backend: confirm unknown-outcome wording, disabled resubmission, verification navigation and no automatic replay.
7. Rotate each screen with a partly completed form and after receiving a result. Put the app in the background, terminate its process using Android Studio/ADB, then restore its retained task. Verify saved input/results and interrupted-write handling. Do not confuse force-stop or clearing app data with normal saved-state recreation.
8. Use a small phone in portrait/landscape and a tablet. Set font size to 200%, enable TalkBack and open the keyboard in each form. Check all fields, long errors, receipt text, navigation labels/roles, announced results, scroll reachability and the submit action above the keyboard.
9. Check Android API 26 and the current target platform. On Android 17, use a controlled HTTPS endpoint/tunnel rather than direct LAN access because the specified two-permission limit excludes the new local-network runtime permission. For release, verify its certificate chain and confirm HTTP URLs fail configuration. Configure your signing key only when preparing your own release.

## Scope limits

No development or production CPVTS API URL was supplied. Placeholder builds intentionally show a configuration state instead of mock parking data. No emulator session, physical-device session, Android-to-live-API request, TalkBack session or screenshot-based layout review has been performed. No app store publication or deployment is part of this deliverable.

````

## gradle.properties

````
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official

````

## gradle/libs.versions.toml

````
[versions]
agp = "9.4.1"
kotlin = "2.2.10"
composeBom = "2025.08.01"
activity = "1.10.1"
lifecycle = "2.9.2"
navigation = "2.9.3"
retrofit = "3.0.0"
okhttp = "4.12.0"
serialization = "1.9.0"
coroutines = "1.10.2"
junit = "4.13.2"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-icons = { module = "androidx.compose.material:material-icons-core" }
activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity" }
lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-savedstate = { module = "androidx.lifecycle:lifecycle-viewmodel-savedstate", version.ref = "lifecycle" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-serialization = { module = "com.squareup.retrofit2:converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "okhttp" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }

````

## gradle/wrapper/gradle-wrapper.jar

Binary file included in the source archive. SHA-256: 497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7

## gradle/wrapper/gradle-wrapper.properties

````
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionSha256Sum=bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01
distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.0-bin.zip
networkTimeout=10000
retries=0
retryBackOffMs=500
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

````

## gradlew

````
#!/bin/sh

#
# Copyright © 2015 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

##############################################################################
#
#   gradlew start up script for POSIX generated by Gradle.
#
#   Important for running:
#
#   (1) You need a POSIX-compliant shell to run this script. If your /bin/sh is
#       noncompliant, but you have some other compliant shell such as ksh or
#       bash, then to run this script, type that shell name before the whole
#       command line, like:
#
#           ksh gradlew
#
#       Busybox and similar reduced shells will NOT work, because this script
#       requires all of these POSIX shell features:
#         * functions;
#         * expansions «$var», «${var}», «${var:-default}», «${var+SET}»,
#           «${var#prefix}», «${var%suffix}», and «$( cmd )»;
#         * compound commands having a testable exit status, especially «case»;
#         * various built-in commands including «command», «set», and «ulimit».
#
#   Important for patching:
#
#   (2) This script targets any POSIX shell, so it avoids extensions provided
#       by Bash, Ksh, etc; in particular arrays are avoided.
#
#       The "traditional" practice of packing multiple parameters into a
#       space-separated string is a well documented source of bugs and security
#       problems, so this is (mostly) avoided, by progressively accumulating
#       options in "$@", and eventually passing that to Java.
#
#       Where the inherited environment variables (DEFAULT_JVM_OPTS, JAVA_OPTS,
#       and GRADLE_OPTS) rely on word-splitting, this is performed explicitly;
#       see the in-line comments for details.
#
#       There are tweaks for specific operating systems such as AIX, CygWin,
#       Darwin, MinGW, and NonStop.
#
#   (3) This script is generated from the Groovy template
#       https://github.com/gradle/gradle/blob/3d91ce3b8caaf77ad09f381f43615b715b53f72c/platforms/jvm/plugins-application/src/main/resources/org/gradle/api/internal/plugins/unixStartScript.txt
#       within the Gradle project.
#
#       You can find Gradle at https://github.com/gradle/gradle/.
#
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# This is normally unused
# shellcheck disable=SC2034
APP_BASE_NAME=${0##*/}
# Discard cd standard output in case $CDPATH is set (https://github.com/gradle/gradle/issues/25036)
APP_HOME=$( cd -P "${APP_HOME:-./}" > /dev/null && printf '%s\n' "$PWD" ) || exit

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in                #(
  CYGWIN* )         cygwin=true  ;; #(
  Darwin* )         darwin=true  ;; #(
  MSYS* | MINGW* )  msys=true    ;; #(
  NONSTOP* )        nonstop=true ;;
esac



# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptors if we can.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      max*)
        # In POSIX sh, ulimit -H is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        MAX_FD=$( ulimit -H -n ) ||
            warn "Could not query maximum file descriptor limit"
    esac
    case $MAX_FD in  #(
      '' | soft) :;; #(
      *)
        # In POSIX sh, ulimit -n is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
    esac
fi

# Collect all arguments for the java command, stacking in reverse order:
#   * args from the command line
#   * the main class name
#   * -classpath
#   * -D...appname settings
#   * --module-path (only if needed)
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and GRADLE_OPTS environment variables.

# For Cygwin or MSYS, switch paths to Windows format before running java
if "$cygwin" || "$msys" ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )

    JAVACMD=$( cygpath --unix "$JAVACMD" )

    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    for arg do
        if
            case $arg in                                #(
              -*)   false ;;                            # don't mess with options #(
              /?*)  t=${arg#/} t=/${t%%/*}              # looks like a POSIX filepath
                    [ -e "$t" ] ;;                      #(
              *)    false ;;
            esac
        then
            arg=$( cygpath --path --ignore --mixed "$arg" )
        fi
        # Roll the args list around exactly as many times as the number of
        # args, so each arg winds up back in the position where it started, but
        # possibly modified.
        #
        # NB: a `for` loop captures its iteration list before it begins, so
        # changing the positional parameters here affects neither the number of
        # iterations, nor the values presented in `arg`.
        shift                   # remove old arg
        set -- "$@" "$arg"      # push replacement arg
    done
fi


# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Collect all arguments for the java command:
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and optsEnvironmentVar are not allowed to contain shell fragments,
#     and any embedded shellness will be escaped.
#   * For example: A user cannot expect ${Hostname} to be expanded, as it is an environment variable and will be
#     treated as '${Hostname}' itself on the command line.

set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
        "$@"

# Stop when "xargs" is not available.
if ! command -v xargs >/dev/null 2>&1
then
    die "xargs is not available"
fi

# Use "xargs" to parse quoted args.
#
# With -n1 it outputs one arg per line, with the quotes and backslashes removed.
#
# In Bash we could simply go:
#
#   readarray ARGS < <( xargs -n1 <<<"$var" ) &&
#   set -- "${ARGS[@]}" "$@"
#
# but POSIX shell has neither arrays nor command substitution, so instead we
# post-process each arg (as a line of input to sed) to backslash-escape any
# character that might be a shell metacharacter, then use eval to reverse
# that process (while maintaining the separation between arguments), and wrap
# the whole thing up as a single "set" statement.
#
# This will of course break if any of these variables contains a newline or
# an unmatched quote.
#

eval "set -- $(
        printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs -n1 |
        sed ' s~[^-[:alnum:]+,./:=@_]~\\&~g; ' |
        tr '\n' ' '
    )" '"$@"'

exec "$JAVACMD" "$@"

````

## gradlew.bat

````
@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  gradlew startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables, and ensure extensions are enabled
setlocal EnableExtensions

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:execute
@rem Setup the command line



@rem Execute gradlew
@rem endlocal doesn't take effect until after the line is parsed and variables are expanded
@rem which allows us to clear the local environment before executing the java command
endlocal & "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %* & call :exitWithErrorLevel

:exitWithErrorLevel
@rem Use "%COMSPEC%" /c exit to allow operators to work properly in scripts
"%COMSPEC%" /c exit %ERRORLEVEL%

````

## README.md

````
# CPVTS Android

Native Kotlin + Jetpack Compose client for the existing CPVTS Node REST API. Single activity, single `:app` module, Material 3, MVVM, StateFlow, SavedStateHandle, coroutines and manual dependency wiring. This project contains no backend, database driver, local parking database, Supabase client, analytics or authentication feature.

## Open and run

1. Extract the source archive and open **the `cpvts-android` folder** in Android Studio.
2. Use an Android Studio version compatible with **Android Gradle Plugin 9.4.1**, **Gradle 9.6.0** and **JDK 17**. Select JDK 17 in Settings → Build Tools → Gradle → Gradle JDK. Older Android Studio versions may require an update.
3. Install Android SDK Platform **37.2** and Build Tools **37.0.0** through SDK Manager. The app uses **compileSdk 37, minor API 2; targetSdk 37; minSdk 26**. Only stable dependencies are used; exact versions are in `gradle/libs.versions.toml`.
4. Let Android Studio create `local.properties` with your `sdk.dir`. Add your reachable CPVTS Node API URL as described below.
5. Sync Gradle, select the `app` configuration and a device or emulator running Android 8.0/API 26 or newer, then Run. Select the `debug` build variant for local HTTP development.

The default configuration uses an intentionally unconfigured API address. It shows a configuration message until rebuilt with your API URL. It contains no database password or API secret. **No APK is bundled:** native AAPT2 resource linking crashed in the execution environment, including for a minimal unrelated manifest. See the exact verification limits in `docs/verification.md` and build locally using the commands below.

## API configuration

The one configuration location is **`app/build.gradle.kts`**, which sets `BuildConfig.API_BASE_URL` separately for debug and release. Only `data/Network.kt` reads that constant. Overrides may be placed in your untracked `local.properties` or passed as Gradle project properties:

```properties
CPVTS_DEV_API_BASE_URL=https://your-reachable-development-host/api/v1/
CPVTS_PROD_API_BASE_URL=https://your-production-host/api/v1/
```

Replace those example hosts; they are not services provisioned by this project. URLs must end in `/api/v1/`, with no credentials, query or fragment. The default named placeholders are `cpvts-dev-api-host.invalid` and `cpvts-production-api-host.invalid`; the repository detects them and does not attempt requests. Production **must use HTTPS**. Debug alone supports HTTP through its own network security configuration. No certificate verification bypass is included.

For the Android emulator, if the existing web project's Node API is running on the host machine on its documented port 5173, the corresponding example is `http://10.0.2.2:5173/api/v1/`. This is an emulator routing example, not a verified endpoint for your environment. A physical device needs a reachable host LAN address, matching server bind/firewall configuration, or an HTTPS tunnel. `localhost` on a device refers to that device. Test the API's `/health` route from the target environment before using parking operations.

**Android 17 local-network restriction:** apps targeting API 37 need an additional runtime permission for direct LAN connections. This project preserves the brief's two-permission limit (`INTERNET` and `ACCESS_NETWORK_STATE`), so use a controlled HTTPS API endpoint/tunnel reachable without direct LAN access when testing on Android 17. The local-address examples apply to older device platforms where local-network protection is not enforced. Adding Android 17 LAN support would require changing the permission requirement and implementing its runtime grant flow. See [Android local-network permission](https://developer.android.com/privacy-and-security/local-network-permission).

**The Supabase project URL, REST Data API URL and Postgres pooler are not the CPVTS Node API.** Keep the database connection, CA certificate, SQL migration and Supabase server key in the web backend. The Android app neither applies `001_parking.sql` nor connects directly to Postgres. It consumes the same server data as the web app. No mobile migration is needed for these workflows.

The supplied contract defines no client authentication protocol. This client sends JSON with `Content-Type: application/json` and no invented authorization header. An authenticated/public deployment requires a documented server authentication contract and corresponding client integration before use outside the existing trusted environment.

## Screens and endpoint mapping

All paths below are relative to `/api/v1/`. Kotlin API functions are declared in `data/ParkingApi.kt` and accessed by `ApiParkingRepository`.

| Screen/action | Method and path | Kotlin API function |
| --- | --- | --- |
| Dashboard, Refresh, refresh after successful write | `GET dashboard` | `dashboard()` |
| Check-in, submit arrival | `POST check-ins` | `checkIn(request)` |
| Check-out, create receipt | `POST check-outs` | `checkOut(request)` |
| Find Vehicle, search | `GET vehicles/{vehicleNumber}` | `find(vehicleNumber)` |
| Daily Summary, choose date/load, refresh after successful write | `GET summary?date=YYYY-MM-DD` | `summary(date)` |
| Contract support, available but not requested by a screen | `GET slots` | `slots()` |
| Contract support, available but not requested by a screen | `GET health` | `health()` |

- **Dashboard:** current total, occupied and available spaces in both server-provided zones; reporting-day arrival count and assessed fees; active vehicles; loading, empty, error and refresh states. Failed refreshes retain figures with an explicit last-known label.
- **Check-in:** uppercase vehicle input, vehicle type, owner category, integer entry hour, validation, first-available server space, full-zone and duplicate responses. Staff/student/visitor rates are informational; final amounts always come from the API.
- **Check-out:** exactly one vehicle number or space ID, integer exit hour, server validation and receipt with server duration, free/billable hours, hourly rate and amount due. Successful responses refresh dashboard and summary. No payment claim.
- **Find Vehicle:** active server record with space, entry hour and status; empty/not-found/error states; shortcut to prefill check-out. Saved search results are explicitly described as the last server result.
- **Daily Summary:** valid ISO reporting date, server-provided arrival/check-out totals and assessed fees, completed sessions filtered by checkout timestamp in `Asia/Kolkata`, and empty/loading/error/retry states.

Full request and response definitions are included in `docs/api-contract.md`, copied from the existing web project.

## Reliability and state

The backend assigns spaces and calculates fees. The app displays its responses without locally allocating spaces or calculating a final fee. Input validation uses the documented vehicle normalization and integer hours 0–23; server messages and field errors remain visible. Overnight rollover is not supported by the contract.

Writes are **never automatically retried or queued**. The OkHttp client disables connection retries and redirects; POST bodies are one-shot to prevent HTTP retry follow-ups. It uses 10-second connection, 15-second read/write and 25-second overall call limits. Debug logs contain only method and response code, with no URLs, vehicle identifiers, headers, bodies or secrets. Release does not install the logger.

During a write, inputs and submit are disabled. The app records an uncertainty marker before sending. Timeout, connection failure, ambiguous server error or restoration of an interrupted write produces an **unknown outcome** state, with links to Vehicle Finder and Dashboard. Submission remains disabled until the user explicitly acknowledges checking the current status. The app never replays an interrupted write. Reads can be retried by the user.

Form inputs, last confirmed check-in, receipt and search result survive activity recreation using SavedStateHandle. Only those small UI values are saved; parking data is not stored in a database. Dashboard/summary reload when the ViewModel is recreated. Android saved-state restoration applies when Android retains the task; it is not durable storage after clearing app data or force-stopping/removing the task.

## Design system

The user's **light-only** requirement takes precedence over the generic Android brief's dark-theme sentence. There is no dynamic color or dark-mode derivation.

| Purpose | Exact colors |
| --- | --- |
| Canvas, navigation, cards, borders | `#F9F7F2`, `#F4EFE6`, `#FFFFFF`, `#E6DFD5` |
| Main and secondary text | `#231F20`, `#736B63` |
| Primary actions and selected states | `#FFB300`, `#FFF8E1` |
| Supporting actions and information | `#1E88E5`, `#E3F2FD` |
| Active, available and confirmed states | `#2E7D32`, `#E8F5E9` |

`ui/Theme.kt` defines the tokens. Error messages use charcoal text, an amber-tinted surface and explicit wording. Action labels use charcoal on amber or blue tint for readable contrast. Cards have warm borders and no elevation. Layouts are scrollable, centered with a 900dp maximum content width, and use 48dp minimum actions. Sticky form submission remains above the keyboard; navigation hides while the keyboard is open. Navigation icons retain full TalkBack labels when larger font scales hide short visual captions. Detail rows stack vertically and text wraps. Actual TalkBack, large-font, landscape and device layout testing remains required; see `docs/verification.md`.

## Build and tests

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

macOS/Linux:

```sh
chmod +x gradlew
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Gradle uses the standard local Android debug signing key, which is not distributed. Release is unsigned until you configure your own signing process; release minification is disabled.

Unit tests use a fake API, a fake repository, coroutine test dispatchers and MockWebServer. They cover success, full zone, duplicate vehicle, checkout receipt, not found, server validation, offline/timeout handling, JSON/path compatibility, no auth headers, no write retries, malformed write responses, double taps, input preservation and saved-state recovery. Tests do not modify the shared parking database.

## Project files

```text
cpvts-android/
  README.md
  SOURCE_FILES.md
  .gitignore
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradlew
  gradlew.bat
  gradle/
    libs.versions.toml
    wrapper/
      gradle-wrapper.jar
      gradle-wrapper.properties
  docs/
    api-contract.md
    verification.md
  app/
    build.gradle.kts
    src/
      main/
        AndroidManifest.xml
        java/com/example/cpvts/
          CpvtsApplication.kt
          MainActivity.kt
          data/
            Models.kt
            Network.kt
            ParkingApi.kt
            ParkingRepository.kt
          ui/
            ParkingViewModel.kt
            Screens.kt
            Theme.kt
        res/
          drawable/ic_parking.xml
          values/strings.xml
          values/styles.xml
      debug/
        AndroidManifest.xml
        res/xml/debug_network_security_config.xml
      test/java/com/example/cpvts/
        RepositoryTest.kt
        HttpContractTest.kt
        ParkingViewModelTest.kt
```

`SOURCE_FILES.md` contains complete contents for every distributable text file under its full relative path. The wrapper JAR is supplied as the actual binary file, with its digest in that listing. Machine-specific `local.properties`, downloaded SDK/Gradle caches, generated build files and signing keys are excluded from the source archive.

## Remaining integration

Development API URL and production API URL were **not provided**. Default package is `com.example.cpvts` (`com.example.cpvts.debug` for debug). Currency INR, IST reporting and rate information follow the supplied PDF/API contract. All required parking endpoints exist; runtime authentication and payment confirmation are not defined by that contract and have not been invented.

Provide the appropriate reachable Node URL, rebuild, then follow the device integration checklist in `docs/verification.md`. No Play Store publication or deployment has been performed.

Toolchain references: [Android Gradle Plugin 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes), [Android 17 setup](https://developer.android.com/about/versions/17/setup-sdk), [Android build configuration](https://developer.android.com/build).

````

## settings.gradle.kts

````
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "CPVTS Android"
include(":app")

````
