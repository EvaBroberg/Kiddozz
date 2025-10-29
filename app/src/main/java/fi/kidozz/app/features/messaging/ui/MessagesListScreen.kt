package fi.kidozz.app.features.messaging.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import fi.kidozz.app.features.messaging.domain.model.ConversationType
import fi.kidozz.app.features.messaging.domain.model.Contact
import fi.kidozz.app.features.messaging.domain.model.ContactType
import fi.kidozz.app.features.messaging.ui.components.MessageListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesListScreen(
    onOpenConversation: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MessagingViewModel,
    modifier: Modifier = Modifier
) {
    val inbox by viewModel.inbox.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Log session info when Messages screen opens
    LaunchedEffect(Unit) {
        android.util.Log.d("MessagesScreen", "Messages screen opened. Filter: $filter, Contacts count: ${contacts.size}")
    }
    
    // Log when filter changes
    LaunchedEffect(filter) {
        android.util.Log.d("MessagesScreen", "Filter changed to: $filter")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = "",
                onValueChange = { /* TODO: Implement search */ },
                placeholder = { Text("Search messages...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == ConversationType.PARENT,
                    onClick = { viewModel.setFilter(ConversationType.PARENT) },
                    label = { Text("Parents") }
                )
                FilterChip(
                    selected = filter == ConversationType.EDUCATOR,
                    onClick = { viewModel.setFilter(ConversationType.EDUCATOR) },
                    label = { Text("Educators") }
                )
                FilterChip(
                    selected = filter == ConversationType.GROUP,
                    onClick = { viewModel.setFilter(ConversationType.GROUP) },
                    label = { Text("Groups") }
                )
            }

            // Content based on filter
            when (filter) {
                ConversationType.PARENT, ConversationType.EDUCATOR -> {
                    if (contacts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ${if (filter == ConversationType.PARENT) "parents" else "educators"} in your groups.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(contacts, key = { it.id }) { contact ->
                                ContactListItem(
                                    contact = contact,
                                    onClick = {
                                        scope.launch {
                                            val conversationId = viewModel.openDirectWith(contact.id)
                                            onOpenConversation(conversationId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                ConversationType.GROUP -> {
                    // Messages list for Groups
                    if (inbox.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No group conversations.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(inbox) { conversation ->
                                MessageListItem(
                                    conversation = conversation,
                                    onClick = { onOpenConversation(conversation.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(contact.name) },
        supportingContent = { 
            Text(
                if (contact.type == ContactType.PARENT) "Parent" else "Educator",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = { 
            Avatar(
                imageUrl = contact.avatarUrl,
                initials = initialsFromName(contact.name)
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Composable
fun Avatar(
    imageUrl: String?,
    initials: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            // TODO: Load image from URL
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun initialsFromName(name: String): String {
    return name.split(" ")
        .take(2)
        .map { it.firstOrNull()?.uppercaseChar() ?: "" }
        .joinToString("")
}