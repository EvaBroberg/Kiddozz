package fi.kidozz.app.features.kiddetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.sample.sampleKidsState
import fi.kidozz.app.data.sample.computeAge
import fi.kidozz.app.features.dashboard.KidsViewModel
import fi.kidozz.app.ui.components.SectionTitle
import fi.kidozz.app.ui.theme.KiddozzTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidDetailScreen(
    kid: Kid,
    onBackClick: () -> Unit,
    kidsViewModel: KidsViewModel? = null,
    modifier: Modifier = Modifier
) {
    var currentAttendance by remember { mutableStateOf(kid.attendance) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kid.full_name) },
                navigationIcon = { 
                    IconButton(onClick = onBackClick) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    } 
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Face, 
                        contentDescription = "Kid's Profile Picture", 
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = kid.full_name, 
                        style = MaterialTheme.typography.headlineSmall, 
                        textAlign = TextAlign.Center, 
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            item { SectionTitle("Basic Information") }
            item { 
                Text("ID: ${kid.id}") 
            }
            item { 
                Text("Age: ${computeAge(kid.dob)} years old") 
            }
            item { 
                Text("Date of Birth: ${kid.dob}") 
            }
            item { 
                Text("Group ID: ${kid.group_id}") 
            }
            item { 
                Text("Daycare ID: ${kid.daycare_id}") 
            }
            
            item { SectionTitle("Attendance") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current Status: ${currentAttendance.uppercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // In Care Button
                            FilterChip(
                                onClick = {
                                    currentAttendance = "in-care"
                                    kidsViewModel?.updateAttendance(kid.id, "in-care")
                                },
                                label = { Text("In Care") },
                                selected = currentAttendance == "in-care",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.Green.copy(alpha = 0.2f),
                                    selectedLabelColor = Color.Green
                                )
                            )
                            
                            // Out Button
                            FilterChip(
                                onClick = {
                                    currentAttendance = "out"
                                    kidsViewModel?.updateAttendance(kid.id, "out")
                                },
                                label = { Text("Out") },
                                selected = currentAttendance == "out",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.Gray.copy(alpha = 0.2f),
                                    selectedLabelColor = Color.Gray
                                )
                            )
                            
                            // Sick Button
                            FilterChip(
                                onClick = {
                                    currentAttendance = "sick"
                                    kidsViewModel?.updateAttendance(kid.id, "sick")
                                },
                                label = { Text("Sick") },
                                selected = currentAttendance == "sick",
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.Red.copy(alpha = 0.2f),
                                    selectedLabelColor = Color.Red
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Kid Detail Screen Preview")
@Composable
fun KidDetailScreenPreview() { 
    KiddozzTheme { 
        KidDetailScreen(sampleKidsState.first(), {}) 
    } 
}