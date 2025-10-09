package fi.kidozz.app.features.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import fi.kidozz.app.data.models.Kid
import org.junit.Rule
import org.junit.Test

class KiddozCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestKid(name: String = "Emma Johnson"): Kid {
        return Kid(
            id = "1",
            full_name = name,
            dob = "2020-03-15",
            daycare_id = "test",
            group_id = "test",
            trusted_adults = emptyList(),
            attendance = "out"
        )
    }

    @Test
    fun kiddozCard_displaysKidName() {
        // GIVEN a Kid object with full_name = "Emma Johnson"
        val testKid = createTestKid("Emma Johnson")

        // WHEN KiddozCard is rendered
        composeTestRule.setContent {
            KiddozCard(kid = testKid)
        }

        // THEN the text "Emma Johnson" is displayed
        composeTestRule.onNodeWithText("Emma Johnson").assertIsDisplayed()
    }

    @Test
    fun kiddozCard_displaysDifferentKidName() {
        // GIVEN a Kid object with a different full_name
        val testKid = createTestKid("Liam Smith")

        // WHEN KiddozCard is rendered
        composeTestRule.setContent {
            KiddozCard(kid = testKid)
        }

        // THEN that full_name appears exactly once
        composeTestRule.onNodeWithText("Liam Smith").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Liam Smith").assertCountEquals(1)
    }

    @Test
    fun kiddozCard_displaysOutButton() {
        // GIVEN a Kid object
        val testKid = createTestKid()

        // WHEN KiddozCard is rendered
        composeTestRule.setContent {
            KiddozCard(kid = testKid)
        }

        // THEN the OUT button is displayed and clickable
        composeTestRule.onNodeWithText("OUT")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun kiddozCard_displaysPersonIcon() {
        // GIVEN a Kid object
        val testKid = createTestKid()

        // WHEN KiddozCard is rendered
        composeTestRule.setContent {
            KiddozCard(kid = testKid)
        }

        // THEN the Person icon is displayed
        composeTestRule.onNodeWithContentDescription("Kid face icon")
            .assertIsDisplayed()
    }

    @Test
    fun kiddozCard_outButtonIsClickable() {
        // GIVEN a Kid object
        val testKid = createTestKid()

        // WHEN KiddozCard is rendered
        composeTestRule.setContent {
            KiddozCard(kid = testKid)
        }

        // THEN the OUT button is clickable (no exception should be thrown)
        composeTestRule.onNodeWithText("OUT").performClick()
        
        // The button should remain displayed after click
        composeTestRule.onNodeWithText("OUT").assertIsDisplayed()
    }

    @Test
    fun kiddozCard_displaysAllRequiredElements() {
        // GIVEN a Kid object
        val testKid = createTestKid("Sophia Davis")

        // WHEN KiddozCard is rendered
        composeTestRule.setContent {
            KiddozCard(kid = testKid)
        }

        // THEN all required elements are displayed
        composeTestRule.onNodeWithText("Sophia Davis").assertIsDisplayed()
        composeTestRule.onNodeWithText("OUT").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Kid face icon").assertIsDisplayed()
    }
}
