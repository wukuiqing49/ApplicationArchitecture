package com.wkq.google.billing.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.wkq.google.GoogleKit
import com.wkq.google.R
import com.wkq.google.billing.GoogleBillingEntitlement
import com.wkq.google.billing.GoogleBillingResponse
import com.wkq.google.billing.GoogleProduct
import com.wkq.google.databinding.FragmentGoogleBillingBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val BILLING_LOG_TAG = "SiteReportBilling"

class GoogleBillingFragment : Fragment() {

    private var _binding: FragmentGoogleBillingBinding? = null
    private val binding: FragmentGoogleBillingBinding
        get() = requireNotNull(_binding)

    private lateinit var pageConfig: GoogleBillingPageConfig
    private lateinit var planAdapter: GoogleBillingPlanAdapter
    private var refreshJob: Job? = null
    private var pendingRedeemRefresh: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        pageConfig = arguments?.getSerializable(ARG_PAGE_CONFIG) as? GoogleBillingPageConfig
            ?: GoogleBillingPageConfig(emptyList())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoogleBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        GoogleKit.billing.setPurchaseUpdatedListener { response, purchases ->
            viewLifecycleOwner.lifecycleScope.launch {
                handlePurchaseUpdated(response, purchases)
            }
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (pendingRedeemRefresh && _binding != null) {
            pendingRedeemRefresh = false
            refresh(showToastOnRefresh = true)
        }
    }

    override fun onDestroyView() {
        GoogleKit.billing.setPurchaseUpdatedListener(null)
        refreshJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    fun refresh(showToastOnRefresh: Boolean = false) {
        if (_binding == null) return
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val productsResult = runCatching { queryConfiguredProducts() }
            val entitlementResult = runCatching { GoogleKit.billing.queryEntitlement() }
            setLoading(false)

            productsResult.onSuccess { products ->
                bindPlans(products)
            }.onFailure { error ->
                bindPlans(emptyList())
                showMessage(error.userMessageOrFallback(R.string.google_billing_load_failed))
            }

            entitlementResult.onSuccess { entitlement ->
                bindEntitlement(entitlement)
                sendResult(EVENT_STATUS_CHANGED, entitlement = entitlement)
                if (showToastOnRefresh) {
                    showMessage(getString(R.string.google_billing_restore_success))
                }
            }.onFailure { error ->
                binding.tvStatusTitle.text = getString(R.string.google_billing_status_inactive)
                binding.tvStatusDesc.text = error.userMessageOrFallback(R.string.google_billing_status_unknown)
                sendResult(EVENT_ERROR, message = binding.tvStatusDesc.text.toString())
            }
        }
    }

    private fun setupViews() {
        binding.tvProductsTitle.text = pageConfig.productsTitle.ifBlank {
            getString(R.string.google_billing_products_title)
        }
        binding.tvProductsDesc.text = pageConfig.productsDescription.ifBlank {
            getString(R.string.google_billing_products_desc)
        }
        binding.btnRestore.text = pageConfig.restoreButtonText.ifBlank {
            getString(R.string.google_billing_restore)
        }
        binding.btnRestore.setOnClickListener {
            refresh(showToastOnRefresh = true)
        }
        binding.btnRedeemCode.text = pageConfig.redeemButtonText.ifBlank {
            getString(R.string.google_billing_redeem_code)
        }
        binding.btnRedeemCode.setOnClickListener {
            if (pageConfig.openRedeemPageOnClick) {
                sendResult(EVENT_REDEEM_REQUESTED)
            } else {
                openRedeemCode()
            }
        }
        planAdapter = GoogleBillingPlanAdapter(pageConfig.purchaseButtonText) { item ->
            startPurchase(item)
        }
        binding.rvPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlans.adapter = planAdapter
        planAdapter.submitList(emptyList())
        binding.tvEmpty.visibility = View.GONE
    }

    private suspend fun queryConfiguredProducts(): List<GoogleProduct> {
        val products = mutableListOf<GoogleProduct>()
        pageConfig.plans
            .groupBy { it.productType }
            .forEach { (productType, plans) ->
                val productIds = plans.map { it.productId }.distinct()
                val result = GoogleKit.billing.queryProducts(productIds, productType)
                products += result.getOrThrow()
            }
        return products
    }

    private fun bindPlans(products: List<GoogleProduct>) {
        val productMap = products.associateBy { it.productId }
        val items = pageConfig.plans.mapNotNull { plan ->
            val product = productMap[plan.billingProductId]
                ?: products.firstOrNull { product ->
                    product.productId == plan.productId ||
                        product.productId.startsWith("${plan.productId}:")
                }
                ?: return@mapNotNull null
            GoogleBillingPlanItem(
                config = plan,
                product = product
            )
        }
        val productSummary = products.joinToString { "${it.productId}@${it.formattedPrice}" }
        val itemSummary = items.joinToString { "${it.config.planId}->${it.product?.productId.orEmpty()}" }
        debugLog("bindPlans products=$productSummary items=$itemSummary")
        planAdapter.submitList(items)
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.tvEmpty.text = pageConfig.emptyText.ifBlank {
            getString(R.string.google_billing_empty)
        }
    }

    private fun bindEntitlement(entitlement: GoogleBillingEntitlement) {
        val activePlanId = entitlement.activePlanId
        binding.tvStatusTitle.text = when {
            entitlement.hasLifetimeUnlock -> getString(R.string.google_billing_status_lifetime)
            entitlement.hasSubscription -> getString(R.string.google_billing_status_active)
            else -> pageConfig.statusTitle.ifBlank {
                getString(R.string.google_billing_status_inactive)
            }
        }
        binding.tvStatusDesc.text = when {
            activePlanId.isNotBlank() -> activePlanId
            pageConfig.statusDescription.isNotBlank() -> pageConfig.statusDescription
            else -> getString(R.string.google_billing_status_free_desc)
        }
    }

    private fun startPurchase(item: GoogleBillingPlanItem) {
        val product = item.product
        if (product == null) {
            debugLog("startPurchase unavailable plan=${item.config.planId} productId=${item.config.productId}")
            showMessage(getString(R.string.google_billing_plan_unavailable))
            return
        }
        debugLog(
            "startPurchase plan=${item.config.planId} productId=${product.productId} " +
                "type=${product.productType} price=${product.formattedPrice} offerToken=${product.offerToken.take(8)}..."
        )
        val response = GoogleKit.billing.launchPurchase(
            activity = requireActivity(),
            productId = product.productId,
            offerToken = product.offerToken
        )
        if (!response.isSuccess &&
            response.responseCode != BillingClient.BillingResponseCode.OK &&
            response.responseCode != BillingClient.BillingResponseCode.USER_CANCELED
        ) {
            val message = response.message.userMessageOrFallback(R.string.google_billing_purchase_failed)
            showMessage(message)
            sendResult(EVENT_ERROR, message = message)
        }
    }

    private fun debugLog(message: String) {
        if (requireContext().applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Log.d(BILLING_LOG_TAG, "ui $message")
        }
    }

    private fun openRedeemCode() {
        val redeemUri = Uri.parse(GOOGLE_PLAY_REDEEM_URL)
        val playStoreIntent = Intent(Intent.ACTION_VIEW, redeemUri).apply {
            setPackage(GOOGLE_PLAY_PACKAGE)
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, redeemUri)
        pendingRedeemRefresh = true
        runCatching {
            startActivity(playStoreIntent)
        }.recoverCatching {
            startActivity(browserIntent)
        }.onSuccess {
            showMessage(getString(R.string.google_billing_redeem_refresh_hint))
        }.onFailure { error ->
            pendingRedeemRefresh = false
            val message = error.userMessageOrFallback(R.string.google_billing_redeem_open_failed)
            showMessage(message)
            sendResult(EVENT_ERROR, message = message)
        }
    }

    private suspend fun handlePurchaseUpdated(
        response: GoogleBillingResponse,
        purchases: List<com.wkq.google.billing.GooglePurchase>
    ) {
        when {
            response.isSuccess && purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } -> {
                purchases.filterNot { it.isAcknowledged }.forEach { purchase ->
                    GoogleKit.billing.acknowledgePurchase(purchase.purchaseToken)
                }
                showMessage(getString(R.string.google_billing_purchase_success))
                refresh()
                sendResult(EVENT_PURCHASED)
            }

            response.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                showMessage(getString(R.string.google_billing_purchase_canceled))
                sendResult(EVENT_CANCELED)
            }

            !response.isSuccess -> {
                val message = response.message.userMessageOrFallback(R.string.google_billing_purchase_failed)
                showMessage(message)
                sendResult(EVENT_ERROR, message = message)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRedeemCode.isEnabled = !loading
        binding.btnRestore.isEnabled = !loading
        binding.rvPlans.visibility = if (loading) View.GONE else View.VISIBLE
        if (loading) {
            binding.tvEmpty.visibility = View.GONE
            planAdapter.submitList(emptyList())
        }
    }

    private fun showMessage(message: String) {
        if (message.isBlank()) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun Throwable.userMessageOrFallback(fallbackRes: Int): String {
        return message.orEmpty().userMessageOrFallback(fallbackRes)
    }

    private fun String.userMessageOrFallback(fallbackRes: Int): String {
        val raw = trim()
        return if (raw.isBlank() || raw.isInternalBillingMessage()) {
            getString(fallbackRes)
        } else {
            raw
        }
    }

    private fun String.isInternalBillingMessage(): Boolean {
        return contains("BillingClient", ignoreCase = true) ||
            contains("product details", ignoreCase = true)
    }

    private fun sendResult(
        event: String,
        message: String = "",
        entitlement: GoogleBillingEntitlement? = null
    ) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putString(KEY_EVENT, event)
                putString(KEY_MESSAGE, message)
                putBoolean(KEY_IS_PRO, entitlement?.isPro ?: false)
                putString(KEY_ACTIVE_PLAN_ID, entitlement?.activePlanId.orEmpty())
            }
        )
    }

    companion object {
        const val RESULT_KEY = "google_billing_result"
        const val KEY_EVENT = "event"
        const val KEY_MESSAGE = "message"
        const val KEY_IS_PRO = "is_pro"
        const val KEY_ACTIVE_PLAN_ID = "active_plan_id"

        const val EVENT_PURCHASED = "purchased"
        const val EVENT_CANCELED = "canceled"
        const val EVENT_ERROR = "error"
        const val EVENT_STATUS_CHANGED = "status_changed"
        const val EVENT_REDEEM_REQUESTED = "redeem_requested"

        private const val ARG_PAGE_CONFIG = "page_config"
        private const val GOOGLE_PLAY_PACKAGE = "com.android.vending"
        private const val GOOGLE_PLAY_REDEEM_URL = "https://play.google.com/redeem"

        fun newInstance(config: GoogleBillingPageConfig): GoogleBillingFragment {
            return GoogleBillingFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PAGE_CONFIG, config)
                }
            }
        }
    }
}
