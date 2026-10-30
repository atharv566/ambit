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
