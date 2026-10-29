package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class SaiphaiRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- FARES ---

    fun observeFares(userId: String = requireUserId()): Flow<List<FareRoute>> = flow {
        val path = "fares"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FareRoute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedBy { it.fareAmount }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getFareById(fareId: String): Result<FareRoute?> = runCatching {
        val docRef = db.collection("fares").document(fareId)
        try {
            val snap = docRef.get().await()
            snap.toObject(FareRoute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createFare(
        fromLocation: String,
        toLocation: String,
        distanceKm: Double,
        fareAmount: Long,
        routeType: String,
        notes: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("fares")
        val payload = mapOf(
            "userId" to uid,
            "fromLocation" to fromLocation.trim().take(100),
            "toLocation" to toLocation.trim().take(100),
            "distanceKm" to distanceKm.coerceIn(0.0, 1000.0),
            "fareAmount" to fareAmount.coerceIn(10L, 100000L),
            "routeType" to routeType,
            "notes" to notes.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val docRef = colRef.add(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateFare(
        fareId: String,
        fromLocation: String,
        toLocation: String,
        distanceKm: Double,
        fareAmount: Long,
        routeType: String,
        notes: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("fares").document(fareId)
        val updates = mapOf(
            "fromLocation" to fromLocation.trim().take(100),
            "toLocation" to toLocation.trim().take(100),
            "distanceKm" to distanceKm.coerceIn(0.0, 1000.0),
            "fareAmount" to fareAmount.coerceIn(10L, 100000L),
            "routeType" to routeType,
            "notes" to notes.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteFare(fareId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("fares").document(fareId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- DRIVERS ---

    fun observeDrivers(userId: String = requireUserId()): Flow<List<DriverProfile>> = flow {
        val path = "drivers"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DriverProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedBy { it.name }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createDriver(
        name: String,
        regNo: String,
        contactPhone: String,
        standName: String,
        vehicleModel: String,
        status: String = "available",
        ratingAvg: Double = 4.9,
        totalTrips: Long = 25L,
        currentLat: Double = 24.1954,
        currentLng: Double = 92.7661,
        isVerified: Boolean = true
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("drivers")
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(100),
            "regNo" to regNo.trim().uppercase().take(30),
            "contactPhone" to contactPhone.trim().take(25),
            "standName" to standName.trim().take(100),
            "vehicleModel" to vehicleModel.trim().take(80),
            "status" to status,
            "ratingAvg" to ratingAvg.coerceIn(0.0, 5.0),
            "totalTrips" to totalTrips.coerceIn(0L, 1000000L),
            "currentLat" to currentLat.coerceIn(-90.0, 90.0),
            "currentLng" to currentLng.coerceIn(-180.0, 180.0),
            "isVerified" to isVerified,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val docRef = colRef.add(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateDriver(
        driverId: String,
        name: String,
        regNo: String,
        contactPhone: String,
        standName: String,
        vehicleModel: String,
        status: String,
        ratingAvg: Double,
        totalTrips: Long,
        currentLat: Double,
        currentLng: Double,
        isVerified: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("drivers").document(driverId)
        val updates = mapOf(
            "name" to name.trim().take(100),
            "regNo" to regNo.trim().uppercase().take(30),
            "contactPhone" to contactPhone.trim().take(25),
            "standName" to standName.trim().take(100),
            "vehicleModel" to vehicleModel.trim().take(80),
            "status" to status,
            "ratingAvg" to ratingAvg.coerceIn(0.0, 5.0),
            "totalTrips" to totalTrips.coerceIn(0L, 1000000L),
            "currentLat" to currentLat.coerceIn(-90.0, 90.0),
            "currentLng" to currentLng.coerceIn(-180.0, 180.0),
            "isVerified" to isVerified,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDriver(driverId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("drivers").document(driverId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- BOOKINGS ---

    fun observeBookings(userId: String = requireUserId()): Flow<List<BookingTicket>> = flow {
        val path = "bookings"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BookingTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getBookingById(bookingId: String): Result<BookingTicket?> = runCatching {
        val docRef = db.collection("bookings").document(bookingId)
        try {
            val snap = docRef.get().await()
            snap.toObject(BookingTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createBooking(
        passengerName: String,
        passengerPhone: String,
        driver: DriverProfile,
        pickupName: String,
        dropoffName: String,
        pickupLat: Double,
        pickupLng: Double,
        dropoffLat: Double,
        dropoffLng: Double,
        distanceKm: Double,
        fareAmount: Long,
        paymentMode: String,
        paymentStatus: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("bookings")
        val ticketCode = "SPH-${Random.nextInt(1000, 9999)}"
        val otpPin = Random.nextInt(1000, 9999).toString()
        val payload = mapOf(
            "userId" to uid,
            "ticketCode" to ticketCode,
            "passengerName" to passengerName.trim().ifEmpty { "Passenger" }.take(100),
            "passengerPhone" to passengerPhone.trim().ifEmpty { "+91 9862000000" }.take(25),
            "driverId" to driver.id.ifEmpty { "driver_default" },
            "driverName" to driver.name.ifEmpty { "Saiphai Auto Driver" }.take(100),
            "driverRegNo" to driver.regNo.ifEmpty { "MZ-05-A-1001" }.take(30),
            "driverPhone" to driver.contactPhone.ifEmpty { "+91 9862145102" }.take(25),
            "pickupName" to pickupName.trim().take(120),
            "dropoffName" to dropoffName.trim().take(120),
            "pickupLat" to pickupLat.coerceIn(-90.0, 90.0),
            "pickupLng" to pickupLng.coerceIn(-180.0, 180.0),
            "dropoffLat" to dropoffLat.coerceIn(-90.0, 90.0),
            "dropoffLng" to dropoffLng.coerceIn(-180.0, 180.0),
            "distanceKm" to distanceKm.coerceIn(0.1, 1000.0),
            "fareAmount" to fareAmount.coerceIn(10L, 100000L),
            "paymentMode" to paymentMode,
            "paymentStatus" to paymentStatus,
            "status" to "driver_assigned",
            "otpPin" to otpPin,
            "driverProgress" to 0.1,
            "passengerRated" to false,
            "driverRated" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val docRef = colRef.add(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateBookingStatusAndProgress(
        bookingId: String,
        newStatus: String,
        newProgress: Double,
        paymentStatus: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("bookings").document(bookingId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "driverProgress" to newProgress.coerceIn(0.0, 1.0),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (paymentStatus != null) {
            updates["paymentStatus"] = paymentStatus
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun markBookingRated(
        bookingId: String,
        byPassenger: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("bookings").document(bookingId)
        val fieldName = if (byPassenger) "passengerRated" else "driverRated"
        val updates = mapOf(
            fieldName to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- REVIEWS ---

    fun observeReviews(userId: String = requireUserId()): Flow<List<RideReview>> = flow {
        val path = "reviews"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(RideReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun submitReview(
        bookingId: String,
        targetId: String,
        targetName: String,
        targetRole: String,
        reviewerName: String,
        rating: Int,
        comment: String,
        routeSummary: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("reviews")
        val payload = mapOf(
            "userId" to uid,
            "bookingId" to bookingId.ifEmpty { "general" }.take(128),
            "targetId" to targetId.ifEmpty { "driver_default" }.take(128),
            "targetName" to targetName.trim().ifEmpty { "Driver" }.take(100),
            "targetRole" to targetRole,
            "reviewerName" to reviewerName.trim().ifEmpty { "Passenger" }.take(100),
            "rating" to rating.coerceIn(1, 5).toLong(),
            "comment" to comment.trim().take(500),
            "routeSummary" to routeSummary.trim().ifEmpty { "Saiphai Local" }.take(150),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val docRef = colRef.add(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    // --- INITIAL DATA SEEDING FOR NEW AUTHENTICATED SESSIONS ---

    suspend fun ensureInitialSaiphaiDataSeeded(userId: String = requireUserId()) {
        try {
            val existingFares = db.collection("fares")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()

            if (existingFares.isEmpty) {
                val defaultFares = listOf(
                    Triple("Saiphai", "Saiphai Khawchhung", Pair(1.0, 50L)) to Pair("local", "Town local flat fare (Khawchhung rate)"),
                    Triple("Saiphai", "N.Chawnpui", Pair(5.0, 250L)) to Pair("inter_village", "Official Association rate (Rs 50/km)"),
                    Triple("Saiphai", "Phainuam", Pair(4.0, 200L)) to Pair("inter_village", "Saiphai - Phainuam standard auto fare"),
                    Triple("Saiphai", "Saihapui 'V'", Pair(4.0, 200L)) to Pair("inter_village", "Saiphai - Saihapui V standard auto fare"),
                    Triple("Saiphai", "Bilkhawthlir", Pair(11.0, 550L)) to Pair("highway", "NH-306 connect route (Rs 50/km)"),
                    Triple("Saiphai", "Kolasib Town", Pair(15.0, 750L)) to Pair("highway", "District HQ full reserve trip"),
                    Triple("Saiphai", "Vairengte Gate", Pair(18.0, 900L)) to Pair("highway", "Border checkgate reserve trip")
                )
                for ((route, meta) in defaultFares) {
                    createFare(
                        fromLocation = route.first,
                        toLocation = route.second,
                        distanceKm = route.third.first,
                        fareAmount = route.third.second,
                        routeType = meta.first,
                        notes = meta.second
                    )
                }
            }

            val existingDrivers = db.collection("drivers")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()

            if (existingDrivers.isEmpty) {
                data class SeedDriver(
                    val name: String,
                    val regNo: String,
                    val phone: String,
                    val stand: String,
                    val model: String,
                    val status: String,
                    val rating: Double,
                    val trips: Long,
                    val lat: Double,
                    val lng: Double
                )
                val defaultDrivers = listOf(
                    SeedDriver("Lalrinfela Sailo", "MZ-05-A-1421", "+91 98621 45102", "Saiphai Bazar Stand", "Bajaj RE Compact 4S", "available", 4.9, 318L, 24.1956, 92.7663),
                    SeedDriver("Zoramthanga Hmar", "MZ-05-A-1809", "+91 87943 22190", "Saiphai Kawn Veng", "TVS King Deluxe", "available", 4.8, 245L, 24.1972, 92.7681),
                    SeedDriver("Vanlalruata Pachuau", "MZ-05-B-0394", "+91 96125 77812", "Saiphai PHC Stand", "Bajaj Maxima Z", "available", 4.9, 410L, 24.1931, 92.7634),
                    SeedDriver("Hmingthanzuala Ralte", "MZ-05-A-2215", "+91 84150 63901", "N.Chawnpui Peng Stand", "Piaggio Ape City+", "on_trip", 4.7, 189L, 24.2085, 92.7790),
                    SeedDriver("Lalbiakdika Chhangte", "MZ-05-B-0912", "+91 93661 80455", "Saiphai Bazar Stand", "Bajaj RE Optima", "available", 5.0, 156L, 24.1950, 92.7658)
                )
                for (d in defaultDrivers) {
                    createDriver(
                        name = d.name,
                        regNo = d.regNo,
                        contactPhone = d.phone,
                        standName = d.stand,
                        vehicleModel = d.model,
                        status = d.status,
                        ratingAvg = d.rating,
                        totalTrips = d.trips,
                        currentLat = d.lat,
                        currentLng = d.lng,
                        isVerified = true
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed_initial_data")
        }
    }
}
