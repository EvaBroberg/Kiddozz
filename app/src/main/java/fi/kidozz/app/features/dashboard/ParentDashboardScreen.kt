package fi.kidozz.app.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.models.Parent
import fi.kidozz.app.data.sample.computeAge
import fi.kidozz.app.ui.components.SectionTitle
import fi.kidozz.app.ui.theme.KiddozzTheme
import fi.kidozz.app.ui.theme.InCareColor
import fi.kidozz.app.ui.theme.OutColor
import fi.kidozz.app.ui.theme.SickColor
import fi.kidozz.app.ui.theme.SecondaryTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    parentId: String,
    parentsViewModel: ParentsViewModel,
    modifier: Modifier = Modifier
) {
    val kids by parentsViewModel.kids.collectAsState()
    val isLoading by parentsViewModel.isLoading.collectAsState()
    val error by parentsViewModel.error.collectAsState()
    
    // Load kids when screen is first displayed
    LaunchedEffect(parentId) {
        parentsViewModel.loadKidsForParent(parentId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "My Kids",
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading kids",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                parentsViewModel.clearError()
                                parentsViewModel.loadKidsForParent(parentId)
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            kids.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No kids found",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(kids) { kid ->
                        Column {
                            KidAccordionCard(kid = kid)
                            if (kid != kids.last()) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidAccordionCard(
    kid: Kid,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Determine status color
    val statusColor = when (kid.attendance.lowercase()) {
        "in-care" -> InCareColor
        "out" -> OutColor
        "sick" -> SickColor
        else -> OutColor // Default to out color
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Ensure proper height calculation
    ) {
        // Status indicator line
        Box(
            modifier = Modifier
                .width(16.dp) // Increased width for better visibility
                .fillMaxHeight()
                .background(statusColor)
        )
        
        // Main card content
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE0E0E0))
                )
                
                // Content
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Collapsed state - kid name, attendance, and chat icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = kid.full_name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Status: ${kid.attendance.uppercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryTextColor
                        )
                    }
                    
                    // Chat icon (placeholder)
                    IconButton(
                        onClick = { /* TODO: Implement chat functionality */ }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Chat with educator",
                            tint = SecondaryTextColor
                        )
                    }
                    
                    // Expand/collapse icon
                    IconButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = SecondaryTextColor
                        )
                    }
                }
                
                // Expanded state - DOB and guardians info
                if (expanded) {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Date of Birth
                        Text(
                            text = "Date of Birth: ${kid.dob}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Age
                        Text(
                            text = "Age: ${computeAge(kid.dob)} years old",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Guardians Info
                        if (kid.parents.isNotEmpty()) {
                            SectionTitle("Guardians")
                            Spacer(modifier = Modifier.height(8.dp))
                            kid.parents.forEach { parent ->
                                GuardianInfoItem(parent = parent)
                                if (parent != kid.parents.last()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
                }
                
                // Bottom border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
    }
}

@Composable
fun GuardianInfoItem(
    parent: Parent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = parent.full_name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            parent.email?.let { email ->
                Text(
                    text = "Email: $email",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            parent.phone_num?.let { phone ->
                Text(
                    text = "Phone: $phone",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Parent Dashboard Screen Preview")
@Composable
fun ParentDashboardScreenPreview() {
    KiddozzTheme {
        // Note: This preview won't work without a real ViewModel
        // In a real app, you'd use a preview ViewModel or mock data
        Text("Parent Dashboard Preview")
    }
}
