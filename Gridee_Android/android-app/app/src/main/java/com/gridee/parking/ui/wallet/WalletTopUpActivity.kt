package com.gridee.parking.ui.wallet

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gridee.parking.data.api.ApiClient
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import kotlinx.coroutines.launch

class WalletTopUpActivity : AppCompatActivity(), PaymentResultListener {

    private var userId: String = ""
    private var amount: Double = 0.0
    private var orderId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = intent.getStringExtra("USER_ID") ?: ""
        amount = intent.getDoubleExtra("AMOUNT", 0.0)
        orderId = intent.getStringExtra("ORDER_ID") ?: ""

        if (userId.isBlank() || amount <= 0.0 || orderId.isBlank()) {
            Toast.makeText(this, "Invalid payment data", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        startCheckout()
    }

    private fun startCheckout() {
        try {
            // Preload checkout for faster launch
            Checkout.preload(applicationContext)

            val checkout = Checkout()
            // Optional: set your key id if you want on client; otherwise Razorpay reads from options
            // For explicitness, pass key id from backend config if available (public key)
            // Note: We don't fetch it dynamically here; server validates payment.

            val options = JSONObject()
            options.put("name", "Gridee")
            options.put("description", "Wallet Top-up")
            options.put("currency", "INR")
            options.put("order_id", orderId)
            options.put("amount", (amount * 100).toInt())

            // Optionally set key explicitly if backend included it in the intent
            val keyFromServer = intent.getStringExtra("KEY_ID")
            val keyToUse = if (!keyFromServer.isNullOrBlank()) keyFromServer else getString(com.gridee.parking.R.string.razorpay_key_id)
            try { checkout.setKeyID(keyToUse) } catch (_: Exception) {}
            // Also include key in options for compatibility with older SDKs
            options.put("key", keyToUse)

            // Prefill details (if available)
            val shared = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
            val email = shared.getString("user_email", null)
            val phone = shared.getString("user_phone", null)

            val prefill = JSONObject()
            email?.let { prefill.put("email", it) }
            phone?.let { prefill.put("contact", it) }
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Checkout error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        // Inform backend to credit wallet
        lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.paymentCallback(
                    com.gridee.parking.data.model.PaymentCallbackRequest(
                        orderId = orderId,
                        paymentId = (razorpayPaymentId ?: ""),
                        success = true,
                        userId = userId,
                        amount = amount
                    )
                )
                if (!resp.isSuccessful) {
                    Toast.makeText(this@WalletTopUpActivity, "Payment success, update failed", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@WalletTopUpActivity, "Wallet recharged successfully", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WalletTopUpActivity, "Callback error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                finish()
            }
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        // Inform backend of failed payment (optional)
        lifecycleScope.launch {
            try {
                ApiClient.apiService.paymentCallback(
                    com.gridee.parking.data.model.PaymentCallbackRequest(
                        orderId = orderId,
                        paymentId = (response ?: ""),
                        success = false,
                        userId = userId,
                        amount = amount
                    )
                )
            } catch (_: Exception) {
            } finally {
                Toast.makeText(this@WalletTopUpActivity, "Payment failed", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
