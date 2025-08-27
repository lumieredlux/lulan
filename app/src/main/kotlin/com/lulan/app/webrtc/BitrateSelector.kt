package com.lulan.app.webrtc

object BitrateSelector {
    data class Params(val width: Int, val height: Int, val fps: Int)
    /** Very simple ladder tuned for screen content. */
    fun selectKbps(p: Params): Int =
        when {
            p.width >= 1920 && p.fps >= 60 -> 8000
            p.width >= 1920 -> 6000
            p.width >= 1280 && p.fps >= 60 -> 4500
            p.width >= 1280 -> 3000
            p.width >= 960 -> 2000
            else -> 1200
        }
}
