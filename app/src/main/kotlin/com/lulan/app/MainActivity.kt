package com.lulan.app

import android.Manifest
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import com.lulan.app.ui.navigation.LuLanNavGraph
import com.lulan.app.ui.theme.LuLanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    private val projectionRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        ScreenProjectionPermissionStore.onResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LuLanTheme {
                LuLanNavGraph(
                    onRequestProjection = {
                        val mgr = getSystemService(MediaProjectionManager::class.java)
                        projectionRequest.launch(mgr.createScreenCaptureIntent())
                    }
                )
            }
        }
    }
}

/** Very small in-memory bridge so Compose screens can await the MediaProjection result. */
object ScreenProjectionPermissionStore {
    private var callback: ((resultCode: Int, data: android.content.Intent?) -> Unit)? = null
    fun register(cb: (Int, android.content.Intent?) -> Unit) { callback = cb }
    fun onResult(result: androidx.activity.result.ActivityResult) {
        callback?.invoke(result.resultCode, result.data)
    }
}
