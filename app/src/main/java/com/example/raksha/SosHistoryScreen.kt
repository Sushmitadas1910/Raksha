package com.example.raksha

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class SosEvent(
    val id: String = "",
    val timestamp: Long = 0L,
    val readableTime: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val resolved: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var events by remember { mutableStateOf(listOf<SosEvent>()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var contacts by remember { mutableStateOf(listOf<LocalContact>()) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val stop = ContactRepository.listenContacts(context, scope) { contacts = it }
        onDispose { stop() }
    }

    LaunchedEffect(Unit) {
        if (uid == null) { loading = false; errorText = "Not logged in"; return@LaunchedEffect }
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("sos_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snap ->
                events = snap.documents.mapNotNull { d ->
                    try {
                        SosEvent(
                            id           = d.id,
                            // timestamp saved as Long in SosManager
                            timestamp    = d.getLong("timestamp") ?: 0L,
                            readableTime = d.getString("readableTime") ?: "",
                            latitude     = d.getDouble("latitude"),
                            longitude    = d.getDouble("longitude"),
                            resolved     = d.getBoolean("resolved") ?: false
                        )
                    } catch (_: Exception) { null }
                }
                loading = false
            }
            .addOnFailureListener { e ->
                errorText = e.message ?: "Failed to load history"
                loading = false
            }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("SOS History") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize()) {

            // Call contacts panel at top
            if (contacts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Call emergency contacts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Tap to call any contact directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                        Spacer(Modifier.height(10.dp))
                        contacts.forEachIndexed { index, contact ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name.ifBlank { "(No name)" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(contact.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                                Button(
                                    onClick = { CallHelper.callNow(context, contact.phone) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (index == 0)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.secondary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Call", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (index < contacts.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }

            // History list
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                when {
                    loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    errorText != null -> Text(
                        "Error: $errorText",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    events.isEmpty() -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No SOS events yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Your SOS history will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(events, key = { it.id }) { event ->
                            SosEventCard(
                                event = event,
                                uid = uid,
                                onOpenMaps = { link ->
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Cannot open Maps",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onResolved = { resolvedEvent ->
                                    // Update local state immediately — no reload needed
                                    events = events.map {
                                        if (it.id == resolvedEvent.id) it.copy(resolved = true)
                                        else it
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SosEventCard(
    event: SosEvent,
    uid: String?,
    onOpenMaps: (String) -> Unit,
    onResolved: (SosEvent) -> Unit
) {
    val mapsLink = if (event.latitude != null && event.longitude != null)
        "https://maps.google.com/?q=${event.latitude},${event.longitude}"
    else null

    // Format timestamp for display if readableTime is missing
    val displayTime = event.readableTime.ifBlank {
        if (event.timestamp > 0)
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date(event.timestamp))
        else "Unknown time"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row: time + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(displayTime,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (event.resolved)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        if (event.resolved) "Resolved" else "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (event.resolved)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Location
            Text(
                text = if (event.latitude != null && event.longitude != null)
                    "📍 %.5f, %.5f".format(event.latitude, event.longitude)
                else
                    "📍 Location unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Maps link
            if (mapsLink != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Open in Maps →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenMaps(mapsLink) }
                )
            }

            // Mark as Resolved button — only shown when still Active
            if (!event.resolved && uid != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .collection("sos_history").document(event.id)
                            .update("resolved", true)
                            .addOnSuccessListener { onResolved(event) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Mark as Resolved ✓") }
            }
        }
    }
}