# 📊 FINAL RECONCILIATION SQL QUERIES

## Purpose
Verify that all 8 tablets have synchronized correctly at closing time by checking:
1. All `message_id` values match across tablets
2. No orphaned messages in `SyncQueueEntity`
3. No unprocessed messages in `ProcessedMessagesEntity`
4. Revenue totals match across all devices

---

## Query 1: Verify Sync Queue is Empty

### Purpose
Ensure all pending messages have been synchronized successfully.

### SQL Query
```sql
-- Check for any pending messages in sync queue
SELECT 
    COUNT(*) as pending_count,
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_messages,
    SUM(CASE WHEN status = 'SYNCING' THEN 1 ELSE 0 END) as syncing_messages,
    SUM(CASE WHEN retry_count >= 5 THEN 1 ELSE 0 END) as failed_messages
FROM sync_queue
WHERE status != 'SYNCED';
```

### Expected Result
```
pending_count | pending_messages | syncing_messages | failed_messages
--------------|------------------|------------------|----------------
     0        |        0         |        0         |       0
```

### Action if Non-Zero
- **pending_messages > 0**: Wait 2 minutes for sync to complete
- **syncing_messages > 0**: Check WiFi connection
- **failed_messages > 0**: Check Dead Letter Vault (requires Manager)

---

## Query 2: Verify All Orders Have Unique message_id

### Purpose
Ensure no duplicate `message_id` values exist (anti-double-billing verification).

### SQL Query
```sql
-- Check for duplicate message IDs in processed_messages
SELECT 
    message_id,
    COUNT(*) as occurrence_count,
    GROUP_CONCAT(sender_id) as sender_devices
FROM processed_messages
GROUP BY message_id
HAVING COUNT(*) > 1;
```

### Expected Result
```
(Empty result set - no duplicates)
```

### Action if Duplicates Found
- **CRITICAL ERROR**: This should NEVER happen
- Contact IT immediately
- Do NOT close reconciliation until resolved
- Document all duplicate `message_id` values

---

## Query 3: Cross-Tablet Message Count Verification

### Purpose
Verify all tablets have processed the same number of messages.

### SQL Query (Run on EACH Tablet)
```sql
-- Get total processed messages count
SELECT 
    COUNT(DISTINCT message_id) as total_messages,
    COUNT(DISTINCT sender_id) as unique_senders,
    MIN(processed_at) as first_message_time,
    MAX(processed_at) as last_message_time
FROM processed_messages
WHERE DATE(processed_at / 1000, 'unixepoch') = DATE('now');
```

### Expected Result (Example)
```
Tablet 1: total_messages = 156, unique_senders = 8
Tablet 2: total_messages = 156, unique_senders = 8
Tablet 3: total_messages = 156, unique_senders = 8
...
Tablet 8: total_messages = 156, unique_senders = 8
```

### Action if Mismatch
- Identify which tablet has different count
- Check that tablet's sync queue
- Verify WiFi connection was stable
- May need to re-sync from master tablet

---

## Query 4: Revenue Reconciliation

### Purpose
Verify total revenue matches across all tablets.

### SQL Query
```sql
-- Calculate total revenue for the day
SELECT 
    COUNT(*) as total_orders,
    SUM(CASE WHEN status = 'PAID' THEN total_amount ELSE 0 END) as total_revenue,
    SUM(CASE WHEN status = 'VOID' THEN total_amount ELSE 0 END) as voided_amount,
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_orders
FROM orders
WHERE DATE(created_at / 1000, 'unixepoch') = DATE('now');
```

### Expected Result (Example - All Tablets Should Match)
```
total_orders | total_revenue | voided_amount | pending_orders
-------------|---------------|---------------|---------------
    156      |   487,500     |    12,000     |       0
```

### Action if Mismatch
- Compare `total_revenue` across all 8 tablets
- Identify discrepancies
- Check for orders with different `status` values
- Verify Manager VOID operations were synced

---

## Query 5: Dead Letter Vault Check

### Purpose
Identify any failed sync messages that require Manager review.

### SQL Query
```sql
-- Check dead letter vault for unresolved messages
SELECT 
    COUNT(*) as total_dead_letters,
    SUM(CASE WHEN requires_manager_review = 1 THEN 1 ELSE 0 END) as needs_review,
    SUM(CASE WHEN resolved_at IS NULL THEN 1 ELSE 0 END) as unresolved,
    GROUP_CONCAT(DISTINCT failure_reason) as failure_reasons
FROM dead_letter_vault
WHERE DATE(created_at / 1000, 'unixepoch') = DATE('now');
```

### Expected Result
```
total_dead_letters | needs_review | unresolved | failure_reasons
-------------------|--------------|------------|----------------
         0         |      0       |     0      |     NULL
```

### Action if Non-Zero
- **REQUIRES MANAGER REVIEW**
- Manager must inspect each dead letter
- Determine if manual reprocessing is needed
- Document failure reasons for IT analysis

---

## Query 6: Idempotency Gate Verification

### Purpose
Verify that the idempotency gate prevented any duplicate processing.

### SQL Query
```sql
-- Check for any message_id that appears in both processed_messages and sync_queue
SELECT 
    sq.message_id,
    sq.message_type,
    sq.status as queue_status,
    pm.processed_at
FROM sync_queue sq
INNER JOIN processed_messages pm ON sq.message_id = pm.message_id
WHERE sq.status = 'PENDING';
```

### Expected Result
```
(Empty result set - no duplicates)
```

### Interpretation
- **Empty result**: Idempotency gate working correctly
- **Non-empty result**: Message was processed but still in queue (investigate)

---

## Query 7: Manager Override Verification

### Purpose
Verify that Manager VOID operations correctly overrode Waiter PENDING orders.

### SQL Query
```sql
-- Check for any orders that were voided by Manager
SELECT 
    o.order_id,
    o.order_number,
    o.status,
    o.version,
    o.updated_at,
    u.name as last_updated_by,
    u.role_level
FROM orders o
LEFT JOIN users u ON o.updated_by = u.user_id
WHERE o.status = 'VOID'
  AND DATE(o.updated_at / 1000, 'unixepoch') = DATE('now')
ORDER BY o.updated_at DESC;
```

### Expected Result (Example)
```
order_id | order_number | status | version | updated_at  | last_updated_by | role_level
---------|--------------|--------|---------|-------------|-----------------|------------
  1042   |   ORD-1042   | VOID   |    3    | 1707043200  | Kyaw (Manager)  | MANAGER
  1038   |   ORD-1038   | VOID   |    2    | 1707042800  | Soe (Owner)     | OWNER
```

### Verification
- All VOID orders should have `role_level = 'MANAGER'` or `'OWNER'`
- If any VOID by `'STAFF'`, investigate (should not be possible)

---

## Query 8: Conflict Resolution Audit

### Purpose
Identify any orders that had version conflicts (multiple updates).

### SQL Query
```sql
-- Find orders with version > 1 (indicating conflict resolution occurred)
SELECT 
    order_id,
    order_number,
    status,
    version,
    created_at,
    updated_at,
    (updated_at - created_at) / 1000 as seconds_to_final_status
FROM orders
WHERE version > 1
  AND DATE(created_at / 1000, 'unixepoch') = DATE('now')
ORDER BY version DESC;
```

### Expected Result (Example)
```
order_id | order_number | status | version | seconds_to_final_status
---------|--------------|--------|---------|------------------------
  1042   |   ORD-1042   | VOID   |    3    |        120
  1035   |   ORD-1035   | PAID   |    2    |         45
```

### Interpretation
- `version > 1`: Order was updated multiple times (normal)
- High version numbers (>5): May indicate sync issues or frequent changes

---

## CLOSING TIME RECONCILIATION CHECKLIST

### Step 1: Run Sync Queue Check (Query 1)
```bash
✅ pending_count = 0
✅ failed_messages = 0
```

### Step 2: Run Duplicate Check (Query 2)
```bash
✅ No duplicate message_id found
```

### Step 3: Run Cross-Tablet Verification (Query 3)
```bash
✅ All 8 tablets have same total_messages count
```

### Step 4: Run Revenue Reconciliation (Query 4)
```bash
✅ All 8 tablets have same total_revenue
✅ pending_orders = 0
```

### Step 5: Run Dead Letter Check (Query 5)
```bash
✅ total_dead_letters = 0
OR
⚠️ Manager reviewed and resolved all dead letters
```

### Step 6: Run Idempotency Verification (Query 6)
```bash
✅ No message_id in both processed_messages and sync_queue
```

### Step 7: Run Manager Override Verification (Query 7)
```bash
✅ All VOID orders by MANAGER or OWNER only
```

### Step 8: Run Conflict Resolution Audit (Query 8)
```bash
✅ Version conflicts resolved correctly
```

---

## AUTOMATED RECONCILIATION SCRIPT

### SQL Script (Run at Closing Time)
```sql
-- COSMIC FORGE POS - CLOSING TIME RECONCILIATION
-- Run this script on the MASTER tablet (Tablet 1)

.mode column
.headers on

-- Header
SELECT '========================================' as '';
SELECT 'COSMIC FORGE POS - DAILY RECONCILIATION' as '';
SELECT 'Date: ' || DATE('now') as '';
SELECT '========================================' as '';
SELECT '' as '';

-- 1. Sync Queue Status
SELECT '1. SYNC QUEUE STATUS' as '';
SELECT '-------------------' as '';
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ PASS: Sync queue is empty'
        ELSE '❌ FAIL: ' || COUNT(*) || ' messages pending'
    END as result
FROM sync_queue
WHERE status != 'SYNCED';
SELECT '' as '';

-- 2. Duplicate Message Check
SELECT '2. DUPLICATE MESSAGE CHECK' as '';
SELECT '-------------------------' as '';
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ PASS: No duplicate message_id found'
        ELSE '❌ CRITICAL: ' || COUNT(*) || ' duplicate message_id found'
    END as result
FROM (
    SELECT message_id, COUNT(*) as cnt
    FROM processed_messages
    GROUP BY message_id
    HAVING cnt > 1
);
SELECT '' as '';

-- 3. Revenue Total
SELECT '3. REVENUE RECONCILIATION' as '';
SELECT '------------------------' as '';
SELECT 
    '✅ Total Orders: ' || COUNT(*) as result
FROM orders
WHERE DATE(created_at / 1000, 'unixepoch') = DATE('now');

SELECT 
    '✅ Total Revenue: ' || SUM(CASE WHEN status = 'PAID' THEN total_amount ELSE 0 END) || ' Kyat' as result
FROM orders
WHERE DATE(created_at / 1000, 'unixepoch') = DATE('now');

SELECT 
    '✅ Voided Amount: ' || SUM(CASE WHEN status = 'VOID' THEN total_amount ELSE 0 END) || ' Kyat' as result
FROM orders
WHERE DATE(created_at / 1000, 'unixepoch') = DATE('now');
SELECT '' as '';

-- 4. Dead Letter Vault
SELECT '4. DEAD LETTER VAULT' as '';
SELECT '-------------------' as '';
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ PASS: No dead letters'
        ELSE '⚠️ WARNING: ' || COUNT(*) || ' dead letters (Manager review required)'
    END as result
FROM dead_letter_vault
WHERE resolved_at IS NULL;
SELECT '' as '';

-- 5. Final Status
SELECT '========================================' as '';
SELECT 'RECONCILIATION COMPLETE' as '';
SELECT 'Timestamp: ' || DATETIME('now') as '';
SELECT '========================================' as '';
```

### How to Run
```bash
# On Tablet 1 (Master)
adb shell
cd /data/data/com.cosmicforge.rms/databases
sqlite3 cosmic_forge_pos.db < reconciliation.sql
```

---

**Last Updated:** February 5, 2026  
**For Support:** CosmicForge IT Team
