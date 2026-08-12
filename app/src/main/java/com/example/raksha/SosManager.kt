package com.example.raksha

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object SosManager {

    fun trigger(
        context: Context,
        silent: Boolean = false,
        onDone: (Boolean) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            if (!silent) Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
            onDone(false)
            return
        }

        if (!silent) {
            vibrate(context)
            Toast.makeText(context, "SOS triggered — sending alerts…", Toast.LENGTH_LONG).show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            val contacts = ContactRepository.getContactsOffline(context)

            if (contacts.isEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    if (!silent) Toast.makeText(
                        context, "No emergency contacts saved!", Toast.LENGTH_LONG
                    ).show()
                    onDone(false)
                }
                return@launch
            }

            getLocation(context) { location ->
                val lat = location?.latitude
                val lng = location?.longitude
                val mapsLink = if (lat != null && lng != null)
                    "https://maps.google.com/?q=$lat,$lng"
                else
                    "Location unavailable"

                // readableTime for display in history screen
                val readableTime = SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a", Locale.getDefault()
                ).format(Date())

                val message = "EMERGENCY ALERT!\n" +
                        "I need help. Please contact me immediately.\n" +
                        "Time: $readableTime\n" +
                        "My location: $mapsLink"

                sendSms(context, contacts.map { it.phone }, message)

                if (!silent && contacts.isNotEmpty()) {
                    CallHelper.callNow(context, contacts[0].phone)
                }

                // Save both Long timestamp AND readable string — fixes history loading
                saveToFirestore(uid, lat, lng, readableTime)

                CoroutineScope(Dispatchers.Main).launch {
                    onDone(true)
                }
            }
        }
    }

    private fun getLocation(context: Context, callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { callback(null); return }

        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { callback(it) }
            .addOnFailureListener {
                client.lastLocation
                    .addOnSuccessListener { callback(it) }
                    .addOnFailureListener { callback(null) }
            }
    }

    private fun sendSms(context: Context, phones: List<String>, message: String) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        @Suppress("DEPRECATION")
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else
            SmsManager.getDefault()

        phones.forEach { phone ->
            try {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun saveToFirestore(uid: String, lat: Double?, lng: Double?, readableTime: String) {
        val data = hashMapOf(
            "timestamp"    to System.currentTimeMillis(),  // Long — for orderBy in history
            "readableTime" to readableTime,                // String — for display
            "latitude"     to (lat ?: 0.0),
            "longitude"    to (lng ?: 0.0),
            "resolved"     to false
        )
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("sos_history")
            .add(data)
    }

    private fun vibrate(context: Context) {
        try {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 300, 100, 300, 100, 600), -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 300, 100, 300, 100, 600), -1)
            }
        } catch (_: Exception) {}
    }
}