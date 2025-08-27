package com.lulan.app.webrtc

import android.content.Context
import android.media.projection.MediaProjection
import com.lulan.app.util.LanOnlyFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class WebRtcManager(
    private val context: Context,
    private val lanOnlyFilter: LanOnlyFilter
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val eglBase = EglBase.create()
    private val pcFactory by lazy { buildFactory() }

    private val peers = ConcurrentHashMap<String, PeerConnection>()
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var screenCapturer: VideoCapturer? = null

    data class Stats(val fps: Int?, val bitrateKbps: Int?, val packetsLost: Int?)
    private val _stats = MutableStateFlow(Stats(null, null, null))
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    fun prepareScreen(projection: MediaProjection, width: Int, height: Int, fps: Int, bitrateKbps: Int) {
        Timber.i("prepareScreen ${width}x$height @$fps ${bitrateKbps}kbps")

        val capturer = ScreenCapturerAndroid(projection, object : MediaProjection.Callback() {})
        screenCapturer = capturer

        val csf = DefaultVideoEncoderFactory(eglBase.eglBaseContext, /*enableIntelVp8Encoder*/true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        // Attach field trials to force hardware where possible
        val options = PeerConnectionFactory.Options()

        videoSource = pcFactory.createVideoSource(capturer.isScreencast)
        val surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        capturer.initialize(surfaceHelper, context, videoSource!!.capturerObserver)
        capturer.startCapture(width, height, fps)

        videoTrack = pcFactory.createVideoTrack("VIDEO", videoSource)
        videoTrack!!.setEnabled(true)

        // Optional mic audio (system audio needs API support)
        audioSource = pcFactory.createAudioSource(MediaConstraints())
        audioTrack = pcFactory.createAudioTrack("AUDIO", audioSource)
        audioTrack!!.setEnabled(true)
    }

    private fun buildFactory(): PeerConnectionFactory {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeer(clientId: String,
                   onLocalOffer: (String) -> Unit,
                   onLocalIce: (JSONObject) -> Unit) {
        val config = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            disableIpv6 = true
        }
        val pc = pcFactory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(cand: IceCandidate?) {
                cand ?: return
                if (lanOnlyFilter.allowAddress(cand.sdpMid ?: "")) {
                    val json = JSONObject().apply {
                        put("candidate", cand.sdp)
                        put("sdpMid", cand.sdpMid)
                        put("sdpMLineIndex", cand.sdpMLineIndex)
                    }
                    onLocalIce(json)
                }
            }
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
        })!!

        peers[clientId] = pc

        // Unified plan: add tracks.
        val transceiverInit = RtpTransceiver.RtpTransceiverInit(RtpTransceiver.Direction.SEND_ONLY)
        videoTrack?.let { pc.addTransceiver(it, transceiverInit) }
        audioTrack?.let { pc.addTransceiver(it, transceiverInit) }

        // DataChannel for control
        val dc = pc.createDataChannel("control", DataChannel.Init())
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {}
            override fun onStateChange() {}
            override fun onMessage(buffer: DataChannel.Buffer) {
                // Handle small JSON control messages (pause/resume/quality)
            }
        })

        scope.launch {
            val offer = pc.createOffer(MediaConstraints())
            pc.setLocalDescription(offer)
            onLocalOffer(offer.description)
        }
    }

    fun setRemoteAnswer(clientId: String, sdp: String) {
        peers[clientId]?.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    fun onRemoteIce(clientId: String, candidate: JSONObject) {
        val c = IceCandidate(
            candidate.optString("sdpMid"),
            candidate.optInt("sdpMLineIndex"),
            candidate.optString("candidate")
        )
        peers[clientId]?.addIceCandidate(c)
    }

    fun stop() {
        screenCapturer?.stopCapture()
        videoTrack?.dispose()
        videoSource?.dispose()
        audioTrack?.dispose()
        audioSource?.dispose()
        peers.values.forEach { it.dispose() }
        peers.clear()
    }
}
