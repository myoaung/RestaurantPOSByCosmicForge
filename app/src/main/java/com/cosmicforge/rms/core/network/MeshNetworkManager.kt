package com.cosmicforge.rms.core.network

import android.content.Context
import android.util.Log
import com.cosmicforge.rms.core.network.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified Mesh Network Manager
 * Orchestrates WiFi Direct and NSD fallback for optimal connectivity
 * Implements the "Whisper" protocol for <1s sync
 */
@Singleton
class MeshNetworkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiDirectManager: WiFiDirectManager,
    private val nsdManager: NSDManager,
    private val socketManager: SocketManager
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _networkMode = MutableStateFlow(NetworkMode.WIFI_DIRECT)
    val networkMode: StateFlow<NetworkMode> = _networkMode.asStateFlow()
    
    private val _allDiscoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val allDiscoveredPeers: StateFlow<List<PeerDevice>> = _allDiscoveredPeers.asStateFlow()
    
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private var deviceId: String = generateDeviceId()
    private var deviceName: String = "CosmicForge-${deviceId.take(6)}"
    
    private var messageHandler: ((SyncMessage) -> Unit)? = null
    
    /**
     * Initialize the mesh network
     */
    fun initialize(deviceName: String? = null, onMessageReceived: (SyncMessage) -> Unit) {
        Log.d(TAG, "Initializing Mesh Network")
        
        deviceName?.let { this.deviceName = it }
        this.messageHandler = onMessageReceived
        
        // Initialize WiFi Direct
        wifiDirectManager.initialize()
        
        // Observe WiFi Direct peers
        scope.launch {
            wifiDirectManager.discoveredPeers.collect { peers ->
                updateAllPeers()
            }
        }
        
        // Observe NSD peers
        scope.launch {
            nsdManager.discoveredServices.collect { peers ->
                updateAllPeers()
            }
        }
        
        // Monitor connection info for WiFi Direct
        scope.launch {
            wifiDirectManager.connectionInfo.collect { info ->
                info?.let {
                    if (it.groupFormed) {
                        handleWiFiDirectConnection(it)
                    }
                }
            }
        }
        
        // Start socket server
        socketManager.startServer { message ->
            messageHandler?.invoke(message)
        }
    }
    
    /**
     * Start discovery using both WiFi Direct and NSD
     */
    fun startDiscovery() {
        Log.d(TAG, "Starting discovery on both WiFi Direct and NSD")
        
        // WiFi Direct for direct P2P
        wifiDirectManager.discoverPeers()
        
        // NSD for local network fallback
        nsdManager.registerService(deviceName)
        nsdManager.startDiscovery()
        
        // Start heartbeat
        startHeartbeat()
    }
    
    /**
     * Stop discovery
     */
    fun stopDiscovery() {
        Log.d(TAG, "Stopping discovery")
        wifiDirectManager.stopPeerDiscovery()
        nsdManager.stopDiscovery()
    }
    
    /**
     * Connect to a peer (auto-select best method)
     */
    fun connectToPeer(peer: PeerDevice) {
        Log.d(TAG, "Connecting to ${peer.deviceName} via ${peer.connectionType}")
        
        when (peer.connectionType) {
            ConnectionType.WIFI_DIRECT -> {
                wifiDirectManager.connectToPeer(peer.deviceAddress)
            }
            ConnectionType.NSD_LOCAL -> {
                socketManager.connectToPeer(peer) { success ->
                    if (success) {
                        updateSyncStatus(connectedPeers = _syncStatus.value.connectedPeers + 1)
                    }
                }
            }
            ConnectionType.DISCONNECTED -> {
                Log.w(TAG, "Cannot connect to disconnected peer")
            }
        }
    }
    
    /**
     * Send sync message to specific peer
     */
    fun sendMessage(peerId: String, message: SyncMessage): Boolean {
        return socketManager.sendMessage(peerId, message)
    }
    
    /**
     * Broadcast message to all connected peers
     */
    fun broadcastMessage(message: SyncMessage): Int {
        return socketManager.broadcastMessage(message)
    }
    
    /**
     * Broadcast chief claim to all devices
     * Critical for chief accountability across tablets
     */
    fun broadcastChiefClaim(
        orderDetailId: Long,
        chiefId: Long,
        chiefName: String
    ) {
        val payload = mapOf(
            "orderDetailId" to orderDetailId,
            "chiefId" to chiefId,
            "chiefName" to chiefName,
            "startTime" to System.currentTimeMillis()
        )
        
        val message = SyncMessage(
            senderId = deviceId,
            messageType = MessageType.CHIEF_CLAIM,
            payload = com.google.gson.Gson().toJson(payload)
        )
        
        val sentCount = broadcastMessage(message)
        Log.d(TAG, "Chief claim broadcast to $sentCount devices: Chief $chiefName claimed detail $orderDetailId")
    }
    
    /**
     * Broadcast order update
     */
    fun broadcastOrderUpdate(orderId: Long, orderData: String) {
        val message = SyncMessage(
            senderId = deviceId,
            messageType = MessageType.ORDER_UPDATE,
            payload = orderData
        )
        broadcastMessage(message)
    }
    
    /**
     * Broadcast table status update
     */
    fun broadcastTableStatusUpdate(tableId: String, status: String) {
        val payload = mapOf(
            "tableId" to tableId,
            "status" to status,
            "timestamp" to System.currentTimeMillis()
        )
        
        val message = SyncMessage(
            senderId = deviceId,
            messageType = MessageType.TABLE_STATUS_UPDATE,
            payload = com.google.gson.Gson().toJson(payload)
        )
        
        broadcastMessage(message)
    }
    
    /**
     * Switch network mode (WiFi Direct / NSD fallback)
     */
    fun switchNetworkMode(mode: NetworkMode) {
        Log.d(TAG, "Switching network mode to $mode")
        _networkMode.value = mode
        
        when (mode) {
            NetworkMode.WIFI_DIRECT -> {
                nsdManager.stopDiscovery()
                wifiDirectManager.discoverPeers()
            }
            NetworkMode.NSD_FALLBACK -> {
                wifiDirectManager.stopPeerDiscovery()
                nsdManager.startDiscovery()
            }
            NetworkMode.HYBRID -> {
                wifiDirectManager.discoverPeers()
                nsdManager.startDiscovery()
            }
        }
    }
    
    /**
     * Get device ID
     */
    fun getDeviceId(): String = deviceId
    
    /**
     * Get device name
     */
    fun getDeviceName(): String = deviceName
    
    /**
     * Cleanup all network resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up mesh network")
        stopDiscovery()
        wifiDirectManager.cleanup()
        nsdManager.cleanup()
        socketManager.cleanup()
        scope.cancel()
    }
    
    private fun handleWiFiDirectConnection(info: android.net.wifi.p2p.WifiP2pInfo) {
        Log.d(TAG, "WiFi Direct connection established. Group owner: ${info.isGroupOwner}")
        
        if (info.isGroupOwner) {
            // This device is the group owner (server)
            Log.d(TAG, "This device is group owner, server already running")
        } else {
            // This device is a client, connect to group owner
            val groupOwnerAddress = info.groupOwnerAddress.hostAddress
            Log.d(TAG, "Connecting to group owner at $groupOwnerAddress")
            
            val ownerPeer = PeerDevice(
                deviceName = "GroupOwner",
                deviceAddress = "$groupOwnerAddress:8888",
                connectionType = ConnectionType.WIFI_DIRECT,
                isGroupOwner = true
            )
            
            socketManager.connectToPeer(ownerPeer) { success ->
                if (success) {
                    Log.d(TAG, "Connected to group owner")
                    updateSyncStatus(connectedPeers = _syncStatus.value.connectedPeers + 1)
                }
            }
        }
    }
    
    private fun updateAllPeers() {
        val wifiDirectPeers = wifiDirectManager.discoveredPeers.value
        val nsdPeers = nsdManager.discoveredServices.value
        
        _allDiscoveredPeers.value = (wifiDirectPeers + nsdPeers).distinctBy { it.deviceAddress }
        
        Log.d(TAG, "Total discovered peers: ${_allDiscoveredPeers.value.size} " +
                "(WiFi Direct: ${wifiDirectPeers.size}, NSD: ${nsdPeers.size})")
    }
    
    private fun startHeartbeat() {
        scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL)
                
                val heartbeat = SyncMessage(
                    senderId = deviceId,
                    messageType = MessageType.HEARTBEAT,
                    payload = deviceName
                )
                
                socketManager.broadcastMessage(heartbeat)
                
                updateSyncStatus(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    connectedPeers = socketManager.connectedPeers.value.size
                )
            }
        }
    }
    
    private fun updateSyncStatus(
        lastSyncTimestamp: Long? = null,
        pendingSyncCount: Int? = null,
        connectedPeers: Int? = null,
        syncErrors: Int? = null
    ) {
        _syncStatus.value = _syncStatus.value.copy(
            lastSyncTimestamp = lastSyncTimestamp ?: _syncStatus.value.lastSyncTimestamp,
            pendingSyncCount = pendingSyncCount ?: _syncStatus.value.pendingSyncCount,
            connectedPeers = connectedPeers ?: _syncStatus.value.connectedPeers,
            syncErrors = syncErrors ?: _syncStatus.value.syncErrors
        )
    }
    
    private fun generateDeviceId(): String {
        return java.util.UUID.randomUUID().toString()
    }
    
    companion object {
        private const val TAG = "MeshNetworkManager"
        private const val HEARTBEAT_INTERVAL = 5000L // 5 seconds
    }
}

/**
 * Network operation mode
 */
enum class NetworkMode {
    WIFI_DIRECT,    // Only WiFi Direct
    NSD_FALLBACK,   // Only NSD
    HYBRID          // Both (recommended)
}
