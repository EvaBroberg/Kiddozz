package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding

/**
 * A reusable page wrapper for screens that use Column/Row/Box layouts (no Lazy components).
 * Provides vertical scrolling for the entire page content.
 * 
 * Use this for screens like:
 * - EducatorDashboardScreen
 * - ParentDashboardScreen  
 * - KidDetailScreen
 * - CalendarScreen (with embedded calendar grid)
 * - AbsenceCalendarDialog content
 */
@Composable
fun ScrollablePage(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) { 
        content() 
    }
}

/**
 * A reusable page wrapper for screens that use LazyColumn/LazyVerticalGrid as their main content.
 * Does NOT add vertical scrolling (the Lazy component handles it).
 * 
 * Use this for screens like:
 * - UpcomingEventsScreen (with LazyColumn)
 * - PastEventsScreen (with LazyColumn)
 * - Any screen with LazyColumn/LazyVerticalGrid as root content
 * 
 * Rule: Never put verticalScroll() around a LazyColumn/LazyVerticalGrid. Use LazyPage instead.
 */
@Composable
fun LazyPage(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .navigationBarsPadding()
            .imePadding()
    ) {
        content()
    }
}

/**
 * A specialized wrapper for dialogs that need scrolling.
 * Similar to ScrollablePage but optimized for dialog content.
 */
@Composable
fun ScrollableDialogContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) { 
        content() 
    }
}
