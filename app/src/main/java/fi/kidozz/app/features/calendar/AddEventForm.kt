package fi.kidozz.app.features.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun AddEventForm(
    onEventAdded: (String, LocalDateTime, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var eventTitle by remember { mutableStateOf(TextFieldValue("")) }
    var eventDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    var location by remember { mutableStateOf(TextFieldValue("")) }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add New Event", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = eventTitle,
            onValueChange = { eventTitle = it },
            label = { Text("Event Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            showDateTimePicker(context) { selectedDateTime ->
                eventDateTime = selectedDateTime
            }
        }) {
            Text("Select Date & Time: ${eventDateTime.format(formatter)}")
        }

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (eventTitle.text.isNotBlank()) {
                    onEventAdded(eventTitle.text, eventDateTime, location.text)
                    eventTitle = TextFieldValue("")
                    location = TextFieldValue("")
                    Toast.makeText(context, "Event added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Title is required", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Event")
        }
    }
}

private fun showDateTimePicker(context: Context, onDateTimeSelected: (LocalDateTime) -> Unit) {
    val now = Calendar.getInstance()

    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val selectedDateTime = LocalDateTime.of(year, month + 1, day, hour, minute)
                    onDateTimeSelected(selectedDateTime)
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
            ).show()
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH)
    ).show()
}