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
