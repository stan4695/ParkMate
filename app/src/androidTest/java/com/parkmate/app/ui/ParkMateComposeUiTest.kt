package com.parkmate.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkmate.app.core.math.TimerProgressState
import com.parkmate.app.core.ui.theme.ParkMateTheme
import com.parkmate.app.feature.details.PaidParkingTimerCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParkMateComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun paidParkingTimerCard_displaysRemainingMinutesAndTriggersAddTimeCallbacks() {
        var addedMinutes = 0
        val sampleState = TimerProgressState(
            isActive = true,
            isExpired = false,
            isAlmostExpired = true,
            remainingMinutes = 18,
            remainingMillis = 18 * 60_000L,
            progressFraction = 0.3f,
            expiryEpochMillis = System.currentTimeMillis() + 18 * 60_000L
        )

        composeTestRule.setContent {
            ParkMateTheme {
                PaidParkingTimerCard(
                    timerState = sampleState,
                    onAdd15Minutes = { addedMinutes += 15 },
                    onAdd30Minutes = { addedMinutes += 30 },
                    onResetTimer = { addedMinutes = 0 }
                )
            }
        }

        composeTestRule.onNodeWithText("18").assertIsDisplayed()
        composeTestRule.onNodeWithText("+15 min").performClick()
        assertEquals(15, addedMinutes)

        composeTestRule.onNodeWithText("+30 min").performClick()
        assertEquals(45, addedMinutes)
    }
}
