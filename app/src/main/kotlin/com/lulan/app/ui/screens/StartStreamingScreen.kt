package com.lulan.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lulan.app.R
import com.lulan.app.ScreenProjectionPermissionStore
import com.lulan.app.service.StreamService
import com.lulan.app.service.StreamService.Companion.MediaProjectionHolder
import com.lulan.app.util.Permissions

@Composable
fun StartStreamingScreen(onRequestProjection: () -> Unit) {
    val ctx = LocalContext.current as Activity
    var width by remember { mutableStateOf(1280) }
    var height by remember { mutableStateOf(720) }
    var fps by remember { mutableStateOf(30) }
    var pin by remember { mutableStateOf("0000") }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        handleProjectionResult = { resultCode, data ->
            if (resultCode == Activity.RESULT_OK && data != null) {
                val mgr = ctx.getSystemService(MediaProjectionManager::class.java)
                MediaProjectionHolder.projection = mgr.getMediaProjection(resultCode, data)
                startService()
            }
        }
    }

    fun startService() {
        Permissions.requestIgnoreBatteryOptimizations(ctx)
        val i = Intent(ctx, StreamService::class.java).apply {
            putExtra("width", width)
            putExtra("height", height)
            putExtra("fps", fps)
            putExtra("pin", pin)
        }
        ctx.startForegroundService(i)
        started = true
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Start Streaming", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = width.toString(), onValueChange = { width = it.toIntOrNull() ?: 1280 }, label = { Text("Width") })
            OutlinedTextField(value = height.toString(), onValueChange = { height = it.toIntOrNull() ?: 720 }, label = { Text("Height") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = fps.toString(), onValueChange = { fps = it.toIntOrNull() ?: 30 }, label = { Text("FPS") })
            OutlinedTextField(value = pin, onValueChange = { pin = it.take(6) }, label = { Text("PIN") })
        }

        Button(
            onClick = { onRequestProjection() },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "start_streaming_button" }
        ) { Text(if (started) "Restart" else "Start") }

        if (started) {
            Text("Streaming active. Open the viewer page advertised via NSD or at http://<device-ip>:7575/  PIN: $pin")
        }
    }
}

// This is called from NavGraph to route the Activity Result back.
var handleProjectionResult: (Int, android.content.Intent?) -> Unit = { _, _ -> }
fun handleProjectionResult(resultCode: Int, data: android.content.Intent?) = handleProjectionResult(resultCode, data)
