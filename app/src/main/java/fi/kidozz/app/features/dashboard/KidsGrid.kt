package fi.kidozz.app.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.ui.styles.EducatorDashboardStyles

@Composable
fun KidsGrid(
    kids: List<Kid>,
    onKidClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = EducatorDashboardStyles.GridPadding,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(EducatorDashboardStyles.GridSpacing),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(EducatorDashboardStyles.GridSpacing)
    ) {
        items(kids) { kid ->
            KiddozCard(
                kid = kid,
                modifier = Modifier.clickable { onKidClick(kid.id) }
            )
        }
    }
}