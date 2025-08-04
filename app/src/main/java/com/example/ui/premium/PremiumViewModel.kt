package com.example.ui.premium

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// A simple data class to hold product info.
data class PremiumProduct(
    val productDetails: ProductDetails,
    val formattedPrice: String
)

class PremiumViewModel(application: Application) : AndroidViewModel(application) {

    private val _product = MutableStateFlow<PremiumProduct?>(null)
    val product = _product.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed = _isSubscribed.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private lateinit var billingClient: BillingClient
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            _error.value = "Subscription failed. Code: ${billingResult.responseCode}"
        }
    }

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(getApplication())
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("PremiumViewModel", "Billing client setup finished.")
                    queryAvailableProducts()
                    checkSubscriptionStatus()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("PremiumViewModel", "Billing client disconnected.")
                // Implement your own retry policy here.
            }
        })
    }

    private fun queryAvailableProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("proximatch_premium_monthly") // Replace with your actual product ID from Google Play Console
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

        billingClient.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val price = productDetails.subscriptionOfferDetails?.first()?.pricingPhases?.pricingPhaseList?.first()?.formattedPrice
                _product.value = PremiumProduct(productDetails, price ?: "N/A")
            }
        }
    }

    private fun checkSubscriptionStatus() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { _, purchases ->
            _isSubscribed.value = purchases.any { it.products.contains("proximatch_premium_monthly") && it.isAcknowledged }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val currentProduct = _product.value?.productDetails ?: return
        val offerToken = currentProduct.subscriptionOfferDetails?.first()?.offerToken ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(currentProduct)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isSubscribed.value = true
                    }
                }
            } else {
                _isSubscribed.value = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
}