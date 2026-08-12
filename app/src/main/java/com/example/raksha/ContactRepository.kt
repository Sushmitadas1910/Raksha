package com.example.raksha

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single source of truth for emergency contacts.
 *
 * READ path (offline-first):
 *   1. Immediately return Room cache (works with zero internet)
 *   2. When Firestore snapshot arrives, update Room + notify caller
 *
 * WRITE path:
 *   1. Write to Firestore (Firestore offline persistence queues it if offline)
 *   2. Firestore snapshot listener then updates Room automatically
 *
 * This means SOS can always read contacts from Room even with no internet.
 */
object ContactRepository {

    /**
     * Start a real-time listener.
     * [onUpdate] is called immediately with the Room cache, then again
     * whenever Firestore delivers new data (online or from its own offline cache).
     *
     * Returns a lambda you must call to stop the listener (call it from
     * DisposableEffect's onDispose).
     */
    fun listenContacts(
        context: Context,
        scope: CoroutineScope,
        onUpdate: (List<LocalContact>) -> Unit
    ): () -> Unit {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dao = RakshaDatabase.get(context).contactDao()

        // Step 1: emit Room cache immediately (no network needed)
        scope.launch(Dispatchers.IO) {
            val cached = dao.getAll()
            withContext(Dispatchers.Main) { onUpdate(cached) }
        }

        if (uid == null) return {}

        // Step 2: register Firestore real-time listener
        val registration = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("emergency_contacts")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val remoteContacts = snap.documents.map { d ->
                    LocalContact(
                        firestoreId = d.id,
                        name = d.getString("name") ?: "",
                        phone = d.getString("phone") ?: ""
                    )
                }

                // Update Room on IO thread, then notify UI
                scope.launch(Dispatchers.IO) {
                    dao.upsertAll(remoteContacts)
                    // Also remove any local contacts that were deleted remotely
                    val remoteIds = remoteContacts.map { it.firestoreId }.toSet()
                    dao.getAll()
                        .filter { it.firestoreId !in remoteIds }
                        .forEach { dao.deleteById(it.firestoreId) }

                    withContext(Dispatchers.Main) { onUpdate(remoteContacts) }
                }
            }

        return { registration.remove() }
    }

    /**
     * Add a contact. Writes to Firestore.
     * If offline, Firestore queues the write and syncs when back online.
     */
    fun addContact(
        context: Context,
        name: String,
        phone: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onFailure("Not logged in"); return
        }

        val data = hashMapOf("name" to name, "phone" to phone)

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("emergency_contacts")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Save failed") }
    }

    /**
     * Delete a contact by Firestore document ID.
     * Also removes from Room immediately for instant offline UI update.
     */
    fun deleteContact(
        context: Context,
        scope: CoroutineScope,
        firestoreId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onFailure("Not logged in"); return
        }

        // Remove from Room immediately for instant UI feedback
        scope.launch(Dispatchers.IO) {
            RakshaDatabase.get(context).contactDao().deleteById(firestoreId)
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("emergency_contacts")
            .document(firestoreId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Delete failed") }
    }

    /**
     * Get contacts synchronously from Room only.
     * Used by SosManager during SOS trigger — must work offline.
     */
    fun getContactsOffline(context: Context): List<LocalContact> {
        return RakshaDatabase.get(context).contactDao().getAll()
    }

    /** Call on logout — wipe local cache. */
    fun clearLocalCache(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            RakshaDatabase.get(context).contactDao().deleteAll()
        }
    }
}