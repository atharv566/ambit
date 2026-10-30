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
