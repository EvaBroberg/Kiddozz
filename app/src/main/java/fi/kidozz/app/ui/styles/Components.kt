package fi.kidozz.app.ui.styles

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Contains reusable UI components that follow the app's design system.
 * These components provide consistent styling and behavior across the application.
 */

/**
 * A rectangular button that fills the full width of its parent.
 * Designed to sit flush with edges and provide a clean, modern appearance.
 * 
 * @param text The text to display on the button
 * @param onClick The callback to execute when the button is clicked
 * @param modifier Optional modifier for the button
 */
@Composable
fun ThumbnailButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Gray,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp
        )
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
