package com.example.raksha

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Handles direct calls to emergency services.
 * Works entirely over GSM — no internet required.
 * 112 is the universal emergency number in India (and all of Europe).
 * It routes to police, ambulance, or fire depending on the operator.
 */
object EmergencyNumberHelper {

    private const val EMERGENCY_NUMBER = "112"

    /**
     * Directly dials 112.
     * Requires CALL_PHONE permission — already declared in your manifest.
     * Falls back to ACTION_DIAL (opens dialer pre-filled) if permission missing,
     * so the user can still call even without the permission.
     */
    fun callEmergency(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$EMERGENCY_NUMBER")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Hard fallback — open dialer
                dialFallback(context)
            }
        } else {
            // No CALL_PHONE permission — open dialer pre-filled with 112
            // User just has to tap the green call button
            dialFallback(context)
            Toast.makeText(
                context,
                "Tap call to dial 112",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun dialFallback(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$EMERGENCY_NUMBER")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open dialer", Toast.LENGTH_SHORT).show()
        }
    }
}