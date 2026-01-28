package com.cosmicforge.pos.data.repository

import com.cosmicforge.pos.data.database.dao.OrderDao
import com.cosmicforge.pos.data.database.dao.SecurityAuditDao
import com.cosmicforge.pos.data.database.entities.OrderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for analytics and reporting
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val securityAuditDao: SecurityAuditDao
) {
    
    /**
     * Get today's sales summary
     */
    suspend fun getTodaySalesSummary(): SalesSummary {
        val todayOrders = orderDao.getTodayOrdersSnapshot()
        val paidOrders = todayOrders.filter { it.paymentStatus == "PAID" }
        
        val cashSales = paidOrders
            .filter { it.paymentMethod == OrderEntity.PAYMENT_CASH }
            .sumOf { it.total }
        
        val kpaySales = paidOrders
            .filter { it.paymentMethod == OrderEntity.PAYMENT_KPAY }
            .sumOf { it.total }
        
        val cbpaySales = paidOrders
            .filter { it.paymentMethod == OrderEntity.PAYMENT_CBPAY }
            .sumOf { it.total }
        
        val totalSales = cashSales + kpaySales + cbpaySales
        val totalOrders = paidOrders.size
        
        return SalesSummary(
            totalSales = totalSales,
            cashSales = cashSales,
            kpaySales = kpaySales,
            cbpaySales = cbpaySales,
            totalOrders = totalOrders,
            averageOrderValue = if (totalOrders > 0) totalSales / totalOrders else 0.0
        )
    }
    
    /**
     * Get parcel fee adjustments for today
     * Critical for pricing integrity audit
     */
    suspend fun getTodayParcelAdjustments(): List<ParcelAdjustment> {
        val todayOrders = orderDao.getTodayOrdersSnapshot()
        
        return todayOrders
            .filter { it.customParcelFee != null }
            .map { order ->
                ParcelAdjustment(
                    orderId = order.orderId,
                    orderNumber = order.orderNumber,
                    defaultFee = 1000.0,
                    customFee = order.customParcelFee ?: 0.0,
                    difference = (order.customParcelFee ?: 0.0) - 1000.0,
                    adjustedBy = order.parcelFeeOverrideBy ?: "Unknown",
                    timestamp = order.createdAt,
                    customerName = order.customerName
                )
            }
    }
    
    /**
     * Generate end-of-day report
     */
    suspend fun generateEndOfDayReport(): EndOfDayReport {
        val salesSummary = getTodaySalesSummary()
        val parcelAdjustments = getTodayParcelAdjustments()
        
        val todayOrders = orderDao.getTodayOrdersSnapshot()
        val dineInOrders = todayOrders.count { it.orderType == "DINE_IN" }
        val parcelOrders = todayOrders.count { it.orderType == "PARCEL" }
        
        val totalParcelFees = todayOrders
            .filter { it.orderType == "PARCEL" }
            .sumOf { it.parcelFee }
        
        val totalParcelFeeAdjustments = parcelAdjustments.sumOf { it.difference }
        
        return EndOfDayReport(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            salesSummary = salesSummary,
            dineInCount = dineInOrders,
            parcelCount = parcelOrders,
            totalParcelFees = totalParcelFees,
            parcelAdjustments = parcelAdjustments,
            totalAdjustmentImpact = totalParcelFeeAdjustments,
            generatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Get chief performance for today
     */
    suspend fun getTodayChiefPerformance(): List<ChiefPerformance> {
        val performanceAudits = securityAuditDao.getTodayAuditsSnapshot()
            .filter { it.actionType == "CHIEF_PERFORMANCE" }
        
        return performanceAudits
            .groupBy { it.userId }
            .map { (chiefId, audits) ->
                val chiefName = audits.firstOrNull()?.userName ?: "Unknown"
                val completedDishes = audits.size
                
                // Extract cook times from actionDetails field
                val cookTimes = audits.mapNotNull { audit ->
                    audit.actionDetails?.let { details ->
                        // Parse "Item: X, Prep Time: Ys" format
                        val match = Regex("""Prep Time: (\d+)s""").find(details)
                        match?.groupValues?.get(1)?.toDoubleOrNull()?.div(60.0) // Convert to minutes
                    }
                }
                
                val avgCookTime = if (cookTimes.isNotEmpty()) {
                    cookTimes.average()
                } else {
                    0.0
                }
                
                ChiefPerformance(
                    chiefId = chiefId,
                    chiefName = chiefName,
                    dishesCompleted = completedDishes,
                    averageCookTimeMinutes = avgCookTime
                )
            }
            .sortedByDescending { it.dishesCompleted }
    }
}

/**
 * Sales summary
 */
data class SalesSummary(
    val totalSales: Double,
    val cashSales: Double,
    val kpaySales: Double,
    val cbpaySales: Double,
    val totalOrders: Int,
    val averageOrderValue: Double
)

/**
 * Parcel fee adjustment record
 */
data class ParcelAdjustment(
    val orderId: Long,
    val orderNumber: String,
    val defaultFee: Double,
    val customFee: Double,
    val difference: Double,
    val adjustedBy: String,
    val timestamp: Long,
    val customerName: String?
)

/**
 * End-of-day report
 */
data class EndOfDayReport(
    val date: String,
    val salesSummary: SalesSummary,
    val dineInCount: Int,
    val parcelCount: Int,
    val totalParcelFees: Double,
    val parcelAdjustments: List<ParcelAdjustment>,
    val totalAdjustmentImpact: Double,
    val generatedAt: Long
)

/**
 * Chief performance metrics
 */
data class ChiefPerformance(
    val chiefId: Long,
    val chiefName: String,
    val dishesCompleted: Int,
    val averageCookTimeMinutes: Double
)
