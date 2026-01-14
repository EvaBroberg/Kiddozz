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
import fi.kidozz.app.core.auth.mapServerRoleToAppRole
import fi.kidozz.app.core.config.AuthConfig
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.data.repository.AuthRepository
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
    
    /**
     * Extract invite token from deep link intent.
     * Returns token if found, null otherwise.
     * Logs token prefix only (never full token).
     */
    internal fun extractInviteToken(intent: android.content.Intent?): String? {
        val data = intent?.data
        if (data != null && data.scheme == "kiddozz" && data.host == "invite") {
            val token = data.getQueryParameter("token")
            if (token != null) {
                val tokenPrefix = if (token.length >= 6) token.take(6) else token.take(token.length)
                Log.d("MainActivity", "Deep link received: token_prefix='$tokenPrefix...'")
                return token
            }
        }
        return null
    }
    
    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Intent will be checked in LaunchedEffect below
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d("Kiddozz", "MainActivity onCreate called")
        
        // Log AUTH_MODE once on app launch
        Log.d("MainActivity", "🔐 AUTH_MODE = ${AuthConfig.authMode}")
        
        // Extract invite token from intent (cold start)
        val initialInviteToken = extractInviteToken(intent)

        // Compose entrypoint: Initialize theme, navigation, and role-based routing
        setContent {
            KiddozzTheme {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }
                val token by tokenManager.tokenFlow.collectAsState(initial = tokenManager.getToken())
                val authUserId by tokenManager.userIdFlow.collectAsState()
                val authDaycareId by tokenManager.daycareIdFlow.collectAsState()
                val loggedIn = !token.isNullOrEmpty()

                // Server-authoritative role state (from /auth/me)
                var serverRole by remember { mutableStateOf<String?>(null) }
                var isResolvingAuth by remember { mutableStateOf(false) }
                
                // Use a single NavController
                // Navigation is handled automatically by NavHost recomposition when session state changes
                val navController = rememberNavController()
                
                // State for invite token from deep link
                var inviteToken by remember { mutableStateOf<String?>(initialInviteToken) }
                
                // Handle onNewIntent: check current intent for invite token
                val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
                LaunchedEffect(Unit) {
                    activity?.let {
                        // Check current intent for invite token (handles onNewIntent case)
                        val currentIntentToken = it.extractInviteToken(it.intent)
                        if (currentIntentToken != null && inviteToken != currentIntentToken) {
                            inviteToken = currentIntentToken
                        }
                    }
                }

                // Hoist ViewModels and repositories at the top level for shared state
                val baseUrl: String = BuildConfig.BASE_URL
                
                // Log BASE_URL once on initialization
                LaunchedEffect(baseUrl) {
                    if (BuildConfig.DEBUG) {
                        Log.d("MainActivity", "BASE_URL: $baseUrl")
                    }
                }
                
                // Create OkHttpClient with AuthInterceptor for authenticated requests
                // Remember to avoid recreating on every recomposition
                val okHttpClient = remember(tokenManager) {
                    okhttp3.OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val request = chain.request()
                            val authToken = tokenManager.getToken()
                            val newRequest = if (authToken != null) {
                                request.newBuilder()
                                    .header("Authorization", "Bearer $authToken")
                                    .build()
                            } else {
                                request
                            }
                            chain.proceed(newRequest)
                        }
                        .build()
                }
                
                // Remember Retrofit to avoid recreating on every recomposition
                val retrofit = remember(baseUrl, okHttpClient) {
                    Retrofit.Builder()
                        .baseUrl(BuildConfig.BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
                
                val authApiService = retrofit.create(fi.kidozz.app.data.api.AuthApiService::class.java)
                val groupsApiService = retrofit.create(fi.kidozz.app.data.api.GroupsApiService::class.java)
                val educatorApiService = retrofit.create(fi.kidozz.app.data.api.EducatorApiService::class.java)
                val kidsApiService = retrofit.create(fi.kidozz.app.data.api.KidsApiService::class.java)
                val parentsApiService = retrofit.create(fi.kidozz.app.data.api.ParentsApiService::class.java)
                val messagingApiService = retrofit.create(fi.kidozz.app.features.messaging.data.api.MessagingApiService::class.java)
                
                // Server-authoritative role resolution: call /auth/me if token exists
                val authRepository = remember(authApiService, tokenManager) { AuthRepository(authApiService, tokenManager) }
                
                LaunchedEffect(token, authRepository) {
                    if (token != null && !isResolvingAuth) {
                        Log.d("MainActivity", "🔐 Token present, calling /auth/me for server-authoritative role")
                        isResolvingAuth = true
                        val result = authRepository.getCurrentUserInfo()
                        result.fold(
                            onSuccess = { userInfo ->
                                val mappedRole = mapServerRoleToAppRole(userInfo.role)
                                serverRole = mappedRole
                                Log.d("MainActivity", "✅ /auth/me success: role='${userInfo.role}' → mapped='$mappedRole', user_id='${userInfo.user_id}', daycare_id='${userInfo.daycare_id}'")
                                isResolvingAuth = false
                            },
                            onFailure = { exception ->
                                Log.e("MainActivity", "❌ /auth/me failed: ${exception.message}")
                                // On 401 (unauthorized), clear token to force re-login
                                if (exception.message?.contains("401") == true || exception.message?.contains("Unauthorized") == true) {
                                    Log.d("MainActivity", "🔐 401 response, clearing token")
                                    tokenManager.clearToken()
                                    serverRole = null
                                }
                                isResolvingAuth = false
                            }
                        )
                    } else if (token == null) {
                        Log.d("MainActivity", "🔐 Token missing, skipping /auth/me")
                        serverRole = null
                        isResolvingAuth = false
                    }
                }
                
                // Session state based on server-authoritative role
                val session = remember(serverRole, loggedIn, isResolvingAuth) {
                    SessionState(
                        isLoggedIn = if (isResolvingAuth) null else loggedIn,
                        role = serverRole
                    )
                }
                
                val groupsRepository = fi.kidozz.app.data.repository.GroupsRepository(groupsApiService)
                val educatorRepository = fi.kidozz.app.data.repository.EducatorRepository(educatorApiService)
                val kidsRepository = fi.kidozz.app.data.repository.KidsRepository(kidsApiService, tokenManager)
                val parentsRepository = fi.kidozz.app.data.repository.ParentsRepository(parentsApiService)
                
                // Messaging dependencies
                val messagingDatabase = fi.kidozz.app.features.messaging.data.db.MessagingDatabase.getDatabase(context)
                val messagingDao = messagingDatabase.messagingDao()
                val messagingWebSocketClient = fi.kidozz.app.features.messaging.data.ws.MessagingWebSocketClient()
                
                // Create SSE client for real-time message events (reuse the same OkHttpClient with auth)
                val messagingSseClient = fi.kidozz.app.features.messaging.data.ws.MessagingSseClient(
                    okHttpClient = okHttpClient,
                    baseUrl = baseUrl,
                    authTokenProvider = { tokenManager.getToken() }
                )
                
                // Create UserSessionManager - will be updated when educator/parent data loads
                val sessionManager = remember {
                    fi.kidozz.app.core.session.UserSessionManager(
                        fi.kidozz.app.core.session.UserSession(
                            userId = authUserId ?: "unknown", // Will be updated from token or educator/parent data
                            role = if (serverRole in listOf("educator", "super_educator")) 
                                fi.kidozz.app.core.session.UserRole.EDUCATOR 
                            else 
                                fi.kidozz.app.core.session.UserRole.PARENT,
                            groupIds = emptySet() // Will be updated when educator/parent loads
                        )
                    )
                }
                
                // Update session.role whenever serverRole changes (server-authoritative)
                LaunchedEffect(serverRole) {
                    val mappedRole = if (serverRole in listOf("educator", "super_educator")) 
                        fi.kidozz.app.core.session.UserRole.EDUCATOR 
                    else 
                        fi.kidozz.app.core.session.UserRole.PARENT
                    
                    val oldRole = sessionManager.session.value.role
                    if (oldRole != mappedRole) {
                        sessionManager.update(
                            sessionManager.session.value.copy(role = mappedRole)
                        )
                        Log.d("SessionRole", "serverRole='$serverRole' → mapped=$mappedRole, session.role was $oldRole")
                    }
                }
                
                // Single instances for the whole NavHost lifetime
                val groupsViewModel = remember { fi.kidozz.app.features.dashboard.GroupsViewModel(groupsRepository) }
                val educatorViewModel = remember { fi.kidozz.app.features.dashboard.EducatorViewModel(educatorRepository) }
                val educatorsListViewModel = remember { fi.kidozz.app.features.dashboard.EducatorsListViewModel(educatorRepository) }
                val kidsViewModel = remember { fi.kidozz.app.features.dashboard.KidsViewModel(kidsRepository) }
                val parentsViewModel = remember { fi.kidozz.app.features.dashboard.ParentsViewModel(parentsRepository) }
                val absenceReasonsViewModel = remember { fi.kidozz.app.features.dashboard.AbsenceReasonsViewModel(kidsRepository) }
                
                val messagingRepository = fi.kidozz.app.features.messaging.data.repo.MessagingRepositoryImpl(
                    messagingApiService, messagingDao, messagingWebSocketClient, messagingSseClient, tokenManager,
                    kidsViewModel.kids, educatorsListViewModel.educators
                )
                
                val messagingViewModel = remember { fi.kidozz.app.features.messaging.ui.MessagingViewModel(messagingRepository, sessionManager) }

                // Load educators for current daycare (from auth token)
                val daycareId = authDaycareId ?: run {
                    Log.w("MainActivity", "Daycare ID not found in token, using fallback")
                    null
                }
                LaunchedEffect(daycareId) {
                    if (daycareId != null && daycareId.isNotBlank()) {
                        Log.d("MainActivity", "Loading data for daycare: $daycareId")
                        educatorsListViewModel.load(daycareId)
                        // Load current educator from token userId if available
                        if (authUserId != null && serverRole in listOf("educator", "super_educator")) {
                            // Load educator by ID from token
                            educatorViewModel.loadCurrentEducatorById(daycareId, authUserId)
                        }
                        kidsViewModel.loadKids(daycareId)
                    } else {
                        Log.w("MainActivity", "Cannot load data: daycare ID is null or empty")
                    }
                }
                
                // Update session with educator's user ID and group IDs when educator loads (reactive)
                // NOTE: Do NOT update role here - role comes from server (/auth/me)
                val currentEducator by educatorViewModel.currentEducator.collectAsState()
                LaunchedEffect(currentEducator, serverRole, authUserId) {
                    val educator = currentEducator
                    if (educator != null && serverRole in listOf("educator", "super_educator")) {
                        // Use educator.id from DB (not from token, as token may have different format)
                        val educatorUserId = educator.id
                        val educatorGroupIds = educator.groups.map { it.id }.toSet()
                        sessionManager.update(
                            sessionManager.session.value.copy(
                                userId = educatorUserId, // ✅ Set userId from educator.id
                                groupIds = educatorGroupIds // ✅ Set groupIds from educator.groups
                                // ✅ Do NOT update role - it comes from TokenManager.roleFlow
                            )
                        )
                        Log.d("EducatorSession", "userId=$educatorUserId, groupIds=$educatorGroupIds")
                        Log.d("SessionUpdate", "role=EDUCATOR, userId=$educatorUserId, groupIds=$educatorGroupIds, daycareId=$daycareId")
                    }
                }
                
                // Update session with parent's user ID and group IDs when kids load (for parents, reactive)
                // NOTE: Do NOT update role here - role comes from server (/auth/me)
                val kids by kidsViewModel.kids.collectAsState()
                
                // Get parent ID from JWT token (sub claim)
                val currentParentId = authUserId
                
                LaunchedEffect(kids, serverRole, currentParentId) {
                    if (serverRole == "parent" && currentParentId != null) {
                        // Filter kids to only those belonging to the logged-in parent
                        val myKids = kids.filter { kid ->
                            kid.parents.any { parent -> parent.id == currentParentId }
                        }
                        
                        Log.d("UserSession", "Parent $currentParentId: found ${myKids.size} kids out of ${kids.size} total kids")
                        
                        // Extract group IDs from only this parent's kids
                        val parentGroupIds = myKids.map { it.group_id }.toSet() // group_id is String, not nullable
                        
                        // Guardrail: warn if parent ID is a placeholder
                        if (currentParentId == "current_user" || currentParentId == "unknown_parent") {
                            Log.w("UserSession", "WARNING: Using placeholder parent ID: $currentParentId. Session may be incorrect.")
                            sessionManager.update(
                                sessionManager.session.value.copy(
                                    userId = currentParentId, // ✅ Set userId from JWT token
                                    groupIds = emptySet() // Empty to prevent showing everyone
                                    // ✅ Do NOT update role - it comes from TokenManager.roleFlow
                                )
                            )
                        } else if (parentGroupIds.isNotEmpty()) {
                            sessionManager.update(
                                sessionManager.session.value.copy(
                                    userId = currentParentId, // ✅ Set userId from JWT token
                                    groupIds = parentGroupIds // ✅ Set groupIds from parent's kids
                                    // ✅ Do NOT update role - it comes from TokenManager.roleFlow
                                )
                            )
                            Log.d("ParentSession", "userId=$currentParentId, groupIds=$parentGroupIds (from ${myKids.size} kids)")
                            Log.d("SessionUpdate", "role=PARENT, userId=$currentParentId, groupIds=$parentGroupIds, daycareId=$daycareId (from ${myKids.size} kids: ${myKids.map { "${it.full_name}(g${it.group_id})" }})")
                        } else {
                            Log.w("UserSession", "No group IDs found for parent $currentParentId from ${myKids.size} kids!")
                            // Keep session with empty groupIds to show empty state (not everyone)
                            sessionManager.update(
                                sessionManager.session.value.copy(
                                    userId = currentParentId, // ✅ Set userId from JWT token
                                    groupIds = emptySet() // ✅ Set empty groupIds
                                    // ✅ Do NOT update role - it comes from TokenManager.roleFlow
                                )
                            )
                            Log.d("ParentSession", "userId=$currentParentId, groupIds=empty (from ${myKids.size} kids)")
                            Log.d("SessionUpdate", "role=PARENT, userId=$currentParentId, groupIds=empty, daycareId=$daycareId")
                        }
                    }
                }

                // Add this debugging log block:
                LaunchedEffect(serverRole) {
                    Log.d("KiddozzSession", "Session updated: logged=${session.isLoggedIn} serverRole='$serverRole'")
                }
                
                // Debug session group IDs
                val currentSession by sessionManager.session.collectAsState()
                LaunchedEffect(currentSession.groupIds) {
                    Log.d("KiddozzSession", "Session group IDs: ${currentSession.groupIds}")
                }

                when {
                    // If invite token is present, show accept invite screen
                    inviteToken != null -> {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.ACCEPT_INVITE
                        ) {
                            composable(Routes.ACCEPT_INVITE) {
                                fi.kidozz.app.features.invite.AcceptInviteScreen(
                                    inviteToken = inviteToken!!,
                                    authRepository = authRepository,
                                    tokenManager = tokenManager,
                                    onSuccess = {
                                        // Clear invite token and let normal auth flow handle routing
                                        inviteToken = null
                                        // Token is saved, so next recomposition will trigger /auth/me check
                                    }
                                )
                            }
                        }
                    }
                    
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
                                when (serverRole?.lowercase()) {
                                    "educator" -> EducatorBottomNavigation(navController)
                                    "super_educator" -> EducatorBottomNavigation(navController)
                                    "parent" -> ParentBottomNavigation(navController)
                                    else -> Spacer(Modifier.height(0.dp))
                                }
                            }
                        ) { innerPadding ->
                            // Determine start destination based on server-authoritative role
                            val startDestination = when (serverRole?.lowercase()) {
                                "educator", "super_educator" -> Routes.EDU_GRAPH
                                "parent" -> "parent_dashboard"
                                else -> Routes.ROLE_SELECTION
                            }
                            NavHost(
                                navController = navController,
                                startDestination = startDestination,
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
                                                    daycareId = daycareId ?: "default-daycare-id",
                                                    onKidClick = { kidId -> navController.navigateToKidDetail(kidId) }
                                                )
                                            },
                                            groupsViewModel = groupsViewModel,
                                            educatorViewModel = educatorViewModel,
                                            kidsViewModel = kidsViewModel,
                                            daycareId = daycareId ?: "default-daycare-id"
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
                                            daycareId = daycareId ?: "default-daycare-id"
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
                                                try {
                                                    val route = fi.kidozz.app.features.messaging.nav.MessagingRoutes.conversation(id)
                                                    android.util.Log.d("MainActivity", "Navigating to conversation route: $route with id: $id")
                                                    navController.navigate(route)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("MainActivity", "Navigation failed for conversationId: $id", e)
                                                    e.printStackTrace()
                                                }
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
                                        parentId = authUserId ?: "unknown_parent",
                                        parentsViewModel = parentsViewModel,
                                        absenceReasonsViewModel = absenceReasonsViewModel,
                                        kidsRepository = kidsRepository
                                    )
                                }

                                composable("menu") { 
                                    fi.kidozz.app.navigation.MenuScreen(
                                        navController = navController,
                                        tokenManager = tokenManager
                                    ) 
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