package com.cosmicforge.pos.ui.payment

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for payment processing with proof capture
 */
@HiltViewModel
class PaymentViewModel @Inject constructor() : ViewModel() {
    
    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val selectedPaymentMethod: StateFlow<PaymentMethod> = _selectedPaymentMethod.asStateFlow()
    
    private val _paymentProofUri = MutableStateFlow<Uri?>(null)
    val paymentProofUri: StateFlow<Uri?> = _paymentProofUri.asStateFlow()
    
    private val _amountReceived = MutableStateFlow("")
    val amountReceived: StateFlow<String> = _amountReceived.asStateFlow()
    
    /**
     * Select payment method
     */
    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
        
        // Reset proof if switching away from digital payments
        if (method == PaymentMethod.CASH) {
            _paymentProofUri.value = null
        }
    }
    
    /**
     * Set payment proof (screenshot for KPay/CBPay)
     */
    fun setPaymentProof(uri: Uri?) {
        _paymentProofUri.value = uri
    }
    
    /**
     * Set amount received
     */
    fun setAmountReceived(amount: String) {
        _amountReceived.value = amount
    }
    
    /**
     * Calculate change
     */
    fun calculateChange(totalAmount: Double): Double {
        val received = _amountReceived.value.toDoubleOrNull() ?: 0.0
        return (received - totalAmount).coerceAtLeast(0.0)
    }
    
    /**
     * Validate payment
     */
    fun validatePayment(totalAmount: Double): PaymentValidation {
        return when (_selectedPaymentMethod.value) {
            PaymentMethod.CASH -> {
                val received = _amountReceived.value.toDoubleOrNull() ?: 0.0
                if (received >= totalAmount) {
                    PaymentValidation.Valid
                } else {
                    PaymentValidation.Invalid("Insufficient amount")
                }
            }
            PaymentMethod.KPAY, PaymentMethod.CBPAY -> {
                if (_paymentProofUri.value != null) {
                    PaymentValidation.Valid
                } else {
                    PaymentValidation.Invalid("Payment proof required")
                }
            }
        }
    }
    
    /**
     * Process payment
     */
    fun processPayment(
        orderId: Long,
        totalAmount: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val validation = validatePayment(totalAmount)) {
                is PaymentValidation.Valid -> {
                    // Payment processing logic here
                    onSuccess()
                }
                is PaymentValidation.Invalid -> {
                    onError(validation.reason)
                }
            }
        }
    }
}

/**
 * Payment methods
 */
enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    KPAY("KPay"),
    CBPAY("CB Pay")
}

/**
 * Payment validation result
 */
sealed class PaymentValidation {
    object Valid : PaymentValidation()
    data class Invalid(val reason: String) : PaymentValidation()
}
