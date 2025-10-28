package fi.kidozz.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.ui.components.LogoutButton

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
