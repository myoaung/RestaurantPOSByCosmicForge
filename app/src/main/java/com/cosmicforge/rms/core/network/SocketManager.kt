package com.cosmicforge.rms.core.network

import android.util.Log
import com.cosmicforge.rms.core.network.model.ConnectionType
import com.cosmicforge.rms.core.network.model.PeerDevice
import com.cosmicforge.rms.core.network.model.SyncMessage
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket communication manager for P2P data transfer
 * Handles both WiFi Direct and NSD connections
 */
@Singleton
class SocketManager @Inject constructor(
    private val gson: Gson
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var serverSocket: ServerSocket? = null
    private val clientSockets = mutableMapOf<String, Socket>()
    
    private val _connectedPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val connectedPeers: StateFlow<List<PeerDevice>> = _connectedPeers.asStateFlow()
    
    private val _receivedMessages = MutableStateFlow<SyncMessage?>(null)
    val receivedMessages: StateFlow<SyncMessage?> = _receivedMessages.asStateFlow()
    
    private var isServerRunning = false
    
    /**
     * Start server socket to accept incoming connections
     */
    fun startServer(port: Int = DEFAULT_PORT, onMessageReceived: (SyncMessage) -> Unit) {
        if (isServerRunning) {
            Log.w(TAG, "Server already running")
            return
        }
        
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isServerRunning = true
                Log.d(TAG, "Server started on port $port")
                
                while (isServerRunning) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let { socket ->
                            Log.d(TAG, "Client connected: ${socket.inetAddress.hostAddress}")
                            handleClientConnection(socket, onMessageReceived)
                        }
                    } catch (e: Exception) {
                        if (isServerRunning) {
                            Log.e(TAG, "Error accepting client connection", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting server", e)
                isServerRunning = false
            }
        }
    }
    
    /**
     * Stop server socket
     */
    fun stopServer() {
        Log.d(TAG, "Stopping server")
        isServerRunning = false
        
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serverSocket = null
    }
    
    /**
     * Connect to a peer as client
     */
    fun connectToPeer(
        peer: PeerDevice,
        onConnected: (Boolean) -> Unit
    ) {
        scope.launch {
            try {
                // Parse host and port from address
                val (host, port) = if (peer.deviceAddress.contains(":")) {
                    val parts = peer.deviceAddress.split(":")
                    val portValue = parts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_PORT
                    parts[0] to portValue
                } else {
                    peer.deviceAddress to DEFAULT_PORT
                }
                
                Log.d(TAG, "Connecting to ${peer.deviceName} at $host:$port")
                val socket = java.net.Socket(host, port)
                clientSockets[peer.deviceId] = socket
                
                withContext(Dispatchers.Main) {
                    onConnected(true)
                    updateConnectedPeers()
                }
                
                Log.d(TAG, "Connected to ${peer.deviceName}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to ${peer.deviceName}", e)
                withContext(Dispatchers.Main) {
                    onConnected(false)
                }
            }
        }
    }
    
    /**
     * Send message to a specific peer
     */
    fun sendMessage(peerId: String, message: SyncMessage): Boolean {
        val socket = clientSockets[peerId]
        if (socket == null || socket.isClosed) {
            Log.e(TAG, "Socket not connected for peer: $peerId")
            return false
        }
        
        return try {
            val output = PrintWriter(socket.getOutputStream(), true)
            val json = gson.toJson(message)
            output.println(json)
            Log.d(TAG, "Sent message to $peerId: ${message.messageType}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to $peerId", e)
            disconnectPeer(peerId)
            false
        }
    }
    
    /**
     * Broadcast message to all connected peers
     */
    fun broadcastMessage(message: SyncMessage): Int {
        var successCount = 0
        clientSockets.keys.forEach { peerId ->
            if (sendMessage(peerId, message)) {
                successCount++
            }
        }
        Log.d(TAG, "Broadcast message to $successCount/${clientSockets.size} peers")
        return successCount
    }
    
    /**
     * Disconnect from a specific peer
     */
    fun disconnectPeer(peerId: String) {
        clientSockets[peerId]?.let { socket ->
            try {
                socket.close()
                Log.d(TAG, "Disconnected from peer: $peerId")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket for $peerId", e)
            }
        }
        clientSockets.remove(peerId)
        updateConnectedPeers()
    }
    
    /**
     * Disconnect all peers
     */
    fun disconnectAll() {
        Log.d(TAG, "Disconnecting all peers")
        clientSockets.keys.toList().forEach { peerId ->
            disconnectPeer(peerId)
        }
    }
    
    /**
     * Cleanup all resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up socket manager")
        stopServer()
        disconnectAll()
        scope.cancel()
    }
    
    private fun handleClientConnection(
        socket: Socket,
        onMessageReceived: (SyncMessage) -> Unit
    ) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                while (socket.isConnected && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    
                    try {
                        val message = gson.fromJson(line, SyncMessage::class.java)
                        Log.d(TAG, "Received message: ${message.messageType} from ${message.senderId}")
                        
                        withContext(Dispatchers.Main) {
                            _receivedMessages.value = message
                            onMessageReceived(message)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client connection", e)
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing client socket", e)
                }
            }
        }
    }
    
    private fun updateConnectedPeers() {
        // This would be populated with actual peer info
        // For now, just update the count
        Log.d(TAG, "Connected peers: ${clientSockets.size}")
    }
    
    companion object {
        private const val TAG = "SocketManager"
        private const val DEFAULT_PORT = 8888
    }
}
