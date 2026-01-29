package com.cosmicforge.rms.core.sync

import android.util.Log
import com.cosmicforge.rms.core.network.MeshNetworkManager
import com.cosmicforge.rms.core.network.model.MessageType
import com.cosmicforge.rms.core.network.model.SyncMessage
import com.cosmicforge.rms.data.database.dao.*
import com.cosmicforge.rms.data.database.entities.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync Engine - Real-time data synchronization across devices
 * Implements local-first persistence with network sync
 */
@Singleton
class SyncEngine @Inject constructor(
    private val meshNetwork: MeshNetworkManager,
    private val orderDao: OrderDao,
    private val orderDetailDao: OrderDetailDao,
    private val tableDao: TableDao,
    private val gson: Gson
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _syncQueue = MutableStateFlow<List<SyncMessage>>(emptyList())
    val syncQueue: StateFlow<List<SyncMessage>> = _syncQueue.asStateFlow()
    
    /**
     * Initialize sync engine
     */
    fun initialize() {
        Log.d(TAG, "Initializing Sync Engine")
        
        // Handle incoming messages
        meshNetwork.initialize(onMessageReceived = ::handleIncomingMessage)
        
        // Start processing sync queue
        startQueueProcessor()
    }
    
    /**
     * Sync order create
     */
    suspend fun syncOrderCreate(order: OrderEntity) {
        // Save locally first (local-first persistence)
        val orderId = orderDao.insertOrder(order)
        Log.d(TAG, "Order saved locally: $orderId")
        
        // Then broadcast to network
        val message = SyncMessage(
            senderId = meshNetwork.getDeviceId(),
            messageType = MessageType.ORDER_CREATE,
            payload = gson.toJson(order),
            version = order.version
        )
        
        queueMessage(message)
    }
    
    /**
     * Sync order update
     */
    suspend fun syncOrderUpdate(order: OrderEntity) {
        // Update locally first
        orderDao.updateOrder(order)
        Log.d(TAG, "Order updated locally: ${order.orderId}")
        
        // Broadcast update
        val message = SyncMessage(
            senderId = meshNetwork.getDeviceId(),
            messageType = MessageType.ORDER_UPDATE,
            payload = gson.toJson(order),
            version = order.version
        )
        
        queueMessage(message)
    }
    
    /**
     * Sync chief claim (critical for accountability)
     */
    suspend fun syncChiefClaim(
        detailId: Long,
        chiefId: Long,
        chiefName: String
    ) {
        // Update locally first
        orderDetailDao.claimByChief(detailId, chiefId)
        Log.d(TAG, "Chief $chiefName claimed detail $detailId locally")
        
        // Broadcast claim to all devices
        meshNetwork.broadcastChiefClaim(detailId, chiefId, chiefName)
    }
    
    /**
     * Sync order detail update
     */
    suspend fun syncOrderDetailUpdate(detail: OrderDetailEntity) {
        // Update locally
        orderDetailDao.updateDetail(detail)
        
        val message = SyncMessage(
            senderId = meshNetwork.getDeviceId(),
            messageType = MessageType.ORDER_DETAIL_UPDATE,
            payload = gson.toJson(detail)
        )
        
        queueMessage(message)
    }
    
    /**
     * Sync table status update
     */
    suspend fun syncTableStatusUpdate(tableId: String, status: String) {
        // Update locally
        tableDao.updateTableStatus(tableId, status)
        Log.d(TAG, "Table $tableId status updated to $status locally")
        
        // Broadcast to network
        meshNetwork.broadcastTableStatusUpdate(tableId, status)
    }
    
    /**
     * Handle incoming sync messages from other devices
     */
    private fun handleIncomingMessage(message: SyncMessage) {
        scope.launch {
            try {
                Log.d(TAG, "Processing incoming message: ${message.messageType} from ${message.senderId}")
                
                when (message.messageType) {
                    MessageType.ORDER_CREATE -> handleOrderCreate(message)
                    MessageType.ORDER_UPDATE -> handleOrderUpdate(message)
                    MessageType.ORDER_DETAIL_UPDATE -> handleOrderDetailUpdate(message)
                    MessageType.CHIEF_CLAIM -> handleChiefClaim(message)
                    MessageType.TABLE_STATUS_UPDATE -> handleTableStatusUpdate(message)
                    MessageType.HEARTBEAT -> handleHeartbeat(message)
                    MessageType.ACK -> Log.d(TAG, "Received ACK from ${message.senderId}")
                    else -> Log.w(TAG, "Unknown message type: ${message.messageType}")
                }
                
                // Send ACK
                sendAck(message)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming message", e)
            }
        }
    }
    
    private suspend fun handleOrderCreate(message: SyncMessage) {
        val order = gson.fromJson(message.payload, OrderEntity::class.java)
        
        // Check if order already exists
        val existing = orderDao.getOrderBySyncId(order.syncId)
        if (existing == null) {
            orderDao.insertOrder(order)
            Log.d(TAG, "Order ${order.orderNumber} created from remote device")
        } else {
            Log.d(TAG, "Order ${order.orderNumber} already exists, skipping")
        }
    }
    
    private suspend fun handleOrderUpdate(message: SyncMessage) {
        val order = gson.fromJson(message.payload, OrderEntity::class.java)
        
        val existing = orderDao.getOrderBySyncId(order.syncId)
        if (existing != null) {
            // Conflict resolution: use version or last-write-wins
            if (message.version >= existing.version) {
                orderDao.updateOrder(order)
                Log.d(TAG, "Order ${order.orderNumber} updated from remote device")
            } else {
                Log.d(TAG, "Order ${order.orderNumber} update ignored (older version)")
            }
        } else {
            // Order doesn't exist, create it
            orderDao.insertOrder(order)
        }
    }
    
    private suspend fun handleOrderDetailUpdate(message: SyncMessage) {
        val detail = gson.fromJson(message.payload, OrderDetailEntity::class.java)
        orderDetailDao.updateDetail(detail)
        Log.d(TAG, "Order detail ${detail.detailId} updated from remote device")
    }
    
    private suspend fun handleChiefClaim(message: SyncMessage) {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val payload: Map<String, Any> = gson.fromJson(message.payload, type)
        
        val detailId = (payload["orderDetailId"] as Double).toLong()
        val chiefId = (payload["chiefId"] as Double).toLong()
        val chiefName = payload["chiefName"] as String
        val startTime = (payload["startTime"] as Double).toLong()
        
        // Update locally
        orderDetailDao.claimByChief(detailId, chiefId, startTime)
        Log.d(TAG, "Chief $chiefName claimed detail $detailId from remote device")
    }
    
    private suspend fun handleTableStatusUpdate(message: SyncMessage) {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val payload: Map<String, Any> = gson.fromJson(message.payload, type)
        
        val tableId = payload["tableId"] as String
        val status = payload["status"] as String
        val timestamp = (payload["timestamp"] as Double).toLong()
        
        tableDao.updateTableStatus(tableId, status, timestamp)
        Log.d(TAG, "Table $tableId status updated to $status from remote device")
    }
    
    private fun handleHeartbeat(message: SyncMessage) {
        Log.d(TAG, "Heartbeat from ${message.payload}")
    }
    
    private fun sendAck(originalMessage: SyncMessage) {
        val ack = SyncMessage(
            senderId = meshNetwork.getDeviceId(),
            messageType = MessageType.ACK,
            payload = originalMessage.messageId
        )
        
        meshNetwork.broadcastMessage(ack)
    }
    
    private fun queueMessage(message: SyncMessage) {
        _syncQueue.value = _syncQueue.value + message
        Log.d(TAG, "Message queued: ${message.messageType}")
    }
    
    private fun startQueueProcessor() {
        scope.launch {
            _syncQueue.collect { queue ->
                if (queue.isNotEmpty()) {
                    val message = queue.first()
                    
                    val sent = meshNetwork.broadcastMessage(message)
                    if (sent > 0) {
                        // Remove from queue after successful broadcast
                        _syncQueue.value = queue.drop(1)
                        Log.d(TAG, "Message sent to $sent peers, removed from queue")
                    } else {
                        Log.w(TAG, "No peers connected, message remains in queue")
                    }
                    
                    delay(100) // Small delay to avoid overwhelming the network
                }
            }
        }
    }
    
    /**
     * Start network discovery and sync
     */
    fun startSync() {
        Log.d(TAG, "Starting network sync")
        meshNetwork.startDiscovery()
    }
    
    /**
     * Stop network sync
     */
    fun stopSync() {
        Log.d(TAG, "Stopping network sync")
        meshNetwork.stopDiscovery()
    }
    
    /**
     * Cleanup
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up sync engine")
        meshNetwork.cleanup()
        scope.cancel()
    }
    
    companion object {
        private const val TAG = "SyncEngine"
    }
}
