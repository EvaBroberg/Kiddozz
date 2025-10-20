package fi.kidozz.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.sample.sampleKidsState
import fi.kidozz.app.features.dashboard.AbsenceReasonsViewModel
import fi.kidozz.app.features.dashboard.EducatorDashboardScreen
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.ui.components.LogoutButton
import fi.kidozz.app.features.dashboard.ParentDashboardScreen
import fi.kidozz.app.features.dashboard.GroupsViewModel
import fi.kidozz.app.features.dashboard.EducatorViewModel
import fi.kidozz.app.features.dashboard.KidsViewModel
import fi.kidozz.app.features.dashboard.ParentsViewModel
import fi.kidozz.app.features.kiddetail.KidDetailScreen
import fi.kidozz.app.features.role.RoleSelectionScreen
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun KiddozzAppHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    role: String?,
    tokenManager: fi.kidozz.app.data.auth.TokenManager
) {
    NavHost(
        navController = navController,
        startDestination = "role_selection"
    ) {
        composable("role_selection") {
            RoleSelectionScreen(
                tokenManager = tokenManager,
                onEducatorViewClick = { navController.navigate("educator_dashboard") },
                onParentViewClick = { navController.navigate("parent_dashboard") },
                onSuperEducatorViewClick = { navController.navigate("educator_dashboard") }
            )
        }

        composable("educator_dashboard") {
            val context = LocalContext.current
            val baseUrl = "http://10.0.2.2:8000"
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            val groupsApiService = retrofit.create(fi.kidozz.app.data.api.GroupsApiService::class.java)
            val educatorApiService = retrofit.create(fi.kidozz.app.data.api.EducatorApiService::class.java)
            val kidsApiService = retrofit.create(fi.kidozz.app.data.api.KidsApiService::class.java)
            
            val groupsRepository = fi.kidozz.app.data.repository.GroupsRepository(groupsApiService)
            val educatorRepository = fi.kidozz.app.data.repository.EducatorRepository(educatorApiService)
            // Use the shared TokenManager instance passed from MainActivity
            val kidsRepository = fi.kidozz.app.data.repository.KidsRepository(kidsApiService, tokenManager)
            
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

        composable("parent_dashboard") {
            val context = LocalContext.current
            val baseUrl = "http://10.0.2.2:8000"
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            val parentsApiService = retrofit.create(fi.kidozz.app.data.api.ParentsApiService::class.java)
            val kidsApiService = retrofit.create(fi.kidozz.app.data.api.KidsApiService::class.java)
            val parentsRepository = fi.kidozz.app.data.repository.ParentsRepository(parentsApiService)
            // Use the shared TokenManager instance passed from MainActivity
            val kidsRepository = fi.kidozz.app.data.repository.KidsRepository(kidsApiService, tokenManager)
            val parentsViewModel = remember { ParentsViewModel(parentsRepository) }
            val absenceReasonsViewModel = remember { AbsenceReasonsViewModel(kidsRepository) }
            
            ParentDashboardScreen(
                parentId = "10",
                parentsViewModel = parentsViewModel,
                absenceReasonsViewModel = absenceReasonsViewModel,
                kidsRepository = kidsRepository,
                modifier = modifier
            )
        }

        composable("kid_detail") {
            val context = LocalContext.current
            val kid = navController.previousBackStackEntry?.savedStateHandle?.get<Kid>("selectedKid")
            if (kid != null) {
                // Set up API services for KidDetailScreen
                val baseUrl = "http://10.0.2.2:8000" // Android emulator localhost
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                
                val kidsApiService = retrofit.create(fi.kidozz.app.data.api.KidsApiService::class.java)
                // Use the shared TokenManager instance passed from MainActivity
                val kidsRepository = fi.kidozz.app.data.repository.KidsRepository(kidsApiService, tokenManager)
                val kidsViewModel = remember { KidsViewModel(kidsRepository) }
                
                KidDetailScreen(
                    kid = kid,
                    onBackClick = { navController.popBackStack() },
                    kidsViewModel = kidsViewModel
                )
            }
        }

        composable("calendar") {
            if (role == "parent") {
                ParentCalendarScreen()
            } else {
                fi.kidozz.app.features.calendar.EducatorCalendarScreen(
                    navController = navController,
                    modifier = modifier
                )
            }
        }

        composable("menu") { MenuScreen() }

        composable("profile") { 
            ProfileScreen(
                navController = navController,
                tokenManager = tokenManager
            ) 
        }

    }
}

@Composable
fun ParentCalendarScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Parent calendar (placeholder)")
    }
}

@Composable
fun MenuScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Menu (placeholder)")
    }
}

@Composable
fun ProfileScreen(
    navController: NavController,
    tokenManager: TokenManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        LogoutButton(
            navController = navController,
            tokenManager = tokenManager
        )
    }
}
