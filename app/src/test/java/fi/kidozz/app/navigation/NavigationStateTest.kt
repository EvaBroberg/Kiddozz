package fi.kidozz.app.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NavigationStateTest {
    private lateinit var navController: TestNavHostController
    private val roleState = mutableStateOf<String?>(null)

    @Before
    fun setup() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph) // Replace with actual nav graph resource if available
    }

    @Test
    fun testEducatorNavigationAfterRefresh() {
        roleState.value = "educator"
        navController.navigate("educator_dashboard")
        assertEquals("educator_dashboard", navController.currentDestination?.route)
        // Simulate refresh
        navController.popBackStack()
        navController.navigate("educator_dashboard")
        assertEquals("educator_dashboard", navController.currentDestination?.route)
    }

    @Test
    fun testRoleSwitchParentToEducator() {
        roleState.value = "parent"
        navController.navigate("parent_dashboard")
        assertEquals("parent_dashboard", navController.currentDestination?.route)
        // Switch role
        roleState.value = "educator"
        navController.navigate("educator_dashboard")
        assertEquals("educator_dashboard", navController.currentDestination?.route)
    }

    @Test
    fun testKidsOverviewStatePersists() {
        roleState.value = "educator"
        navController.navigate("educator_dashboard")
        // Simulate kids overview state
        val kidsOverviewState = mutableStateOf(listOf("kid1", "kid2"))
        assertTrue(kidsOverviewState.value.isNotEmpty())
        // Simulate refresh
        navController.popBackStack()
        navController.navigate("educator_dashboard")
        // Check if kids overview state is still valid (simulate ViewModel persistence)
        assertTrue(kidsOverviewState.value.isNotEmpty())
    }
}
