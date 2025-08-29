package fi.kidozz.app.features.role

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.ui.theme.KiddozzTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onEducatorViewClick: () -> Unit,
    onParentViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Kiddozz Daycare App") }) },
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
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onEducatorViewClick) { Text("Educator View") }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onParentViewClick) { Text("Parent View") }
        }
    }
}

@Preview(showBackground = true, name = "Role Selection Screen")
@Composable
fun RoleSelectionScreenPreview() {
    KiddozzTheme {
        RoleSelectionScreen(onEducatorViewClick = {}, onParentViewClick = {})
    }
}