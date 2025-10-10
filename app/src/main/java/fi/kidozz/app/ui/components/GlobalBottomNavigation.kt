package fi.kidozz.app.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import fi.kidozz.app.core.NavigationSection

@Composable
fun GlobalBottomNavigationBase(
    navController: NavController,
    sections: List<NavigationSection>,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Log.d("BottomNav", "Rendering sections: ${sections.map { it.name }}")

    NavigationBar(modifier = modifier.testTag("navigation_bar")) {
        sections.forEach { section ->
            val targetRoute = when (section) {
                NavigationSection.KidsOverview -> "educator_dashboard"
                NavigationSection.Calendar -> "calendar"
                NavigationSection.Menu -> "menu"
                NavigationSection.Profile -> "profile"
            }
            val selected = currentRoute == targetRoute

            NavigationBarItem(
                selected = selected,
                icon = { Icon(section.icon, contentDescription = section.title) },
                label = { Text(section.title) },
                onClick = {
                    if (!selected) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ParentBottomNavigation(navController: NavController) {
    val sections = listOf(
        NavigationSection.Calendar,
        NavigationSection.Menu,
        NavigationSection.Profile
    )
    GlobalBottomNavigationBase(navController, sections)
}

@Composable
fun EducatorBottomNavigation(navController: NavController) {
    val sections = listOf(
        NavigationSection.KidsOverview,
        NavigationSection.Calendar,
        NavigationSection.Menu,
        NavigationSection.Profile
    )
    GlobalBottomNavigationBase(navController, sections)
}