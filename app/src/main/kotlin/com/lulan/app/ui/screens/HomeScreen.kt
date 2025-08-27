package com.lulan.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onStart: () -> Unit, onDiscover: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LuLan — LAN-only WebRTC screen streaming", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start Streaming") }
            Button(onClick = onDiscover, modifier = Modifier.fillMaxWidth()) { Text("Discover Devices") }
            Button(onClick = onStats, modifier = Modifier.fillMaxWidth()) { Text("Stats") }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
        }
    }
}
