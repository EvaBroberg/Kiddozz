package fi.kidozz.app.ui.styles

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A custom card component designed for thumbnail layouts.
 * 
 * This card removes all default margins and padding from left, right, and bottom edges,
 * making it ideal for grid layouts where cards should sit flush against each other.
 * The card expands to full width of its parent and maintains consistent Material3 styling.
 * 
 * @param modifier Optional modifier for the card
 * @param backgroundColor The background color of the card (defaults to White)
 * @param content The content to display inside the card
 */
@Composable
fun ThumbnailCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        content()
    }
}
