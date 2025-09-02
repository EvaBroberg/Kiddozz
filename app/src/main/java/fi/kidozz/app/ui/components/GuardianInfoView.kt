package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Guardian

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun GuardianInfoView(guardian: Guardian, isAuthorizedPickup: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("Name: ${guardian.name}", style = MaterialTheme.typography.bodyLarge)
        Text("Phone: ${guardian.phone}", style = MaterialTheme.typography.bodyMedium)
        Text("Email: ${guardian.email}", style = MaterialTheme.typography.bodyMedium)
        if (isAuthorizedPickup || guardian.relationship.isNotBlank()) {
            Text("Relationship: ${guardian.relationship}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
