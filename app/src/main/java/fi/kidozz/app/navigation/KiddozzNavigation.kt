package fi.kidozz.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.sample.sampleKidsState
import fi.kidozz.app.features.dashboard.EducatorDashboardScreen
import fi.kidozz.app.features.dashboard.GroupsViewModel
import fi.kidozz.app.features.dashboard.EducatorViewModel
import fi.kidozz.app.features.dashboard.KidsViewModel
import fi.kidozz.app.features.kiddetail.KidDetailScreen
import fi.kidozz.app.features.role.RoleSelectionScreen
import fi.kidozz.app.features.calendar.UpcomingEventsScreen
import fi.kidozz.app.features.calendar.PreviousEventsScreen
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
                onSuperEducatorViewClick = { navController.navigate("educator_dashboard") }, // Same as educator for now
                modifier = modifier
            )
        }

        composable("educator_dashboard") {
            // Set up real API services
            val baseUrl = "http://10.0.2.2:8000" // Android emulator localhost
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            val groupsApiService = retrofit.create(fi.kidozz.app.data.api.GroupsApiService::class.java)
            val educatorApiService = retrofit.create(fi.kidozz.app.data.api.EducatorApiService::class.java)
            val kidsApiService = retrofit.create(fi.kidozz.app.data.api.KidsApiService::class.java)
            
            val groupsRepository = fi.kidozz.app.data.repository.GroupsRepository(groupsApiService)
            val educatorRepository = fi.kidozz.app.data.repository.EducatorRepository(educatorApiService)
            val kidsRepository = fi.kidozz.app.data.repository.KidsRepository(kidsApiService)
            
            val groupsViewModel = remember { GroupsViewModel(groupsRepository) }
            val educatorViewModel = remember { EducatorViewModel(educatorRepository) }
            val kidsViewModel = remember { KidsViewModel(kidsRepository) }
            
            EducatorDashboardScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() },
                onKidClick = { kid ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("selectedKid", kid)
                    navController.navigate("kid_detail")
                },
                modifier = modifier,
                groupsViewModel = groupsViewModel,
                educatorViewModel = educatorViewModel,
                kidsViewModel = kidsViewModel,
                daycareId = "default-daycare-id"
            )
        }

        composable("kid_detail") {
            val kid = navController.previousBackStackEntry?.savedStateHandle?.get<Kid>("selectedKid")
            if (kid != null) {
                KidDetailScreen(
                    kid = kid,
                    onBackClick = { navController.popBackStack() }
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


