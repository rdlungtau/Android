package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditRoad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BookingTicket
import com.example.data.DriverProfile
import com.example.data.FareRoute
import com.example.data.RideReview
import com.example.data.SaiphaiRepository
import com.example.ui.AdminAnalyticsSection
import com.example.ui.AdminDispatchBookingsSection
import com.example.ui.AdminDriverUpdateSection
import com.example.ui.AdminFareUpdateSection
import com.example.ui.AdminTab
import com.example.ui.AppRoleMode
import com.example.ui.AuthScreen
import com.example.ui.DriverDirectorySection
import com.example.ui.FareListSection
import com.example.ui.MapAndBookingSection
import com.example.ui.PassengerTab
import com.example.ui.RidesAndReviewsSection
import com.example.ui.SaiphaiNotificationHelper
import com.example.ui.SaiphaiViewModel
import com.example.ui.UiState
import com.example.ui.signOut
import com.example.ui.theme.AutoGreen
import com.example.ui.theme.EmblemGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SaiphaiNavy
import com.example.ui.theme.SaiphaiNavyDark
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SaiphaiNotificationHelper.ensureChannel(this)
        setContent {
            MyApplicationTheme {
                AppRootNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppRootNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isMizoPreAuth by remember { mutableStateOf(true) }
    val user = currentUser

    if (user == null) {
        AuthScreen(
            isMizo = isMizoPreAuth,
            onToggleLanguage = { isMizoPreAuth = !isMizoPreAuth },
            onAuthSuccess = {}
        )
    } else {
        SaiphaiAuthenticatedApp(
            currentUserId = user.uid,
            userDisplayName = user.displayName ?: user.email?.substringBefore('@') ?: "Passenger"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaiphaiAuthenticatedApp(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: SaiphaiViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SaiphaiViewModel(
                    repository = SaiphaiRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = userDisplayName
                )
            }
        }
    )

    // Request Android 13+ notification permission once signed in
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val isMizo by viewModel.isMizo.collectAsStateWithLifecycle()
    val roleMode by viewModel.roleMode.collectAsStateWithLifecycle()
    val passengerTab by viewModel.passengerTab.collectAsStateWithLifecycle()
    val adminTab by viewModel.adminTab.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val faresState by viewModel.faresState.collectAsStateWithLifecycle()
    val driversState by viewModel.driversState.collectAsStateWithLifecycle()
    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
    val reviewsState by viewModel.reviewsState.collectAsStateWithLifecycle()

    val fares = (faresState as? UiState.Success<List<FareRoute>>)?.data ?: emptyList()
    val drivers = (driversState as? UiState.Success<List<DriverProfile>>)?.data ?: emptyList()
    val bookings = (bookingsState as? UiState.Success<List<BookingTicket>>)?.data ?: emptyList()
    val reviews = (reviewsState as? UiState.Success<List<RideReview>>)?.data ?: emptyList()

    val activeBooking = remember(bookings) {
        bookings.firstOrNull { it.status in listOf("booked", "driver_assigned", "arriving", "in_progress") }
    }

    // BackHandler for secondary tabs and Admin/Driver mode
    BackHandler(
        enabled = roleMode == AppRoleMode.DRIVER_ADMIN || passengerTab != PassengerTab.BOOK_MAP
    ) {
        if (roleMode == AppRoleMode.DRIVER_ADMIN) {
            if (adminTab != AdminTab.ANALYTICS) {
                viewModel.setAdminTab(AdminTab.ANALYTICS)
            } else {
                viewModel.setRoleMode(AppRoleMode.PASSENGER)
            }
        } else {
            viewModel.setPassengerTab(PassengerTab.BOOK_MAP)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SaiphaiNavyDark,
                        titleContentColor = Color.White
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.saiphai_logo),
                                contentDescription = stringResource(R.string.logo_content_desc),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, EmblemGold, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SAIPHAI AUTO",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmblemGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (roleMode == AppRoleMode.PASSENGER) {
                                        if (isMizo) "Passenger Ticket App" else "Passenger Mode"
                                    } else {
                                        if (isMizo) "Admin / Driver Mode" else "Admin & Driver Portal"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    actions = {
                        // Role Switcher Chip (Passenger <-> Admin/Driver)
                        FilterChip(
                            selected = roleMode == AppRoleMode.DRIVER_ADMIN,
                            onClick = {
                                viewModel.setRoleMode(
                                    if (roleMode == AppRoleMode.PASSENGER) AppRoleMode.DRIVER_ADMIN
                                    else AppRoleMode.PASSENGER
                                )
                            },
                            label = {
                                Text(
                                    text = if (roleMode == AppRoleMode.PASSENGER) "Admin/Driver" else "Passenger",
                                    color = EmblemGold,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (roleMode == AppRoleMode.PASSENGER) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = "Switch Role Mode",
                                    tint = EmblemGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AutoGreen.copy(alpha = 0.3f),
                                containerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.testTag("role_mode_switcher")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Mizo / English Language Toggle
                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier.testTag("top_language_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = stringResource(R.string.switch_language_desc),
                                tint = EmblemGold
                            )
                        }

                        // Sign Out Button
                        IconButton(
                            onClick = {
                                signOut(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = stringResource(R.string.sign_out),
                                tint = Color.White
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    SaiphaiBottomNavigationBar(
                        roleMode = roleMode,
                        passengerTab = passengerTab,
                        adminTab = adminTab,
                        isMizo = isMizo,
                        activeBookingCount = if (activeBooking != null) 1 else 0,
                        onSelectPassengerTab = viewModel::setPassengerTab,
                        onSelectAdminTab = viewModel::setAdminTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    SaiphaiSideNavigationRail(
                        roleMode = roleMode,
                        passengerTab = passengerTab,
                        adminTab = adminTab,
                        isMizo = isMizo,
                        onSelectPassengerTab = viewModel::setPassengerTab,
                        onSelectAdminTab = viewModel::setAdminTab
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Notification / Feedback Banner
                        if (statusBanner != null) {
                            item {
                                Surface(
                                    color = AutoGreen,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = statusBanner ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.clearBanner() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss notification",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Loading indicator if initial stream is loading
                        if (faresState is UiState.Loading && driversState is UiState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = AutoGreen)
                                }
                            }
                        }

                        item {
                            if (roleMode == AppRoleMode.PASSENGER) {
                                when (passengerTab) {
                                    PassengerTab.BOOK_MAP -> {
                                        MapAndBookingSection(
                                            draft = draft,
                                            drivers = drivers,
                                            activeBooking = activeBooking,
                                            isMizo = isMizo,
                                            onSelectPickup = viewModel::selectPickupLocation,
                                            onSelectDropoff = viewModel::selectDropoffLocation,
                                            onSetPinTarget = viewModel::setPinSelectionTarget,
                                            onMapTap = viewModel::onMapCanvasTapped,
                                            onUpdatePassengerDetails = viewModel::updateDraftPassengerDetails,
                                            onSelectDriver = viewModel::selectDriverForBooking,
                                            onConfirmBooking = { paidOnline ->
                                                viewModel.confirmBooking(
                                                    availableDrivers = drivers,
                                                    paymentPaidOnline = paidOnline,
                                                    onBookingCreated = { title, body ->
                                                        SaiphaiNotificationHelper.sendStatusNotification(
                                                            context,
                                                            title,
                                                            body
                                                        )
                                                    }
                                                )
                                            },
                                            onAdvanceRideStage = { booking ->
                                                viewModel.advanceBookingRideStage(booking) { title, body ->
                                                    SaiphaiNotificationHelper.sendStatusNotification(
                                                        context,
                                                        title,
                                                        body
                                                    )
                                                }
                                            },
                                            onShowTicketModal = viewModel::showTicketModal
                                        )
                                    }

                                    PassengerTab.FARE_LIST -> {
                                        FareListSection(
                                            fares = fares,
                                            isMizo = isMizo,
                                            onSelectFareForBooking = viewModel::applyFareRouteToBooking
                                        )
                                    }

                                    PassengerTab.DRIVERS -> {
                                        DriverDirectorySection(
                                            drivers = drivers,
                                            isMizo = isMizo,
                                            onSelectDriverForBooking = viewModel::selectDriverForBooking
                                        )
                                    }

                                    PassengerTab.RIDES_REVIEWS -> {
                                        RidesAndReviewsSection(
                                            bookings = bookings,
                                            reviews = reviews,
                                            isMizo = isMizo,
                                            onAdvanceRideStage = { booking ->
                                                viewModel.advanceBookingRideStage(booking) { title, body ->
                                                    SaiphaiNotificationHelper.sendStatusNotification(
                                                        context,
                                                        title,
                                                        body
                                                    )
                                                }
                                            },
                                            onCancelBooking = viewModel::cancelBooking,
                                            onSubmitReview = viewModel::submitRideReview
                                        )
                                    }
                                }
                            } else {
                                when (adminTab) {
                                    AdminTab.ANALYTICS -> {
                                        AdminAnalyticsSection(
                                            fares = fares,
                                            drivers = drivers,
                                            bookings = bookings,
                                            reviews = reviews,
                                            isMizo = isMizo
                                        )
                                    }

                                    AdminTab.DISPATCH_BOOKINGS -> {
                                        AdminDispatchBookingsSection(
                                            bookings = bookings,
                                            isMizo = isMizo,
                                            onAdvanceStage = { booking ->
                                                viewModel.advanceBookingRideStage(booking) { title, body ->
                                                    SaiphaiNotificationHelper.sendStatusNotification(
                                                        context,
                                                        title,
                                                        body
                                                    )
                                                }
                                            },
                                            onRatePassenger = { booking, stars, comment ->
                                                viewModel.submitRideReview(
                                                    booking = booking,
                                                    rateDriver = false,
                                                    rating = stars,
                                                    comment = comment
                                                )
                                            }
                                        )
                                    }

                                    AdminTab.UPDATE_FARES -> {
                                        AdminFareUpdateSection(
                                            fares = fares,
                                            isMizo = isMizo,
                                            onSaveFare = viewModel::saveFareRoute,
                                            onDeleteFare = viewModel::deleteFareRoute
                                        )
                                    }

                                    AdminTab.UPDATE_DRIVERS -> {
                                        AdminDriverUpdateSection(
                                            drivers = drivers,
                                            isMizo = isMizo,
                                            onSaveDriver = viewModel::saveDriverProfile,
                                            onDeleteDriver = viewModel::deleteDriverProfile
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaiphaiBottomNavigationBar(
    roleMode: AppRoleMode,
    passengerTab: PassengerTab,
    adminTab: AdminTab,
    isMizo: Boolean,
    activeBookingCount: Int,
    onSelectPassengerTab: (PassengerTab) -> Unit,
    onSelectAdminTab: (AdminTab) -> Unit
) {
    NavigationBar {
        if (roleMode == AppRoleMode.PASSENGER) {
            NavigationBarItem(
                selected = passengerTab == PassengerTab.BOOK_MAP,
                onClick = { onSelectPassengerTab(PassengerTab.BOOK_MAP) },
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                label = { Text(if (isMizo) "Map & Book" else "Book Map") },
                modifier = Modifier.testTag("nav_tab_map")
            )
            NavigationBarItem(
                selected = passengerTab == PassengerTab.FARE_LIST,
                onClick = { onSelectPassengerTab(PassengerTab.FARE_LIST) },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                label = { Text(if (isMizo) "Fare List" else "Fares") },
                modifier = Modifier.testTag("nav_tab_fares")
            )
            NavigationBarItem(
                selected = passengerTab == PassengerTab.DRIVERS,
                onClick = { onSelectPassengerTab(PassengerTab.DRIVERS) },
                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                label = { Text(if (isMizo) "Drivers" else "Drivers") },
                modifier = Modifier.testTag("nav_tab_drivers")
            )
            NavigationBarItem(
                selected = passengerTab == PassengerTab.RIDES_REVIEWS,
                onClick = { onSelectPassengerTab(PassengerTab.RIDES_REVIEWS) },
                icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                label = {
                    Text(
                        if (activeBookingCount > 0) "Tickets ($activeBookingCount)"
                        else if (isMizo) "History" else "Tickets"
                    )
                },
                modifier = Modifier.testTag("nav_tab_tickets")
            )
        } else {
            NavigationBarItem(
                selected = adminTab == AdminTab.ANALYTICS,
                onClick = { onSelectAdminTab(AdminTab.ANALYTICS) },
                icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                label = { Text("Analytics") },
                modifier = Modifier.testTag("admin_tab_analytics")
            )
            NavigationBarItem(
                selected = adminTab == AdminTab.DISPATCH_BOOKINGS,
                onClick = { onSelectAdminTab(AdminTab.DISPATCH_BOOKINGS) },
                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                label = { Text("Dispatch") },
                modifier = Modifier.testTag("admin_tab_dispatch")
            )
            NavigationBarItem(
                selected = adminTab == AdminTab.UPDATE_FARES,
                onClick = { onSelectAdminTab(AdminTab.UPDATE_FARES) },
                icon = { Icon(Icons.Default.EditRoad, contentDescription = null) },
                label = { Text(if (isMizo) "Fare Update" else "Fares") },
                modifier = Modifier.testTag("admin_tab_fares")
            )
            NavigationBarItem(
                selected = adminTab == AdminTab.UPDATE_DRIVERS,
                onClick = { onSelectAdminTab(AdminTab.UPDATE_DRIVERS) },
                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                label = { Text(if (isMizo) "Driver Update" else "Drivers") },
                modifier = Modifier.testTag("admin_tab_drivers")
            )
        }
    }
}

@Composable
private fun SaiphaiSideNavigationRail(
    roleMode: AppRoleMode,
    passengerTab: PassengerTab,
    adminTab: AdminTab,
    isMizo: Boolean,
    onSelectPassengerTab: (PassengerTab) -> Unit,
    onSelectAdminTab: (AdminTab) -> Unit
) {
    NavigationRail {
        if (roleMode == AppRoleMode.PASSENGER) {
            NavigationRailItem(
                selected = passengerTab == PassengerTab.BOOK_MAP,
                onClick = { onSelectPassengerTab(PassengerTab.BOOK_MAP) },
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                label = { Text(if (isMizo) "Map & Book" else "Book") }
            )
            NavigationRailItem(
                selected = passengerTab == PassengerTab.FARE_LIST,
                onClick = { onSelectPassengerTab(PassengerTab.FARE_LIST) },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                label = { Text("Fare List") }
            )
            NavigationRailItem(
                selected = passengerTab == PassengerTab.DRIVERS,
                onClick = { onSelectPassengerTab(PassengerTab.DRIVERS) },
                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                label = { Text("Drivers") }
            )
            NavigationRailItem(
                selected = passengerTab == PassengerTab.RIDES_REVIEWS,
                onClick = { onSelectPassengerTab(PassengerTab.RIDES_REVIEWS) },
                icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                label = { Text("Tickets") }
            )
        } else {
            NavigationRailItem(
                selected = adminTab == AdminTab.ANALYTICS,
                onClick = { onSelectAdminTab(AdminTab.ANALYTICS) },
                icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                label = { Text("Analytics") }
            )
            NavigationRailItem(
                selected = adminTab == AdminTab.DISPATCH_BOOKINGS,
                onClick = { onSelectAdminTab(AdminTab.DISPATCH_BOOKINGS) },
                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                label = { Text("Dispatch") }
            )
            NavigationRailItem(
                selected = adminTab == AdminTab.UPDATE_FARES,
                onClick = { onSelectAdminTab(AdminTab.UPDATE_FARES) },
                icon = { Icon(Icons.Default.EditRoad, contentDescription = null) },
                label = { Text("Fares") }
            )
            NavigationRailItem(
                selected = adminTab == AdminTab.UPDATE_DRIVERS,
                onClick = { onSelectAdminTab(AdminTab.UPDATE_DRIVERS) },
                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                label = { Text("Drivers") }
            )
        }
    }
}
