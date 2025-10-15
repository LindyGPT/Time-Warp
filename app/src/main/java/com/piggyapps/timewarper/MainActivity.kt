package com.piggyapps.timewarper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.piggyapps.timewarper.ui.theme.TimeWarperTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val displayedTimeMillis = MutableStateFlow(System.currentTimeMillis())
    private var lastRealtimeWhenPaused = SystemClock.elapsedRealtime()
    private var timeWarpFactor = 2F
    private var isPaused = false

    override fun onResume() {
        super.onResume()
        isPaused = false
        val realtimeElapsedDuringPause = SystemClock.elapsedRealtime() - lastRealtimeWhenPaused
        val timeWarpFactor = getCurrentTimeWarpFactor()
        val timeToAdd = (realtimeElapsedDuringPause * timeWarpFactor).toLong()
        displayedTimeMillis.value += timeToAdd
    }

    private fun getCurrentTimeWarpFactor(): Float {
        val deadZone = 1.5
        var randomFactor: Float

        val rangeIsInsideDeadZone = timeWarpFactor <= deadZone
        if (rangeIsInsideDeadZone) {
            randomFactor = Random.nextFloat() * (timeWarpFactor - 1f) + 1f
        } else {
            do {
                randomFactor = Random.nextFloat() * (timeWarpFactor - 1f) + 1f
            } while (randomFactor < deadZone)
        }

        return if (Random.nextBoolean()) randomFactor else 1 / randomFactor
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        lastRealtimeWhenPaused = SystemClock.elapsedRealtime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            TimeWarperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    val currentDisplayedTime by displayedTimeMillis.collectAsState()
                    var showDialog by remember { mutableStateOf(false) }

                    CurrentTimeDisplay(
                        displayedTimeMillis = currentDisplayedTime,
                        onLongPress = { showDialog = true },
                    )


                    TimeWarpFactorDialog(
                        showDialog = showDialog,
                        onDismiss = { showDialog = false },
                        currentTimeWarpFactor = timeWarpFactor,
                        onTimeWarpFactorChange = { timeWarpFactor = it },
                    )
                }



                LaunchedEffect(Unit) {
                    while (true) {
                        delay(10L)
                        if (!isPaused) updateDisplayedTime()
                    }
                }
            }
        }
    }


    private fun updateDisplayedTime() {
        displayedTimeMillis.value += 10L;
    }
}

@Composable
fun CurrentTimeDisplay(
    modifier: Modifier = Modifier, displayedTimeMillis: Long, onLongPress: () -> Unit = {}
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(displayedTimeMillis) {
        val fakedTime = LocalDateTime.ofEpochSecond(
            displayedTimeMillis / 1000, 0, java.time.ZoneOffset.ofHours(2)
        )
        currentTime = fakedTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress()
                    })
            }, contentAlignment = Alignment.Center
    ) {
        Text(
            text = currentTime, style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 60.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun TimeWarpFactorDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    currentTimeWarpFactor: Float,
    onTimeWarpFactorChange: (Float) -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Time warp") },
            text = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "x${"%.1f".format(currentTimeWarpFactor)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                    )
                    Slider(
                        value = currentTimeWarpFactor, onValueChange = { sliderValue ->
                            onTimeWarpFactorChange(sliderValue)
                        }, valueRange = 1f..10f
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            },
        )
    }
}