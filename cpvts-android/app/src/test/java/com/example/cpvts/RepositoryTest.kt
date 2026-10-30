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
