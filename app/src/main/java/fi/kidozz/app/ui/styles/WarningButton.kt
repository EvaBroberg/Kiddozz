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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFAE7E4),
            contentColor = Color(0xFFE77170),
            disabledContainerColor = Color(0xFFFAE7E4).copy(alpha = 0.5f),
            disabledContentColor = Color(0xFFE77170).copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
