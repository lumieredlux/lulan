package com.lulan.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lulan.app.util.LanOnlyFilter
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(private val filter: LanOnlyFilter): ViewModel() {
    var lanOnly by mutableStateOf(true)
        private set
    init {
        viewModelScope.launch {
            filter.lanOnly.collectLatest { lanOnly = it }
        }
    }
    fun setLanOnly(b: Boolean) { filter.setLanOnly(b) }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("LAN-only mode")
            Switch(checked = vm.lanOnly, onCheckedChange = vm::setLanOnly)
        }
        Text("Battery optimization: ensure LuLan is whitelisted to keep streaming when locked.")
        Text("Security: share the session PIN out-of-band.")
    }
}
