package fi.kidozz.app.features.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import fi.kidozz.app.data.models.Kid

@Composable
fun KiddozCard(kid: Kid) {
    Card {
        Column {
            Text(text = kid.full_name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "DOB: ${kid.dob}")
            Text(text = "Group: ${kid.group_id}")
        }
    }
}