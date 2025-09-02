package fi.kidozz.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.CalendarEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EventAccordion(
    events: MutableList<CalendarEvent>, 
    onEventUpdated: (CalendarEvent) -> Unit = {},
    onEventDeleted: (CalendarEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        events.forEach { event ->
            EventAccordionItem(
                event = event,
                onEventUpdated = onEventUpdated,
                onEventDeleted = onEventDeleted
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun EventAccordionItem(
    event: CalendarEvent,
    onEventUpdated: (CalendarEvent) -> Unit = {},
    onEventDeleted: (CalendarEvent) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(TextFieldValue(event.title)) }
    var editedDescription by remember { mutableStateOf(TextFieldValue(event.description)) }
    var editedDateTime by remember { mutableStateOf(event.dateTime) }
    
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val isPastEvent = event.dateTime.isBefore(LocalDateTime.now())

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEditing) {
                    // Edit mode - editable title
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                } else {
                    // View mode - display title
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { expanded = !expanded }
                    )
                }
                
                if (isEditing) {
                    // Save and Cancel buttons
                    IconButton(onClick = { 
                        val updatedEvent = event.copy(
                            title = editedTitle.text,
                            description = editedDescription.text,
                            dateTime = editedDateTime,
                            date = editedDateTime.toLocalDate().toString(),
                            startTime = editedDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                        )
                        onEventUpdated(updatedEvent)
                        isEditing = false
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = { 
                        isEditing = false
                        editedTitle = TextFieldValue(event.title)
                        editedDescription = TextFieldValue(event.description)
                        editedDateTime = event.dateTime
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Edit and Delete buttons
                    IconButton(onClick = { 
                        isEditing = true
                        expanded = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Event",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = { onEventDeleted(event) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete Event",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isEditing) {
                    // Edit mode - editable fields
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (!isPastEvent) {
                        // Only allow date editing for future events
                        Button(
                            onClick = { /* TODO: Show date picker */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Date: ${editedDateTime.format(formatter)}")
                        }
                    } else {
                        // Show read-only date for past events
                        Text(
                            text = "Date: ${editedDateTime.format(formatter)} (Past Event)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // View mode - display fields
                    Text(text = "Date: ${event.dateTime.format(formatter)}")
                    if (event.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
