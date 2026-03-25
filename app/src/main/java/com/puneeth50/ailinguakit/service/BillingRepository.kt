package com.puneeth50.ailinguakit.service

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.puneeth50.ailinguakit.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_PREMIUM_CACHED = "premium_cached"
private const val PREMIUM_INAPP_ID = "linguakit_premium"
private const val PREMIUM_SUBS_ID = "linguakit_premium_sub"

data class PremiumProduct(
    val productDetails: ProductDetails,
    val productType: String,
    val formattedPrice: String
)

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val prefs: SharedPreferences
) {
    private val appContext = context.applicationContext
    private val _isPremium = MutableStateFlow(prefs.getBoolean(PREF_PREMIUM_CACHED, false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _premiumProduct = MutableStateFlow<PremiumProduct?>(null)
    val premiumProduct: StateFlow<PremiumProduct?> = _premiumProduct.asStateFlow()

    private val _billingReady = MutableStateFlow(false)
    val billingReady: StateFlow<Boolean> = _billingReady.asStateFlow()

    private val _billingMessage = MutableStateFlow<String?>(null)
    val billingMessage: StateFlow<String?> = _billingMessage.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when {
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                purchases.forEach(::handlePurchase)
            }
            billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingMessage.value = string(R.string.purchase_canceled)
            }
            billingResult.responseCode != BillingClient.BillingResponseCode.OK -> {
                _billingMessage.value = billingResult.debugMessage.ifBlank { string(R.string.purchase_failed) }
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        connect()
    }

    private fun connect() {
        if (billingClient.isReady) {
            _billingReady.value = true
            refreshPurchases()
            queryPremiumProduct()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingReady.value = true
                    refreshPurchases()
                    queryPremiumProduct()
                } else {
                    _billingReady.value = false
                    _billingMessage.value = billingResult.debugMessage.ifBlank { string(R.string.billing_unavailable) }
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingReady.value = false
            }
        })
    }

    fun refreshPurchases() {
        if (!billingClient.isReady) {
            connect()
            return
        }

        queryPurchases(BillingClient.ProductType.INAPP) { hasInApp ->
            queryPurchases(BillingClient.ProductType.SUBS) { hasSubs ->
                persistPremiumState(hasInApp || hasSubs)
            }
        }
    }

    fun launchPurchase(activity: Activity): Boolean {
        val product = _premiumProduct.value ?: run {
            _billingMessage.value = string(R.string.premium_purchase_not_available)
            return false
        }
        if (!billingClient.isReady) {
            connect()
            _billingMessage.value = string(R.string.billing_connecting_try_again)
            return false
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product.productDetails)

        if (product.productType == BillingClient.ProductType.SUBS) {
            val offerToken = product.productDetails.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerToken
                ?: return false
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
        return true
    }

    fun clearBillingMessage() {
        _billingMessage.value = null
    }

    private fun queryPremiumProduct() {
        queryProductType(
            productId = PREMIUM_SUBS_ID,
            productType = BillingClient.ProductType.SUBS
        )
        queryProductType(
            productId = PREMIUM_INAPP_ID,
            productType = BillingClient.ProductType.INAPP
        )
    }

    private fun queryProductType(productId: String, productType: String) {
        if (!billingClient.isReady) return

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(productType)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val details = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val price = when (productType) {
                BillingClient.ProductType.SUBS -> details.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice
                else -> details.oneTimePurchaseOfferDetails?.formattedPrice
            } ?: string(R.string.see_play_store)

            if (_premiumProduct.value == null || productType == BillingClient.ProductType.SUBS) {
                _premiumProduct.value = PremiumProduct(details, productType, price)
            }
        }
    }

    private fun queryPurchases(productType: String, onResult: (Boolean) -> Unit) {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onResult(purchases.any(::isPremiumPurchase))
            } else {
                onResult(_isPremium.value)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!isPremiumPurchase(purchase)) return

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            persistPremiumState(true)

            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        _billingMessage.value = result.debugMessage.ifBlank {
                            string(R.string.premium_purchase_acknowledged_locally)
                        }
                    }
                }
            }
        }
    }

    private fun isPremiumPurchase(purchase: Purchase): Boolean {
        return purchase.products.any { it == PREMIUM_INAPP_ID || it == PREMIUM_SUBS_ID } &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
    }

    private fun persistPremiumState(isPremium: Boolean) {
        prefs.edit().putBoolean(PREF_PREMIUM_CACHED, isPremium).apply()
        _isPremium.value = isPremium
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    private fun string(@StringRes resId: Int, vararg args: Any): String {
        return appContext.getString(resId, *args)
    }
}
