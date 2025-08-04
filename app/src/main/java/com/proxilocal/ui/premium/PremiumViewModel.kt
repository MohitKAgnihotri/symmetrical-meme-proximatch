package com.proxilocal.ui.premium

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// A more robust state holder for the screen
sealed class PremiumScreenState {
    object Loading : PremiumScreenState()
    data class Success(val product: PremiumProduct) : PremiumScreenState()
    object Subscribed : PremiumScreenState()
    data class Error(val message: String) : PremiumScreenState()
}

data class PremiumProduct(
    val productDetails: ProductDetails,
    val formattedPrice: String
)

class PremiumViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PremiumScreenState>(PremiumScreenState.Loading)
    val uiState = _uiState.asStateFlow()

    private lateinit var billingClient: BillingClient
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            _uiState.value = PremiumScreenState.Error("Subscription failed. Code: ${billingResult.responseCode}")
        }
    }

    fun initializeBilling() {
        _uiState.value = PremiumScreenState.Loading
        billingClient = BillingClient.newBuilder(getApplication())
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("PremiumViewModel", "Billing client setup finished.")
                    viewModelScope.launch {
                        checkSubscriptionStatus()
                    }
                } else {
                    _uiState.value = PremiumScreenState.Error("Billing client setup failed. Code: ${billingResult.responseCode}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w("PremiumViewModel", "Billing client disconnected.")
            }
        })
    }

    private suspend fun checkSubscriptionStatus() {
        val purchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        val isSubscribed = purchasesResult.purchasesList.any { it.products.contains("proximatch_premium_monthly") && it.isAcknowledged }

        if (isSubscribed) {
            _uiState.value = PremiumScreenState.Subscribed
        } else {
            queryAvailableProducts()
        }
    }

    private fun queryAvailableProducts() {
        val productList = listOf(QueryProductDetailsParams.Product.newBuilder()
            .setProductId("proximatch_premium_monthly")
            .setProductType(BillingClient.ProductType.SUBS).build())
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

        billingClient.queryProductDetailsAsync(params.build()) { _, productDetailsList ->
            if (productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val price = productDetails.subscriptionOfferDetails?.first()?.pricingPhases?.pricingPhaseList?.first()?.formattedPrice
                _uiState.value = PremiumScreenState.Success(PremiumProduct(productDetails, price ?: "N/A"))
            } else {
                _uiState.value = PremiumScreenState.Error("No products found. Check your Play Console setup.")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val currentState = _uiState.value
        if (currentState !is PremiumScreenState.Success) return

        val productDetails = currentState.product.productDetails
        val offerToken = productDetails.subscriptionOfferDetails?.first()?.offerToken ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _uiState.value = PremiumScreenState.Subscribed
                }
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