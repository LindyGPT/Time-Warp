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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

private val millisecondsWhenFirstLaunched = System.currentTimeMillis()

class MainActivity : ComponentActivity() {
    private val displayedTimeMillis = MutableStateFlow(System.currentTimeMillis())
    private var lastRealtimeWhenPaused = SystemClock.elapsedRealtime()
    private var timeWarpFactor = 2F
    private var isPaused = false

    override fun onResume() {
        super.onResume()
        isPaused = false
        val realtimeElapsedDuringPause = SystemClock.elapsedRealtime() - lastRealtimeWhenPaused
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
                        delay(100L)
                        if (!isPaused)
                            displayedTimeMillis.value += (100L * timeWarpFactor).toLong()
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentTimeDisplay(
    displayedTimeMillis: Long, onLongPress: () -> Unit = {}
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(displayedTimeMillis) {
        val fakedTime = LocalDateTime.ofEpochSecond(
            displayedTimeMillis / 1000, 0, java.time.ZoneOffset.ofHours(2)
        )
        currentTime = fakedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress()
                    })
            }, contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTime, style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 60.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            RealTimeDots()
        }

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
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        value = mapWarpToSlider(currentTimeWarpFactor),
                        onValueChange = { sliderValue ->
                            onTimeWarpFactorChange(mapSliderToWarp(sliderValue))
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

@Composable
fun RealTimeDots() {
    var filledDots by remember { mutableIntStateOf(0) }
    val totalDots = 12

    LaunchedEffect(Unit) {
        while (filledDots < totalDots) {
            val timePassed = System.currentTimeMillis() - millisecondsWhenFirstLaunched
            delay(100L)
            filledDots = ((timePassed / (15 * 60 * 1000)) % (totalDots + 1)).toInt()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalDots) { index ->
            val filled = index < filledDots
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (filled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}