package com.qopy.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.net.InetAddress

data class DiscoveredDevice(
    val deviceId: String,
    val deviceName: String,
    val osType: String,
    val host: InetAddress,
    val port: Int,
    val publicKey: ByteArray?
)

class NsdDiscovery(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val SERVICE_TYPE = "_qopy._tcp."
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun registerService(deviceId: String, deviceName: String, port: Int, publicKey: ByteArray) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = deviceId
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("id", deviceId)
            setAttribute("name", deviceName)
            setAttribute("os", "android")
            setAttribute("proto_ver", "1")
            setAttribute("pubkey", publicKey.joinToString("") { "%02x".format(it) })
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.i("NsdDiscovery", "Service registered: ${NsdServiceInfo.serviceName}")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w("NsdDiscovery", "Registration failed: $errorCode")
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.w("NsdDiscovery", "Failed to register NSD service: ${e.message}")
        }
    }

    fun startDiscovery(selfDeviceId: String, onDeviceFound: (DiscoveredDevice) -> Unit) {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == SERVICE_TYPE && service.serviceName != selfDeviceId) {
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val id = serviceInfo.attributes["id"]?.let { String(it) } ?: serviceInfo.serviceName
                            if (id == selfDeviceId) return

                            val name = serviceInfo.attributes["name"]?.let { String(it) } ?: serviceInfo.serviceName
                            val os = serviceInfo.attributes["os"]?.let { String(it) } ?: "unknown"
                            val pubHex = serviceInfo.attributes["pubkey"]?.let { String(it) }
                            val pk = pubHex?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()

                            val dev = DiscoveredDevice(
                                deviceId = id,
                                deviceName = name,
                                osType = os,
                                host = serviceInfo.host,
                                port = serviceInfo.port,
                                publicKey = pk
                            )
                            onDeviceFound(dev)
                        }
                    })
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.w("NsdDiscovery", "Failed to start NSD discovery: ${e.message}")
        }
    }

    fun stop() {
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.w("NsdDiscovery", "Error stopping NSD: ${e.message}")
        }
    }
}
