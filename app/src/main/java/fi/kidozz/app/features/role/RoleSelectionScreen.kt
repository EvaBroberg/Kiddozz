package fi.kidozz.app.features.role

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.BuildConfig
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.data.models.Educator
import fi.kidozz.app.data.models.Parent
import fi.kidozz.app.data.network.NetworkModule
import fi.kidozz.app.data.repository.AuthRepository
import fi.kidozz.app.ui.theme.KiddozzTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onEducatorViewClick: () -> Unit,
    onParentViewClick: () -> Unit,
    onSuperEducatorViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Only show role selection in staging builds
    if (!BuildConfig.DEBUG) {
        // In production, go directly to the main app
        LaunchedEffect(Unit) {
            onEducatorViewClick()
        }
        return
    }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val authRepository = remember {
        AuthRepository(
            NetworkModule.authApiService,
            TokenManager(context)
        )
    }
    
    fun handleRoleSelection(role: String) {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                // For now, we'll use a hardcoded daycare ID
                // In a real app, this would come from user selection or configuration
                val daycareId = "default-daycare-id"
                
                when (role) {
                    "educator" -> {
                        val result = authRepository.getEducators(daycareId)
                        result.fold(
                            onSuccess = { educators ->
                                val educator = educators.find { it.role == "educator" }
                                if (educator != null) {
                                    val loginResult = authRepository.devLoginAsEducator(educator.id)
                                    loginResult.fold(
                                        onSuccess = { tokenResponse ->
                                            Log.d("Kiddozz", "Logged in as educator ${educator.full_name}")
                                            Toast.makeText(
                                                context,
                                                "Hello, ${educator.full_name}!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            authRepository.loginWithToken(tokenResponse.access_token)
                                            onEducatorViewClick()
                                        },
                                        onFailure = { exception ->
                                            errorMessage = "Failed to login as educator: ${exception.message}"
                                        }
                                    )
                                } else {
                                    errorMessage = "No educator found"
                                }
                            },
                            onFailure = { exception ->
                                errorMessage = "Failed to fetch educators: ${exception.message}"
                            }
                        )
                    }
                    "parent" -> {
                        val result = authRepository.getParents(daycareId)
                        result.fold(
                            onSuccess = { parents ->
                                val parent = parents.find { it.full_name.lowercase().contains("sara") }
                                if (parent != null) {
                                    val loginResult = authRepository.devLoginAsParent(parent.id)
                                    loginResult.fold(
                                        onSuccess = { tokenResponse ->
                                            Log.d("Kiddozz", "Logged in as parent ${parent.full_name}")
                                            Toast.makeText(
                                                context,
                                                "Hello, ${parent.full_name}!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            authRepository.loginWithToken(tokenResponse.access_token)
                                            onParentViewClick()
                                        },
                                        onFailure = { exception ->
                                            errorMessage = "Failed to login as parent: ${exception.message}"
                                        }
                                    )
                                } else {
                                    errorMessage = "No parent found"
                                }
                            },
                            onFailure = { exception ->
                                errorMessage = "Failed to fetch parents: ${exception.message}"
                            }
                        )
                    }
                    "super_educator" -> {
                        val result = authRepository.getEducators(daycareId)
                        result.fold(
                            onSuccess = { educators ->
                                val educator = educators.find { it.role == "super_educator" }
                                if (educator != null) {
                                    val loginResult = authRepository.devLoginAsEducator(educator.id)
                                    loginResult.fold(
                                        onSuccess = { tokenResponse ->
                                            Log.d("Kiddozz", "Logged in as super educator ${educator.full_name}")
                                            Toast.makeText(
                                                context,
                                                "Hello, ${educator.full_name}!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            authRepository.loginWithToken(tokenResponse.access_token)
                                            onSuperEducatorViewClick()
                                        },
                                        onFailure = { exception ->
                                            errorMessage = "Failed to login as super educator: ${exception.message}"
                                        }
                                    )
                                } else {
                                    errorMessage = "No super educator found"
                                }
                            },
                            onFailure = { exception ->
                                errorMessage = "Failed to fetch educators: ${exception.message}"
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Kiddozz Daycare App - Staging") }) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to Kiddozz!")
            Text("Staging Environment", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading...")
            } else {
                Button(
                    onClick = { handleRoleSelection("educator") },
                    enabled = !isLoading
                ) { 
                    Text("Educator View") 
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { handleRoleSelection("parent") },
                    enabled = !isLoading
                ) { 
                    Text("Parent View") 
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { handleRoleSelection("super_educator") },
                    enabled = !isLoading
                ) { 
                    Text("Super-Educator View") 
                }
            }
            
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Role Selection Screen")
@Composable
fun RoleSelectionScreenPreview() {
    KiddozzTheme {
        RoleSelectionScreen(
            onEducatorViewClick = {}, 
            onParentViewClick = {},
            onSuperEducatorViewClick = {}
        )
    }
}