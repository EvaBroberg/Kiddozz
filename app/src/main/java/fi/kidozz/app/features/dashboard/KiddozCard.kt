package fi.kidozz.app.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Kid

@Composable
fun KiddozCard(kid: Kid, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Face, contentDescription = "Kid icon", modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = kid.name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            }
            Text(
                text = kid.attendanceStatus.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = if (kid.attendanceStatus == "SICK") Color.Black else Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (kid.attendanceStatus) {
                            "IN" -> Color.Green.copy(alpha = 0.7f)
                            "SICK" -> Color.Yellow
                            else -> Color.Gray
                        }
                    )
                    .padding(vertical = 4.dp)
            )
        }
    }
}