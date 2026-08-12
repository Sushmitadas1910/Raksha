package com.example.raksha

import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

enum class FakeCallState { RINGING, ACTIVE, ENDED }

@Composable
fun FakeCallScreen(navController: NavController) {
    val context = LocalContext.current
    var callState by remember { mutableStateOf(FakeCallState.RINGING) }
    var callDuration by remember { mutableStateOf(0) }
    var sosTriggered by remember { mutableStateOf(false) }
    var sosFiring by remember { mutableStateOf(false) }

    // ── Ringtone player ───────────────────────────────────────────────────────
    val mediaPlayer = remember {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_RING)
                setDataSource(context, uri)
                isLooping = true
                prepare()
            }
        } catch (e: Exception) {
            null
        }
    }

    // Start ringing when screen opens, stop when answered or declined
    LaunchedEffect(callState) {
        when (callState) {
            FakeCallState.RINGING -> {
                try { mediaPlayer?.start() } catch (_: Exception) {}
            }
            else -> {
                try { mediaPlayer?.stop() } catch (_: Exception) {}
            }
        }
    }

    // Stop and release player when screen closes
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    // Call timer
    LaunchedEffect(callState) {
        if (callState != FakeCallState.ACTIVE) return@LaunchedEffect
        while (callState == FakeCallState.ACTIVE) {
            delay(1000L); callDuration++
        }
    }

    // Auto-answer after 3.5 seconds
    LaunchedEffect(Unit) {
        delay(3500L)
        if (callState == FakeCallState.RINGING) callState = FakeCallState.ACTIVE
    }

    val mins = callDuration / 60
    val secs = callDuration % 60

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // Avatar with pulse when ringing
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(if (callState == FakeCallState.RINGING) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(Color(0xFF6C63FF)),
                contentAlignment = Alignment.Center
            ) {
                Text("M", fontSize = 58.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            Text("Maa", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(Modifier.height(8.dp))

            Text(
                text = when (callState) {
                    FakeCallState.RINGING -> "Incoming call…"
                    FakeCallState.ACTIVE -> "%02d:%02d".format(mins, secs)
                    FakeCallState.ENDED -> "Call ended"
                },
                fontSize = 16.sp,
                color = Color(0xFFAAAAAA)
            )

            Spacer(Modifier.weight(1f))

            // Status hints
            when {
                sosTriggered -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x4400C853)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅", fontSize = 20.sp)
                            Text(
                                "SOS sent silently to all your emergency contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF80FF9F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                callState == FakeCallState.ACTIVE && !sosFiring -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔇", fontSize = 16.sp)
                            Text(
                                "Tap the mute button below to silently alert your contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCCCCCC)
                            )
                        }
                    }
                }
                sosFiring -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FF6600)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFFF9966)
                            )
                            Text(
                                "Sending SOS…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9966)
                            )
                        }
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(32.dp))

            // Buttons
            when (callState) {
                FakeCallState.RINGING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FakeCallBtn(
                            icon = Icons.Default.CallEnd,
                            label = "Decline",
                            color = Color(0xFFE53935),
                            onClick = { navController.popBackStack() }
                        )
                        FakeCallBtn(
                            icon = Icons.Default.Call,
                            label = "Accept",
                            color = Color(0xFF43A047),
                            onClick = { callState = FakeCallState.ACTIVE }
                        )
                    }
                }
                FakeCallState.ACTIVE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FakeCallBtn(
                            icon = Icons.Default.CallEnd,
                            label = "End",
                            color = Color(0xFFE53935),
                            onClick = { navController.popBackStack() }
                        )
                        // Looks like mute — secretly sends SOS
                        FakeCallBtn(
                            icon = Icons.Default.VolumeOff,
                            label = if (sosTriggered) "Sent ✓" else "Mute",
                            color = if (sosTriggered) Color(0xFF444444) else Color(0xFF555555),
                            enabled = !sosTriggered && !sosFiring,
                            onClick = {
                                if (!sosTriggered && !sosFiring) {
                                    sosFiring = true
                                    SosManager.trigger(context, silent = true) {
                                        sosFiring = false
                                        sosTriggered = true
                                    }
                                }
                            }
                        )
                    }
                }
                FakeCallState.ENDED -> {}
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun FakeCallBtn(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(if (enabled) color else Color(0xFF333333))
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFFAAAAAA), textAlign = TextAlign.Center)
    }
}