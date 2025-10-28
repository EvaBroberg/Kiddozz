package fi.kidozz.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.filter
import fi.kidozz.app.BuildConfig
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.navigation.Routes
import fi.kidozz.app.ui.components.ParentBottomNavigation
import fi.kidozz.app.ui.components.EducatorBottomNavigation
import fi.kidozz.app.ui.theme.KiddozzTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class SessionState(val isLoggedIn: Boolean?, val role: String?)

// Navigation helper functions
private fun androidx.navigation.NavController.navigateToKidsOverview() {
    navigate(Routes.KIDS_OVERVIEW) {
        popUpTo(Routes.EDU_GRAPH) { inclusive = false }
        launchSingleTop = true
    }
}

private fun androidx.navigation.NavController.navigateToCalendar() {
    navigate(Routes.CALENDAR) { launchSingleTop = true }
}

private fun androidx.navigation.NavController.navigateToKidDetail(kidId: String) {
    navigate(Routes.kidDetail(kidId))
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Log the active backend URL for debugging
        Log.d("Kiddozz", "Backend URL: " + BuildConfig.BASE_URL)
        Log.d("Kiddozz", "MainActivity onCreate called")

        // Compose entrypoint: Initialize theme, navigation, and role-based routing
        setContent {
            KiddozzTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val tokenManager = remember { TokenManager(context) }
                val role by tokenManager.roleFlow.collectAsState(initial = tokenManager.getRole())
                val token by tokenManager.tokenFlow.collectAsState(initial = tokenManager.getToken())
                val loggedIn = !token.isNullOrEmpty()

                val session = remember(role, loggedIn) {
                    SessionState(isLoggedIn = loggedIn, role = role)
                }

                // Hoist ViewModels and repositories at the top level for shared state
                val baseUrl = "http://10.0.2.2:8000"
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                
                val groupsApiService = retrofit.create(fi.kidozz.app.data.api.GroupsApiService::class.java)
                val educatorApiService = retrofit.create(fi.kidozz.app.data.api.EducatorApiService::class.java)
                val kidsApiService = retrofit.create(fi.kidozz.app.data.api.KidsApiService::class.java)
                val parentsApiService = retrofit.create(fi.kidozz.app.data.api.ParentsApiService::class.java)
                val messagingApiService = retrofit.create(fi.kidozz.app.features.messaging.data.api.MessagingApiService::class.java)
                
                val groupsRepository = fi.kidozz.app.data.repository.GroupsRepository(groupsApiService)
                val educatorRepository = fi.kidozz.app.data.repository.EducatorRepository(educatorApiService)
                val kidsRepository = fi.kidozz.app.data.repository.KidsRepository(kidsApiService, tokenManager)
                val parentsRepository = fi.kidozz.app.data.repository.ParentsRepository(parentsApiService)
                
                // Messaging dependencies
                val messagingDatabase = fi.kidozz.app.features.messaging.data.db.MessagingDatabase.getDatabase(context)
                val messagingDao = messagingDatabase.messagingDao()
                val messagingWebSocketClient = fi.kidozz.app.features.messaging.data.ws.MessagingWebSocketClient()
                val messagingRepository = fi.kidozz.app.features.messaging.data.repo.MessagingRepositoryImpl(
                    messagingApiService, messagingDao, messagingWebSocketClient
                )
                
                // Single instances for the whole NavHost lifetime
                val groupsViewModel = remember { fi.kidozz.app.features.dashboard.GroupsViewModel(groupsRepository) }
                val educatorViewModel = remember { fi.kidozz.app.features.dashboard.EducatorViewModel(educatorRepository) }
                val kidsViewModel = remember { fi.kidozz.app.features.dashboard.KidsViewModel(kidsRepository) }
                val parentsViewModel = remember { fi.kidozz.app.features.dashboard.ParentsViewModel(parentsRepository) }
                val absenceReasonsViewModel = remember { fi.kidozz.app.features.dashboard.AbsenceReasonsViewModel(kidsRepository) }
                val messagingViewModel = remember { fi.kidozz.app.features.messaging.ui.MessagingViewModel(messagingRepository) }

                // Add this debugging log block:
                LaunchedEffect(session.role) {
                    Log.d("KiddozzSession", "Session updated: logged=${session.isLoggedIn} role='${session.role}'")
                }

                when {
                    session.isLoggedIn == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    session.isLoggedIn == false -> {
                        // Show only role selection, no bottom nav
                        NavHost(
                            navController = navController,
                            startDestination = Routes.ROLE_SELECTION
                        ) {
                            composable(Routes.ROLE_SELECTION) {
                                fi.kidozz.app.features.role.RoleSelectionScreen(
                                    tokenManager = tokenManager,
                                    onEducatorViewClick = { navController.navigate(Routes.EDU_GRAPH) },
                                    onParentViewClick = { navController.navigate("parent_dashboard") },
                                    onSuperEducatorViewClick = { navController.navigate(Routes.EDU_GRAPH) }
                                )
                            }
                        }
                    }

                    session.role == null -> {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.ROLE_SELECTION
                        ) {
                            composable(Routes.ROLE_SELECTION) {
                                fi.kidozz.app.features.role.RoleSelectionScreen(
                                    tokenManager = tokenManager,
                                    onEducatorViewClick = { navController.navigate(Routes.EDU_GRAPH) },
                                    onParentViewClick = { navController.navigate("parent_dashboard") },
                                    onSuperEducatorViewClick = { navController.navigate(Routes.EDU_GRAPH) }
                                )
                            }
                        }
                    }

                    else -> {
                        Scaffold(
                            bottomBar = {
                                when (session.role?.lowercase()) {
                                    "educator" -> EducatorBottomNavigation(navController)
                                    "super_educator" -> EducatorBottomNavigation(navController)
                                    "parent" -> ParentBottomNavigation(navController)
                                    else -> Spacer(Modifier.height(0.dp))
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = Routes.ROLE_SELECTION,
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                composable(Routes.ROLE_SELECTION) {
                                    fi.kidozz.app.features.role.RoleSelectionScreen(
                                        tokenManager = tokenManager,
                                        onEducatorViewClick = { navController.navigate(Routes.EDU_GRAPH) },
                                        onParentViewClick = { navController.navigate("parent_dashboard") },
                                        onSuperEducatorViewClick = { navController.navigate(Routes.EDU_GRAPH) }
                                    )
                                }

                                navigation(
                                    startDestination = Routes.KIDS_OVERVIEW,
                                    route = Routes.EDU_GRAPH
                                ) {
                                    composable(Routes.KIDS_OVERVIEW) {
                                        fi.kidozz.app.features.dashboard.EducatorDashboardScreen(
                                            onSelectKidsOverview = { navController.navigateToKidsOverview() },
                                            onSelectCalendar = { navController.navigateToCalendar() },
                                            content = {
                                                // Create a wrapper that uses the ViewModels
                                                fi.kidozz.app.features.dashboard.EducatorDashboardContent(
                                                    groupsViewModel = groupsViewModel,
                                                    educatorViewModel = educatorViewModel,
                                                    kidsViewModel = kidsViewModel,
                                                    daycareId = "default-daycare-id",
                                                    onKidClick = { kidId -> navController.navigateToKidDetail(kidId) }
                                                )
                                            },
                                            groupsViewModel = groupsViewModel,
                                            educatorViewModel = educatorViewModel,
                                            kidsViewModel = kidsViewModel,
                                            daycareId = "default-daycare-id"
                                        )
                                    }
                                    composable(Routes.CALENDAR) {
                                        fi.kidozz.app.features.dashboard.EducatorDashboardScreen(
                                            onSelectKidsOverview = { navController.navigateToKidsOverview() },
                                            onSelectCalendar = { /* already here */ },
                                            content = {
                                                fi.kidozz.app.features.calendar.EducatorCalendarScreen(
                                                    navController = navController
                                                )
                                            },
                                            groupsViewModel = groupsViewModel,
                                            educatorViewModel = educatorViewModel,
                                            kidsViewModel = kidsViewModel,
                                            daycareId = "default-daycare-id"
                                        )
                                    }
                                }

                                navigation(
                                    startDestination = fi.kidozz.app.features.messaging.nav.MessagingRoutes.MESSAGES_LIST,
                                    route = fi.kidozz.app.features.messaging.nav.MessagingRoutes.MESSAGES_GRAPH
                                ) {
                                    composable(fi.kidozz.app.features.messaging.nav.MessagingRoutes.MESSAGES_LIST) {
                                        fi.kidozz.app.features.messaging.ui.MessagesListScreen(
                                            onOpenConversation = { id ->
                                                navController.navigate(fi.kidozz.app.features.messaging.nav.MessagingRoutes.conversation(id))
                                            },
                                            onBack = { navController.popBackStack() },
                                            viewModel = messagingViewModel
                                        )
                                    }
                                    composable(
                                        route = fi.kidozz.app.features.messaging.nav.MessagingRoutes.CONVERSATION_ROUTE,
                                        arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
                                    ) { backStackEntry ->
                                        val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
                                        fi.kidozz.app.features.messaging.ui.ConversationScreen(
                                            conversationId = conversationId,
                                            onBack = { navController.popBackStack() },
                                            viewModel = messagingViewModel
                                        )
                                    }
                                }

                                composable(
                                    route = Routes.KID_DETAIL_ROUTE,
                                    arguments = listOf(navArgument("kidId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val kidId = backStackEntry.arguments?.getString("kidId").orEmpty()
                                    fi.kidozz.app.features.kiddetail.KidDetailScreen(
                                        kidId = kidId,
                                        onBack = { navController.popBackStack() },
                                        kidsViewModel = kidsViewModel
                                    )
                                }

                                composable("parent_dashboard") {
                                    fi.kidozz.app.features.dashboard.ParentDashboardScreen(
                                        parentId = "10",
                                        parentsViewModel = parentsViewModel,
                                        absenceReasonsViewModel = absenceReasonsViewModel,
                                        kidsRepository = kidsRepository
                                    )
                                }

                                composable("menu") { 
                                    fi.kidozz.app.navigation.MenuScreen(navController = navController) 
                                }

                                composable("profile") { 
                                    fi.kidozz.app.navigation.ProfileScreen(
                                        navController = navController,
                                        tokenManager = tokenManager
                                    ) 
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}