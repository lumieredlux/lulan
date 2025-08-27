package com.lulan.app.capture

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class ScreenCaptureController(private val context: Context) {

    private val _projection = MutableStateFlow<MediaProjection?>(null)
    val projection = _projection.asStateFlow()

    fun handlePermission(resultCode: Int, data: Intent?) {
        val mgr = context.getSystemService(MediaProjectionManager::class.java)
        if (resultCode == RESULT_OK && data != null) {
            val proj = mgr.getMediaProjection(resultCode, data)
            _projection.value = proj
            Timber.i("MediaProjection acquired")
        } else {
            Timber.w("MediaProjection denied")
        }
    }

    fun stop() {
        _projection.value?.stop()
        _projection.value = null
    }
}
