package fi.kidozz.app.ui.styles

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.kidozz.app.ui.theme.SecondaryTextColor

/**
 * A basic button component with grey outline and transparent background.
 * Used for secondary actions like cancel buttons.
 */
@Composable
fun BasicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SecondaryTextColor,
            disabledContentColor = SecondaryTextColor.copy(alpha = 0.5f)
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = enabled),
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
