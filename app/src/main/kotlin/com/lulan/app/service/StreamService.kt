package com.lulan.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.lulan.app.MainActivity
import com.lulan.app.R
import com.lulan.app.signaling.NsdHelper
import com.lulan.app.signaling.WebSocketSignalingServer
import com.lulan.app.webrtc.BitrateSelector
import com.lulan.app.webrtc.WebRtcManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class StreamService : Service() {

    @Inject lateinit var webRtc: WebRtcManager
    @Inject lateinit var signaling: WebSocketSignalingServer
    @Inject lateinit var nsd: NsdHelper

    private var wakeLock: PowerManager.WakeLock? = null
    private var regListener: android.net.nsd.NsdManager.RegistrationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LuLan::stream").apply { acquire() }
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val width = intent?.getIntExtra("width", 1280) ?: 1280
        val height = intent?.getIntExtra("height", 720) ?: 720
        val fps = intent?.getIntExtra("fps", 30) ?: 30
        val pin = intent?.getStringExtra("pin") ?: "0000"
        val projectionToken = MediaProjectionHolder.projection ?: run {
            stopSelf(); return START_NOT_STICKY
        }

        val kbps = BitrateSelector.selectKbps(BitrateSelector.Params(width, height, fps))
        webRtc.prepareScreen(projectionToken, width, height, fps, kbps)
        signaling.pin = pin

        val port = signaling.start(7575)
        regListener = nsd.registerService(port, "LuLan-${android.os.Build.MODEL}")

        signaling.onClientConnected = { clientId ->
            webRtc.createPeer(
                clientId,
                onLocalOffer = { sdp -> signaling.sendOffer(clientId, sdp) },
                onLocalIce = { cand -> signaling.sendIce(clientId, cand) }
            )
        }
        signaling.onClientAnswer = { clientId, sdp ->
            webRtc.setRemoteAnswer(clientId, sdp)
        }

        startForeground(1, buildNotification("Streaming on LAN : http://<this-device>:$port  PIN:$pin"))
        return START_STICKY
    }

    override fun onDestroy() {
        signaling.stop()
        regListener?.let {
            (getSystemService(Context.NSD_SERVICE) as android.net.nsd.NsdManager).unregisterService(it)
        }
        webRtc.stop()
        wakeLock?.release()
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel("lulan", "LuLan", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(text: String): Notification {
        val intent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, "lulan")
            .setSmallIcon(R.drawable.ic_lulan)
            .setContentTitle("LuLan — Streaming")
            .setContentText(text)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }

    companion object {
        object MediaProjectionHolder {
            var projection: android.media.projection.MediaProjection? = null
        }
    }
}
