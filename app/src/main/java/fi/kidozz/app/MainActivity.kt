package fi.kidozz.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import fi.kidozz.app.navigation.KiddozzAppHost
import fi.kidozz.app.ui.theme.KiddozzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Log the active backend URL for debugging
        Log.d("Kiddozz", "Backend URL: " + BuildConfig.BASE_URL)
        
        setContent {
            KiddozzTheme {
                val navController = rememberNavController()
                KiddozzAppHost(navController = navController)
            }
        }
    }
}