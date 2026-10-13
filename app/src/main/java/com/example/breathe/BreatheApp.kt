package com.example.breathe

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

private enum class Screen { Setup, Session, Done }

/** Share of each breath spent breathing in; the longer exhale is what calms you down. */
private const val INHALE_SHARE = 0.45f

@Composable
fun BreatheApp() {
    MaterialTheme {
        val context = LocalContext.current
        val haptics = remember { Haptics(context) }
        var screen by remember { mutableStateOf(Screen.Setup) }
        val prefs = remember { context.getSharedPreferences("breathe", Context.MODE_PRIVATE) }
        var minutes by remember { mutableIntStateOf(prefs.getInt("minutes", 2)) }
        var breathsPerMinute by remember { mutableIntStateOf(prefs.getInt("bpm", 7)) }
        var strength by remember { mutableIntStateOf(prefs.getInt("strength", 2)) }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (screen) {
                Screen.Setup -> SetupScreen(
                    minutes = minutes,
                    breathsPerMinute = breathsPerMinute,
                    onMinutes = { minutes = it.coerceIn(1, 10) },
                    onBreaths = { breathsPerMinute = it.coerceIn(4, 10) },
                    strength = strength,
                    onStrength = { strength = it.coerceIn(1, 3) },
                    onStart = {
                        prefs.edit()
                            .putInt("minutes", minutes)
                            .putInt("bpm", breathsPerMinute)
                            .putInt("strength", strength)
                            .apply()
                        haptics.strength = strength
                        screen = Screen.Session
                    }
                )
                Screen.Session -> SessionScreen(
                    minutes = minutes,
                    breathsPerMinute = breathsPerMinute,
                    haptics = haptics,
                    onFinished = { screen = Screen.Done },
                    onExit = { screen = Screen.Setup }
                )
                Screen.Done -> DoneScreen(onAgain = { screen = Screen.Setup })
            }
        }
    }
}

@Composable
private fun SessionScreen(
    minutes: Int,
    breathsPerMinute: Int,
    haptics: Haptics,
    onFinished: () -> Unit,
    onExit: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    BackHandler {
        haptics.cancel()
        onExit()
    }

    // One loop drives both the animation and the haptics, so they can't drift apart.
    LaunchedEffect(Unit) {
        val cycleMs = 60_000L / breathsPerMinute
        val inhaleMs = (cycleMs * INHALE_SHARE).toLong()
        val exhaleMs = cycleMs - inhaleMs
        val ease = CubicBezierEasing(0.37f, 0f, 0.63f, 1f) // sine in-out
        var completed = false
        try {
            delay(800) // a beat to settle before the first breath
            repeat(minutes * breathsPerMinute) {
                haptics.inhale(inhaleMs)
                progress.animateTo(1f, tween(inhaleMs.toInt(), easing = ease))
                haptics.exhale(exhaleMs)
                progress.animateTo(0f, tween(exhaleMs.toInt(), easing = ease))
            }
            completed = true
        } finally {
            if (!completed) haptics.cancel()
        }
        haptics.done()
        onFinished()
    }

    BloomCanvas(progress = { progress.value })
}

@Composable
private fun SetupScreen(
    minutes: Int,
    breathsPerMinute: Int,
    onMinutes: (Int) -> Unit,
    onBreaths: (Int) -> Unit,
    strength: Int,
    onStrength: (Int) -> Unit,
    onStart: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { Text("Breathe", style = MaterialTheme.typography.title3) }
        item {
            SettingRow(
                label = "minutes",
                value = minutes.toString(),
                onMinus = { onMinutes(minutes - 1) },
                onPlus = { onMinutes(minutes + 1) }
            )
        }
        item {
            SettingRow(
                label = "breaths/min",
                value = breathsPerMinute.toString(),
                onMinus = { onBreaths(breathsPerMinute - 1) },
                onPlus = { onBreaths(breathsPerMinute + 1) }
            )
        }
        item {
            SettingRow(
                label = "haptics",
                value = when (strength) { 1 -> "Soft"; 3 -> "Firm"; else -> "Med" },
                onMinus = { onStrength(strength - 1) },
                onPlus = { onStrength(strength + 1) }
            )
        }
        item {
            Chip(
                onClick = onStart,
                label = { Text("Start", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onMinus, modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize)) { Text("−") }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
            Text(value, style = MaterialTheme.typography.title2)
            Text(label, style = MaterialTheme.typography.caption2)
        }
        Button(onClick = onPlus, modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize)) { Text("+") }
    }
}

@Composable
private fun DoneScreen(onAgain: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Nicely done", style = MaterialTheme.typography.title2)
        Chip(
            onClick = onAgain,
            label = { Text("Again", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            colors = ChipDefaults.primaryChipColors(),
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
