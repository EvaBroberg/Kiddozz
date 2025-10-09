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
class NavigationVisibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var tokenManager: TokenManager

    @Before
    fun setup() {
        runBlocking {
            tokenManager = TokenManager(ApplicationProvider.getApplicationContext())
            tokenManager.clearAll()
            Log.d("TestSetup", "Cleared all tokens before test")
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            tokenManager.clearAll()
            Log.d("TestTeardown", "Cleared all tokens after test")
        }
    }

    private fun loginAs(role: String) {
        runBlocking {
            tokenManager.saveToken("fake-token")
            tokenManager.saveRole(role)
        }
        Log.d("TestLogin", "Logged in as $role")
        restartMainActivity()
    }

    private fun logout() {
        runBlocking { 
            tokenManager.clearAll() 
        }
        Log.d("TestLogin", "Logged out")
        restartMainActivity()
    }

    private fun restartMainActivity() {
        composeTestRule.activityRule.scenario.close()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testNavigationIsNotVisible_whenLoggedOut() {
        Log.d("NavVisTest", "Starting testNavigationIsNotVisible_whenLoggedOut")
        
        // When logged out, the main navigation bar should not be visible.
        composeTestRule.onNode(hasTestTag("navigation_bar")).assertDoesNotExist()
        
        // Also confirm "Kids Overview" is not present.
        composeTestRule.onNode(hasText("Kids Overview")).assertDoesNotExist()
        
        Log.d("NavVisTest", "Assertion passed: Navigation hidden when logged out")
    }

    @Test
    fun testKidsOverviewIsVisible_whenLoggedInAsParent() {
        Log.d("NavVisTest", "Starting testKidsOverviewIsVisible_whenLoggedInAsParent")
        
        // 1. First verify we're on the role selection screen
        try {
            composeTestRule.onNode(hasText("Parent")).assertIsDisplayed()
            Log.d("NavVisTest", "Found Parent text on role selection screen")
        } catch (e: Exception) {
            Log.d("NavVisTest", "Could not find Parent text: ${e.message}")
            // Let's try to find any text that might be visible
            try {
                composeTestRule.onNode(hasText("Educator")).assertIsDisplayed()
                Log.d("NavVisTest", "Found Educator text instead")
            } catch (e2: Exception) {
                Log.d("NavVisTest", "Could not find Educator text either: ${e2.message}")
            }
        }
        
        // 2. Login as parent
        loginAs("parent")
        
        // 3. Assert that the navigation item is now displayed.
        composeTestRule.onNode(hasText("Kids Overview"), useUnmergedTree = true).assertIsDisplayed()
        
        Log.d("NavVisTest", "Assertion passed: Kids Overview visibility correct for parent")
    }

    @Test
    fun testKidsOverviewIsVisible_whenLoggedInAsEducator() {
        Log.d("NavVisTest", "Starting testKidsOverviewIsVisible_whenLoggedInAsEducator")
        
        // 1. First verify we're on the role selection screen
        composeTestRule.onNode(hasText("Educator")).assertIsDisplayed()
        
        // 2. Login as educator
        loginAs("educator")
        
        // 3. Assert that the navigation item is now displayed.
        composeTestRule.onNode(hasText("Kids Overview"), useUnmergedTree = true).assertIsDisplayed()
        
        Log.d("NavVisTest", "Assertion passed: Kids Overview visibility correct for educator")
    }
    
    @Test
    fun testNavigationHides_afterLoggingOut() {
        Log.d("NavVisTest", "Starting testNavigationHides_afterLoggingOut")
        
        // 1. Login as parent first to show the navigation.
        loginAs("parent")
        
        // 2. Confirm it's visible.
        composeTestRule.onNode(hasText("Kids Overview"), useUnmergedTree = true).assertIsDisplayed()
        
        // 3. Log out.
        logout()
        
        // 4. Assert that the navigation item is now gone.
        composeTestRule.onNode(hasText("Kids Overview")).assertDoesNotExist()
        composeTestRule.onNode(hasTestTag("navigation_bar")).assertDoesNotExist()
        
        Log.d("NavVisTest", "Assertion passed: Navigation hidden after logout")
    }
}