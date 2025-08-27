package com.lulan.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lulan.app.signaling.NsdHelper
import org.koin.mp.KoinPlatform
import kotlinx.coroutines.flow.collectLatest
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val nsd: NsdHelper
) : ViewModel() {
    var devices by mutableStateOf(listOf<NsdHelper.Discovered>())
        private set

    fun start() {
        viewModelScope.launch {
            nsd.discover().collectLatest { d ->
                devices = (devices + d).distinctBy { it.name + it.host + it.port }
            }
        }
    }
}

@Composable
fun DiscoverScreen(vm: DiscoverViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.start() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Discovered LuLan devices on LAN", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(vm.devices.size) { idx ->
                val d = vm.devices[idx]
                ListItem(
                    headlineContent = { Text(d.name) },
                    supportingContent = { Text("${d.host}:${d.port}") },
                    modifier = Modifier.clickable {
                        // Open viewer in external browser
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://${d.host}:${d.port}/")))
                    }
                )
                Divider()
            }
        }
    }
}
