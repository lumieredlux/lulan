package com.lulan.app.signaling

import android.content.Context
import com.lulan.app.util.LanIpUtil
import com.lulan.app.util.LanOnlyFilter
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import timber.log.Timber
import java.net.InetSocketAddress

/**
 * Embedded HTTP + WebSocket for signaling and hosting /assets/web/* viewer.
 * - GET / -> serves index.html
 * - GET /viewer.js -> serves viewer JS
 * - WS /signal?pin=1234 -> signaling. Server sends OFFER, expects ANSWER.
 */
class WebSocketSignalingServer(
    private val context: Context,
    private val lanOnlyFilter: LanOnlyFilter
) {
    data class Client(val id: String, val socket: ClientSocket)
    private var httpd: Httpd? = null
    private var wsPort: Int = 0
    private val _clients = MutableStateFlow<Map<String, Client>>(emptyMap())
    val clients = _clients.asStateFlow()
    var pin: String = "0000"
    var onClientAnswer: ((clientId: String, sdp: String) -> Unit)? = null
    var onClientConnected: ((clientId: String) -> Unit)? = null
    var onClientClosed: ((clientId: String) -> Unit)? = null

    fun start(preferredPort: Int = 7575): Int {
        val host = LanIpUtil.pickLanAddress() ?: "0.0.0.0"
        httpd = Httpd(host, preferredPort).apply {
            start(SOCKET_READ_TIMEOUT, false)
        }
        wsPort = preferredPort
        Timber.i("HTTP/WS server on $host:$wsPort")
        return wsPort
    }

    fun stop() {
        httpd?.stop()
        _clients.value.values.forEach { it.socket.close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "shutdown", false) }
        _clients.value = emptyMap()
    }

    fun sendOffer(clientId: String, sdp: String) {
        _clients.value[clientId]?.socket?.send(JSONObject(mapOf("type" to "offer", "sdp" to sdp)).toString())
    }

    fun sendIce(clientId: String, candidate: JSONObject) {
        _clients.value[clientId]?.socket?.send(JSONObject(mapOf("type" to "ice", "candidate" to candidate)).toString())
    }

    private inner class Httpd(host: String, port: Int) : NanoWSD(InetSocketAddress(host, port)) {

        override fun openWebSocketHandshake(handshake: IHTTPSession): WebSocket {
            val remoteIp = handshake.remoteIpAddress
            if (!lanOnlyFilter.allowAddress(remoteIp)) {
                Timber.w("Rejected non-LAN WS from $remoteIp")
                throw NanoHTTPD.ResponseException(NanoHTTPD.Response.Status.FORBIDDEN, "LAN only")
            }
            val q = handshake.parameters
            val clientPin = q["pin"]?.firstOrNull() ?: ""
            if (clientPin != pin) {
                throw NanoHTTPD.ResponseException(NanoHTTPD.Response.Status.UNAUTHORIZED, "PIN mismatch")
            }
            return ClientSocket(this)
        }

        override fun serveHttp(session: IHTTPSession): Response {
            val uri = session.uri
            val mime = if (uri.endsWith(".js")) "application/javascript" else "text/html"
            val assetPath = when (uri) {
                "/", "/index.html" -> "web/index.html"
                "/viewer.js" -> "web/viewer.js"
                else -> "web/index.html"
            }
            val stream = context.assets.open(assetPath)
            return newFixedLengthResponse(Response.Status.OK, mime, stream, stream.available().toLong())
        }
    }

    inner class ClientSocket(server: NanoWSD) : WebSocket(server, null) {
        private val id = System.currentTimeMillis().toString(16) + hashCode().toString(16)

        override fun onOpen() {
            Timber.i("WS client connected: $id")
            _clients.value = _clients.value + (id to Client(id, this))
            onClientConnected?.invoke(id)
        }

        override fun onClose(code: NanoWSD.WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
            Timber.i("WS client closed: $id ($reason)")
            _clients.value = _clients.value - id
            onClientClosed?.invoke(id)
        }

        override fun onMessage(msg: NanoWSD.WebSocketFrame?) {
            val text = msg?.textPayload ?: return
            val json = JSONObject(text)
            when (json.optString("type")) {
                "answer" -> onClientAnswer?.invoke(id, json.optString("sdp"))
                "ice" -> { /* viewer ICE -> forward handled in WebRtcManager via callback setter */ }
            }
        }

        override fun onPong(p0: NanoWSD.WebSocketFrame?) {}
        override fun onException(ex: java.lang.Exception?) { Timber.e(ex, "WS error") }
    }
}
