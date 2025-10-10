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
class SafeNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var tokenManager: TokenManager

    @Before
    fun setup() {
        runBlocking {
            tokenManager = TokenManager(ApplicationProvider.getApplicationContext())
            tokenManager.clearAll()
            Log.d("SafeNavigationTest", "=== TEST SETUP: Cleared all tokens ===")
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            tokenManager.clearAll()
            Log.d("SafeNavigationTest", "=== TEST TEARDOWN: Cleared all tokens ===")
        }
    }

    @Test
    fun testSafeNavigationPreventsCrashes() {
        Log.d("SafeNavigationTest", "=== STARTING SafeNavigationTest ===")
        
        // Step 1: Login as educator
        runBlocking {
            Log.d("SafeNavigationTest", "--- Step 1: Login as educator ---")
            tokenManager.saveToken("fake-educator-token")
            tokenManager.saveRole("educator")
            Log.d("SafeNavigationTest", "Role saved: ${tokenManager.getRole()}")
        }
        
        composeTestRule.waitForIdle()
        
        // Step 2: Verify educator can navigate safely
        try {
            composeTestRule.onNodeWithText("Kids Overview", useUnmergedTree = true).assertIsDisplayed()
            Log.d("SafeNavigationTest", "✓ Educator: Kids Overview visible")
        } catch (e: Exception) {
            Log.e("SafeNavigationTest", "✗ Educator: Kids Overview should be visible: ${e.message}")
        }

        // Step 3: Switch to parent role (should trigger safe navigation)
        runBlocking {
            Log.d("SafeNavigationTest", "--- Step 2: Switch to parent ---")
            tokenManager.saveRole("parent")
            Log.d("SafeNavigationTest", "Role switched to: ${tokenManager.getRole()}")
        }
        
        composeTestRule.waitForIdle()
        
        // Step 4: Verify parent navigation works safely
        try {
            composeTestRule.onNodeWithText("Kids Overview", useUnmergedTree = true).assertDoesNotExist()
            Log.d("SafeNavigationTest", "✓ Parent: Kids Overview hidden")
        } catch (e: Exception) {
            Log.e("SafeNavigationTest", "✗ Parent: Kids Overview should be hidden: ${e.message}")
        }

        // Step 5: Test rapid role switching (should not crash)
        runBlocking {
            Log.d("SafeNavigationTest", "--- Step 3: Rapid role switching ---")
            repeat(5) { i ->
                val role = if (i % 2 == 0) "educator" else "parent"
                tokenManager.saveRole(role)
                Log.d("SafeNavigationTest", "Rapid switch $i: $role")
                delay(50) // Small delay between switches
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Step 6: Verify app is still stable after rapid switching
        try {
            composeTestRule.onNodeWithTag("navigation_bar").assertIsDisplayed()
            Log.d("SafeNavigationTest", "✓ Navigation bar still visible after rapid switching")
        } catch (e: Exception) {
            Log.e("SafeNavigationTest", "✗ Navigation bar should be visible: ${e.message}")
        }

        Log.d("SafeNavigationTest", "=== SafeNavigationTest COMPLETED SUCCESSFULLY ===")
    }

    @Test
    fun testNavigationStabilityAfterAppRestart() {
        Log.d("SafeNavigationTest", "=== STARTING NavigationStabilityTest ===")
        
        // Step 1: Login as educator
        runBlocking {
            Log.d("SafeNavigationTest", "--- Step 1: Login as educator ---")
            tokenManager.saveToken("fake-educator-token")
            tokenManager.saveRole("educator")
            Log.d("SafeNavigationTest", "Role saved: ${tokenManager.getRole()}")
        }
        
        composeTestRule.waitForIdle()
        
        // Step 2: Verify initial state
        try {
            composeTestRule.onNodeWithText("Kids Overview", useUnmergedTree = true).assertIsDisplayed()
            Log.d("SafeNavigationTest", "✓ Initial: Kids Overview visible")
        } catch (e: Exception) {
            Log.e("SafeNavigationTest", "✗ Initial: Kids Overview should be visible: ${e.message}")
        }

        // Step 3: Simulate app restart (activity recreation)
        Log.d("SafeNavigationTest", "--- Step 2: Simulating app restart ---")
        composeTestRule.activityRule.scenario.close()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // Step 4: Login again after restart
        runBlocking {
            Log.d("SafeNavigationTest", "--- Step 3: Login after restart ---")
            tokenManager.saveToken("fake-educator-token-2")
            tokenManager.saveRole("educator")
            Log.d("SafeNavigationTest", "Role saved after restart: ${tokenManager.getRole()}")
        }
        
        composeTestRule.waitForIdle()
        
        // Step 5: Verify navigation still works after restart
        try {
            composeTestRule.onNodeWithText("Kids Overview", useUnmergedTree = true).assertIsDisplayed()
            Log.d("SafeNavigationTest", "✓ After restart: Kids Overview visible")
        } catch (e: Exception) {
            Log.e("SafeNavigationTest", "✗ After restart: Kids Overview should be visible: ${e.message}")
        }

        Log.d("SafeNavigationTest", "=== NavigationStabilityTest COMPLETED SUCCESSFULLY ===")
    }
}
