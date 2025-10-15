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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.piggyapps.timewarper.ui.theme.TimeWarperTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import android.os.SystemClock
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val displayedTimeMillis = MutableStateFlow(System.currentTimeMillis())
    private var lastRealtimeWhenPaused: Long = SystemClock.elapsedRealtime()
    private var timeWarpFactor: Double = 10.0

    override fun onResume() {
        super.onResume()
        val realtimeElapsedDuringPause = SystemClock.elapsedRealtime() - lastRealtimeWhenPaused
        displayedTimeMillis.value += (realtimeElapsedDuringPause * timeWarpFactor).toLong()
    }

    override fun onPause() {
        super.onPause()
        lastRealtimeWhenPaused = SystemClock.elapsedRealtime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            TimeWarperTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val currentDisplayedTime by displayedTimeMillis.collectAsState()
                    CurrentTimeDisplay(displayedTimeMillis = currentDisplayedTime)
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        updateDisplayedTime()
                        delay(10L)
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
fun CurrentTimeDisplay(modifier: Modifier = Modifier, displayedTimeMillis: Long) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(displayedTimeMillis) {
        val fakedTime = LocalDateTime.ofEpochSecond(displayedTimeMillis / 1000, 0, java.time.ZoneOffset.ofHours(2))
        currentTime = fakedTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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


