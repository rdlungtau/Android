package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SaiphaiRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveFare_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SaiphaiRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createFare(
                fromLocation = "Saiphai",
                toLocation = "N.Chawnpui",
                distanceKm = 5.0,
                fareAmount = 250L,
                routeType = "inter_village",
                notes = "Rs 50/km official rate"
            )
        }
        assertTrue("Expected createFare to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val fareId = createResult.getOrThrow()

        val emittedFares = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFares(uid).first { list -> list.any { it.id == fareId } }
        }
        assertTrue(emittedFares.any { it.id == fareId && it.fareAmount == 250L })
    }

    @Test
    fun createBookingAndUpdateProgress_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SaiphaiRepository(firestore)

        val driver = DriverProfile(
            id = "driver_test_1",
            userId = uid,
            name = "Lalrinfela Sailo",
            regNo = "MZ-05-A-1421",
            contactPhone = "+91 9862145102"
        )

        val bookingResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBooking(
                passengerName = "Zorama",
                passengerPhone = "+91 8794112233",
                driver = driver,
                pickupName = "Saiphai Bazar Stand",
                dropoffName = "N.Chawnpui",
                pickupLat = 24.1954,
                pickupLng = 92.7661,
                dropoffLat = 24.2255,
                dropoffLng = 92.7932,
                distanceKm = 5.0,
                fareAmount = 250L,
                paymentMode = "online_upi",
                paymentStatus = "paid"
            )
        }
        assertTrue("Expected createBooking to succeed: ${bookingResult.exceptionOrNull()}", bookingResult.isSuccess)
        val bookingId = bookingResult.getOrThrow()

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateBookingStatusAndProgress(
                bookingId = bookingId,
                newStatus = "in_progress",
                newProgress = 0.65
            )
        }
        assertTrue("Expected updateBookingStatusAndProgress to succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)
    }

    @Test
    fun getBookingById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SaiphaiRepository(firestore)
        val driver = DriverProfile(
            id = "driver_test_1",
            userId = aliceUid,
            name = "Lalrinfela Sailo",
            regNo = "MZ-05-A-1421",
            contactPhone = "+91 9862145102"
        )
        val bookingId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createBooking(
                passengerName = "Alice",
                passengerPhone = "+91 8794000001",
                driver = driver,
                pickupName = "Saiphai",
                dropoffName = "Phainuam",
                pickupLat = 24.1954,
                pickupLng = 92.7661,
                dropoffLat = 24.1720,
                dropoffLng = 92.7430,
                distanceKm = 4.0,
                fareAmount = 200L,
                paymentMode = "cash",
                paymentStatus = "pending"
            ).getOrThrow()
        }

        // Switch to Bob and verify cross-user access is denied
        signInTestUser(BOB_EMAIL)
        val bobRepo = SaiphaiRepository(firestore)
        val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getBookingById(bookingId)
        }
        assertTrue("Expected cross-user read to fail", bobReadResult.isFailure)
        val firestoreEx = bobReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeFares_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SaiphaiRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeFares("unauth_uid").first()
            }
            fail("Expected observeFares while unauthenticated to throw exception")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@saiphai.test"
        const val BOB_EMAIL = "bob@saiphai.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
