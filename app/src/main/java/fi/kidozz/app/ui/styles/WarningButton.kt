package fi.kidozz.app.ui.styles

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A reusable warning button component with consistent styling.
 * Features a warning color scheme and full-width design with margins.
 */
@Composable
fun WarningButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFDE9D2),
            contentColor = Color(0xFFED9738),
            disabledContainerColor = Color(0xFFE0E0E0), // Light grey background
            disabledContentColor = Color(0xFF9E9E9E) // Dark grey text
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
