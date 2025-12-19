package fi.kidozz.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.navigation.Routes
import fi.kidozz.app.ui.components.LogoutButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    navController: NavController,
    tokenManager: TokenManager
) {
    val menuItems = listOf(
        MenuItem("Messages", Icons.AutoMirrored.Filled.Message) {
            navController.navigate(fi.kidozz.app.features.messaging.nav.MessagingRoutes.MESSAGES_GRAPH) {
                launchSingleTop = true
            }
        },
        MenuItem("Switch Role", Icons.Default.SwapHoriz) {
            // Clear the current role so user can select a new one
            // This will trigger MainActivity to show the role selection screen
            tokenManager.clearRole()
            // Navigation will be handled automatically by MainActivity recomposition
        },
        MenuItem("Settings", Icons.Default.Settings) {
            // TODO: Navigate to settings
        },
        MenuItem("Help", Icons.AutoMirrored.Filled.Help) {
            // TODO: Navigate to help
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(menuItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = item.onClick
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

data class MenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

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