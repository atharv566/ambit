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
