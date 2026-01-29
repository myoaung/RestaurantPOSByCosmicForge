package com.cosmicforge.rms.core.network

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.cosmicforge.rms.core.network.model.ConnectionType
import com.cosmicforge.rms.core.network.model.PeerDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WiFi Direct Manager for P2P device discovery and connection
 * Handles direct peer-to-peer communication without router
 */
@Singleton
class WiFiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context
) : P2PEventListener {
    
    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }
    
    private var channel: WifiP2pManager.Channel? = null
    private val receiver = WiFiDirectBroadcastReceiver()
    
    private val _isP2PEnabled = MutableStateFlow(false)
    val isP2PEnabled: StateFlow<Boolean> = _isP2PEnabled.asStateFlow()
    
    private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerDevice>> = _discoveredPeers.asStateFlow()
    
    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()
    
    private var isRegistered = false
    
    /**
     * Initialize WiFi Direct
     */
    fun initialize() {
        Log.d(TAG, "Initializing WiFi Direct")
        
        if (manager == null) {
            Log.e(TAG, "WiFi Direct is not supported on this device")
            return
        }
        
        channel = manager?.initialize(context, context.mainLooper, null)
        
        // Register broadcast receiver
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        
        if (channel != null) {
            receiver.initialize(manager!!, channel!!, this)
            context.registerReceiver(receiver, intentFilter)
            isRegistered = true
            Log.d(TAG, "WiFi Direct initialized successfully")
        }
    }
    
    /**
     * Start peer discovery
     */
    fun discoverPeers() {
        if (!checkPermissions()) {
            Log.e(TAG, "Missing required permissions for peer discovery")
            return
        }
        
        Log.d(TAG, "Starting peer discovery")
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Peer discovery initiated successfully")
            }
            
            override fun onFailure(reason: Int) {
                val errorMsg = when (reason) {
                    WifiP2pManager.ERROR -> "Internal error"
                    WifiP2pManager.P2P_UNSUPPORTED -> "P2P unsupported"
                    WifiP2pManager.BUSY -> "System busy"
                    else -> "Unknown error: $reason"
                }
                Log.e(TAG, "Peer discovery failed: $errorMsg")
            }
        })
    }
    
    /**
     * Stop peer discovery
     */
    fun stopPeerDiscovery() {
        Log.d(TAG, "Stopping peer discovery")
        manager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Peer discovery stopped")
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Failed to stop peer discovery: $reason")
            }
        })
    }
    
    /**
     * Connect to a specific peer
     */
    fun connectToPeer(peerAddress: String) {
        if (!checkPermissions()) {
            Log.e(TAG, "Missing required permissions for connection")
            return
        }
        
        val config = WifiP2pConfig().apply {
            deviceAddress = peerAddress
        }
        
        Log.d(TAG, "Connecting to peer: $peerAddress")
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connection initiated to $peerAddress")
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Connection failed to $peerAddress: $reason")
            }
        })
    }
    
    /**
     * Disconnect from all peers
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from all peers")
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Disconnected successfully")
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Disconnect failed: $reason")
            }
        })
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up WiFi Direct resources")
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
                isRegistered = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
        
        disconnect()
        channel?.close()
        channel = null
    }
    
    // P2PEventListener implementation
    override fun onP2PStateChanged(isEnabled: Boolean) {
        Log.d(TAG, "P2P state changed: enabled=$isEnabled")
        _isP2PEnabled.value = isEnabled
    }
    
    override fun onPeersChanged(peers: List<WifiP2pDevice>) {
        Log.d(TAG, "Peers changed. Count: ${peers.size}")
        _discoveredPeers.value = peers.map { device ->
            PeerDevice(
                deviceName = device.deviceName,
                deviceAddress = device.deviceAddress,
                connectionType = ConnectionType.WIFI_DIRECT,
                isConnected = device.status == WifiP2pDevice.CONNECTED
            )
        }
    }
    
    override fun onConnectionChanged(info: WifiP2pInfo) {
        Log.d(TAG, "Connection changed. Group formed: ${info.groupFormed}, Is owner: ${info.isGroupOwner}")
        _connectionInfo.value = info
    }
    
    override fun onThisDeviceChanged() {
        Log.d(TAG, "This device info changed")
    }
    
    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    companion object {
        private const val TAG = "WiFiDirectManager"
    }
}
