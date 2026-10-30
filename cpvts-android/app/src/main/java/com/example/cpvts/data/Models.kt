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
