package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.BookingTicket
import com.example.data.DriverProfile
import com.example.data.SaiphaiGeoUtils
import com.example.ui.theme.AutoGreen
import com.example.ui.theme.EmblemGold
import com.example.ui.theme.SaiphaiNavy
import com.example.ui.theme.SaiphaiNavyDark
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapAndBookingSection(
    draft: BookingDraftState,
    drivers: List<DriverProfile>,
    activeBooking: BookingTicket?,
    isMizo: Boolean,
    onSelectPickup: (String, Double, Double) -> Unit,
    onSelectDropoff: (String, Double, Double, Double?, Long?) -> Unit,
    onSetPinTarget: (Boolean) -> Unit,
    onMapTap: (Double, Double) -> Unit,
    onUpdatePassengerDetails: (String, String, String) -> Unit,
    onSelectDriver: (DriverProfile) -> Unit,
    onConfirmBooking: (Boolean) -> Unit,
    onAdvanceRideStage: (BookingTicket) -> Unit,
    onShowTicketModal: (BookingTicket) -> Unit
) {
    val context = LocalContext.current
    var showPaymentGatewayDialog by remember { mutableStateOf(false) }
    var upiIdInput by remember { mutableStateOf("passenger@okaxis") }
    var gpsStatusText by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentGpsForPickup(context) { lat, lng, label ->
                onSelectPickup(label, lat, lng)
                gpsStatusText = if (isMizo) "GPS Location lak a ni: $label" else "GPS Location set: $label"
            }
        } else {
            gpsStatusText = if (isMizo) {
                "GPS Permission pek a ni lo; Saiphai Map aṭangin thlang rawh."
            } else {
                "GPS permission declined; tap any hub or point on the map."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Live Ride Tracking Banner if user has an ongoing ride
        if (activeBooking != null && activeBooking.status in listOf("booked", "driver_assigned", "arriving", "in_progress")) {
            ActiveRideLiveCard(
                booking = activeBooking,
                isMizo = isMizo,
                onAdvanceStage = { onAdvanceRideStage(activeBooking) },
                onViewTicket = { onShowTicketModal(activeBooking) },
                onCallDriver = { dialPhoneNumber(context, activeBooking.driverPhone) },
                onShareTicket = { shareBookingTicket(context, activeBooking, isMizo) }
            )
        }

        // Interactive Saiphai & Mizoram Map Card (Rs 50/km)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMizo) "Saiphai GPS Map & Kalduhna Thlanna" else "Saiphai Interactive Map & Route Picker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMizo) {
                                "Map-ah hmet la emaw khua thlang rawh • Rate: Rs 50 / km"
                            } else {
                                "Tap map to drop pin or choose hub below • Rate: Rs 50 / km"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Locate Me GPS Button
                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                fetchCurrentGpsForPickup(context) { lat, lng, label ->
                                    onSelectPickup(label, lat, lng)
                                    gpsStatusText = if (isMizo) "GPS Location: $label" else "GPS Location: $label"
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("gps_locate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Use GPS Location",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "GPS", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (gpsStatusText != null) {
                    Text(
                        text = gpsStatusText!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = AutoGreen,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Switch: Tap Map sets Pickup vs Destination
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = draft.selectingPickupPin,
                        onClick = { onSetPinTarget(true) },
                        label = {
                            Text(if (isMizo) "1. Chhuahna Pin (Pickup)" else "1. Set Pickup Pin")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("select_pickup_pin_mode")
                    )
                    FilterChip(
                        selected = !draft.selectingPickupPin,
                        onClick = { onSetPinTarget(false) },
                        label = {
                            Text(if (isMizo) "2. Kalduhna Pin (Dropoff)" else "2. Set Destination Pin")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("select_dropoff_pin_mode")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Map Canvas
                SaiphaiInteractiveMapCanvas(
                    pickupLat = draft.pickupLat,
                    pickupLng = draft.pickupLng,
                    dropoffLat = draft.dropoffLat,
                    dropoffLng = draft.dropoffLng,
                    pickupLabel = draft.pickupName,
                    dropoffLabel = draft.dropoffName,
                    drivers = drivers,
                    activeBooking = activeBooking,
                    onTapCoordinate = onMapTap
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Distance & Fare Breakdown Banner (Rs 50/km)
                Surface(
                    color = SaiphaiNavyDark,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMizo) "KILOMETER HISAP FARE (Rs 50/km)" else "DISTANCE FARE CALCULATOR (Rs 50/km)",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmblemGold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${draft.pickupName} → ${draft.dropoffName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format(
                                    "Distance: %.1f km × Rs %d/km",
                                    draft.distanceKm,
                                    SaiphaiGeoUtils.RATE_PER_KM
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        Surface(
                            color = EmblemGold,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Rs ${draft.fareAmount}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = SaiphaiNavyDark,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("calculated_fare_badge")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Destination Hub Chips (Saiphai Khawchhung Rs 50, N.Chawnpui Rs 250, etc.)
                Text(
                    text = if (isMizo) "Kalduhna Hmun Thlan Awlsam (Quick Destinations):" else "Quick Select Destinations:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SaiphaiGeoUtils.HUBS.forEach { hub ->
                        val isSelected = draft.dropoffName.contains(hub.nameMizo.take(8), ignoreCase = true) ||
                            draft.dropoffName.contains(hub.nameEn.take(8), ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectDropoff(
                                    if (isMizo) hub.nameMizo else hub.nameEn,
                                    hub.lat,
                                    hub.lng,
                                    hub.defaultDistanceKm,
                                    hub.defaultFareFromCenter
                                )
                            },
                            label = {
                                Text(
                                    text = "${if (isMizo) hub.nameMizo else hub.nameEn} • Rs ${hub.defaultFareFromCenter}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AutoGreen.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag("hub_chip_${hub.id}")
                        )
                    }
                }
            }
        }

        // Booking Form & Payment Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isMizo) "Ticket Lakna & Payment Mode" else "Passenger Details & Payment Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = draft.passengerName,
                        onValueChange = { onUpdatePassengerDetails(it, draft.passengerPhone, draft.paymentMode) },
                        label = { Text(if (isMizo) "Passenger Hming" else "Passenger Name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("passenger_name_input")
                    )
                    OutlinedTextField(
                        value = draft.passengerPhone,
                        onValueChange = { onUpdatePassengerDetails(draft.passengerName, it, draft.paymentMode) },
                        label = { Text(if (isMizo) "Phone No." else "Phone Number") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("passenger_phone_input")
                    )
                }

                // Driver Selector Horizontal Row
                if (drivers.isNotEmpty()) {
                    Text(
                        text = if (isMizo) "Driver Thlang Rawh (Auto Reg. No):" else "Choose Available Driver:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        drivers.forEach { drv ->
                            val isSelected = draft.selectedDriverId == drv.id ||
                                (draft.selectedDriverId.isBlank() && drv == drivers.first())
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectDriver(drv) },
                                label = {
                                    Text("${drv.name} (${drv.regNo})")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Payment Mode Selection: Cash / Online UPI / Card
                Text(
                    text = if (isMizo) "Payment Mode (Cash / Online):" else "Select Payment Mode:",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentOptionChip(
                        selected = draft.paymentMode == "cash",
                        label = if (isMizo) "Cash (Pawisa fai)" else "Cash on Ride",
                        icon = Icons.Default.LocalAtm,
                        onClick = { onUpdatePassengerDetails(draft.passengerName, draft.passengerPhone, "cash") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_mode_cash")
                    )
                    PaymentOptionChip(
                        selected = draft.paymentMode == "online_upi",
                        label = "Online UPI / QR",
                        icon = Icons.Default.AccountBalanceWallet,
                        onClick = { onUpdatePassengerDetails(draft.passengerName, draft.passengerPhone, "online_upi") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_mode_upi")
                    )
                    PaymentOptionChip(
                        selected = draft.paymentMode == "online_card",
                        label = "Card / Net",
                        icon = Icons.Default.CreditCard,
                        onClick = { onUpdatePassengerDetails(draft.passengerName, draft.passengerPhone, "online_card") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_mode_card")
                    )
                }

                // Book Auto Ticket CTA Button
                Button(
                    onClick = {
                        if (draft.paymentMode == "cash") {
                            onConfirmBooking(false)
                        } else {
                            showPaymentGatewayDialog = true
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AutoGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("book_ticket_button")
                ) {
                    Icon(imageVector = Icons.Default.ConfirmationNumber, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isMizo) {
                            "AUTO TICKET LA RAWH • Rs ${draft.fareAmount}"
                        } else {
                            "BOOK AUTO TICKET • Rs ${draft.fareAmount}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Online Payment Gateway Modal
    if (showPaymentGatewayDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentGatewayDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = AutoGreen,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = if (isMizo) "Saiphai Auto Online Payment Gateway" else "Secure Online Payment Gateway",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = SaiphaiNavyDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SAIPHAI AUTO ASSOCIATION MERCHANT",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmblemGold
                            )
                            Text(
                                text = "Rs ${draft.fareAmount}.00",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${draft.pickupName} → ${draft.dropoffName} (${draft.distanceKm} km)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = upiIdInput,
                        onValueChange = { upiIdInput = it },
                        label = {
                            Text(
                                if (draft.paymentMode == "online_upi") "UPI ID / GPay / PhonePe" else "Card Last 4 / Reference"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (isMizo) {
                            "Online payment tih zawhah Ticket leh 4-digit Ride OTP pek chhuah nghal a ni ang."
                        } else {
                            "Instant verified payment receipt & 4-digit Ride OTP will be generated upon confirmation."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaymentGatewayDialog = false
                        onConfirmBooking(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                    modifier = Modifier.testTag("confirm_online_payment_button")
                ) {
                    Text(if (isMizo) "Pay Rs ${draft.fareAmount} & Book" else "Pay Rs ${draft.fareAmount} Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentGatewayDialog = false }) {
                    Text(if (isMizo) "Sut leh" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun PaymentOptionChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) AutoGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) AutoGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AutoGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SaiphaiInteractiveMapCanvas(
    pickupLat: Double,
    pickupLng: Double,
    dropoffLat: Double,
    dropoffLng: Double,
    pickupLabel: String,
    dropoffLabel: String,
    drivers: List<DriverProfile>,
    activeBooking: BookingTicket?,
    onTapCoordinate: (Double, Double) -> Unit
) {
    // Bounding box covering Saiphai, N.Chawnpui, Phainuam, Bilkhawthlir, Kolasib, Vairengte
    val minLat = 24.1500
    val maxLat = 24.5200
    val minLng = 92.6600
    val maxLng = 92.8200

    fun toNormalizedOffset(lat: Double, lng: Double, width: Float, height: Float): Offset {
        val xRatio = ((lng - minLng) / (maxLng - minLng)).coerceIn(0.05, 0.95).toFloat()
        val yRatio = (1.0 - ((lat - minLat) / (maxLat - minLat)).coerceIn(0.06, 0.94)).toFloat()
        return Offset(xRatio * width, yRatio * height)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0E2238))
            .border(1.5.dp, EmblemGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .testTag("saiphai_interactive_map")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val lng = minLng + (tapOffset.x / size.width.coerceAtLeast(1)) * (maxLng - minLng)
                        val lat = minLat + (1f - (tapOffset.y / size.height.coerceAtLeast(1))) * (maxLat - minLat)
                        onTapCoordinate(lat, lng)
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // Topographic grid lines
            val gridStepX = w / 6f
            val gridStepY = h / 5f
            for (i in 1..5) {
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(i * gridStepX, 0f),
                    end = Offset(i * gridStepX, h),
                    strokeWidth = 1f
                )
            }
            for (j in 1..4) {
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, j * gridStepY),
                    end = Offset(w, j * gridStepY),
                    strokeWidth = 1f
                )
            }

            // Draw highway & inter-village road network connecting Saiphai hubs
            val roadPath = Path().apply {
                val saiphai = toNormalizedOffset(24.1954, 92.7661, w, h)
                val nChawnpui = toNormalizedOffset(24.2255, 92.7932, w, h)
                val phainuam = toNormalizedOffset(24.1720, 92.7430, w, h)
                val bilkhawthlir = toNormalizedOffset(24.3310, 92.7190, w, h)
                val vairengte = toNormalizedOffset(24.5020, 92.7610, w, h)
                val kolasib = toNormalizedOffset(24.2240, 92.6760, w, h)

                moveTo(phainuam.x, phainuam.y)
                lineTo(saiphai.x, saiphai.y)
                lineTo(nChawnpui.x, nChawnpui.y)
                moveTo(saiphai.x, saiphai.y)
                lineTo(bilkhawthlir.x, bilkhawthlir.y)
                lineTo(vairengte.x, vairengte.y)
                moveTo(bilkhawthlir.x, bilkhawthlir.y)
                lineTo(kolasib.x, kolasib.y)
            }
            drawPath(
                path = roadPath,
                color = Color.White.copy(alpha = 0.18f),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // Draw Hubs as subtle nodes
            SaiphaiGeoUtils.HUBS.forEach { hub ->
                val pt = toNormalizedOffset(hub.lat, hub.lng, w, h)
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = 5f,
                    center = pt
                )
            }

            // Active Route Line from Pickup to Dropoff
            val startPt = toNormalizedOffset(pickupLat, pickupLng, w, h)
            val endPt = toNormalizedOffset(dropoffLat, dropoffLng, w, h)
            val controlPt = Offset(
                x = (startPt.x + endPt.x) / 2f,
                y = minOf(startPt.y, endPt.y) - 24f
            )
            val activeRoutePath = Path().apply {
                moveTo(startPt.x, startPt.y)
                quadraticTo(controlPt.x, controlPt.y, endPt.x, endPt.y)
            }
            drawPath(
                path = activeRoutePath,
                color = EmblemGold,
                style = Stroke(
                    width = 7f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                )
            )

            // Available Driver Markers
            drivers.take(5).forEachIndexed { idx, drv ->
                val dPt = toNormalizedOffset(
                    drv.currentLat + (idx * 0.004),
                    drv.currentLng - (idx * 0.003),
                    w,
                    h
                )
                drawCircle(
                    color = AutoGreen,
                    radius = 9f,
                    center = dPt
                )
            }

            // Pickup Pin (Auto Green Circle)
            drawCircle(color = Color.White, radius = 15f, center = startPt)
            drawCircle(color = AutoGreen, radius = 11f, center = startPt)

            // Dropoff Pin (Emblem Gold Circle)
            drawCircle(color = Color.White, radius = 15f, center = endPt)
            drawCircle(color = EmblemGold, radius = 11f, center = endPt)

            // Live Tracking Auto Rickshaw Position if active booking exists
            if (activeBooking != null && activeBooking.status in listOf("booked", "driver_assigned", "arriving", "in_progress")) {
                val t = activeBooking.driverProgress.coerceIn(0.0, 1.0).toFloat()
                val liveX = (1 - t) * (1 - t) * startPt.x + 2 * (1 - t) * t * controlPt.x + t * t * endPt.x
                val liveY = (1 - t) * (1 - t) * startPt.y + 2 * (1 - t) * t * controlPt.y + t * t * endPt.y
                val livePt = Offset(liveX, liveY)
                drawCircle(color = EmblemGold.copy(alpha = 0.35f), radius = 24f, center = livePt)
                drawCircle(color = SaiphaiNavyDark, radius = 14f, center = livePt)
                drawCircle(color = EmblemGold, radius = 10f, center = livePt)
            }
        }

        // Map Legend Overlay Top-Left
        Surface(
            color = SaiphaiNavyDark.copy(alpha = 0.85f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(
                    text = "● Pickup: $pickupLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF4ADE80),
                    maxLines = 1
                )
                Text(
                    text = "● Drop: $dropoffLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmblemGold,
                    maxLines = 1
                )
            }
        }

        // Bottom-Right Map Coordinate Badge
        Surface(
            color = SaiphaiNavyDark.copy(alpha = 0.85f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        ) {
            Text(
                text = String.format("Saiphai GPS • %.3f°N, %.3f°E", dropoffLat, dropoffLng),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ActiveRideLiveCard(
    booking: BookingTicket,
    isMizo: Boolean,
    onAdvanceStage: () -> Unit,
    onViewTicket: () -> Unit,
    onCallDriver: () -> Unit,
    onShareTicket: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SaiphaiNavy),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, EmblemGold)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AutoGreen,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "LIVE TRACKING • ${booking.status.uppercase().replace('_', ' ')}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = EmblemGold,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "OTP PIN: ${booking.otpPin}",
                        style = MaterialTheme.typography.labelLarge,
                        color = SaiphaiNavyDark,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${booking.pickupName} → ${booking.dropoffName}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Driver: ${booking.driverName} • Reg: ${booking.driverRegNo} • Rs ${booking.fareAmount} (${booking.distanceKm} km)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCallDriver,
                    colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call Driver", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isMizo) "Driver Bia" else "Call")
                }

                OutlinedButton(
                    onClick = onShareTicket,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmblemGold),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Ticket", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                Button(
                    onClick = onAdvanceStage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmblemGold,
                        contentColor = SaiphaiNavyDark
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("advance_ride_stage_button")
                ) {
                    Text(
                        text = when (booking.status) {
                            "booked" -> "Assign Auto"
                            "driver_assigned" -> "Driver Arriving"
                            "arriving" -> "Verify OTP & Start"
                            else -> "Complete Ride"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchCurrentGpsForPickup(
    context: Context,
    onResult: (Double, Double, String) -> Unit
) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val label = String.format("GPS Pickup (%.4f, %.4f)", loc.latitude, loc.longitude)
                onResult(loc.latitude, loc.longitude, label)
            } else {
                onResult(24.1954, 92.7661, "Saiphai Bazar GPS Stand")
            }
        }.addOnFailureListener {
            onResult(24.1954, 92.7661, "Saiphai Bazar GPS Stand")
        }
    } catch (_: Exception) {
        onResult(24.1954, 92.7661, "Saiphai Bazar GPS Stand")
    }
}

fun dialPhoneNumber(context: Context, phone: String) {
    val sanitized = phone.filter { it.isDigit() || it == '+' }
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

fun shareBookingTicket(context: Context, ticket: BookingTicket, isMizo: Boolean) {
    val shareBody = buildString {
        appendLine("🛺 SAIPHAI AUTO DRIVER ASSOCIATION — OFFICIAL TICKET")
        appendLine("Ticket No: ${ticket.ticketCode} | Ride OTP: ${ticket.otpPin}")
        appendLine("Passenger: ${ticket.passengerName} (${ticket.passengerPhone})")
        appendLine("Route: ${ticket.pickupName} → ${ticket.dropoffName}")
        appendLine("Distance: ${ticket.distanceKm} km (Rate: Rs 50/km)")
        appendLine("Total Fare: Rs ${ticket.fareAmount} (${ticket.paymentMode.uppercase()} - ${ticket.paymentStatus.uppercase()})")
        appendLine("Driver: ${ticket.driverName} | Reg No: ${ticket.driverRegNo} | Phone: ${ticket.driverPhone}")
        appendLine("Status: ${ticket.status.uppercase()}")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Saiphai Auto Ticket ${ticket.ticketCode}")
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }
    val chooser = Intent.createChooser(
        sendIntent,
        if (isMizo) "Saiphai Auto Ticket Share-na" else "Share Saiphai Auto Ticket"
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(chooser)
    } catch (_: Exception) {
    }
}
