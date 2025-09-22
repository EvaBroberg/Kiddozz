package fi.kidozz.app.ui.styles

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Contains styles specific to the educator dashboard views.
 * These provide consistent styling for dashboard components like kid cards, grids, and labels.
 */

object EducatorDashboardStyles {
    // Grid Spacing
    val GridSpacing = 8.dp
    val GridPadding = PaddingValues(16.dp)
    
    // Kid Card Styles
    val KidCardElevation = 2.dp
    val KidCardPadding = PaddingValues(12.dp)
    val KidCardSpacing = 4.dp
    
    // Dashboard Layout
    val DashboardPadding = PaddingValues(16.dp)
    val SectionSpacing = 16.dp
    val FilterButtonPadding = PaddingValues(8.dp)
    
    // Text Alignment
    val KidNameAlignment = androidx.compose.ui.text.style.TextAlign.Start
    val KidDetailsAlignment = androidx.compose.ui.text.style.TextAlign.Start
}

/**
 * Composable function that creates a styled kid card with consistent appearance.
 */
@Composable
fun KidCardStyle(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = EducatorDashboardStyles.KidCardElevation
        ),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.KidCardBackground
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        content()
    }
}

/**
 * Composable function that creates a styled section container.
 */
@Composable
fun SectionContainerStyle(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.padding(EducatorDashboardStyles.DashboardPadding)
    ) {
        content()
    }
}
