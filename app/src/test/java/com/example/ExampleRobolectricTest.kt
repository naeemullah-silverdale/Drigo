package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SampleDataProvider
import com.example.data.model.RecurringFrequency
import com.example.data.remote.AiTripMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CityLink Rideshare", appName)
    }

    @Test
    fun `sample data provider generates trips`() {
        val trips = SampleDataProvider.initialTrips
        assertTrue(trips.isNotEmpty())
        assertTrue(trips.any { it.recurringFrequency == RecurringFrequency.WEEKDAYS })
    }

    @Test
    fun `ai trip matcher scores trips accurately`() {
        val prefs = SampleDataProvider.initialPreferences
        val trips = SampleDataProvider.initialTrips
        val recommendations = AiTripMatcher.generatePersonalizedMatches(prefs, trips)
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.first().matchScorePercent >= 70)
    }
}
