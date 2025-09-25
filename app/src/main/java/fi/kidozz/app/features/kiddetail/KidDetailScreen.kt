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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.models.Parent
import fi.kidozz.app.data.models.TrustedAdult
import fi.kidozz.app.data.sample.sampleKidsState
import fi.kidozz.app.data.sample.computeAge
import fi.kidozz.app.features.dashboard.KidsViewModel
import fi.kidozz.app.ui.components.SectionTitle
import fi.kidozz.app.ui.components.AccordionCard
import fi.kidozz.app.ui.styles.AttendanceSegmentedControl
import fi.kidozz.app.ui.theme.KiddozzTheme

@Composable
fun GuardiansInfoSection(kid: Kid, modifier: Modifier = Modifier) {
    if (kid.parents.isNullOrEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "No guardians available",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        Column(modifier = modifier) {
            kid.parents.forEach { parent ->
                GuardianAccordion(parent = parent)
            }
        }
    }
}

@Composable
fun GuardianAccordion(parent: Parent, modifier: Modifier = Modifier) {
    AccordionCard(
        title = parent.full_name,
        subtitle = null,
        showChatButton = false,
        expandedContent = {
            parent.email?.let {
                GuardianInfoRow(label = "Email", value = it)
            }
            parent.phone_num?.let {
                GuardianInfoRow(label = "Phone", value = it)
            }
        },
        modifier = modifier
    )
}

@Composable
fun TrustedAdultsSection(kid: Kid, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        kid.trusted_adults.forEach { trustedAdult ->
            TrustedAdultAccordion(trustedAdult = trustedAdult)
        }
    }
}

@Composable
fun TrustedAdultAccordion(trustedAdult: TrustedAdult, modifier: Modifier = Modifier) {
    AccordionCard(
        title = trustedAdult.name,
        subtitle = null,
        showChatButton = false,
        expandedContent = {
            trustedAdult.email?.let {
                GuardianInfoRow(label = "Email", value = it)
            }
            trustedAdult.phone_num?.let {
                GuardianInfoRow(label = "Phone", value = it)
            }
            trustedAdult.address?.let {
                GuardianInfoRow(label = "Address", value = it)
            }
        },
        modifier = modifier
    )
}

@Composable
fun GuardianInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

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
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp), 
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
            
            // Attendance toggle right after kid's image and name
            item {
                AttendanceSegmentedControl(
                    selectedAttendance = currentAttendance,
                    onAttendanceChange = { newAttendance ->
                        currentAttendance = newAttendance
                        kidsViewModel?.updateAttendance(kid.id, newAttendance)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            
            // Age and Date of Birth
            item { 
                Text(
                    text = "Age: ${computeAge(kid.dob)} years old",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) 
            }
            item { 
                Text(
                    text = "Date of Birth: ${kid.dob}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) 
            }
            
            // Group Name (using group_id for now, could be enhanced to fetch actual group name)
            item { 
                Text(
                    text = "Group: Group ${kid.group_id}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) 
            }
            
            // Allergies (always show to indicate system is checking)
            item {
                Text(
                    text = "Allergies: ${kid.allergies ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Need to Know
            item {
                if (!kid.need_to_know.isNullOrBlank()) {
                    Text(
                        text = "Good to Know: ${kid.need_to_know}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            item { 
                SectionTitle(
                    "Guardians Info",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) 
            }
            item {
                GuardiansInfoSection(
                    kid = kid,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Only show Trusted Adults section if there are trusted adults
            if (!kid.trusted_adults.isNullOrEmpty()) {
                item { 
                    SectionTitle(
                        "Trusted Adults",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) 
                }
                item {
                    TrustedAdultsSection(
                        kid = kid,
                        modifier = Modifier.fillMaxWidth()
                    )
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