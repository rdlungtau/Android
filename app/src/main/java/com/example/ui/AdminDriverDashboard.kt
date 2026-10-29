package com.example.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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

@Composable
fun AdminAnalyticsSection(
    fares: List<FareRoute>,
    drivers: List<DriverProfile>,
    bookings: List<BookingTicket>,
    reviews: List<RideReview>,
    isMizo: Boolean
) {
    val totalRevenue = bookings.filter { it.status != "cancelled" }.sumOf { it.fareAmount }
    val completedCount = bookings.count { it.status == "completed" }
    val activeTripsCount = bookings.count { it.status in listOf("booked", "driver_assigned", "arriving", "in_progress") }
    val availableDriversCount = drivers.count { it.status == "available" }
    val avgRating = if (reviews.isNotEmpty()) {
        reviews.map { it.rating }.average()
    } else if (drivers.isNotEmpty()) {
        drivers.map { it.ratingAvg }.average()
    } else 4.9

    val cashCount = bookings.count { it.paymentMode == "cash" }
    val onlineCount = bookings.count { it.paymentMode != "cash" }
    val totalPayCount = (cashCount + onlineCount).coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SaiphaiNavy)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (isMizo) {
                        "SAIPHAI AUTO ADMIN & ANALYTICS DASHBOARD"
                    } else {
                        "ASSOCIATION ADMIN ANALYTICS DASHBOARD"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = EmblemGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isMizo) {
                        "Fare list update na, Driver data update na, Booking notification leh sum lakluh chhinchhiahna."
                    } else {
                        "Real-time revenue metrics, fleet utilization, payment mode split & route performance."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
        }

        // 4 KPI Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiStatCard(
                title = if (isMizo) "Sum Lakluh (Revenue)" else "Total Revenue",
                value = "Rs $totalRevenue",
                subtitle = "${bookings.size} Total Tickets",
                accentColor = AutoGreen,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = if (isMizo) "Active / Completed" else "Active / Completed",
                value = "$activeTripsCount / $completedCount",
                subtitle = "Live Dispatch Status",
                accentColor = EmblemGold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiStatCard(
                title = if (isMizo) "Drivers Awl Mek" else "Available Drivers",
                value = "$availableDriversCount / ${drivers.size}",
                subtitle = "${fares.size} Official Routes",
                accentColor = AutoGreen,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                title = if (isMizo) "Association Rating" else "Fleet Rating",
                value = String.format("%.1f ★", avgRating),
                subtitle = "${reviews.size} Verified Reviews",
                accentColor = EmblemGold,
                modifier = Modifier.weight(1f)
            )
        }

        // Payment Mode Split Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isMizo) "Payment Mode Split (Cash vs Online UPI/Card)" else "Payment Mode Split (Cash vs Online)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { (onlineCount.toFloat() / totalPayCount.toFloat()).coerceIn(0.15f, 1f) },
                    color = AutoGreen,
                    trackColor = EmblemGold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Online UPI/Card: $onlineCount tickets",
                        style = MaterialTheme.typography.labelLarge,
                        color = AutoGreen
                    )
                    Text(
                        text = "Cash on Ride: $cashCount tickets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminFareUpdateSection(
    fares: List<FareRoute>,
    isMizo: Boolean,
    onSaveFare: (String?, String, String, Double, Long, String, String) -> Unit,
    onDeleteFare: (String) -> Unit
) {
    var editingFare by remember { mutableStateOf<FareRoute?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMizo) "Fare List Update Na" else "Manage & Update Fare List",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isMizo) "Rate thar dah belhna emaw siamṭhatna (Rs 50/km)" else "Add, edit, or remove official Saiphai Auto routes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    editingFare = null
                    showDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                modifier = Modifier.testTag("admin_add_fare_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isMizo) "Add Fare" else "Add Route")
            }
        }

        fares.forEach { fare ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${fare.fromLocation} – ${fare.toLocation}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rs ${fare.fareAmount} • ${fare.distanceKm} km (${fare.routeType})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AutoGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (fare.notes.isNotBlank()) {
                            Text(
                                text = fare.notes,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                editingFare = fare
                                showDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Fare", tint = SaiphaiNavy)
                        }
                        IconButton(onClick = { onDeleteFare(fare.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Fare",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var fromLoc by remember { mutableStateOf(editingFare?.fromLocation ?: "Saiphai") }
        var toLoc by remember { mutableStateOf(editingFare?.toLocation ?: "") }
        var distText by remember { mutableStateOf((editingFare?.distanceKm ?: 5.0).toString()) }
        var fareText by remember { mutableStateOf((editingFare?.fareAmount ?: 250L).toString()) }
        var routeType by remember { mutableStateOf(editingFare?.routeType ?: "inter_village") }
        var notes by remember { mutableStateOf(editingFare?.notes ?: "Official Association Rate") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    if (editingFare == null) {
                        if (isMizo) "Fare Route Thar Dahna" else "Add New Fare Route"
                    } else {
                        if (isMizo) "Fare Route Siamṭhatna" else "Update Fare Route"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fromLoc,
                        onValueChange = { fromLoc = it },
                        label = { Text("From Location") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = toLoc,
                        onValueChange = { toLoc = it },
                        label = { Text(if (isMizo) "Kalduhna Hmun (To Location)" else "To Location") },
                        singleLine = true,
                        modifier = Modifier.testTag("fare_dialog_to_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = distText,
                            onValueChange = {
                                distText = it
                                val d = it.toDoubleOrNull()
                                if (d != null && d > 0) {
                                    fareText = (d * 50).toLong().coerceAtLeast(50L).toString()
                                }
                            },
                            label = { Text("Distance (km)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fareText,
                            onValueChange = { fareText = it },
                            label = { Text("Fare (Rs)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fare_dialog_amount_input")
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dist = distText.toDoubleOrNull() ?: 1.0
                        val amount = fareText.toLongOrNull() ?: 50L
                        if (toLoc.isNotBlank()) {
                            onSaveFare(editingFare?.id, fromLoc, toLoc, dist, amount, routeType, notes)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                    modifier = Modifier.testTag("save_fare_dialog_button")
                ) {
                    Text(if (isMizo) "Save Fare" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminDriverUpdateSection(
    drivers: List<DriverProfile>,
    isMizo: Boolean,
    onSaveDriver: (DriverProfile?, String, String, String, String, String, String) -> Unit,
    onDeleteDriver: (String) -> Unit
) {
    var editingDriver by remember { mutableStateOf<DriverProfile?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMizo) "Driver Data Update Na" else "Manage & Update Drivers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isMizo) "Driver Hming, Reg. No, Phone No. leh Duty Status update-na" else "Update driver registration numbers, phones & availability",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    editingDriver = null
                    showDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                modifier = Modifier.testTag("admin_add_driver_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isMizo) "Add Driver" else "Add Driver")
            }
        }

        drivers.forEach { driver ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${driver.name} (${driver.regNo})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${driver.contactPhone} • ${driver.standName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Status: ${driver.status.uppercase()} • ${driver.vehicleModel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AutoGreen
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                editingDriver = driver
                                showDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Driver", tint = SaiphaiNavy)
                        }
                        IconButton(onClick = { onDeleteDriver(driver.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Driver",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf(editingDriver?.name ?: "") }
        var regNo by remember { mutableStateOf(editingDriver?.regNo ?: "MZ-05-A-") }
        var phone by remember { mutableStateOf(editingDriver?.contactPhone ?: "+91 ") }
        var stand by remember { mutableStateOf(editingDriver?.standName ?: "Saiphai Bazar Stand") }
        var model by remember { mutableStateOf(editingDriver?.vehicleModel ?: "Bajaj RE Compact 4S") }
        var status by remember { mutableStateOf(editingDriver?.status ?: "available") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    if (editingDriver == null) {
                        if (isMizo) "Driver Thar Dah Belhna" else "Register New Auto Driver"
                    } else {
                        if (isMizo) "Driver Data Siamṭhatna" else "Update Driver Profile"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isMizo) "Driver Hming" else "Driver Full Name") },
                        singleLine = true,
                        modifier = Modifier.testTag("driver_dialog_name_input")
                    )
                    OutlinedTextField(
                        value = regNo,
                        onValueChange = { regNo = it },
                        label = { Text("Auto Reg. No (e.g. MZ-05-A-1421)") },
                        singleLine = true,
                        modifier = Modifier.testTag("driver_dialog_reg_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = stand,
                        onValueChange = { stand = it },
                        label = { Text("Stand Name") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("available", "on_trip", "offline").forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.replace('_', ' ').uppercase()) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && regNo.isNotBlank()) {
                            onSaveDriver(editingDriver, name, regNo, phone, stand, model, status)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                    modifier = Modifier.testTag("save_driver_dialog_button")
                ) {
                    Text(if (isMizo) "Save Driver" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminDispatchBookingsSection(
    bookings: List<BookingTicket>,
    isMizo: Boolean,
    onAdvanceStage: (BookingTicket) -> Unit,
    onRatePassenger: (BookingTicket, Int, String) -> Unit
) {
    val context = LocalContext.current
    var ratingTargetBooking by remember { mutableStateOf<BookingTicket?>(null) }
    var otpInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isMizo) {
                "Booking Notification, Location & OTP Dispatch Console"
            } else {
                "Live Booking Notifications, GPS Coordinates & OTP Dispatch"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        bookings.forEach { booking ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = EmblemGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${booking.ticketCode} • Passenger: ${booking.passengerName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = SaiphaiNavyDark,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "OTP: ${booking.otpPin}",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmblemGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Pickup: ${booking.pickupName} (${String.format("%.4f, %.4f", booking.pickupLat, booking.pickupLng)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Dropoff: ${booking.dropoffName} (${String.format("%.4f, %.4f", booking.dropoffLat, booking.dropoffLng)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Fare: Rs ${booking.fareAmount} (${booking.distanceKm} km) • Payment: ${booking.paymentMode.uppercase()} (${booking.paymentStatus.uppercase()})",
                        style = MaterialTheme.typography.labelLarge,
                        color = AutoGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { dialPhoneNumber(context, booking.passengerPhone) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isMizo) "Passenger Bia" else "Call Passenger")
                        }

                        if (booking.status !in listOf("completed", "cancelled")) {
                            Button(
                                onClick = { onAdvanceStage(booking) },
                                colors = ButtonDefaults.buttonColors(containerColor = AutoGreen),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text(
                                    when (booking.status) {
                                        "booked" -> "Accept & Assign"
                                        "driver_assigned" -> "Mark Arriving"
                                        "arriving" -> "Verify OTP & Start"
                                        else -> "Complete Trip"
                                    }
                                )
                            }
                        } else if (booking.status == "completed" && !booking.driverRated) {
                            Button(
                                onClick = { ratingTargetBooking = booking },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmblemGold,
                                    contentColor = SaiphaiNavyDark
                                ),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text(if (isMizo) "Rate Passenger" else "Rate Passenger")
                            }
                        }
                    }
                }
            }
        }
    }

    if (ratingTargetBooking != null) {
        val b = ratingTargetBooking!!
        AlertDialog(
            onDismissRequest = { ratingTargetBooking = null },
            title = { Text("Rate Passenger: ${b.passengerName}") },
            text = {
                Text(
                    if (isMizo) {
                        "Passenger ${b.passengerName} chu 5-Star rating leh polite passenger badge pe rawh."
                    } else {
                        "Submit a 5-Star rating for passenger ${b.passengerName} on trip ${b.ticketCode}."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRatePassenger(b, 5, "Polite and punctual passenger!")
                        ratingTargetBooking = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoGreen)
                ) {
                    Text("Submit 5★ Rating")
                }
            },
            dismissButton = {
                TextButton(onClick = { ratingTargetBooking = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
