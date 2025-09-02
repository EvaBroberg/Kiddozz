package fi.kidozz.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.sample.sampleKidsState
import fi.kidozz.app.features.dashboard.EducatorDashboardScreen
import fi.kidozz.app.features.kiddetail.KidDetailScreen
import fi.kidozz.app.features.role.RoleSelectionScreen
import fi.kidozz.app.features.calendar.UpcomingEventsScreen
import fi.kidozz.app.features.calendar.PreviousEventsScreen

@Composable
fun KiddozzAppHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "role_selection"
    ) {
        composable("role_selection") {
            RoleSelectionScreen(
                onEducatorViewClick = { navController.navigate("educator_dashboard") },
                onParentViewClick = { /* TODO: Navigate to Parent View */ },
                modifier = modifier
            )
        }

        composable("educator_dashboard") {
            EducatorDashboardScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() },
                onKidClick = { kid ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("selectedKid", kid)
                    navController.navigate("kid_detail")
                },
                modifier = modifier,
                kidsList = sampleKidsState
            )
        }

        composable("kid_detail") {
            val kid = navController.previousBackStackEntry?.savedStateHandle?.get<Kid>("selectedKid")
            if (kid != null) {
                KidDetailScreen(
                    kid = kid,
                    onBackClick = { navController.popBackStack() },
                    onAttendanceStatusChange = { newStatus ->
                        val kidIndex = sampleKidsState.indexOfFirst { it.id == kid.id }
                        if (kidIndex != -1) {
                            sampleKidsState[kidIndex] = sampleKidsState[kidIndex].copy(attendanceStatus = newStatus)
                        }
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "selectedKid",
                            kid.copy(attendanceStatus = newStatus)
                        )
                    }
                )
            }
        }

        composable("upcoming_events") {
            UpcomingEventsScreen(
                onBackClick = { navController.popBackStack() },
                modifier = modifier
            )
        }

        composable("previous_events") {
            PreviousEventsScreen(
                onBackClick = { navController.popBackStack() },
                modifier = modifier
            )
        }
    }
}


