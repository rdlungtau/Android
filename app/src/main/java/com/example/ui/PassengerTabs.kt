package com.example.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BookingTicket
import com.example.data.DriverProfile
import com.example.data.FareRoute
import com.example.data.RideReview
import com.example.ui.theme.AutoGreen
import com.example.ui.theme.EmblemGold
import com.example.ui.theme.SaiphaiNavy
import com.example.ui.theme.SaiphaiNavyDark
import com.example.ui.theme.StatusAvailable
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnTrip

@Composable
fun FareListSection(
    fares: List<FareRoute>,
    isMizo: Boolean,
    onSelectFareForBooking: (FareRoute) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(fares, searchQuery) {
        if (searchQuery.isBlank()) fares
        else fares.filter {
            it.fromLocation.contains(searchQuery, ignoreCase = true) ||
                it.toLocation.contains(searchQuery, ignoreCase = true) ||
                it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Banner with Share Fare List Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SaiphaiNavy)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMizo) "SAIPHAI AUTO OFFICIAL FARE LIST" else "OFFICIAL ASSOCIATION FARE CHART",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmblemGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isMizo) {
                            "Saiphai khawchhung Rs 50 • Saiphai - N.Chawnpui Rs 250 • Standard Rate: Rs 50/km"
                        } else {
                            "Town Local Rs 50 • Saiphai - N.Chawnpui Rs 250 • Standard Rate: Rs 50/km"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                IconButton(
                    onClick = {
                        val fareText = buildString {
                            appendLine("🛺 SAIPHAI AUTO DRIVER ASSOCIATION — OFFICIAL FARE LIST")
                            fares.forEach { f ->
                                appendLine("• ${f.fromLocation} - ${f.toLocation}: Rs ${f.fareAmount} (${f.distanceKm} km)")
                            }
                            appendLine("Standard Distance Rate: Rs 50 / km")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, fareText)
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Share Fare List").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                    modifier = Modifier.testTag("share_fare_list_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Fare List",
                        tint = EmblemGold
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = {
                Text(if (isMizo) "Khua emaw veng zawng rawh (eg. Chawnpui)..." else "Search route or destination...")
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fare_search_input")
        )

        filtered.forEach { fare ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fare_item_card_${fare.toLocation}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AutoGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${fare.fromLocation} – ${fare.toLocation}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${fare.distanceKm} km • ${fare.routeType.replace('_', ' ').uppercase()}" +
                                if (fare.notes.isNotBlank()) " • ${fare.notes}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = EmblemGold.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Rs ${fare.fareAmount}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { onSelectFareForBooking(fare) },
                            colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMizo) "Thlang" else "Book",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriverDirectorySection(
    drivers: List<DriverProfile>,
    isMizo: Boolean,
    onSelectDriverForBooking: (DriverProfile) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filteredDrivers = remember(drivers, query) {
        if (query.isBlank()) drivers
        else drivers.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.regNo.contains(query, ignoreCase = true) ||
                it.standName.contains(query, ignoreCase = true) ||
                it.contactPhone.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SaiphaiNavy)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isMizo) {
                        "SAIPHAI AUTO DRIVER LIST & PHONE NO."
                    } else {
                        "REGISTERED DRIVER DIRECTORY & HOTLINE"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = EmblemGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isMizo) {
                        "Driver Hming, Auto Reg. No, Stand leh Phone Call Button awlsam taka biak theihna."
                    } else {
                        "Verified Saiphai Auto Driver Association members with Registration No., Stand & direct Call button."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = {
                Text(if (isMizo) "Driver hming, Reg. No (MZ-05...) zawng rawh" else "Search driver name, Reg. No, or stand...")
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("driver_search_input")
        )

        filteredDrivers.forEach { driver ->
            val statusColor = when (driver.status) {
                "available" -> StatusAvailable
                "on_trip" -> StatusOnTrip
                else -> StatusOffline
            }
            val statusLabel = when (driver.status) {
                "available" -> if (isMizo) "Awl mek (Available)" else "Available"
                "on_trip" -> if (isMizo) "Tlan mek (On Trip)" else "On Trip"
                else -> "Offline"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("driver_card_${driver.regNo}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SaiphaiNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = EmblemGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = driver.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (driver.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified Association Driver",
                                            tint = AutoGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${driver.standName} • ${driver.vehicleModel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Reg No, Phone No, Rating Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Indian Commercial Yellow License Plate Badge
                        Surface(
                            color = EmblemGold,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "REG: ${driver.regNo}",
                                style = MaterialTheme.typography.labelLarge,
                                color = SaiphaiNavyDark,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = driver.contactPhone,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = EmblemGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = String.format("%.1f (%d)", driver.ratingAvg, driver.totalTrips),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Call Button & Book Driver Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { dialPhoneNumber(context, driver.contactPhone) },
                            colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_driver_button_${driver.regNo}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call Driver",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMizo) "Call Driver" else "Call Now",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onSelectDriverForBooking(driver) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("book_driver_button_${driver.regNo}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ConfirmationNumber,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMizo) "Ticket La" else "Book Auto",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RidesAndReviewsSection(
    bookings: List<BookingTicket>,
    reviews: List<RideReview>,
    isMizo: Boolean,
    onAdvanceRideStage: (BookingTicket) -> Unit,
    onCancelBooking: (BookingTicket) -> Unit,
    onSubmitReview: (BookingTicket, Boolean, Int, String) -> Unit
) {
    val context = LocalContext.current
    var reviewTargetBooking by remember { mutableStateOf<BookingTicket?>(null) }
    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var rateAsPassenger by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (isMizo) "Ticket Lak Tawhte & Live Ride History" else "My Tickets, Live Tracking & Ride History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (bookings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = EmblemGold,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isMizo) "Ticket lak a la awm lo." else "No auto tickets booked yet.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isMizo) {
                            "Book & Map tab aṭangin i kalduhna thlang la ticket la rawh."
                        } else {
                            "Select your route on the Map tab or Fare List to book your first Saiphai Auto ticket."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            bookings.forEach { ticket ->
                val isCompleted = ticket.status == "completed"
                val isCancelled = ticket.status == "cancelled"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_history_card_${ticket.ticketCode}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                color = SaiphaiNavyDark,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${ticket.ticketCode} • OTP: ${ticket.otpPin}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmblemGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = when {
                                    isCompleted -> AutoGreen.copy(alpha = 0.16f)
                                    isCancelled -> MaterialTheme.colorScheme.errorContainer
                                    else -> EmblemGold.copy(alpha = 0.22f)
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = ticket.status.uppercase().replace('_', ' '),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        isCompleted -> AutoGreen
                                        isCancelled -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> SaiphaiNavyDark
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "${ticket.pickupName} → ${ticket.dropoffName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Driver: ${ticket.driverName} (${ticket.driverRegNo}) • Phone: ${ticket.driverPhone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Distance: ${ticket.distanceKm} km (Rs 50/km)",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Rs ${ticket.fareAmount} • ${ticket.paymentMode.uppercase()} (${ticket.paymentStatus.uppercase()})",
                                style = MaterialTheme.typography.labelLarge,
                                color = AutoGreen,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (!isCompleted && !isCancelled) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { ticket.driverProgress.toFloat().coerceIn(0.05f, 1f) },
                                    color = AutoGreen,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = if (isMizo) {
                                        "Live Tracking: ${(ticket.driverProgress * 100).toInt()}% tlan tawh"
                                    } else {
                                        "Live Route Progress: ${(ticket.driverProgress * 100).toInt()}%"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { shareBookingTicket(context, ticket, isMizo) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_ticket_btn_${ticket.ticketCode}")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Ticket", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }

                            OutlinedButton(
                                onClick = { dialPhoneNumber(context, ticket.driverPhone) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call Driver", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call")
                            }

                            if (!isCompleted && !isCancelled) {
                                Button(
                                    onClick = { onAdvanceRideStage(ticket) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text(
                                        text = if (ticket.status == "in_progress") "Finish Ride" else "Next Step",
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = { onCancelBooking(ticket) }) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Cancel Booking",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else if (isCompleted) {
                                Button(
                                    onClick = {
                                        reviewTargetBooking = ticket
                                        rateAsPassenger = !ticket.passengerRated
                                        reviewRating = 5
                                        reviewComment = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmblemGold,
                                        contentColor = SaiphaiNavyDark
                                    ),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("rate_ride_btn_${ticket.ticketCode}")
                                ) {
                                    Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isMizo) "Rate/Review" else "Rate Ride")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Community Reviews Section (Both Driver & Passenger ratings)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isMizo) {
                "Driver & Passenger Rating leh Review-te (${reviews.size})"
            } else {
                "Verified Driver & Passenger Reviews (${reviews.size})"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        reviews.forEach { rev ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${rev.targetName} (${rev.targetRole.uppercase()})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            repeat(rev.rating.toInt().coerceIn(1, 5)) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = EmblemGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "\"${rev.comment}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "By ${rev.reviewerName} • Route: ${rev.routeSummary}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Two-Way Rating & Review Dialog (Rate Driver or Rate Passenger)
    if (reviewTargetBooking != null) {
        val targetBooking = reviewTargetBooking!!
        AlertDialog(
            onDismissRequest = { reviewTargetBooking = null },
            title = {
                Text(
                    text = if (isMizo) "Driver & Passenger Rating Pekna" else "Rate & Review Ride Experience",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { rateAsPassenger = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (rateAsPassenger) AutoGreen.copy(alpha = 0.16f) else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isMizo) "Rate Driver" else "Rate Driver")
                        }
                        OutlinedButton(
                            onClick = { rateAsPassenger = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!rateAsPassenger) AutoGreen.copy(alpha = 0.16f) else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isMizo) "Rate Passenger" else "Rate Passenger")
                        }
                    }

                    Text(
                        text = if (rateAsPassenger) {
                            "Driver: ${targetBooking.driverName} (${targetBooking.driverRegNo})"
                        } else {
                            "Passenger: ${targetBooking.passengerName}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (star in 1..5) {
                            IconButton(onClick = { reviewRating = star }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$star Stars",
                                    tint = if (star <= reviewRating) EmblemGold else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = {
                            Text(if (isMizo) "I ngaihdan ziak rawh (Comment)" else "Write your feedback...")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitReview(targetBooking, rateAsPassenger, reviewRating, reviewComment)
                        reviewTargetBooking = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoGreen)
                ) {
                    Text(if (isMizo) "Submit Review" else "Submit Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewTargetBooking = null }) {
                    Text(if (isMizo) "Khawih lo" else "Close")
                }
            }
        )
    }
}
