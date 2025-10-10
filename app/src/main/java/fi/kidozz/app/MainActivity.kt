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
import kotlinx.coroutines.flow.filter
import fi.kidozz.app.BuildConfig
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.navigation.KiddozzAppHost
import fi.kidozz.app.ui.components.ParentBottomNavigation
import fi.kidozz.app.ui.components.EducatorBottomNavigation
import fi.kidozz.app.ui.theme.KiddozzTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class SessionState(val isLoggedIn: Boolean?, val role: String?)

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
                        KiddozzAppHost(
                            navController = navController,
                            role = null,
                            tokenManager = tokenManager
                        )
                    }

                    session.role == null -> {
                        KiddozzAppHost(
                            navController = navController,
                            role = null,
                            tokenManager = tokenManager
                        )
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
                            KiddozzAppHost(
                                navController = navController,
                                modifier = Modifier.padding(innerPadding),
                                role = session.role,
                                tokenManager = tokenManager
                            )
                        }
                    }
                }
            }
        }
    }
}