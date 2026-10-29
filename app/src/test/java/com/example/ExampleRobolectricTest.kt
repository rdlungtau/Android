package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SaiphaiGeoUtils
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
    fun `read string from context and verify Rs 50 per km calculator`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Saiphai Auto", appName)

        // Verify local minimum fare Rs 50 and 5km N.Chawnpui fare Rs 250
        assertEquals(50L, SaiphaiGeoUtils.calculateFareRs(1.0))
        assertEquals(250L, SaiphaiGeoUtils.calculateFareRs(5.0))
        assertTrue(SaiphaiGeoUtils.HUBS.any { it.nameMizo.contains("N.Chawnpui") && it.defaultFareFromCenter == 250L })
    }
}
