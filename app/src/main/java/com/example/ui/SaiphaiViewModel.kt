package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookingTicket
import com.example.data.DriverProfile
import com.example.data.FareRoute
import com.example.data.RideReview
import com.example.data.SaiphaiGeoUtils
import com.example.data.SaiphaiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppRoleMode {
    PASSENGER,
    DRIVER_ADMIN
}

enum class PassengerTab {
    BOOK_MAP,
    FARE_LIST,
    DRIVERS,
    RIDES_REVIEWS
}

enum class AdminTab {
    ANALYTICS,
    DISPATCH_BOOKINGS,
    UPDATE_FARES,
    UPDATE_DRIVERS
}

data class BookingDraftState(
    val pickupName: String = "Saiphai Bazar Stand (Center)",
    val pickupLat: Double = 24.1954,
    val pickupLng: Double = 92.7661,
    val dropoffName: String = "N.Chawnpui",
    val dropoffLat: Double = 24.2255,
    val dropoffLng: Double = 92.7932,
    val distanceKm: Double = 5.0,
    val fareAmount: Long = 250L,
    val isCustomPinMode: Boolean = false,
    val selectingPickupPin: Boolean = false,
    val passengerName: String = "",
    val passengerPhone: String = "+91 9862",
    val selectedDriverId: String = "",
    val paymentMode: String = "cash", // 'cash', 'online_upi', 'online_card'
    val isFixedAssociationRoute: Boolean = true
)

class SaiphaiViewModel(
    private val repository: SaiphaiRepository,
    val currentUserId: String,
    defaultDisplayName: String = ""
) : ViewModel() {

    private val _isMizo = MutableStateFlow(true)
    val isMizo: StateFlow<Boolean> = _isMizo.asStateFlow()

    private val _roleMode = MutableStateFlow(AppRoleMode.PASSENGER)
    val roleMode: StateFlow<AppRoleMode> = _roleMode.asStateFlow()

    private val _passengerTab = MutableStateFlow(PassengerTab.BOOK_MAP)
    val passengerTab: StateFlow<PassengerTab> = _passengerTab.asStateFlow()

    private val _adminTab = MutableStateFlow(AdminTab.ANALYTICS)
    val adminTab: StateFlow<AdminTab> = _adminTab.asStateFlow()

    private val _draft = MutableStateFlow(
        BookingDraftState(passengerName = defaultDisplayName.ifBlank { "Saiphai Passenger" })
    )
    val draft: StateFlow<BookingDraftState> = _draft.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _activeTicketForModal = MutableStateFlow<BookingTicket?>(null)
    val activeTicketForModal: StateFlow<BookingTicket?> = _activeTicketForModal.asStateFlow()

    // Two-tier Kotlin Flow Error Handling Architecture
    val faresState: StateFlow<UiState<List<FareRoute>>> = repository.observeFares(currentUserId)
        .map<List<FareRoute>, UiState<List<FareRoute>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing fares", error)
            emit(UiState.Error(error.message ?: "Failed to load fare list"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val driversState: StateFlow<UiState<List<DriverProfile>>> = repository.observeDrivers(currentUserId)
        .map<List<DriverProfile>, UiState<List<DriverProfile>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing drivers", error)
            emit(UiState.Error(error.message ?: "Failed to load driver list"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val bookingsState: StateFlow<UiState<List<BookingTicket>>> = repository.observeBookings(currentUserId)
        .map<List<BookingTicket>, UiState<List<BookingTicket>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing bookings", error)
            emit(UiState.Error(error.message ?: "Failed to load bookings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val reviewsState: StateFlow<UiState<List<RideReview>>> = repository.observeReviews(currentUserId)
        .map<List<RideReview>, UiState<List<RideReview>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing reviews", error)
            emit(UiState.Error(error.message ?: "Failed to load reviews"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureInitialSaiphaiDataSeeded(currentUserId)
        }
    }

    fun toggleLanguage() {
        _isMizo.value = !_isMizo.value
    }

    fun setRoleMode(mode: AppRoleMode) {
        _roleMode.value = mode
    }

    fun setPassengerTab(tab: PassengerTab) {
        _passengerTab.value = tab
    }

    fun setAdminTab(tab: AdminTab) {
        _adminTab.value = tab
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showTicketModal(ticket: BookingTicket?) {
        _activeTicketForModal.value = ticket
    }

    // --- MAP & FARE SELECTION ---

    fun selectPickupLocation(name: String, lat: Double, lng: Double) {
        val current = _draft.value
        val dist = SaiphaiGeoUtils.calculateRoadDistanceKm(lat, lng, current.dropoffLat, current.dropoffLng)
        val fare = SaiphaiGeoUtils.calculateFareRs(dist)
        _draft.value = current.copy(
            pickupName = name,
            pickupLat = lat,
            pickupLng = lng,
            distanceKm = dist,
            fareAmount = fare,
            isFixedAssociationRoute = false
        )
    }

    fun selectDropoffLocation(
        name: String,
        lat: Double,
        lng: Double,
        presetDistanceKm: Double? = null,
        presetFareRs: Long? = null
    ) {
        val current = _draft.value
        val dist = presetDistanceKm ?: SaiphaiGeoUtils.calculateRoadDistanceKm(
            current.pickupLat,
            current.pickupLng,
            lat,
            lng
        )
        val fare = presetFareRs ?: SaiphaiGeoUtils.calculateFareRs(dist)
        _draft.value = current.copy(
            dropoffName = name,
            dropoffLat = lat,
            dropoffLng = lng,
            distanceKm = dist,
            fareAmount = fare,
            isFixedAssociationRoute = presetFareRs != null
        )
    }

    fun applyFareRouteToBooking(route: FareRoute) {
        val matchingHub = SaiphaiGeoUtils.HUBS.find {
            it.nameMizo.contains(route.toLocation, ignoreCase = true) ||
                it.nameEn.contains(route.toLocation, ignoreCase = true) ||
                route.toLocation.contains(it.nameEn.take(6), ignoreCase = true)
        }
        val dropLat = matchingHub?.lat ?: (24.1954 + (route.distanceKm * 0.006))
        val dropLng = matchingHub?.lng ?: (92.7661 + (route.distanceKm * 0.005))
        _draft.value = _draft.value.copy(
            pickupName = route.fromLocation,
            pickupLat = 24.1954,
            pickupLng = 92.7661,
            dropoffName = route.toLocation,
            dropoffLat = dropLat,
            dropoffLng = dropLng,
            distanceKm = route.distanceKm.coerceAtLeast(1.0),
            fareAmount = route.fareAmount,
            isFixedAssociationRoute = true
        )
        _passengerTab.value = PassengerTab.BOOK_MAP
        _statusBanner.value = if (_isMizo.value) {
            "Route thlan a ni: ${route.fromLocation} -> ${route.toLocation} (Rs ${route.fareAmount})"
        } else {
            "Selected route: ${route.fromLocation} -> ${route.toLocation} (Rs ${route.fareAmount})"
        }
    }

    fun selectDriverForBooking(driver: DriverProfile) {
        _draft.value = _draft.value.copy(selectedDriverId = driver.id)
        _passengerTab.value = PassengerTab.BOOK_MAP
        _statusBanner.value = if (_isMizo.value) {
            "Driver thlan: ${driver.name} (${driver.regNo})"
        } else {
            "Driver selected: ${driver.name} (${driver.regNo})"
        }
    }

    fun updateDraftPassengerDetails(name: String, phone: String, paymentMode: String) {
        _draft.value = _draft.value.copy(
            passengerName = name,
            passengerPhone = phone,
            paymentMode = paymentMode
        )
    }

    fun setPinSelectionTarget(selectingPickup: Boolean) {
        _draft.value = _draft.value.copy(selectingPickupPin = selectingPickup)
    }

    fun onMapCanvasTapped(lat: Double, lng: Double) {
        val current = _draft.value
        val formattedCoords = String.format("%.4f°N, %.4f°E", lat, lng)
        if (current.selectingPickupPin) {
            selectPickupLocation("Map Pin ($formattedCoords)", lat, lng)
        } else {
            selectDropoffLocation("Map Pin ($formattedCoords)", lat, lng)
        }
    }

    // --- BOOKING CREATION & REAL-TIME TRACKING ---

    fun confirmBooking(
        availableDrivers: List<DriverProfile>,
        paymentPaidOnline: Boolean,
        onBookingCreated: (String, String) -> Unit
    ) {
        val d = _draft.value
        val chosenDriver = availableDrivers.find { it.id == d.selectedDriverId }
            ?: availableDrivers.firstOrNull { it.status == "available" }
            ?: availableDrivers.firstOrNull()
            ?: DriverProfile(
                id = "saiphai_default_driver",
                userId = currentUserId,
                name = "Lalrinfela Sailo",
                regNo = "MZ-05-A-1421",
                contactPhone = "+91 98621 45102"
            )

        val paymentStatus = if (paymentPaidOnline || d.paymentMode != "cash") "paid" else "pending"

        viewModelScope.launch {
            val result = repository.createBooking(
                passengerName = d.passengerName,
                passengerPhone = d.passengerPhone,
                driver = chosenDriver,
                pickupName = d.pickupName,
                dropoffName = d.dropoffName,
                pickupLat = d.pickupLat,
                pickupLng = d.pickupLng,
                dropoffLat = d.dropoffLat,
                dropoffLng = d.dropoffLng,
                distanceKm = d.distanceKm,
                fareAmount = d.fareAmount,
                paymentMode = d.paymentMode,
                paymentStatus = paymentStatus
            )
            result.onSuccess { bookingId ->
                val msg = if (_isMizo.value) {
                    "Auto Ticket lak fel a ni! Driver: ${chosenDriver.name} (${chosenDriver.regNo})"
                } else {
                    "Auto Ticket Confirmed! Driver: ${chosenDriver.name} (${chosenDriver.regNo})"
                }
                _statusBanner.value = msg
                onBookingCreated(
                    "Saiphai Auto Ticket Confirmed",
                    "${d.pickupName} → ${d.dropoffName} (Rs ${d.fareAmount}) • Driver: ${chosenDriver.name}"
                )
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to create booking"
            }
        }
    }

    fun advanceBookingRideStage(
        booking: BookingTicket,
        onNotification: (String, String) -> Unit
    ) {
        val (nextStatus, nextProgress, noteTitle) = when (booking.status) {
            "booked" -> Triple("driver_assigned", 0.2, "Driver Assigned • ${booking.driverName}")
            "driver_assigned" -> Triple("arriving", 0.45, "Auto Arriving at Pickup • ${booking.driverRegNo}")
            "arriving" -> Triple("in_progress", 0.75, "Ride Started (OTP ${booking.otpPin} Verified)")
            "in_progress" -> Triple("completed", 1.0, "Ride Completed • Rs ${booking.fareAmount}")
            else -> return
        }
        val newPaymentStatus = if (nextStatus == "completed") "paid" else booking.paymentStatus

        viewModelScope.launch {
            repository.updateBookingStatusAndProgress(
                bookingId = booking.id,
                newStatus = nextStatus,
                newProgress = nextProgress,
                paymentStatus = newPaymentStatus
            ).onSuccess {
                onNotification(
                    noteTitle,
                    "${booking.pickupName} → ${booking.dropoffName} (${booking.ticketCode})"
                )
            }
        }
    }

    fun cancelBooking(booking: BookingTicket) {
        if (booking.status == "completed" || booking.status == "cancelled") return
        viewModelScope.launch {
            repository.updateBookingStatusAndProgress(
                bookingId = booking.id,
                newStatus = "cancelled",
                newProgress = booking.driverProgress
            ).onSuccess {
                _statusBanner.value = if (_isMizo.value) {
                    "Ticket ${booking.ticketCode} cancel a ni."
                } else {
                    "Ticket ${booking.ticketCode} has been cancelled."
                }
            }
        }
    }

    // --- RATINGS & REVIEWS ---

    fun submitRideReview(
        booking: BookingTicket,
        rateDriver: Boolean,
        rating: Int,
        comment: String
    ) {
        viewModelScope.launch {
            val targetId = if (rateDriver) booking.driverId else booking.userId
            val targetName = if (rateDriver) "${booking.driverName} (${booking.driverRegNo})" else booking.passengerName
            val targetRole = if (rateDriver) "driver" else "passenger"
            val reviewerName = if (rateDriver) booking.passengerName else booking.driverName

            repository.submitReview(
                bookingId = booking.id,
                targetId = targetId,
                targetName = targetName,
                targetRole = targetRole,
                reviewerName = reviewerName,
                rating = rating,
                comment = comment.ifBlank {
                    if (_isMizo.value) "Tlan tluang tak a ni!" else "Smooth and punctual ride!"
                },
                routeSummary = "${booking.pickupName} → ${booking.dropoffName}"
            ).onSuccess {
                repository.markBookingRated(booking.id, byPassenger = rateDriver)
                _statusBanner.value = if (_isMizo.value) {
                    "Rating & Review thehluh a ni, kan lawm e!"
                } else {
                    "Thank you! Your rating & review has been saved."
                }
            }
        }
    }

    // --- ADMIN / DRIVER CRUD OPERATIONS ---

    fun saveFareRoute(
        existingId: String?,
        fromLocation: String,
        toLocation: String,
        distanceKm: Double,
        fareAmount: Long,
        routeType: String,
        notes: String
    ) {
        viewModelScope.launch {
            val result = if (existingId.isNullOrBlank()) {
                repository.createFare(fromLocation, toLocation, distanceKm, fareAmount, routeType, notes)
            } else {
                repository.updateFare(existingId, fromLocation, toLocation, distanceKm, fareAmount, routeType, notes)
            }
            result.onSuccess {
                _statusBanner.value = if (_isMizo.value) "Fare list update fel a ni!" else "Fare list updated!"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to update fare"
            }
        }
    }

    fun deleteFareRoute(fareId: String) {
        viewModelScope.launch {
            repository.deleteFare(fareId)
        }
    }

    fun saveDriverProfile(
        existingDriver: DriverProfile?,
        name: String,
        regNo: String,
        contactPhone: String,
        standName: String,
        vehicleModel: String,
        status: String
    ) {
        viewModelScope.launch {
            val result = if (existingDriver == null || existingDriver.id.isBlank()) {
                repository.createDriver(
                    name = name,
                    regNo = regNo,
                    contactPhone = contactPhone,
                    standName = standName,
                    vehicleModel = vehicleModel,
                    status = status
                )
            } else {
                repository.updateDriver(
                    driverId = existingDriver.id,
                    name = name,
                    regNo = regNo,
                    contactPhone = contactPhone,
                    standName = standName,
                    vehicleModel = vehicleModel,
                    status = status,
                    ratingAvg = existingDriver.ratingAvg,
                    totalTrips = existingDriver.totalTrips,
                    currentLat = existingDriver.currentLat,
                    currentLng = existingDriver.currentLng,
                    isVerified = existingDriver.isVerified
                )
            }
            result.onSuccess {
                _statusBanner.value = if (_isMizo.value) "Driver data update fel a ni!" else "Driver data updated!"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to update driver"
            }
        }
    }

    fun deleteDriverProfile(driverId: String) {
        viewModelScope.launch {
            repository.deleteDriver(driverId)
        }
    }

    private companion object {
        const val TAG = "SaiphaiVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
