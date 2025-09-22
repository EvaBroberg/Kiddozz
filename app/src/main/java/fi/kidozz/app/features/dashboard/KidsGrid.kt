package fi.kidozz.app.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.repository.KidsRepository
import fi.kidozz.app.ui.styles.EducatorDashboardStyles

@Composable
fun KidsGrid(
    daycareId: String,
    groupId: String?,
    repository: KidsRepository
) {
    val kids by produceState<List<Kid>>(initialValue = emptyList(), daycareId, groupId) {
        value = repository.fetchKids(daycareId, groupId)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = EducatorDashboardStyles.GridPadding,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(EducatorDashboardStyles.GridSpacing),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(EducatorDashboardStyles.GridSpacing)
    ) {
        items(kids) { kid ->
            KiddozCard(kid = kid)
        }
    }
}

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