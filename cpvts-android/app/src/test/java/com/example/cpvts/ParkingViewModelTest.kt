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
