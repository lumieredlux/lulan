package com.lulan.app.signaling

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class NsdHelper(private val context: Context) {
    private val nsd: NsdManager = context.getSystemService(NsdManager::class.java)

    fun registerService(port: Int, name: String): NsdManager.RegistrationListener {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = name
            serviceType = "_lulan._tcp"
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Timber.i("NSD registered: ${info.serviceName}@${info.port}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Timber.e("NSD registration failed: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Timber.i("NSD unregistered")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Timber.e("NSD unregistration failed: $errorCode")
            }
        }
        nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        return listener
    }

    data class Discovered(val name: String, val host: String, val port: Int)

    fun discover(): Flow<Discovered> = callbackFlow {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Timber.i("NSD discovery started")
            override fun onDiscoveryStopped(serviceType: String) = Timber.i("NSD discovery stopped")
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("NSD start failed: $errorCode")
                close()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("NSD stop failed: $errorCode")
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType == "_lulan._tcp.") {
                    nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) =
                            Timber.e("Resolve failed: $errorCode")
                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            trySend(
                                Discovered(
                                    name = resolved.serviceName,
                                    host = resolved.host.hostAddress ?: "",
                                    port = resolved.port
                                )
                            )
                        }
                    })
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.w("NSD service lost: ${serviceInfo.serviceName}")
            }
        }
        nsd.discoverServices("_lulan._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { nsd.stopServiceDiscovery(listener) }
    }
}
