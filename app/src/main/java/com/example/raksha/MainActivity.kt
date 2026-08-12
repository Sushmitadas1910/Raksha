package com.example.raksha

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.raksha.ui.theme.RakshaTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        @Suppress("DEPRECATION")
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()

        // Fix: API level check for startForegroundService
        val serviceIntent = Intent(this, VolumeButtonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        enableEdgeToEdge()

        setContent {
            RakshaTheme {
                val navController = rememberNavController()
                val startDestination =
                    if (FirebaseAuth.getInstance().currentUser != null) "home" else "login"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("login")    { LoginScreen(navController) }
                    composable("register") { RegisterScreen(navController) }
                    composable("home")     { HomeScreen(navController) }
                    composable("history")  { SosHistoryScreen(navController) }
                    composable("contacts") { EmergencyContactsScreen(navController) }
                    composable("timer")    { SafetyTimerScreen(navController) }
                    composable("fakecall") { FakeCallScreen(navController) }
                }
            }
        }
    }

    // Fix: onKeyDown instead of dispatchKeyEvent (no library group conflict)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Intent(this, VolumeButtonService::class.java).also {
                it.putExtra("action", "VOLUME_PRESS")
                startService(it)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}