package fi.kidozz.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import fi.kidozz.app.core.NavigationSection

@Composable
fun GlobalBottomNavigation(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier
    ) {
        NavigationSection.values().forEach { section ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = section.icon, 
                        contentDescription = section.title
                    ) 
                },
                label = { Text(section.title) },
                selected = when (section) {
                    NavigationSection.KidsOverview -> currentRoute == "educator_dashboard" || currentRoute == "parent_dashboard"
                    NavigationSection.Calendar -> currentRoute == "calendar"
                    NavigationSection.Menu -> currentRoute == "menu"
                    NavigationSection.Profile -> currentRoute == "profile"
                },
                onClick = {
                    when (section) {
                        NavigationSection.KidsOverview -> {
                            // Navigate to appropriate dashboard based on current user role
                            if (currentRoute == "parent_dashboard") {
                                navController.navigate("parent_dashboard") {
                                    popUpTo("parent_dashboard") { inclusive = true }
                                }
                            } else {
                                navController.navigate("educator_dashboard") {
                                    popUpTo("educator_dashboard") { inclusive = true }
                                }
                            }
                        }
                        NavigationSection.Calendar -> {
                            navController.navigate("calendar")
                        }
                        NavigationSection.Menu -> {
                            navController.navigate("menu")
                        }
                        NavigationSection.Profile -> {
                            navController.navigate("profile")
                        }
                    }
                }
            )
        }
    }
}
