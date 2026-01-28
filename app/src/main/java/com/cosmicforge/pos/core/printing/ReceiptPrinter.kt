package com.cosmicforge.pos.core.printing

import com.cosmicforge.pos.data.database.entities.OrderEntity
import com.cosmicforge.pos.data.database.entities.OrderDetailEntity
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Receipt printer for 80mm and 58mm thermal printers
 */
@Singleton
class ReceiptPrinter @Inject constructor() {
    
    /**
     * Generate receipt for 80mm printer
     */
    fun generate80mmReceipt(
        order: OrderEntity,
        details: List<OrderDetailEntity>,
        shopName: String = "COSMIC FORGE POS"
    ): String {
        return buildString {
            // Header
            appendLine("═".repeat(48))
            appendLine(shopName.center(48))
            appendLine("═".repeat(48))
            appendLine()
            
            // Order info
            appendLine("Order: ${order.orderNumber}")
            appendLine("Date: ${formatDateTime(order.createdAt)}")
            appendLine("Type: ${if (order.orderType == "PARCEL") "PARCEL 📦" else "DINE-IN 🍽️"}")
            
            if (order.tableId != null) {
                appendLine("Table: ${order.tableId}")
            }
            
            if (order.customerName != null) {
                appendLine("Customer: ${order.customerName}")
            }
            
            appendLine()
            appendLine("─".repeat(48))
            
            // Items
            appendLine("QTY  ITEM${" ".repeat(29)}AMOUNT")
            appendLine("─".repeat(48))
            
            details.forEach { detail ->
                val qty = " ${detail.quantity}x ".padEnd(5)
                val itemName = detail.itemName.take(29).padEnd(29)
                val amount = "${detail.totalPrice.toInt()}".padStart(9)
                appendLine("$qty$itemName$amount")
            }
            
            appendLine("─".repeat(48))
            
            // Totals
            appendLine("Subtotal:${" ".repeat(29)}${order.subtotal.toInt()}".padStart(48))
            
            if (order.parcelFee > 0) {
                val feeLabel = if (order.customParcelFee != null) {
                    "Parcel Fee (Custom):"
                } else {
                    "Parcel Fee:"
                }
                appendLine("$feeLabel${" ".repeat(48 - feeLabel.length - order.parcelFee.toInt().toString().length)}${order.parcelFee.toInt()}")
            }
            
            if (order.tax > 0) {
                appendLine("Tax:${" ".repeat(35)}${order.tax.toInt()}".padStart(48))
            }
            
            if (order.discount > 0) {
                appendLine("Discount:${" ".repeat(30)}-${order.discount.toInt()}".padStart(48))
            }
            
            appendLine("═".repeat(48))
            appendLine("TOTAL:${" ".repeat(33)}${order.total.toInt()} MMK".padStart(48))
            appendLine("═".repeat(48))
            
            // Payment info
            appendLine()
            order.paymentMethod?.let { method ->
                appendLine("Payment: $method")
                
                // Payment proof status
                when (method) {
                    "KPAY", "CBPAY" -> {
                        appendLine("Payment Proof: ✓ VERIFIED")
                    }
                    "CASH" -> {
                        appendLine("Cash Payment")
                    }
                    else -> {
                        // Other payment methods
                    }
                }
            }
            
            appendLine()
            appendLine("─".repeat(48))
            appendLine("Thank You! ကျေးဇူးတင်ပါတယ်!".center(48))
            appendLine("Come Again! နောက်တစ်ကြိမ်လာပါနော်။".center(48))
            appendLine("─".repeat(48))
            appendLine()
            appendLine()
            appendLine()
        }
    }
    
    /**
     * Generate receipt for 58mm printer (narrower)
     */
    fun generate58mmReceipt(
        order: OrderEntity,
        details: List<OrderDetailEntity>,
        shopName: String = "COSMIC FORGE POS"
    ): String {
        return buildString {
            // Header
            appendLine("═".repeat(32))
            appendLine(shopName.center(32))
            appendLine("═".repeat(32))
            appendLine()
            
            // Order info
            appendLine("Order: ${order.orderNumber}")
            appendLine("Date: ${formatDateTime(order.createdAt)}")
            appendLine("Type: ${if (order.orderType == "PARCEL") "PARCEL" else "DINE-IN"}")
            
            if (order.tableId != null) {
                appendLine("Table: ${order.tableId}")
            }
            
            if (order.customerName != null) {
                appendLine("Cust: ${order.customerName}")
            }
            
            appendLine()
            appendLine("─".repeat(32))
            
            // Items
            details.forEach { detail ->
                appendLine("${detail.quantity}x ${detail.itemName}")
                appendLine("${" ".repeat(20)}${detail.totalPrice.toInt()} MMK")
            }
            
            appendLine("─".repeat(32))
            
            // Totals
            appendLine("Subtotal: ${order.subtotal.toInt()} MMK")
            
            if (order.parcelFee > 0) {
                val feeLabel = if (order.customParcelFee != null) {
                    "Parcel (Custom):"
                } else {
                    "Parcel Fee:"
                }
                appendLine("$feeLabel ${order.parcelFee.toInt()} MMK")
            }
            
            if (order.tax > 0) {
                appendLine("Tax: ${order.tax.toInt()} MMK")
            }
            
            if (order.discount > 0) {
                appendLine("Discount: -${order.discount.toInt()} MMK")
            }
            
            appendLine("═".repeat(32))
            appendLine("TOTAL: ${order.total.toInt()} MMK")
            appendLine("═".repeat(32))
            
            // Payment info
            appendLine()
            order.paymentMethod?.let { method ->
                appendLine("Payment: $method")
                
                when (method) {
                    "KPAY", "CBPAY" -> {
                        appendLine("Proof: ✓ VERIFIED")
                    }
                    else -> {
                        // Other payment methods
                    }
                }
            }
            
            appendLine()
            appendLine("─".repeat(32))
            appendLine("Thank You!".center(32))
            appendLine("ကျေးဇူးတင်ပါတယ်!".center(32))
            appendLine("─".repeat(32))
            appendLine()
            appendLine()
        }
    }
    
    /**
     * Format date time for receipt
     */
    private fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            .format(Date(timestamp))
    }
    
    /**
     * Center text
     */
    private fun String.center(width: Int): String {
        if (this.length >= width) return this
        val padding = (width - this.length) / 2
        return " ".repeat(padding) + this + " ".repeat(width - this.length - padding)
    }
}
