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
 * Sync Engine - Antigravity Protocol Implementation
 * 
 * Implements offline-first sync with hybrid conflict resolution:
 * - Persistent outbox queue (survives app kill)
 * - Versioning + timestamp-based conflict resolution
 * - Status priority ranking (Manager > Staff)
 * - SHA-256 checksum validation
 * - Exponential backoff retry (100ms, 200ms, 400ms, 800ms, 1600ms)
 * - Dead letter vault for failed syncs
 * 
 * Stone Tier Requirements:
 * ✓ Outbox Pattern: Orders persist in SyncQueueEntity
 * ✓ Exponential Backoff: Retry delays increase exponentially
 * ✓ Manager Supremacy: VOID (100) beats PENDING (10)
 */
@Singleton
class SyncEngine @Inject constructor(
    private val meshNetwork: MeshNetworkManager,
    private val orderDao: OrderDao,
    private val orderDetailDao: OrderDetailDao,
    private val tableDao: TableDao,
    private val syncQueueDao: SyncQueueDao,
    private val deadLetterDao: DeadLetterDao,
    private val processedMessagesDao: ProcessedMessagesDao, // Security Gate: Idempotency
    private val gson: Gson
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Observe queue size for UI (replaces in-memory queue)
    val queueSize: Flow<Int> = syncQueueDao.observeQueueSize()
    
    // Observe dead letter count for UI badge
    val deadLetterCount: Flow<Int> = deadLetterDao.observeUnresolvedCount()
    
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
     * Stone Tier: Persists to database queue before attempting sync
     */
    suspend fun syncOrderCreate(order: OrderEntity) {
        // Save locally first (local-first persistence)
        val orderId = orderDao.insertOrder(order)
        Log.d(TAG, "Order saved locally: $orderId")
        
        // Queue to persistent database (survives app kill)
        val payload = gson.toJson(order)
        val checksum = ChecksumUtil.generateChecksum(payload)
        
        val queueItem = SyncQueueEntity(
            messageId = java.util.UUID.randomUUID().toString(),
            messageType = MessageType.ORDER_CREATE.name,
            payload = payload,
            checksum = checksum,
            version = order.version,
            timestamp = System.currentTimeMillis(),
            highResTimestamp = System.nanoTime(),
            isAdditive = ConflictResolver.isAdditiveChange(MessageType.ORDER_CREATE.name),
            status = SyncStatus.PENDING
        )
        
        syncQueueDao.insertMessage(queueItem)
        Log.d(TAG, "Order queued for sync: ${order.orderNumber}")
    }
    
    /**
     * Sync order update with conflict resolution
     */
    suspend fun syncOrderUpdate(order: OrderEntity, userRole: String = "STAFF") {
        // Update locally first
        orderDao.updateOrder(order)
        Log.d(TAG, "Order updated locally: ${order.orderId}")
        
        // Queue with priority based on user role
        val payload = gson.toJson(order)
        val checksum = ChecksumUtil.generateChecksum(payload)
        val priority = ConflictResolver.calculateMessagePriority(userRole, "UPDATE")
        
        val queueItem = SyncQueueEntity(
            messageId = java.util.UUID.randomUUID().toString(),
            messageType = MessageType.ORDER_UPDATE.name,
            payload = payload,
            checksum = checksum,
            version = order.version,
            timestamp = System.currentTimeMillis(),
            highResTimestamp = System.nanoTime(),
            priority = priority,
            status = SyncStatus.PENDING
        )
        
        syncQueueDao.insertMessage(queueItem)
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
        
        // Queue to persistent database
        val payload = gson.toJson(detail)
        val checksum = ChecksumUtil.generateChecksum(payload)
        
        val queueItem = SyncQueueEntity(
            messageId = java.util.UUID.randomUUID().toString(),
            messageType = MessageType.ORDER_DETAIL_UPDATE.name,
            payload = payload,
            checksum = checksum,
            version = 1,
            timestamp = System.currentTimeMillis(),
            highResTimestamp = System.nanoTime(),
            status = SyncStatus.PENDING
        )
        
        syncQueueDao.insertMessage(queueItem)
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
     * Security Gate: Idempotency check prevents double-processing
     */
    private fun handleIncomingMessage(message: SyncMessage) {
        scope.launch {
            try {
                // SECURITY GATE: Idempotency Check (CRITICAL)
                // Must check BEFORE processing to prevent double-billing
                val alreadyProcessed = processedMessagesDao.isProcessed(message.messageId) > 0
                if (alreadyProcessed) {
                    Log.w(TAG, "⚠️ DUPLICATE MESSAGE REJECTED: ${message.messageId} (${message.messageType})")
                    // Send ACK anyway to prevent sender from retrying
                    sendAck(message)
                    return@launch
                }
                
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
            
            // Mark as processed (Security Gate: Idempotency)
            processedMessagesDao.markAsProcessed(
                ProcessedMessageEntity(
                    messageId = message.messageId,
                    messageType = message.messageType.name,
                    senderId = message.senderId,
                    checksum = message.checksum ?: "",
                    payloadHash = ChecksumUtil.generateChecksum(message.payload)
                )
            )
        } else {
            Log.d(TAG, "Order ${order.orderNumber} already exists, skipping")
        }
    }
    
    private suspend fun handleOrderUpdate(message: SyncMessage) {
        // Validate checksum first
        if (!ChecksumUtil.validateChecksum(message.payload, message.checksum ?: "")) {
            Log.e(TAG, "Checksum validation failed for order update")
            return
        }
        
        val order = gson.fromJson(message.payload, OrderEntity::class.java)
        val existing = orderDao.getOrderBySyncId(order.syncId)
        
        if (existing != null) {
            // Stone Tier: Hybrid conflict resolution
            val shouldAccept = ConflictResolver.resolveVersionConflict(
                localVersion = existing.version,
                remoteVersion = order.version,
                localTimestamp = existing.updatedAt,
                remoteTimestamp = order.updatedAt
            )
            
            if (shouldAccept) {
                // Check for status conflict (Manager Supremacy)
                val resolvedStatus = ConflictResolver.resolveStatusConflict(
                    localStatus = existing.status,
                    remoteStatus = order.status
                )
                
                val mergedOrder = order.copy(status = resolvedStatus)
                orderDao.updateOrder(mergedOrder)
                Log.d(TAG, "Order ${order.orderNumber} updated (v${order.version})")
            } else {
                Log.d(TAG, "Order ${order.orderNumber} update rejected (older version)")
            }
        } else {
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
    
    /**
     * Process sync queue with exponential backoff
     * Stone Tier: Implements retry delays (100ms, 200ms, 400ms, 800ms, 1600ms)
     */
    
    private fun startQueueProcessor() {
        scope.launch {
            while (isActive) {
                try {
                    val pendingMessages = syncQueueDao.getPendingMessages()
                    
                    for (queueItem in pendingMessages) {
                        // Stone Tier: Exponential backoff
                        val retryDelay = SyncQueueEntity.getRetryDelay(queueItem.retryCount)
                        
                        // Check if enough time has passed since last attempt
                        val timeSinceLastAttempt = queueItem.lastAttemptAt?.let {
                            System.currentTimeMillis() - it
                        } ?: Long.MAX_VALUE
                        
                        if (timeSinceLastAttempt < retryDelay) {
                            continue // Not ready to retry yet
                        }
                        
                        // Update status to SYNCING
                        syncQueueDao.updateMessageStatus(
                            queueId = queueItem.queueId,
                            status = SyncStatus.SYNCING
                        )
                        
                        // Attempt to send
                        val success = attemptSync(queueItem)
                        
                        if (success) {
                            // Success - remove from queue
                            syncQueueDao.deleteMessage(queueItem.queueId)
                            Log.d(TAG, "Synced: ${queueItem.messageType}")
                        } else {
                            // Failed - increment retry count
                            syncQueueDao.incrementRetryCount(
                                queueId = queueItem.queueId,
                                errorMessage = "Network error"
                            )
                            
                            // Check if max retries exceeded
                            if (queueItem.retryCount + 1 >= SyncQueueEntity.MAX_RETRY_COUNT) {
                                // Stone Tier: Move to Dead Letter Vault
                                moveToDeadLetterVault(queueItem)
                                syncQueueDao.deleteMessage(queueItem.queueId)
                                Log.w(TAG, "Moved to dead letter vault: ${queueItem.messageType}")
                            }
                        }
                    }
                    
                    delay(500) // Check queue every 500ms
                } catch (e: Exception) {
                    Log.e(TAG, "Queue processor error", e)
                    delay(1000)
                }
            }
        }
    }
    
    /**
     * Attempt to sync a queue item
     */
    private suspend fun attemptSync(queueItem: SyncQueueEntity): Boolean {
        return try {
            val message = SyncMessage(
                messageId = queueItem.messageId,
                senderId = meshNetwork.getDeviceId(),
                messageType = MessageType.valueOf(queueItem.messageType),
                payload = queueItem.payload,
                timestamp = queueItem.timestamp,
                version = queueItem.version,
                checksum = queueItem.checksum,
                highResTimestamp = queueItem.highResTimestamp,
                priority = queueItem.priority
            )
            
            val sent = meshNetwork.broadcastMessage(message)
            sent > 0
        } catch (e: Exception) {
            Log.e(TAG, "Sync attempt failed", e)
            false
        }
    }
    
    /**
     * Move failed sync to dead letter vault
     * Stone Tier: Manager can review and resolve these
     */
    private suspend fun moveToDeadLetterVault(queueItem: SyncQueueEntity) {
        val deadLetter = DeadLetterEntity(
            originalMessageId = queueItem.messageId,
            messageType = queueItem.messageType,
            payload = queueItem.payload,
            checksum = queueItem.checksum,
            failureReason = DeadLetterEntity.REASON_NETWORK_TIMEOUT,
            failureCount = queueItem.retryCount,
            lastError = queueItem.errorMessage,
            requiresManagerReview = true
        )
        
        deadLetterDao.insertDeadLetter(deadLetter)
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
