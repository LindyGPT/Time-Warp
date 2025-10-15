package com.piggyapps.timewarper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
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
    private var lastRealtimeWhenPaused: Long = SystemClock.elapsedRealtime()
    private var timeWarpUpperFactor: Float = 2.5F
    private var timeWarpMinFactor: Float = 0.4F
    private var isPaused: Boolean = false

    override fun onResume() {
        super.onResume()
        isPaused = false
        val realtimeElapsedDuringPause = SystemClock.elapsedRealtime() - lastRealtimeWhenPaused
        val timeWarpFactor =
            Random.nextFloat() * (timeWarpUpperFactor - timeWarpMinFactor) + timeWarpMinFactor
        val timeToAdd = (realtimeElapsedDuringPause * timeWarpFactor).toLong()
        displayedTimeMillis.value += timeToAdd
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
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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
                        currentTimeWarpUpperFactor = timeWarpUpperFactor,
                        onTimeWarpUpperFactorChange = { timeWarpUpperFactor = it },
                        currentTimeWarpMinFactor = timeWarpMinFactor,
                        onTimeWarpMinFactorChange = { timeWarpMinFactor = it },
                    )
                }



                LaunchedEffect(Unit) {
                    while (true) {
                        delay(10L)
                        if (!isPaused)
                            updateDisplayedTime()
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
    modifier: Modifier = Modifier,
    displayedTimeMillis: Long,
    onLongPress: () -> Unit = {}
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(displayedTimeMillis) {
        val fakedTime = LocalDateTime.ofEpochSecond(
            displayedTimeMillis / 1000,
            0,
            java.time.ZoneOffset.ofHours(2)
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
                    }
                )
            }, contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = currentTime, label = "time_animation") {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 60.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun TimeWarpFactorDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    currentTimeWarpUpperFactor: Float,
    onTimeWarpUpperFactorChange: (Float) -> Unit,
    currentTimeWarpMinFactor: Float,
    onTimeWarpMinFactorChange: (Float) -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set Time Warp Factor") },
            text = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Max factor: x${"%.1f".format(currentTimeWarpUpperFactor)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                    )
                    Slider(
                        value = mapWarpToSlider(currentTimeWarpUpperFactor),
                        onValueChange = { sliderValue ->
                            onTimeWarpUpperFactorChange(mapSliderToWarp(sliderValue))
                        },
                        valueRange = 0f..1f
                    )

                    Text(
                        text = "Min factor: x${"%.2f".format(currentTimeWarpMinFactor)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                    )
                    Slider(
                        value = mapWarpToSlider(currentTimeWarpMinFactor),
                        onValueChange = { sliderValue ->
                            onTimeWarpMinFactorChange(mapSliderToWarp(sliderValue))
                        },
                        valueRange = 0f..1f
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text("Reset")
                }
            }
        )
    }
}


private fun mapSliderToWarp(sliderValue: Float): Float {
    return if (sliderValue <= 0.5f) {
        0.1f + (sliderValue * 2 * 0.9f)
    } else {
        1f + ((sliderValue - 0.5f) * 2 * 9f)
    }
}

private fun mapWarpToSlider(warpValue: Float): Float {
    return if (warpValue <= 1f) {
        (warpValue - 0.1f) / 0.9f / 2f
    } else {
        0.5f + ((warpValue - 1f) / 9f / 2f)
    }
}
