package com.cosmicforge.rms.core.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Broadcast receiver for WiFi Direct P2P events
 */
class WiFiDirectBroadcastReceiver @Inject constructor() : BroadcastReceiver() {
    
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var listener: P2PEventListener? = null
    
    fun initialize(
        manager: WifiP2pManager,
        channel: WifiP2pManager.Channel,
        listener: P2PEventListener
    ) {
        this.manager = manager
        this.channel = channel
        this.listener = listener
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // WiFi Direct is enabled or disabled
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                Log.d(TAG, "WiFi P2P state changed: enabled=$isEnabled")
                listener?.onP2PStateChanged(isEnabled)
            }
            
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // List of available peers has changed
                Log.d(TAG, "Peers list changed")
                manager?.requestPeers(channel) { peerList ->
                    listener?.onPeersChanged(peerList.deviceList.toList())
                }
            }
            
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Connection state has changed
                Log.d(TAG, "Connection state changed")
                manager?.requestConnectionInfo(channel) { info ->
                    listener?.onConnectionChanged(info)
                }
            }
            
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // This device's details have changed
                Log.d(TAG, "This device changed")
                listener?.onThisDeviceChanged()
            }
        }
    }
    
    companion object {
        private const val TAG = "WiFiDirectReceiver"
    }
}

/**
 * Listener for P2P events
 */
interface P2PEventListener {
    fun onP2PStateChanged(isEnabled: Boolean)
    fun onPeersChanged(peers: List<android.net.wifi.p2p.WifiP2pDevice>)
    fun onConnectionChanged(info: android.net.wifi.p2p.WifiP2pInfo)
    fun onThisDeviceChanged()
}
