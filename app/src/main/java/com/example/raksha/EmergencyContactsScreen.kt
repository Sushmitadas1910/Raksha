package com.example.raksha

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    if (uid == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
        return
    }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(listOf<LocalContact>()) }
    var loading by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf<LocalContact?>(null) }

    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val stop = ContactRepository.listenContacts(context, scope) { updated ->
            contacts = updated
            loading = false
        }
        onDispose { stop() }
    }

    val maxLimit = 5
    val canAddMore = contacts.size < maxLimit

    fun cleanPhone(raw: String) = raw.trim()
        .replace(" ", "").replace("-", "")
        .replace("(", "").replace(")", "")

    fun isValidPhone(p: String) = cleanPhone(p).matches(Regex("^\\+?\\d{10,15}$"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Contacts") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
        ) {
            Text(
                text = "Saved: ${contacts.size} / $maxLimit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Contact Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canAddMore
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (e.g. +91XXXXXXXXXX)") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canAddMore
            )

            Spacer(Modifier.height(12.dp))

            val saveEnabled = canAddMore
                    && name.trim().isNotEmpty()
                    && phone.trim().isNotEmpty()
                    && isValidPhone(phone)

            Button(
                onClick = {
                    val cleaned = cleanPhone(phone)
                    if (contacts.any { cleanPhone(it.phone) == cleaned }) {
                        Toast.makeText(context, "Phone already saved", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    ContactRepository.addContact(
                        context = context,
                        name = name.trim(),
                        phone = cleaned,
                        onSuccess = {
                            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                            name = ""; phone = ""
                        },
                        onFailure = { err ->
                            Toast.makeText(context, "Save failed: $err", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveEnabled
            ) {
                Text(if (canAddMore) "Save Contact" else "Limit Reached (5)")
            }

            Spacer(Modifier.height(16.dp))

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(contacts, key = { it.firestoreId }) { c ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                text = c.name.ifBlank { "(No name)" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = c.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { confirmDelete = c }) {
                                Icon(Icons.Default.Delete, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete contact?") },
            text = { Text("This contact will be removed permanently.") },
            confirmButton = {
                Button(onClick = {
                    val toDelete = confirmDelete ?: return@Button
                    ContactRepository.deleteContact(
                        context = context,
                        scope = scope,
                        firestoreId = toDelete.firestoreId,
                        onSuccess = {
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { err ->
                            Toast.makeText(context, "Delete failed: $err", Toast.LENGTH_LONG).show()
                        }
                    )
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            }
        )
    }
}