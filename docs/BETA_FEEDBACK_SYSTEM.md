# BETA FEEDBACK TRACKING SYSTEM
**Phase 4: Field Validation & Operational Handover**

**Effective Date**: February 5, 2026  
**Version**: 1.0

---

## BETA FEEDBACK DATABASE SCHEMA

### Table: `beta_feedback_events`

```sql
CREATE TABLE beta_feedback_events (
    event_id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- Identification
    timestamp BIGINT NOT NULL,           -- Unix milliseconds (Feb 5, 2026)
    device_id TEXT NOT NULL,             -- Tablet ID (e.g., TABLET_001, TABLET_002)
    user_id INTEGER,                     -- Waiter/Chef/Manager ID
    user_role TEXT,                      -- WAITER, CHEF, MANAGER
    
    -- Performance Metrics
    latency_ms INTEGER,                  -- Response time in milliseconds
    network_type TEXT,                   -- WiFi, Bluetooth, P2P Mesh
    signal_strength INTEGER,             -- Signal % (0-100)
    
    -- Sync Status
    sync_status TEXT,                    -- PENDING, SYNCING, SYNCED, FAILED
    queue_size INTEGER,                  -- Number of items in SyncQueue
    retry_count INTEGER,                 -- Number of transmission attempts
    
    -- Error Tracking
    error_code TEXT,                     -- System error code (if any)
    error_description TEXT,              -- Human-readable error
    error_stack_trace TEXT,              -- Full stack trace (optional)
    
    -- Operation Type
    operation_type TEXT,                 -- ORDER_CREATE, ORDER_UPDATE, PAYMENT, CHIEF_CLAIM, etc.
    message_type TEXT,                   -- Sync message type
    payload_size_bytes INTEGER,          -- Message size
    
    -- Business Context
    table_id TEXT,                       -- Table served
    order_id BIGINT,                     -- Order being processed
    order_amount_kyat DECIMAL(10, 2),    -- Order total
    
    -- System Health
    database_size_mb DOUBLE,             -- SQLite database size
    free_memory_mb INTEGER,              -- Available memory
    battery_percent INTEGER,             -- Device battery %
    
    -- Feedback Notes
    user_feedback TEXT,                  -- Free-text feedback
    severity_level TEXT,                 -- INFO, WARNING, ERROR, CRITICAL
    
    -- Metadata
    app_version TEXT,                    -- "9.0.0"
    android_sdk INTEGER,                 -- 24, 28, 30, 34, etc.
    restaurant_id TEXT,                  -- For multi-restaurant deployments
    
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
);
```

### Table: `device_registry`

```sql
CREATE TABLE devices (
    device_id TEXT PRIMARY KEY,
    
    -- Device Information
    device_name TEXT,                    -- "Kitchen Tablet 1", "Waiter Tablet A"
    device_role TEXT,                    -- KDS, WAITER, REGISTER, BACKUP
    android_model TEXT,                  -- "Samsung Galaxy Tab S7"
    android_version INTEGER,             -- 12, 13, 14
    
    -- Network Configuration
    ip_address TEXT,                     -- Local network IP
    mac_address TEXT,                    -- MAC address
    p2p_capable BOOLEAN,                 -- WiFi Direct support
    ble_capable BOOLEAN,                 -- Bluetooth Low Energy
    
    -- Deployment Info
    first_deployment_date BIGINT,        -- When first installed
    last_sync_at BIGINT,                 -- Last successful sync
    total_uptime_hours INTEGER,          -- Cumulative uptime
    
    -- Health Metrics
    crash_count INTEGER DEFAULT 0,       -- App crashes
    last_crash_at BIGINT,
    frozen_count INTEGER DEFAULT 0,      -- App freezes
    last_frozen_at BIGINT
);
```

### Table: `performance_metrics`

```sql
CREATE TABLE performance_metrics (
    metric_id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    timestamp BIGINT NOT NULL,
    device_id TEXT NOT NULL,
    
    -- Latency Breakdown (milliseconds)
    ui_render_time_ms INTEGER,          -- Time to show UI
    database_query_time_ms INTEGER,     -- Room DAO query time
    sync_encode_time_ms INTEGER,        -- JSON serialization
    network_transmit_time_ms INTEGER,   -- Send over network
    network_receive_time_ms INTEGER,    -- Receive response
    conflict_resolution_time_ms INTEGER, -- ConflictResolver logic
    total_operation_time_ms INTEGER,    -- End-to-end
    
    -- Throughput
    messages_per_second DOUBLE,
    bytes_per_second DOUBLE,
    
    -- Queue Processing
    pending_queue_items INTEGER,
    queue_drain_rate_items_per_min DOUBLE,
    
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
);
```

### Table: `conflict_resolution_log`

```sql
CREATE TABLE conflict_resolutions (
    resolution_id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    timestamp BIGINT NOT NULL,
    device_id TEXT NOT NULL,
    
    -- Conflict Details
    order_id BIGINT NOT NULL,
    local_status TEXT,                   -- What this tablet had
    remote_status TEXT,                  -- What remote tablet had
    
    -- Resolution
    decision_type TEXT,                  -- STATUS_PRIORITY, VERSION, NANOSECOND_TIEBREAKER
    local_priority INTEGER,              -- Priority of local status
    remote_priority INTEGER,             -- Priority of remote status
    winning_status TEXT,                 -- Which status won
    
    -- Timestamps (for tie-breaking analysis)
    local_timestamp_ms BIGINT,
    remote_timestamp_ms BIGINT,
    local_timestamp_ns BIGINT,           -- Nanosecond precision
    remote_timestamp_ns BIGINT,
    
    -- Versions
    local_version INTEGER,
    remote_version INTEGER,
    
    FOREIGN KEY (device_id) REFERENCES devices(device_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
```

### Table: `network_events`

```sql
CREATE TABLE network_events (
    event_id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    timestamp BIGINT NOT NULL,
    device_id TEXT NOT NULL,
    
    -- Event Type
    event_type TEXT,                    -- WIFI_CONNECTED, WIFI_DISCONNECTED, P2P_DISCOVERED, etc.
    event_description TEXT,
    
    -- Network Details
    network_name TEXT,                  -- SSID or P2P name
    network_type TEXT,                  -- WiFi, Bluetooth, P2P Mesh
    signal_strength INTEGER,            -- -127 to 0 dBm
    connected_peers INTEGER,            -- Number of connected devices
    
    -- Duration
    connection_duration_seconds INTEGER, -- How long connected
    
    -- Recovery
    auto_recovery_attempted BOOLEAN,
    recovery_successful BOOLEAN,
    recovery_time_seconds INTEGER,
    
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
);
```

### Table: `dead_letter_vault_events`

```sql
CREATE TABLE dead_letter_events (
    event_id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    timestamp BIGINT NOT NULL,
    device_id TEXT NOT NULL,
    
    -- Message Details
    message_id TEXT NOT NULL,
    message_type TEXT,                  -- ORDER_CREATE, ORDER_UPDATE, PAYMENT, etc.
    
    -- Failure Info
    failure_reason TEXT,                -- NETWORK_TIMEOUT, CHECKSUM_MISMATCH, etc.
    failure_count INTEGER,              -- Number of failed retries
    last_error TEXT,
    
    -- Recovery
    recovered BOOLEAN DEFAULT FALSE,
    recovery_timestamp BIGINT,
    recovery_method TEXT,               -- MANUAL, AUTOMATIC, RESYNC
    
    -- Impact
    affected_order_id BIGINT,
    affected_amount_kyat DECIMAL(10, 2),
    
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
);
```

---

## REAL-TIME FEEDBACK DASHBOARD

### Daily Metrics Summary

```
┌─────────────────────────────────────────────────────────────┐
│               COSMIC FORGE POS - BETA FEEDBACK              │
│                    Daily Report 2026-02-05                  │
└─────────────────────────────────────────────────────────────┘

DEVICE STATUS:
├─ Tablet 1 (KDS):        🟢 ONLINE (147 events, 0 errors)
├─ Tablet 2 (Waiter):     🟢 ONLINE (289 events, 1 conflict)
├─ Tablet 3 (Waiter):     🟢 ONLINE (234 events, 0 errors)
├─ Tablet 4 (Waiter):     🟠 INTERMITTENT (156 events, 5 reconnects)
├─ Tablet 5 (Register):   🟢 ONLINE (498 events, 0 errors)
├─ Tablet 6 (KDS):        🟢 ONLINE (312 events, 2 dead letters)
├─ Tablet 7 (Chef):       🟢 ONLINE (189 events, 0 errors)
└─ Tablet 8 (Chef):       🟡 OFFLINE (last seen 12 min ago)

PERFORMANCE METRICS:
├─ Average Latency:       87 ms (target: <100 ms) ✓
├─ P99 Latency:           245 ms (max: 500 ms) ✓
├─ Queue Drain Rate:      12.3 items/min (steady)
├─ Sync Success Rate:     99.8% (1 out of 500 failed)
├─ Message Loss Rate:     0.0% (guaranteed by idempotency)
└─ Conflict Resolution:   8 events (all resolved correctly)

NETWORK HEALTH:
├─ WiFi Signal:           -55 dBm (excellent)
├─ P2P Mesh:              7/8 tablets connected
├─ BLE Backup:            Available (not needed)
├─ Disconnects Today:     2 (Tablet 4 WiFi flake)
└─ Recovery Time Avg:     3.2 seconds

BUSINESS IMPACT:
├─ Orders Processed:      487
├─ Total Revenue:         487,500 Kyat
├─ Failed Payments:       0 (0%)
├─ Double-Charges:        0 (0%)
├─ Conflict Resolutions:  8 (all correct)
└─ Customer Complaints:   0

TOP ISSUES:
1. ⚠️  Tablet 4 WiFi intermittent (5 reconnects) → IT: Check WiFi AP location
2. ⚠️  Dead letter vault: 2 items (network timeouts) → Manager reviewed, resynced
3. 📊 Tablet 8 offline 12 min → Unknown cause, needs investigation
```

---

## PERFORMANCE ANALYSIS QUERY

### Find Slow Operations

```sql
-- Identify which operations are slowest
SELECT 
    operation_type,
    device_id,
    COUNT(*) as num_operations,
    AVG(latency_ms) as avg_latency_ms,
    MAX(latency_ms) as max_latency_ms,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY latency_ms) as p95_latency_ms
FROM beta_feedback_events
WHERE timestamp > (strftime('%s', 'now') - 3600) * 1000  -- Last hour
GROUP BY operation_type, device_id
ORDER BY avg_latency_ms DESC;

-- Result Example:
-- operation_type  device_id  num  avg_latency  max_latency  p95_latency
-- ORDER_CREATE    TABLET_2   42   156 ms       523 ms       287 ms
-- PAYMENT         TABLET_5   18    94 ms       234 ms       178 ms
-- CHIEF_CLAIM     TABLET_1   67    23 ms        67 ms        45 ms
```

### Detect Network Issues

```sql
-- Find devices with connectivity problems
SELECT 
    device_id,
    device_name,
    COUNT(*) as disconnect_count,
    AVG(recovery_time_seconds) as avg_recovery_sec,
    MAX(recovery_time_seconds) as max_recovery_sec
FROM network_events
WHERE event_type LIKE '%DISCONNECT%'
  AND timestamp > (strftime('%s', 'now') - 86400) * 1000  -- Last 24 hours
GROUP BY device_id
HAVING disconnect_count > 2
ORDER BY disconnect_count DESC;

-- Result Example:
-- device_id   device_name      disconnects  avg_recovery  max_recovery
-- TABLET_4    Waiter Station 2      5        3.2 sec      8.7 sec
-- TABLET_8    Chef Station 1        1        2.1 sec      2.1 sec
```

### Analyze Conflict Resolutions

```sql
-- Review all conflict resolutions today
SELECT 
    resolution_id,
    timestamp,
    device_id,
    order_id,
    local_status,
    remote_status,
    decision_type,
    winning_status,
    CASE 
        WHEN decision_type = 'STATUS_PRIORITY' THEN 'Priority-based'
        WHEN decision_type = 'VERSION' THEN 'Version-based'
        WHEN decision_type = 'NANOSECOND_TIEBREAKER' THEN 'Nanosecond precision'
    END as resolution_method
FROM conflict_resolutions
WHERE DATE(timestamp / 1000, 'unixepoch') = DATE('now')
ORDER BY timestamp DESC;

-- Result Example:
-- resolution_id  time  device  order  local    remote   decision    winning  method
-- 142            14:30 T_001   1042   PENDING  VOID     PRIORITY    VOID     Priority-based
-- 143            14:31 T_002   1043   READY    IN_PROG  VERSION     READY    Version-based
-- 144            14:32 T_003   1044   PAID     PAID     NANOTIME    PAID     Nanosecond
```

### Dead Letter Vault Health

```sql
-- Check unresolved dead letter items
SELECT 
    event_id,
    timestamp,
    device_id,
    message_id,
    message_type,
    failure_reason,
    failure_count,
    affected_order_id,
    affected_amount_kyat,
    CASE WHEN recovered THEN '✓ Recovered' ELSE '⚠️ Pending' END as status
FROM dead_letter_events
WHERE recovered = FALSE
  AND timestamp > (strftime('%s', 'now') - 86400) * 1000  -- Last 24 hours
ORDER BY timestamp DESC;

-- Result Example (should be empty for healthy system):
-- event_id  time    device  message_type  reason            failure_count  status
-- (no results = system healthy!)
```

---

## ALERT THRESHOLDS

### Critical Alerts (Immediate Action Required)

| Metric | Threshold | Action |
|--------|-----------|--------|
| **Latency (P99)** | > 2 seconds | Check network/database |
| **Failed Sync Rate** | > 5% | Investigate mesh network |
| **Dead Letter Vault** | > 5 unresolved | Manager review required |
| **Double-Charge Rate** | > 0 | EMERGENCY - System shutdown |
| **Tablet Offline** | > 30 minutes | Device troubleshooting |

### Warning Alerts (Monitor & Investigate)

| Metric | Threshold | Action |
|--------|-----------|--------|
| **Latency (P95)** | > 500ms | Monitor next hour |
| **Conflict Resolutions** | > 20/hour | Check for data inconsistencies |
| **Network Disconnects** | > 3/hour | Investigate WiFi AP |
| **Queue Stuck Items** | > 5 | Check dead letter vault |
| **Device Crashes** | > 1/day | App logs review |

---

## FEEDBACK COLLECTION PROTOCOL

### Automatic Logging (Every Transaction)
- All operations automatically logged to `beta_feedback_events`
- No user action required
- Includes metrics even on success (baseline)

### Waiter Manual Feedback
```
After completing order:
    "Did you experience any issues?" [Yes] [No]
    
If Yes:
    ├─ "What was the issue?"
    │  ├─ Slow response
    │  ├─ Order didn't sync
    │  ├─ Screen froze
    │  ├─ Payment failed
    │  └─ Other
    ├─ "How severe?" (1-5 stars)
    └─ Additional notes (text)
```

### Manager Daily Report
```
Manager Dashboard → Reports → Beta Feedback
├─ Summary of top issues
├─ Performance metrics
├─ Device health status
├─ Conflict resolutions
└─ Recommended actions
```

---

## EXAMPLE FEEDBACK DATA

### Tablet 4 - Intermittent Issue

```
Device: TABLET_4 (Waiter Station 2)
Issue: WiFi keeps disconnecting during peak hours

Timeline:
├─ 12:45:23  WiFi DISCONNECT        signal -78 dBm
├─ 12:45:26  Failed to sync (Order #1078)
├─ 12:45:27  WiFi RECONNECT         recovery time 4.2s
├─ 12:45:28  Queued message synced  ✓
├─ 13:02:15  WiFi DISCONNECT        signal -82 dBm
├─ 13:02:19  Failed to sync (Order #1095)
├─ 13:02:20  BLE Backup activated
├─ 13:02:25  WiFi RECONNECT         recovery time 5.1s
└─ 13:02:26  Queued message synced  ✓

Recommendation: Reposition WiFi AP closer to Waiter Station 2
Impact: Zero customer impact (backups worked)
```

### Conflict Resolution Event

```
Order #1042 - Manager Override

Timeline:
├─ 14:30:00  Order created (Waiter Ahmed)
├─ 14:30:00  Status: PENDING        (priority 10)
├─ 14:35:15  Manager Kyaw updates   (wants to void)
├─ 14:35:15  Status: VOID           (priority 100)
│
│ CONFLICT DETECTED:
│ ├─ Local status (Tablet 2):   PENDING (Ahmed)
│ ├─ Remote status (Tablet 3):  VOID    (Kyaw)
│ ├─ Decision Type: STATUS_PRIORITY
│ ├─ Local Priority: 10
│ ├─ Remote Priority: 100
│ └─ Winner: VOID (Manager's authority enforced)
│
└─ Result: ✓ Correct resolution (Manager supremacy maintained)
```

---

## END-OF-BETA CHECKLIST

### Before Production Deployment

- [ ] Review all alerts from beta feedback
- [ ] Confirm zero double-charges in database
- [ ] Verify all conflict resolutions were correct
- [ ] Check dead letter vault is empty (or reviewed)
- [ ] Device uptime > 95% on all tablets
- [ ] Network stability: < 5 disconnects/day
- [ ] Average latency < 150ms
- [ ] Sync success rate > 99.5%
- [ ] No unresolved customer complaints
- [ ] Manager sign-off on all changes

---

**Feedback System Active Since**: February 5, 2026  
**Next Review**: February 7, 2026 (48-hour checkpoint)  
**Production Deployment Target**: February 15, 2026
