package fi.kidozz.app.features.invite

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.data.repository.AuthRepository

@Composable
fun AcceptInviteScreen(
    inviteToken: String,
    authRepository: AuthRepository,
    tokenManager: TokenManager,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var phoneNum by remember { mutableStateOf("") }
    var showPhoneField by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Log token prefix only (first 6 chars)
    val tokenPrefix = if (inviteToken.length >= 6) inviteToken.take(6) else inviteToken.take(inviteToken.length)
    LaunchedEffect(inviteToken) {
        Log.d("AcceptInvite", "Invite received: token_prefix='$tokenPrefix...'")
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Accept Invite",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Show token prefix (masked) for debugging - non-selectable to prevent accidental copying
        DisableSelection {
            Text(
                text = "Token: $tokenPrefix...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Name field (always visible)
        OutlinedTextField(
            value = name,
            onValueChange = { 
                name = it
                errorMessage = null
            },
            label = { Text("Full Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading,
            singleLine = true
        )
        
        // Phone field (shown if needed or if showPhoneField is true)
        if (showPhoneField) {
            OutlinedTextField(
                value = phoneNum,
                onValueChange = { 
                    phoneNum = it
                    errorMessage = null
                },
                label = { Text("Phone Number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !isLoading,
                singleLine = true
            )
        }
        
        // Error message
        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        // Accept button
        Button(
            onClick = {
                if (name.isBlank()) {
                    errorMessage = "Name is required"
                    return@Button
                }
                
                isLoading = true
                errorMessage = null
                
                // Log acceptance attempt
                Log.d("AcceptInvite", "Accept started: token_prefix='$tokenPrefix...', name='$name'")
                
                // Call accept-invite API
                coroutineScope.launch {
                    val result = authRepository.acceptInvite(
                        token = inviteToken,
                        name = name.trim(),
                        phoneNum = phoneNum.takeIf { it.isNotBlank() }
                    )
                    
                    result.fold(
                        onSuccess = { response ->
                            Log.d("AcceptInvite", "Accept success: token_prefix='$tokenPrefix...', user_id='${response.user_id}', role='${response.role}'")
                            // Save token
                            tokenManager.saveToken(response.access_token)
                            // Navigate to main app (will trigger /auth/me check)
                            onSuccess()
                        },
                        onFailure = { exception ->
                            isLoading = false
                            val errorMsg = exception.message ?: "Failed to accept invite"
                            Log.w("AcceptInvite", "Accept failed: token_prefix='$tokenPrefix...', error='$errorMsg'")
                            
                            // Check if error mentions phone_num - if so, show phone field
                            if (errorMsg.contains("phone", ignoreCase = true)) {
                                showPhoneField = true
                                errorMessage = errorMsg
                            } else {
                                errorMessage = errorMsg
                            }
                        }
                    )
                }
            },
            enabled = !isLoading && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Accept Invite")
            }
        }
    }
}

