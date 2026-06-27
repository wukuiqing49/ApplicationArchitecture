package com.wkq.google.billing

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.wkq.google.GoogleKit
import com.wkq.google.model.SubscriptionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Google Play Billing 统一入口。
 */
object GoogleBillingManager : GoogleBillingGateway {

    private const val TAG = "SiteReportBilling"
    private const val PREF_NAME = "google_billing_state"
    private const val KEY_SANDBOX_PURCHASED_PRODUCT_ID = "sandbox_purchased_product_id"

    private val productCache = ConcurrentHashMap<String, ProductDetails>()
    private val clientLock = Any()

    @Volatile
    private var billingClient: BillingClient? = null

    @Volatile
    private var sandboxFallbackEnabled: Boolean = false

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var purchaseUpdatedListener: ((GoogleBillingResponse, List<GooglePurchase>) -> Unit)? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        val response = result.toGoogleResponse()
        val googlePurchases = purchases.orEmpty().map { it.toGooglePurchase() }
        debugLog(
            "purchasesUpdated code=${result.responseCode} message=${result.debugMessage} " +
                "purchases=${googlePurchases.joinToString { purchase -> purchase.products.joinToString("|") }}"
        )
        purchaseUpdatedListener?.invoke(response, googlePurchases)
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        sandboxFallbackEnabled = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        debugLog(
            "initialize debuggable=$sandboxFallbackEnabled package=${context.packageName} " +
                "version=${context.versionNameCode()} installer=${context.installerPackageNameCompat()} " +
                "signatureSha256=${context.signingCertSha256()}"
        )
        if (billingClient != null) return
        synchronized(clientLock) {
            if (billingClient != null) return
            billingClient = BillingClient.newBuilder(context.applicationContext)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .build()
        }
    }

    fun setPurchaseUpdatedListener(
        listener: ((GoogleBillingResponse, List<GooglePurchase>) -> Unit)?
    ) {
        purchaseUpdatedListener = listener
    }

    suspend fun connect(context: Context? = null): GoogleBillingResponse {
        context?.let { initialize(it) }
        val client = billingClient
            ?: return GoogleBillingResponse(
                isSuccess = false,
                responseCode = BillingClient.BillingResponseCode.ERROR,
                message = "BillingClient is not initialized."
            )
        if (client.isReady) {
            debugLog("connect already ready")
            return GoogleBillingResponse(true, BillingClient.BillingResponseCode.OK)
        }
        debugLog("connect start")
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    debugLog("connect disconnected")
                }

                override fun onBillingSetupFinished(result: BillingResult) {
                    debugLog("connect finished code=${result.responseCode} message=${result.debugMessage}")
                    if (continuation.isActive) {
                        continuation.resume(result.toGoogleResponse())
                    }
                }
            })
        }
    }

    suspend fun queryProducts(
        productIds: List<String>,
        productType: String
    ): Result<List<GoogleProduct>> {
        val client = billingClient
            ?: return Result.failure(IllegalStateException("BillingClient is not initialized."))
        val connectResult = connect()
        if (!connectResult.isSuccess) {
            return Result.failure(IllegalStateException(connectResult.message))
        }
        if (productIds.isEmpty()) {
            debugLog("queryProducts skip empty productIds type=$productType")
            return Result.success(emptyList())
        }
        debugLog("queryProducts request type=$productType ids=${productIds.joinToString()}")

        return suspendCancellableCoroutine { continuation ->
            val products = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build()
            client.queryProductDetailsAsync(params) { billingResult, queryResult ->
                val response = billingResult.toGoogleResponse()
                debugLog(
                    "queryProducts result type=$productType code=${billingResult.responseCode} " +
                        "message=${billingResult.debugMessage} count=${queryResult.productDetailsList.size}"
                )
                if (!response.isSuccess) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException(response.message)))
                    }
                    return@queryProductDetailsAsync
                }
                val productDetails = queryResult.productDetailsList
                productDetails.forEach { details ->
                    val offerSummary = details.subscriptionOfferDetails.orEmpty()
                        .joinToString { offer -> "${offer.basePlanId}:${offer.offerToken.take(8)}..." }
                    debugLog(
                        "productDetails id=${details.productId} type=${details.productType} " +
                            "title=${details.title} offers=$offerSummary"
                    )
                }
                productDetails.forEach { productCache[it.productId] = it }
                if (continuation.isActive) {
                    continuation.resume(Result.success(productDetails.flatMap { it.toGoogleProducts() }))
                }
            }
        }
    }

    suspend fun queryConfiguredCatalog(): Result<GoogleBillingCatalog> {
        val config = GoogleKit.currentConfig()
        val subscriptions = queryProducts(
            productIds = config.billingSubscriptionIds,
            productType = GoogleProductType.SUBS
        ).getOrElse { return Result.failure(it) }
        val oneTimeProducts = queryProducts(
            productIds = config.billingInAppProductIds,
            productType = GoogleProductType.IN_APP
        ).getOrElse { return Result.failure(it) }
        return Result.success(
            GoogleBillingCatalog(
                subscriptions = subscriptions,
                oneTimeProducts = oneTimeProducts
            )
        )
    }

    fun launchPurchase(
        activity: Activity,
        productId: String,
        offerToken: String = ""
    ): GoogleBillingResponse {
        val client = billingClient
            ?: return GoogleBillingResponse(
                isSuccess = false,
                responseCode = BillingClient.BillingResponseCode.ERROR,
                message = "BillingClient is not initialized."
            )
        
        // 拆分 productId 以支持 "realProductId:basePlanId"
        val parts = productId.split(":")
        val realProductId = parts[0]
        
        val productDetails = productCache[realProductId]
            ?: return GoogleBillingResponse(
                isSuccess = false,
                responseCode = BillingClient.BillingResponseCode.ERROR,
                message = "Query product details before launching purchase."
            )
        debugLog(
            "launchPurchase request productId=$productId realProductId=$realProductId " +
                "inputOfferToken=${offerToken.take(8)}... cacheHit=true"
        )
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        
        val resolvedOfferToken = offerToken.ifBlank {
            if (parts.size > 1) {
                val basePlanId = parts[1]
                productDetails.subscriptionOfferDetails?.firstOrNull { it.basePlanId == basePlanId }?.offerToken.orEmpty()
            } else {
                productDetails.defaultOfferToken()
            }
        }
        
        if (resolvedOfferToken.isNotBlank()) {
            productDetailsParamsBuilder.setOfferToken(resolvedOfferToken)
        }
        debugLog(
            "launchPurchase resolved productId=$productId resolvedOfferToken=${resolvedOfferToken.take(8)}... " +
                "availableOffers=${productDetails.subscriptionOfferDetails.orEmpty().joinToString { it.basePlanId }}"
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()
        val result = client.launchBillingFlow(activity, flowParams)
        debugLog("launchPurchase result code=${result.responseCode} message=${result.debugMessage}")
        return result.toGoogleResponse()
    }

    suspend fun queryActivePurchases(
        productType: String = GoogleProductType.SUBS
    ): Result<List<GooglePurchase>> {
        val client = billingClient
            ?: return Result.failure(IllegalStateException("BillingClient is not initialized."))
        val connectResult = connect()
        if (!connectResult.isSuccess) {
            return Result.failure(IllegalStateException(connectResult.message))
        }
        return suspendCancellableCoroutine { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build()
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                val response = billingResult.toGoogleResponse()
                debugLog(
                    "queryActivePurchases type=$productType code=${billingResult.responseCode} " +
                        "message=${billingResult.debugMessage} count=${purchases.size}"
                )
                if (continuation.isActive) {
                    if (response.isSuccess) {
                        continuation.resume(Result.success(purchases.map { it.toGooglePurchase() }))
                    } else {
                        continuation.resume(Result.failure(IllegalStateException(response.message)))
                    }
                }
            }
        }
    }

    suspend fun acknowledgePurchase(purchaseToken: String): GoogleBillingResponse {
        val client = billingClient
            ?: return GoogleBillingResponse(
                isSuccess = false,
                responseCode = BillingClient.BillingResponseCode.ERROR,
                message = "BillingClient is not initialized."
            )
        val connectResult = connect()
        if (!connectResult.isSuccess) {
            return connectResult
        }
        return suspendCancellableCoroutine { continuation ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            client.acknowledgePurchase(params) { result ->
                if (continuation.isActive) {
                    continuation.resume(result.toGoogleResponse())
                }
            }
        }
    }

    override suspend fun queryEntitlement(): GoogleBillingEntitlement {
        val subscriptions = queryActivePurchases(GoogleProductType.SUBS)
            .getOrDefault(emptyList())
            .filterPurchased()
        val oneTimeProducts = queryActivePurchases(GoogleProductType.IN_APP)
            .getOrDefault(emptyList())
            .filterPurchased()

        acknowledgeIfNeeded(subscriptions + oneTimeProducts)

        val activeSubscriptionIds = subscriptions.flatMap { it.products }.distinct().toMutableList()
        val ownedOneTimeProductIds = oneTimeProducts.flatMap { it.products }.distinct().toMutableList()

        // 加上本地沙盒模拟的数据兜底，方便调试
        val sandboxProduct = if (sandboxFallbackEnabled) {
            appContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                ?.getString(KEY_SANDBOX_PURCHASED_PRODUCT_ID, "")
                .orEmpty()
        } else {
            ""
        }
        if (sandboxProduct.isNotEmpty()) {
            val cleanSandboxProductId = sandboxProduct.substringBefore(":")
            val config = GoogleKit.requireConfig()
            if (config.billingInAppProductIds.contains(cleanSandboxProductId)) {
                if (!ownedOneTimeProductIds.contains(cleanSandboxProductId)) {
                    ownedOneTimeProductIds.add(cleanSandboxProductId)
                }
            } else {
                if (!activeSubscriptionIds.contains(cleanSandboxProductId)) {
                    activeSubscriptionIds.add(cleanSandboxProductId)
                }
            }
        }

        return GoogleBillingEntitlement(
            isPro = activeSubscriptionIds.isNotEmpty() || ownedOneTimeProductIds.isNotEmpty(),
            hasSubscription = activeSubscriptionIds.isNotEmpty(),
            hasLifetimeUnlock = ownedOneTimeProductIds.isNotEmpty(),
            activeSubscriptionIds = activeSubscriptionIds,
            ownedOneTimeProductIds = ownedOneTimeProductIds,
            source = if (sandboxProduct.isNotEmpty()) "sandbox_simulation" else "google_play"
        )
    }

    override suspend fun querySubscriptionStatus(userId: String): SubscriptionStatus {
        val entitlement = queryEntitlement()
        return SubscriptionStatus(
            isActive = entitlement.isPro,
            expireTimeMillis = 0L,
            planId = entitlement.activePlanId,
            source = entitlement.source
        )
    }

    fun endConnection() {
        productCache.clear()
        synchronized(clientLock) {
            billingClient?.endConnection()
            billingClient = null
        }
    }

    private suspend fun acknowledgeIfNeeded(purchases: List<GooglePurchase>) {
        purchases
            .filterNot { it.isAcknowledged }
            .forEach { purchase ->
                runCatching {
                    acknowledgePurchase(purchase.purchaseToken)
                }
            }
    }

    private fun debugLog(message: String) {
        if (sandboxFallbackEnabled) {
            Log.d(TAG, message)
        }
    }

    private fun Context.versionNameCode(): String {
        return runCatching {
            @Suppress("DEPRECATION")
            val info = packageManager.getPackageInfo(packageName, 0)
            "${info.versionName}/${info.versionCode}"
        }.getOrDefault("unknown")
    }

    private fun Context.installerPackageNameCompat(): String {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sourceInfo = packageManager.getInstallSourceInfo(packageName)
                sourceInfo.installingPackageName
                    ?: sourceInfo.initiatingPackageName
                    ?: sourceInfo.originatingPackageName
                    ?: "unknown"
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName) ?: "unknown"
            }
        }.getOrDefault("unknown")
    }

    private fun Context.signingCertSha256(): String {
        return runCatching {
            @Suppress("DEPRECATION")
            val info = packageManager.getPackageInfo(
                packageName,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES
                } else {
                    PackageManager.GET_SIGNATURES
                }
            )
            @Suppress("DEPRECATION")
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners.orEmpty()
            } else {
                info.signatures.orEmpty()
            }
            val bytes = signatures.firstOrNull()?.toByteArray() ?: return@runCatching "empty"
            MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString(":") { byte -> "%02X".format(byte) }
        }.getOrDefault("unknown")
    }
}

private fun ProductDetails.toGoogleProducts(): List<GoogleProduct> {
    if (productType == GoogleProductType.IN_APP) {
        return listOf(
            GoogleProduct(
                productId = productId,
                productType = productType,
                title = title,
                description = description,
                formattedPrice = formattedPrice(),
                offerToken = ""
            )
        )
    }
    
    val offers = subscriptionOfferDetails.orEmpty()
    if (offers.isEmpty()) {
        return listOf(
            GoogleProduct(
                productId = productId,
                productType = productType,
                title = title,
                description = description,
                formattedPrice = formattedPrice(),
                offerToken = ""
            )
        )
    }
    
    return offers.map { offer ->
        val basePlanId = offer.basePlanId
        val price = offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice.orEmpty()
        GoogleProduct(
            productId = if (basePlanId.isNotBlank()) "$productId:$basePlanId" else productId,
            productType = productType,
            title = title,
            description = description,
            formattedPrice = price,
            offerToken = offer.offerToken
        )
    }
}

private fun ProductDetails.formattedPrice(): String {
    oneTimePurchaseOfferDetails?.formattedPrice?.let { return it }
    return subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.firstOrNull()
        ?.formattedPrice
        .orEmpty()
}

private fun ProductDetails.defaultOfferToken(): String {
    return subscriptionOfferDetails?.firstOrNull()?.offerToken.orEmpty()
}

private fun Purchase.toGooglePurchase(): GooglePurchase {
    return GooglePurchase(
        products = products,
        purchaseToken = purchaseToken,
        purchaseTimeMillis = purchaseTime,
        isAcknowledged = isAcknowledged,
        purchaseState = purchaseState
    )
}

private fun BillingResult.toGoogleResponse(): GoogleBillingResponse {
    return GoogleBillingResponse(
        isSuccess = responseCode == BillingClient.BillingResponseCode.OK,
        responseCode = responseCode,
        message = debugMessage
    )
}

private fun List<GooglePurchase>.filterPurchased(): List<GooglePurchase> {
    return filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
}
