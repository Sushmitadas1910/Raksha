package com.example.raksha

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyTimerScreen(navController: NavController) {
    val context = LocalContext.current

    val presetOptions = listOf(1, 5, 10, 15, 30)
    var selectedMinutes by remember { mutableStateOf(5) }
    var customInput by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    var customError by remember { mutableStateOf<String?>(null) }

    var totalSeconds by remember { mutableStateOf(300) }
    var secondsLeft by remember { mutableStateOf(300) }
    var isRunning by remember { mutableStateOf(false) }
    var sosTriggered by remember { mutableStateOf(false) }

    fun applyMinutes(mins: Int) {
        selectedMinutes = mins
        secondsLeft = mins * 60
        totalSeconds = mins * 60
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (secondsLeft > 0 && isRunning) {
            delay(1000L)
            secondsLeft--
        }
        if (secondsLeft == 0 && isRunning) {
            isRunning = false
            sosTriggered = true
            SosManager.trigger(context, silent = true) {}
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0)
            secondsLeft.toFloat() / totalSeconds.toFloat() else 0f,
        animationSpec = tween(800),
        label = "timer_progress"
    )

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val isWarning = isRunning && secondsLeft <= 30

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Timer") },
                navigationIcon = {
                    IconButton(onClick = { isRunning = false; navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        if (sosTriggered) {
            Box(
                modifier = Modifier.padding(pad).fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("🚨", fontSize = 64.sp)
                        Text("SOS Triggered",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            "Timer expired. Emergency contacts have been alerted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { sosTriggered = false; applyMinutes(selectedMinutes) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Reset Timer") }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("⏱️", fontSize = 28.sp)
                            Column {
                                Text("How it works",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Set a timer before walking alone. If you don't cancel before zero, SOS is automatically sent to all your emergency contacts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    if (!isRunning) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Select duration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetOptions.forEach { mins ->
                                    FilterChip(
                                        selected = selectedMinutes == mins && !showCustomInput,
                                        onClick = {
                                            showCustomInput = false
                                            customError = null
                                            applyMinutes(mins)
                                        },
                                        label = { Text("${mins}m",
                                            textAlign = TextAlign.Center) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                FilterChip(
                                    selected = showCustomInput,
                                    onClick = { showCustomInput = !showCustomInput },
                                    label = { Text("Custom") },
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            if (showCustomInput) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customInput,
                                        onValueChange = { input ->
                                            if (input.all { it.isDigit() } && input.length <= 3) {
                                                customInput = input
                                                customError = null
                                            }
                                        },
                                        label = { Text("Enter minutes (1–180)") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number),
                                        isError = customError != null,
                                        supportingText = customError?.let { { Text(it) } },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            Text("min",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 12.dp))
                                        }
                                    )
                                    Button(
                                        onClick = {
                                            val mins = customInput.toIntOrNull()
                                            when {
                                                mins == null || mins <= 0 ->
                                                    customError = "Enter a valid number"
                                                mins > 180 ->
                                                    customError = "Maximum is 180 minutes"
                                                else -> {
                                                    customError = null
                                                    applyMinutes(mins)
                                                    showCustomInput = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("Set ${customInput.ifBlank { "?" }} minutes") }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "Timer set for: $selectedMinutes minute${if (selectedMinutes != 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        Text("Timer running — $selectedMinutes min set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Timer circle
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 14.dp,
                        color = if (isWarning) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "%02d:%02d".format(minutes, seconds),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWarning) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (isRunning) {
                                if (isWarning) "SOS in ${secondsLeft}s!" else "Tap to cancel"
                            } else "ready",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isWarning) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bottom buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isWarning) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "⚠️ SOS fires in $secondsLeft seconds — tap Cancel if you're safe!",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (!isRunning) {
                        Button(
                            onClick = {
                                totalSeconds = selectedMinutes * 60
                                secondsLeft = selectedMinutes * 60
                                isRunning = true
                            },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            elevation = ButtonDefaults.buttonElevation(6.dp)
                        ) {
                            Text("Start Safety Timer — $selectedMinutes min",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { isRunning = false; applyMinutes(selectedMinutes) },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error),
                            elevation = ButtonDefaults.buttonElevation(6.dp)
                        ) {
                            Text("✓  Cancel — I'm Safe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}