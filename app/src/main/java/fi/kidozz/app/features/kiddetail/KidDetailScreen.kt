package fi.kidozz.app.features.kiddetail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.sample.sampleKidsState
import fi.kidozz.app.ui.components.GuardianInfoView
import fi.kidozz.app.ui.components.SectionTitle
import fi.kidozz.app.ui.theme.KiddozzTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidDetailScreen(
    kid: Kid,
    onBackClick: () -> Unit,
    onAttendanceStatusChange: (newStatus: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kid.name) },
                navigationIcon = { 
                    IconButton(onClick = onBackClick) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    } 
                }
            )
        },
        modifier = modifier.fillMaxSize()
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
                        text = kid.name, 
                        style = MaterialTheme.typography.headlineSmall, 
                        textAlign = TextAlign.Center, 
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val outAlpha by animateFloatAsState(
                        targetValue = if (kid.attendanceStatus == "OUT") 1.0f else 0.5f, 
                        label = "OutButtonAlpha"
                    )
                    Button(
                        onClick = { onAttendanceStatusChange("OUT") }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), 
                        modifier = Modifier.alpha(outAlpha)
                    ) { 
                        Text("OUT") 
                    }
                    
                    val sickAlpha by animateFloatAsState(
                        targetValue = if (kid.attendanceStatus == "SICK") 1.0f else 0.5f, 
                        label = "SickButtonAlpha"
                    )
                    Button(
                        onClick = { onAttendanceStatusChange("SICK") }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow), 
                        modifier = Modifier.alpha(sickAlpha)
                    ) { 
                        Text("SICK", color = Color.Black) 
                    }
                    
                    val inAlpha by animateFloatAsState(
                        targetValue = if (kid.attendanceStatus == "IN") 1.0f else 0.5f, 
                        label = "InButtonAlpha"
                    )
                    Button(
                        onClick = { onAttendanceStatusChange("IN") }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green), 
                        modifier = Modifier.alpha(inAlpha)
                    ) { 
                        Text("IN CARE") 
                    }
                }
            }
            
            item { SectionTitle("Allergies") }
            item { 
                Text(kid.allergies.joinToString(", ").ifEmpty { "None specified" }) 
            }
            
            item { SectionTitle("Need to Know") }
            item { 
                Text(kid.needToKnow.ifEmpty { "Nothing specific" }) 
            }
            
            item { SectionTitle("Primary Guardian") }
            item { GuardianInfoView(kid.primaryGuardian) }
            
            kid.secondaryGuardian?.let {
                item { SectionTitle("Secondary Guardian") }
                item { GuardianInfoView(it) }
            }
            
            if (kid.authorizedPickups.isNotEmpty()) {
                item { SectionTitle("Authorized Pickups") }
                kid.authorizedPickups.forEach { pickup -> 
                    item { GuardianInfoView(pickup, isAuthorizedPickup = true) } 
                }
            }
            
            item { SectionTitle("Address") }
            item { 
                Text(kid.address.ifEmpty { "Not specified" }) 
            }
        }
    }
}

@Preview(showBackground = true, name = "Kid Detail Screen Preview")
@Composable
fun KidDetailScreenPreview() { 
    KiddozzTheme { 
        KidDetailScreen(sampleKidsState.first(), {}, {}) 
    } 
}