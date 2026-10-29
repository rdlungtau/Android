package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.AutoGreen
import com.example.ui.theme.EmblemGold
import com.example.ui.theme.SaiphaiNavy
import com.example.ui.theme.SaiphaiNavyDark
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AuthScreen(
    isMizo: Boolean,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SaiphaiNavyDark, SaiphaiNavy, Color(0xFF0A2E1F))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language Toggle Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilterChip(
                    selected = true,
                    onClick = onToggleLanguage,
                    label = {
                        Text(
                            text = if (isMizo) "Mizo • Switch to EN" else "English • Mizo-in",
                            color = EmblemGold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.switch_language_desc),
                            tint = EmblemGold,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("auth_language_toggle")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Official Saiphai Auto Driver Association Logo
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clip(CircleShape)
                    .border(3.dp, EmblemGold, CircleShape)
                    .background(SaiphaiNavyDark),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.saiphai_logo),
                    contentDescription = stringResource(R.string.logo_content_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = EmblemGold.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmblemGold.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "ESTD : 2020 • SAIPHAI, MIZORAM",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmblemGold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SAIPHAI AUTO\nTICKET BOOKING",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isMizo) {
                    "Saiphai Auto Driver Association official ticket lakna, Fare list enna, Driver biakna leh Map aṭanga kalduhna thlanna (Rs 50/km)."
                } else {
                    "Official Saiphai Auto Driver Association ticket booking, live GPS map fare calculator (Rs 50/km), driver directory & real-time tracking."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature highlights card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuthFeatureRow(
                        icon = Icons.Default.LocationOn,
                        title = if (isMizo) "Map & Fare List (Rs 50/km)" else "Interactive Map & Fare List (Rs 50/km)",
                        subtitle = if (isMizo) "Saiphai khawchhung Rs 50 • Saiphai - N.Chawnpui Rs 250" else "Saiphai Local Rs 50 • N.Chawnpui Rs 250 & custom GPS pins"
                    )
                    AuthFeatureRow(
                        icon = Icons.Default.DirectionsCar,
                        title = if (isMizo) "Driver List, Reg. No & Call" else "Verified Driver Directory & Direct Call",
                        subtitle = if (isMizo) "Auto Reg. No, Phone No. leh Live Ride Tracking" else "Instant call button, vehicle registration & live tracking"
                    )
                    AuthFeatureRow(
                        icon = Icons.Default.VerifiedUser,
                        title = if (isMizo) "Ride OTP PIN & Online/Cash Payment" else "Ride OTP Verification & Cash/UPI Payment",
                        subtitle = if (isMizo) "Passenger leh Driver tan him taka chuan theihna" else "4-digit ride verification OTP, digital ticket share & admin panel"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmblemGold,
                    contentColor = SaiphaiNavyDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SaiphaiNavyDark,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isMizo) "Lut mek..." else "Signing in...",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.sign_in_google),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isMizo) {
                    "Google Account hmangin him takin lut rawh (Passenger & Admin/Driver)"
                } else {
                    "Secure cloud sync powered by Google Sign-In for Passengers, Drivers & Admins"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AutoGreen.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmblemGold,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}
