# Keep WebRTC and NanoHTTPD classes (reflection / JNI)
-keep class org.webrtc.** { *; }
-keep class fi.iki.elonen.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
