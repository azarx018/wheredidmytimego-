package com.timetrace.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Today / Yesterday / custom-date chip row (brief section 5: "Allow selecting:
 * Today, Yesterday, Custom date if practical"). The Material3 DatePicker returns
 * UTC midnight for the picked day regardless of device time zone, so we convert
 * using UTC specifically here to land on the calendar date the user actually
 * tapped, not a day off depending on the user's offset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val yesterday = remember { today.minusDays(1) }
    var showPicker by remember { mutableStateOf(false) }
    val isCustom = selectedDate != today && selectedDate != yesterday

    Row(modifier = modifier.padding(vertical = 8.dp)) {
        FilterChip(
            selected = selectedDate == today,
            onClick = { onDateSelected(today) },
            label = { Text("Today") },
            modifier = Modifier.padding(end = 8.dp)
        )
        FilterChip(
            selected = selectedDate == yesterday,
            onClick = { onDateSelected(yesterday) },
            label = { Text("Yesterday") },
            modifier = Modifier.padding(end = 8.dp)
        )
        FilterChip(
            selected = isCustom,
            onClick = { showPicker = true },
            label = {
                Text(
                    if (isCustom) selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))
                    else "Custom"
                )
            }
        )
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        onDateSelected(picked)
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
