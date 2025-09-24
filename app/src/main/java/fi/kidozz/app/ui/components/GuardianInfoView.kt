package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.TrustedAdult

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun TrustedAdultInfoView(trustedAdult: TrustedAdult) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = "Name: ${trustedAdult.name}", 
            style = MaterialTheme.typography.bodyLarge
        )
        trustedAdult.phone_num?.let { phone ->
            Text(
                text = "Phone: $phone", 
                style = MaterialTheme.typography.bodyMedium
            )
        }
        trustedAdult.email?.let { email ->
            Text(
                text = "Email: $email", 
                style = MaterialTheme.typography.bodyMedium
            )
        }
        trustedAdult.address?.let { address ->
            Text(
                text = "Address: $address", 
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
