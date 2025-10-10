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
            navController.navigate("role_selection") { 
                popUpTo(0) 
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = modifier
    ) {
        Text("Logout", color = MaterialTheme.colorScheme.onError)
    }
}
