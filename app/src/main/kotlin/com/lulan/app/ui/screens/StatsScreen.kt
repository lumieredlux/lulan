package com.lulan.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lulan.app.webrtc.WebRtcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class StatsViewModel @Inject constructor(private val webrtc: WebRtcManager): ViewModel() {
    var text by mutableStateOf("No stats")
        private set
    fun start() {
        viewModelScope.launch {
            webrtc.stats.collectLatest {
                text = "FPS: ${it.fps ?: "-"}  Bitrate: ${it.bitrateKbps ?: "-"} kbps  Lost: ${it.packetsLost ?: "-"}"
            }
        }
    }
}

@Composable
fun StatsScreen(vm: StatsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.start() }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Live Stats", style = MaterialTheme.typography.titleLarge)
        Text(vm.text, style = MaterialTheme.typography.bodyLarge)
    }
}
