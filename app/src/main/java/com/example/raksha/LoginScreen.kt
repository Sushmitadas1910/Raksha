package com.example.raksha

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validateEmail(v: String) = when {
        v.isBlank() -> "Email is required"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches() -> "Enter a valid email"
        else -> null
    }

    fun validatePassword(v: String) = when {
        v.isBlank() -> "Password is required"
        v.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }

    fun attemptLogin() {
        focusManager.clearFocus()
        val eErr = validateEmail(email)
        val pErr = validatePassword(password)
        emailError = eErr; passwordError = pErr
        if (eErr != null || pErr != null) return
        isLoading = true
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                isLoading = false
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                val msg = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found for this email"
                    is FirebaseAuthInvalidCredentialsException -> "Incorrect password"
                    else -> "Login failed. Please try again"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Raksha",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Your personal safety companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; if (emailError != null) emailError = validateEmail(it) },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; if (passwordError != null) passwordError = validatePassword(it) },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide" else "Show"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { attemptLogin() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = {
                    val trimmed = email.trim()
                    if (trimmed.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                        Toast.makeText(context, "Enter your email above first", Toast.LENGTH_SHORT).show()
                    } else {
                        auth.sendPasswordResetEmail(trimmed)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Reset link sent to $trimmed", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Could not send reset email", Toast.LENGTH_SHORT).show()
                            }
                    }
                }) { Text("Forgot password?") }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { attemptLogin() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Login", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { navController.navigate("register") }) {
                    Text("Register", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}