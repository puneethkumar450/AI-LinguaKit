package com.puneeth50.ailinguakit.service

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
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
private const val PREF_PREMIUM_PLAN_TYPE = "premium_plan_type"
private const val PREMIUM_INAPP_ID = "linguakit_premium"
private const val PREMIUM_SUBS_ID = "linguakit_premium_sub"

enum class PremiumPlanType {
    MONTHLY,
    YEARLY,
    LIFETIME
}

data class PremiumPlanOption(
    val type: PremiumPlanType,
    val productDetails: ProductDetails,
    val productType: String,
    val formattedPrice: String,
    val offerToken: String? = null,
    val billingPeriod: String? = null
)

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val prefs: SharedPreferences
) {
    private val appContext = context.applicationContext
    private val _isPremium = MutableStateFlow(prefs.getBoolean(PREF_PREMIUM_CACHED, false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _availablePremiumPlans = MutableStateFlow<List<PremiumPlanOption>>(emptyList())
    val availablePremiumPlans: StateFlow<List<PremiumPlanOption>> = _availablePremiumPlans.asStateFlow()

    private val _activePremiumPlanType = MutableStateFlow(loadCachedPlanType())
    val activePremiumPlanType: StateFlow<PremiumPlanType?> = _activePremiumPlanType.asStateFlow()

    private val _billingReady = MutableStateFlow(false)
    val billingReady: StateFlow<Boolean> = _billingReady.asStateFlow()

    private val _billingMessage = MutableStateFlow<String?>(null)
    val billingMessage: StateFlow<String?> = _billingMessage.asStateFlow()

    private var pendingPlanType: PremiumPlanType? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when {
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                purchases.forEach(::handlePurchase)
            }
            billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                pendingPlanType = null
                _billingMessage.value = string(R.string.purchase_canceled)
            }
            billingResult.responseCode != BillingClient.BillingResponseCode.OK -> {
                pendingPlanType = null
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
            queryPremiumPlans()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingReady.value = true
                    refreshPurchases()
                    queryPremiumPlans()
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

        queryPurchases(BillingClient.ProductType.INAPP) { inAppPurchase, inAppSuccess ->
            queryPurchases(BillingClient.ProductType.SUBS) { subPurchase, subSuccess ->
                if (!inAppSuccess || !subSuccess) {
                    if (!_isPremium.value) {
                        persistPremiumState(false, null)
                    }
                    return@queryPurchases
                }

                when {
                    inAppPurchase != null -> persistPremiumState(true, PremiumPlanType.LIFETIME)
                    subPurchase != null -> persistPremiumState(true, resolveActiveSubscriptionPlanType())
                    else -> persistPremiumState(false, null)
                }
            }
        }
    }

    fun launchPurchase(activity: Activity, planType: PremiumPlanType): Boolean {
        val plan = _availablePremiumPlans.value.firstOrNull { it.type == planType } ?: run {
            _billingMessage.value = string(R.string.premium_purchase_not_available)
            return false
        }
        if (!billingClient.isReady) {
            connect()
            _billingMessage.value = string(R.string.billing_connecting_try_again)
            return false
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(plan.productDetails)

        if (plan.productType == BillingClient.ProductType.SUBS) {
            val offerToken = plan.offerToken ?: return false
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        pendingPlanType = plan.type
        billingClient.launchBillingFlow(activity, flowParams)
        return true
    }

    fun launchPurchase(activity: Activity): Boolean {
        val preferredPlan = _availablePremiumPlans.value
            .sortedBy { it.type.sortOrder() }
            .firstOrNull()
            ?: return false
        return launchPurchase(activity, preferredPlan.type)
    }

    fun clearBillingMessage() {
        _billingMessage.value = null
    }

    private fun queryPremiumPlans() {
        querySubscriptionPlans()
        queryLifetimePlan()
    }

    private fun querySubscriptionPlans() {
        if (!billingClient.isReady) return

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_SUBS_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val details = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val plans = details.subscriptionOfferDetails.orEmpty()
                .mapNotNull { offer ->
                    val phase = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return@mapNotNull null
                    val type = when (phase.billingPeriod) {
                        "P1M" -> PremiumPlanType.MONTHLY
                        "P1Y" -> PremiumPlanType.YEARLY
                        else -> null
                    } ?: return@mapNotNull null

                    PremiumPlanOption(
                        type = type,
                        productDetails = details,
                        productType = BillingClient.ProductType.SUBS,
                        formattedPrice = phase.formattedPrice,
                        offerToken = offer.offerToken,
                        billingPeriod = phase.billingPeriod
                    )
                }
                .distinctBy { it.type }
                .sortedBy { it.type.sortOrder() }

            setSubscriptionPlans(plans)
        }
    }

    private fun queryLifetimePlan() {
        if (!billingClient.isReady) return

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_INAPP_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val details = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val price = details.oneTimePurchaseOfferDetails?.formattedPrice ?: string(R.string.see_play_store)
            setLifetimePlan(
                PremiumPlanOption(
                    type = PremiumPlanType.LIFETIME,
                    productDetails = details,
                    productType = BillingClient.ProductType.INAPP,
                    formattedPrice = price
                )
            )
        }
    }

    private fun queryPurchases(productType: String, onResult: (Purchase?, Boolean) -> Unit) {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onResult(purchases.firstOrNull(::isPremiumPurchase), true)
            } else {
                onResult(null, false)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!isPremiumPurchase(purchase)) return

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val activePlanType = when {
                purchase.products.any { it == PREMIUM_INAPP_ID } -> PremiumPlanType.LIFETIME
                purchase.products.any { it == PREMIUM_SUBS_ID } ->
                    pendingPlanType?.takeIf { it != PremiumPlanType.LIFETIME }
                        ?: resolveActiveSubscriptionPlanType()
                else -> null
            }

            persistPremiumState(true, activePlanType)
            pendingPlanType = null

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

    private fun persistPremiumState(isPremium: Boolean, activePlanType: PremiumPlanType?) {
        prefs.edit().putBoolean(PREF_PREMIUM_CACHED, isPremium).apply()
        prefs.edit {
            if (isPremium && activePlanType != null) {
                putString(PREF_PREMIUM_PLAN_TYPE, activePlanType.name)
            } else if (!isPremium) {
                remove(PREF_PREMIUM_PLAN_TYPE)
            }
        }
        _isPremium.value = isPremium
        if (isPremium) {
            _activePremiumPlanType.value = activePlanType
        } else {
            _activePremiumPlanType.value = null
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    private fun setSubscriptionPlans(plans: List<PremiumPlanOption>) {
        val nonSubscriptionPlans = _availablePremiumPlans.value.filter { it.productType != BillingClient.ProductType.SUBS }
        _availablePremiumPlans.value = (nonSubscriptionPlans + plans).sortedBy { it.type.sortOrder() }
    }

    private fun setLifetimePlan(plan: PremiumPlanOption) {
        val withoutLifetime = _availablePremiumPlans.value.filter { it.type != PremiumPlanType.LIFETIME }
        _availablePremiumPlans.value = (withoutLifetime + plan).sortedBy { it.type.sortOrder() }
    }

    private fun resolveActiveSubscriptionPlanType(): PremiumPlanType? {
        val cached = _activePremiumPlanType.value
        if (cached == PremiumPlanType.MONTHLY || cached == PremiumPlanType.YEARLY) {
            return cached
        }

        val subscriptionPlans = _availablePremiumPlans.value.filter { it.productType == BillingClient.ProductType.SUBS }
        return if (subscriptionPlans.size == 1) subscriptionPlans.first().type else null
    }

    private fun loadCachedPlanType(): PremiumPlanType? {
        return prefs.getString(PREF_PREMIUM_PLAN_TYPE, null)
            ?.let { stored -> runCatching { PremiumPlanType.valueOf(stored) }.getOrNull() }
    }

    private fun PremiumPlanType.sortOrder(): Int = when (this) {
        PremiumPlanType.YEARLY -> 0
        PremiumPlanType.MONTHLY -> 1
        PremiumPlanType.LIFETIME -> 2
    }

    private fun string(@StringRes resId: Int, vararg args: Any): String {
        return appContext.getString(resId, *args)
    }
}
