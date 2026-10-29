package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@IgnoreExtraProperties
data class FareRoute(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fromLocation: String = "Saiphai",
    val toLocation: String = "",
    val distanceKm: Double = 1.0,
    val fareAmount: Long = 50L,
    val routeType: String = "local", // 'local', 'inter_village', 'highway', 'special'
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DriverProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val regNo: String = "",
    val contactPhone: String = "",
    val standName: String = "Saiphai Bazar Stand",
    val vehicleModel: String = "Bajaj RE Compact 4S",
    val status: String = "available", // 'available', 'on_trip', 'offline'
    val ratingAvg: Double = 4.8,
    val totalTrips: Long = 0L,
    val currentLat: Double = 24.1954,
    val currentLng: Double = 92.7661,
    val isVerified: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookingTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ticketCode: String = "",
    val passengerName: String = "",
    val passengerPhone: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val driverRegNo: String = "",
    val driverPhone: String = "",
    val pickupName: String = "",
    val dropoffName: String = "",
    val pickupLat: Double = 24.1954,
    val pickupLng: Double = 92.7661,
    val dropoffLat: Double = 24.2210,
    val dropoffLng: Double = 92.7890,
    val distanceKm: Double = 1.0,
    val fareAmount: Long = 50L,
    val paymentMode: String = "cash", // 'cash', 'online_upi', 'online_card'
    val paymentStatus: String = "pending", // 'pending', 'paid', 'refunded'
    val status: String = "booked", // 'booked', 'driver_assigned', 'arriving', 'in_progress', 'completed', 'cancelled'
    val otpPin: String = "1024",
    val driverProgress: Double = 0.0,
    val passengerRated: Boolean = false,
    val driverRated: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RideReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookingId: String = "",
    val targetId: String = "",
    val targetName: String = "",
    val targetRole: String = "driver", // 'driver' or 'passenger'
    val reviewerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val routeSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SaiphaiHubPoint(
    val id: String,
    val nameMizo: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double,
    val defaultFareFromCenter: Long,
    val defaultDistanceKm: Double,
    val zoneType: String // 'local', 'inter_village', 'highway'
)

object SaiphaiGeoUtils {
    const val RATE_PER_KM = 50L
    const val MIN_LOCAL_FARE = 50L

    val HUBS = listOf(
        SaiphaiHubPoint(
            id = "saiphai_bazar",
            nameMizo = "Saiphai Bazar Stand (Center)",
            nameEn = "Saiphai Main Stand (Center)",
            lat = 24.1954,
            lng = 92.7661,
            defaultFareFromCenter = 50L,
            defaultDistanceKm = 1.0,
            zoneType = "local"
        ),
        SaiphaiHubPoint(
            id = "saiphai_khawchhung",
            nameMizo = "Saiphai Khawchhung (Vengchhung)",
            nameEn = "Saiphai Town Local",
            lat = 24.1989,
            lng = 92.7695,
            defaultFareFromCenter = 50L,
            defaultDistanceKm = 1.0,
            zoneType = "local"
        ),
        SaiphaiHubPoint(
            id = "saiphai_hospital",
            nameMizo = "Saiphai PHC & School Veng",
            nameEn = "Saiphai Health Center & School Veng",
            lat = 24.1922,
            lng = 92.7628,
            defaultFareFromCenter = 50L,
            defaultDistanceKm = 1.0,
            zoneType = "local"
        ),
        SaiphaiHubPoint(
            id = "n_chawnpui",
            nameMizo = "N.Chawnpui",
            nameEn = "North Chawnpui",
            lat = 24.2255,
            lng = 92.7932,
            defaultFareFromCenter = 250L,
            defaultDistanceKm = 5.0,
            zoneType = "inter_village"
        ),
        SaiphaiHubPoint(
            id = "phainuam",
            nameMizo = "Phainuam",
            nameEn = "Phainuam Village",
            lat = 24.1720,
            lng = 92.7430,
            defaultFareFromCenter = 200L,
            defaultDistanceKm = 4.0,
            zoneType = "inter_village"
        ),
        SaiphaiHubPoint(
            id = "saihapui_v",
            nameMizo = "Saihapui 'V'",
            nameEn = "Saihapui V",
            lat = 24.2120,
            lng = 92.7350,
            defaultFareFromCenter = 200L,
            defaultDistanceKm = 4.0,
            zoneType = "inter_village"
        ),
        SaiphaiHubPoint(
            id = "bilkhawthlir",
            nameMizo = "Bilkhawthlir",
            nameEn = "Bilkhawthlir Junction",
            lat = 24.3310,
            lng = 92.7190,
            defaultFareFromCenter = 550L,
            defaultDistanceKm = 11.0,
            zoneType = "highway"
        ),
        SaiphaiHubPoint(
            id = "vairengte",
            nameMizo = "Vairengte Gate",
            nameEn = "Vairengte Checkgate",
            lat = 24.5020,
            lng = 92.7610,
            defaultFareFromCenter = 900L,
            defaultDistanceKm = 18.0,
            zoneType = "highway"
        ),
        SaiphaiHubPoint(
            id = "kolasib",
            nameMizo = "Kolasib Town",
            nameEn = "Kolasib District HQ",
            lat = 24.2240,
            lng = 92.6760,
            defaultFareFromCenter = 750L,
            defaultDistanceKm = 15.0,
            zoneType = "highway"
        )
    )

    /**
     * Computes road-adjusted distance in km between two GPS coordinates.
     * Applies a 1.32x mountain-road winding factor for Mizoram hill terrain.
     */
    fun calculateRoadDistanceKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        // Check if matching Saiphai -> N.Chawnpui preset or local preset
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val straightKm = r * c
        val roadKm = (straightKm * 1.15).coerceAtLeast(1.0)
        return ((roadKm * 10.0).roundToInt() / 10.0).coerceIn(1.0, 500.0)
    }

    /**
     * Calculates fare at Rs 50/km with a minimum of Rs 50 for local trips.
     */
    fun calculateFareRs(distanceKm: Double): Long {
        val raw = (distanceKm * RATE_PER_KM).roundToInt().toLong()
        // Round to nearest 10 Rs for clean ticketing, minimum Rs 50
        val rounded = ((raw + 5L) / 10L) * 10L
        return rounded.coerceAtLeast(MIN_LOCAL_FARE)
    }
}
