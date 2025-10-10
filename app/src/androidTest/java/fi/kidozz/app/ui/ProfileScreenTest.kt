package fi.kidozz.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.kidozz.app.MainActivity
import fi.kidozz.app.data.auth.TokenManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var tokenManager: TokenManager

    @Before
    fun setup() {
        runBlocking {
            tokenManager = TokenManager(ApplicationProvider.getApplicationContext())
            tokenManager.clearAll()
            Log.d("ProfileScreenTest", "Cleared all tokens before test")
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            tokenManager.clearAll()
            Log.d("ProfileScreenTest", "Cleared all tokens after test")
        }
    }

    private fun loginAs(role: String) {
        runBlocking {
            tokenManager.saveToken("fake-token")
            tokenManager.saveRole(role)
        }
        Log.d("ProfileScreenTest", "Logged in as $role")
        restartMainActivity()
    }

    private fun restartMainActivity() {
        composeTestRule.activityRule.scenario.close()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    @Test
    fun logoutButtonClearsSessionAndNavigates() {
        Log.d("ProfileScreenTest", "=== STARTING logoutButtonClearsSessionAndNavigates ===")
        
        // Step 1: Login as educator
        loginAs("educator")
        
        // Step 2: Navigate to profile screen
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.waitForIdle()
        
        // Step 3: Verify logout button exists
        composeTestRule.onNodeWithText("Logout").assertExists()
        Log.d("ProfileScreenTest", "✓ Logout button found")
        
        // Step 4: Click logout button
        composeTestRule.onNodeWithText("Logout").performClick()
        composeTestRule.waitForIdle()
        
        // Step 5: Verify session is cleared
        val token = tokenManager.getToken()
        val role = tokenManager.getRole()
        
        assert(token == null) { "Token should be null after logout, but was: $token" }
        assert(role == null) { "Role should be null after logout, but was: $role" }
        
        Log.d("ProfileScreenTest", "✓ Session cleared successfully")
        
        // Step 6: Verify navigation to role selection (no bottom nav should be visible)
        composeTestRule.onNodeWithTag("navigation_bar").assertDoesNotExist()
        Log.d("ProfileScreenTest", "✓ Navigated to role selection (no bottom nav)")
        
        Log.d("ProfileScreenTest", "=== logoutButtonClearsSessionAndNavigates COMPLETED SUCCESSFULLY ===")
    }

    @Test
    fun logoutButtonWorksForParentRole() {
        Log.d("ProfileScreenTest", "=== STARTING logoutButtonWorksForParentRole ===")
        
        // Step 1: Login as parent
        loginAs("parent")
        
        // Step 2: Navigate to profile screen
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.waitForIdle()
        
        // Step 3: Verify logout button exists
        composeTestRule.onNodeWithText("Logout").assertExists()
        Log.d("ProfileScreenTest", "✓ Logout button found for parent")
        
        // Step 4: Click logout button
        composeTestRule.onNodeWithText("Logout").performClick()
        composeTestRule.waitForIdle()
        
        // Step 5: Verify session is cleared
        val token = tokenManager.getToken()
        val role = tokenManager.getRole()
        
        assert(token == null) { "Token should be null after logout, but was: $token" }
        assert(role == null) { "Role should be null after logout, but was: $role" }
        
        Log.d("ProfileScreenTest", "✓ Parent session cleared successfully")
        
        // Step 6: Verify navigation to role selection
        composeTestRule.onNodeWithTag("navigation_bar").assertDoesNotExist()
        Log.d("ProfileScreenTest", "✓ Parent navigated to role selection")
        
        Log.d("ProfileScreenTest", "=== logoutButtonWorksForParentRole COMPLETED SUCCESSFULLY ===")
    }

    @Test
    fun profileScreenShowsCorrectContent() {
        Log.d("ProfileScreenTest", "=== STARTING profileScreenShowsCorrectContent ===")
        
        // Step 1: Login as educator
        loginAs("educator")
        
        // Step 2: Navigate to profile screen
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.waitForIdle()
        
        // Step 3: Verify profile screen content
        composeTestRule.onNodeWithText("Profile").assertExists()
        composeTestRule.onNodeWithText("Logout").assertExists()
        
        Log.d("ProfileScreenTest", "✓ Profile screen shows correct content")
        
        Log.d("ProfileScreenTest", "=== profileScreenShowsCorrectContent COMPLETED SUCCESSFULLY ===")
    }
}
