package fi.kidozz.app.features.messaging.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fi.kidozz.app.features.messaging.domain.model.ConversationType
import fi.kidozz.app.features.messaging.ui.components.MessageListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesListScreen(
    onOpenConversation: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MessagingViewModel,
    modifier: Modifier = Modifier
) {
    val inbox by viewModel.inbox.collectAsState()
    val filter by viewModel.filter.collectAsState()

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
                    selected = filter == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("All") }
                )
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

            // Messages list
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