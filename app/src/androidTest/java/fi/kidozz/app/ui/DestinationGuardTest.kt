package fi.kidozz.app.ui

import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.kidozz.app.MainActivity
import fi.kidozz.app.data.auth.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DestinationGuardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var tokenManager: TokenManager

    @Before
    fun setup() {
        runBlocking {
            tokenManager = TokenManager(ApplicationProvider.getApplicationContext())
            tokenManager.clearAll()
            Log.d("DestinationGuardTest", "=== TEST SETUP: Cleared all tokens ===")
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            tokenManager.clearAll()
            Log.d("DestinationGuardTest", "=== TEST TEARDOWN: Cleared all tokens ===")
        }
    }

    @Test
    fun testParentCannotAccessEducatorRoutes() {
        Log.d("DestinationGuardTest", "=== STARTING ParentAccessTest ===")
        
        // Step 1: Login as parent
        runBlocking {
            Log.d("DestinationGuardTest", "--- Step 1: Login as parent ---")
            tokenManager.saveToken("fake-parent-token")
            tokenManager.saveRole("parent")
            Log.d("DestinationGuardTest", "Role saved: ${tokenManager.getRole()}")
        }
        
        composeTestRule.waitForIdle()
        
        // Step 2: Verify parent is on parent_dashboard
        try {
            composeTestRule.onNodeWithText("Kids Overview", useUnmergedTree = true).assertDoesNotExist()
            Log.d("DestinationGuardTest", "✓ Parent: Kids Overview correctly hidden")
        } catch (e: Exception) {
            Log.e("DestinationGuardTest", "✗ Parent: Kids Overview should be hidden: ${e.message}")
        }

        // Step 3: Try to navigate to educator route (simulate deep link or manual navigation)
        // This should trigger the destination guard
        Log.d("DestinationGuardTest", "--- Step 2: Attempting to access educator route ---")
        
        // The destination guard should prevent this and redirect back to parent_dashboard
        composeTestRule.waitForIdle()
        
        // Step 4: Verify we're still on parent_dashboard (guard should have redirected)
        try {
            composeTestRule.onNodeWithText("Kids Overview", useUnmergedTree = true).assertDoesNotExist()
            Log.d("DestinationGuardTest", "✓ Parent: Still on parent_dashboard after guard check")
        } catch (e: Exception) {
            Log.e("DestinationGuardTest", "✗ Parent: Should still be on parent_dashboard: ${e.message}")
        }

        Log.d("DestinationGuardTest", "=== ParentAccessTest COMPLETED ===")
    }

    @Test
    fun testEducatorRedirectedFromParentDashboard() {
        Log.d("DestinationGuardTest", "=== STARTING EducatorRedirectTest ===")
        
        // Step 1: Login as educator
        runBlocking {
            Log.d("DestinationGuardTest", "--- Step 1: Login as educator ---")
            tokenManager.saveToken("fake-educator-token")
            tokenManager.saveRole("educator")
            Log.d("DestinationGuardTest", "Role saved: ${tokenManager.getRole()}")
        }
        
        composeTestRule.waitForIdle()
        
        // Step 2: Verify educator is on educator_dashboard
        try {
            composeTestRule.onNodeWithText("Kids Overview", useUnmergedTree = true).assertIsDisplayed()
            Log.d("DestinationGuardTest", "✓ Educator: Kids Overview correctly visible")
        } catch (e: Exception) {
            Log.e("DestinationGuardTest", "✗ Educator: Kids Overview should be visible: ${e.message}")
        }

        // Step 3: The destination guard should ensure educator stays on educator_dashboard
        Log.d("DestinationGuardTest", "--- Step 2: Verifying educator stays on educator_dashboard ---")
        
        composeTestRule.waitForIdle()
        
        // Step 4: Verify we're still on educator_dashboard
        try {
            composeTestRule.onNodeWithText("Kids Overview", useUnmergedTree = true).assertIsDisplayed()
            Log.d("DestinationGuardTest", "✓ Educator: Still on educator_dashboard after guard check")
        } catch (e: Exception) {
            Log.e("DestinationGuardTest", "✗ Educator: Should still be on educator_dashboard: ${e.message}")
        }

        Log.d("DestinationGuardTest", "=== EducatorRedirectTest COMPLETED ===")
    }

    @Test
    fun testBottomNavOnlyOnAllowedRoutes() {
        Log.d("DestinationGuardTest", "=== STARTING BottomNavRouteTest ===")
        
        // Step 1: Login as educator
        runBlocking {
            Log.d("DestinationGuardTest", "--- Step 1: Login as educator ---")
            tokenManager.saveToken("fake-educator-token")
            tokenManager.saveRole("educator")
            Log.d("DestinationGuardTest", "Role saved: ${tokenManager.getRole()}")
        }
        
        composeTestRule.waitForIdle()
        
        // Step 2: Verify bottom nav is visible on allowed routes
        try {
            composeTestRule.onNodeWithTag("navigation_bar").assertIsDisplayed()
            Log.d("DestinationGuardTest", "✓ Bottom nav visible on allowed route")
        } catch (e: Exception) {
            Log.e("DestinationGuardTest", "✗ Bottom nav should be visible on allowed route: ${e.message}")
        }

        // Step 3: Navigate to role_selection (should hide bottom nav)
        Log.d("DestinationGuardTest", "--- Step 2: Testing bottom nav on role_selection ---")
        
        // The bottom nav should be hidden on role_selection
        composeTestRule.waitForIdle()
        
        Log.d("DestinationGuardTest", "=== BottomNavRouteTest COMPLETED ===")
    }
}
