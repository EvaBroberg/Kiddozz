package fi.kidozz.app.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.ui.styles.EducatorDashboardStyles

@Composable
fun KidsGrid(
    filteredKids: List<Kid>,
    onKidClick: (Kid) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = EducatorDashboardStyles.GridPadding,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(EducatorDashboardStyles.GridSpacing),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(EducatorDashboardStyles.GridSpacing)
    ) {
        items(filteredKids) { kid ->
            KiddozCard(
                kid = kid,
                modifier = Modifier.clickable { onKidClick(kid) }
            )
        }
    }
}