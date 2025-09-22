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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.sample.sampleKidsState
import fi.kidozz.app.data.sample.computeAge
import fi.kidozz.app.ui.components.SectionTitle
import fi.kidozz.app.ui.theme.KiddozzTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidDetailScreen(
    kid: Kid,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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