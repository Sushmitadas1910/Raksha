package com.example.raksha

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val userEmail = auth.currentUser?.email ?: "Unknown"
    val scope = rememberCoroutineScope()

    var showSosDialog by remember { mutableStateOf(false) }
    var sosFiring by remember { mutableStateOf(false) }
    var silentMode by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Shake confirmation dialog state
    var showShakeConfirm by remember { mutableStateOf(false) }
    var shakeCountdown by remember { mutableStateOf(5) }
    var shakeCountdownJob by remember { mutableStateOf<Job?>(null) }

    // Call next dialog
    var showCallNextDialog by remember { mutableStateOf(false) }
    var callNextIndex by remember { mutableStateOf(1) }

    var contacts by remember { mutableStateOf(listOf<LocalContact>()) }
    DisposableEffect(Unit) {
        val stop = ContactRepository.listenContacts(context, scope) { contacts = it }
        onDispose { stop() }
    }

    val isOffline by remember { derivedStateOf { !isNetworkAvailable(context) } }

    val sosPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE
    )

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) showSosDialog = true
        else scope.launch {
            snackbarHostState.showSnackbar("Grant Location, SMS and Call permissions")
        }
    }

    fun fireSos() {
        sosFiring = true
        SosManager.trigger(context, silent = silentMode) { success ->
            sosFiring = false
            if (success && !silentMode && contacts.size > 1) {
                callNextIndex = 1
                showCallNextDialog = true
            }
        }
    }

    // Shake detection with confirmation countdown
    DisposableEffect(Unit) {
        val detector = ShakeDetector(context) {
            if (!sosFiring && !showShakeConfirm) {

                if (silentMode) {
                    // Silent mode — fire immediately, no confirmation
                    fireSos()
                } else {
                    // Normal mode — show 5-second confirmation countdown
                    // Vibrate to alert user that shake was detected
                    try {
                        @Suppress("DEPRECATION")
                        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            v.vibrate(300)
                        }
                    } catch (_: Exception) {}

                    shakeCountdown = 5
                    showShakeConfirm = true

                    // Start countdown — if reaches 0, fire SOS
                    shakeCountdownJob = scope.launch {
                        for (i in 4 downTo 0) {
                            delay(1000L)
                            shakeCountdown = i
                        }
                        // Countdown finished — fire SOS
                        showShakeConfirm = false
                        fireSos()
                    }
                }
            }
        }
        detector.start()
        onDispose { detector.stop() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
        HomeScreenUi(
            userEmail = userEmail,
            sosFiring = sosFiring,
            isOffline = isOffline,
            silentMode = silentMode,
            onSosClick = { permLauncher.launch(sosPermissions) },
            on112Click = { EmergencyNumberHelper.callEmergency(context) },
            onHistoryClick = { navController.navigate("history") },
            onContactsClick = { navController.navigate("contacts") },
            onTimerClick = { navController.navigate("timer") },
            onFakeCallClick = { navController.navigate("fakecall") },
            onSilentToggle = { silentMode = it },
            onLogoutClick = {
                auth.signOut()
                navController.navigate("login") { popUpTo("home") { inclusive = true } }
            }
        )
    }

    // ── Shake confirmation dialog — cancellable countdown ────────────────────
    if (showShakeConfirm) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss = cancel SOS
                shakeCountdownJob?.cancel()
                showShakeConfirm = false
                shakeCountdown = 5
            },
            title = {
                Text(
                    "Shake detected!",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "SOS will fire automatically in:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    // Big countdown number
                    Text(
                        text = "$shakeCountdown",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (shakeCountdown <= 2)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap CANCEL if this was accidental",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                // Send NOW button — don't wait for countdown
                Button(
                    onClick = {
                        shakeCountdownJob?.cancel()
                        showShakeConfirm = false
                        fireSos()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Send SOS Now") }
            },
            dismissButton = {
                // Cancel — was accidental
                OutlinedButton(
                    onClick = {
                        shakeCountdownJob?.cancel()
                        showShakeConfirm = false
                        shakeCountdown = 5
                    }
                ) { Text("Cancel — Accidental") }
            }
        )
    }

    // ── SOS button confirm dialog ─────────────────────────────────────────────
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { if (!sosFiring) showSosDialog = false },
            title = { Text("Send SOS?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will:\n• SMS all emergency contacts\n" +
                            "• Save your location\n" +
                            (if (!silentMode) "• Auto-call your first contact\n\n" else "\n") +
                            if (silentMode) "Silent mode ON — no call, no sound."
                            else if (isOffline) "Offline — SMS and call still work."
                            else "You are online."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        fireSos()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Send SOS") }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Call Next dialog ──────────────────────────────────────────────────────
    if (showCallNextDialog && callNextIndex < contacts.size) {
        AlertDialog(
            onDismissRequest = { showCallNextDialog = false },
            title = {
                Text("SOS Sent ✅", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Contact 1 was auto-called. Call others?",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    contacts.drop(1).forEach { contact ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name.ifBlank { "(No name)" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(contact.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilledTonalButton(
                                    onClick = { CallHelper.callNow(context, contact.phone) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Call, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Call", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCallNextDialog = false }) { Text("Done") }
            }
        )
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}