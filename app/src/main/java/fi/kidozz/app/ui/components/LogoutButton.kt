package fi.kidozz.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import fi.kidozz.app.data.auth.TokenManager

@Composable
fun LogoutButton(
    navController: NavController,
    tokenManager: TokenManager,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            tokenManager.clearAll()
            // Navigation will be handled automatically by MainActivity recomposition
            // when session.isLoggedIn becomes false or session.role becomes null
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = modifier
    ) {
        Text("Logout", color = MaterialTheme.colorScheme.onError)
    }
}
