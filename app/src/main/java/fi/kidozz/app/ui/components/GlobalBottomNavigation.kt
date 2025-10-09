@file:Suppress("NAME_SHADOWING")

package fi.kidozz.app.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import fi.kidozz.app.core.NavigationSection

@Composable
fun GlobalBottomNavigation(
    navController: NavController,
    role: String?,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var stableRole by rememberSaveable { mutableStateOf<String?>(null) }
    val normalized = role?.trim()?.lowercase()

    LaunchedEffect(role, stableRole) {
        Log.d("BottomNavCheck", "Incoming role='$role' normalized='$normalized' stableRole(before)='$stableRole'")
    }

    if (!normalized.isNullOrBlank()) {
        stableRole = normalized
    } else if (role == null) {
        stableRole = null
    }

    LaunchedEffect(stableRole) {
        Log.d("BottomNavCheck", "StableRole(final)='$stableRole'")
    }

    if (stableRole.isNullOrBlank() || currentRoute == "role_selection") {
        Spacer(Modifier.height(0.dp))
        return
    }

    // Keep your filtering logic simple and safe:
    val canonicalRole = role?.trim()?.lowercase()
    val allSections = NavigationSection.values().toList()
    val visibleSections =
        if (canonicalRole == "parent") allSections.filter { it != NavigationSection.KidsOverview }
        else allSections

    Log.d("BottomNav", "role='$role' → visibleSections=${visibleSections.map { it.name }}")

    NavigationBar(modifier = modifier.testTag("navigation_bar")) {
        visibleSections.forEach { section ->
            val targetRoute = when (section) {
                NavigationSection.KidsOverview ->
                    if (stableRole == "parent") "parent_dashboard" else "educator_dashboard"
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
                        // Correct navigation options per StackOverflow fix
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