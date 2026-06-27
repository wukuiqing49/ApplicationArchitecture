package com.wkq.google.billing.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.wkq.google.GoogleKit
import com.wkq.google.R
import com.wkq.google.billing.GoogleBillingEntitlement
import com.wkq.google.databinding.FragmentGoogleBillingRedeemBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GoogleBillingRedeemFragment : Fragment() {

    private var _binding: FragmentGoogleBillingRedeemBinding? = null
    private val binding: FragmentGoogleBillingRedeemBinding
        get() = requireNotNull(_binding)

    private var refreshJob: Job? = null
    private var pendingRedeemRefresh: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoogleBillingRedeemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
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
        refreshJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    private fun setupViews() {
        binding.btnRedeem.setOnClickListener {
            openRedeemCode()
        }
        binding.btnRefresh.setOnClickListener {
            refresh(showToastOnRefresh = true)
        }
    }

    private fun refresh(showToastOnRefresh: Boolean = false) {
        if (_binding == null) return
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val entitlementResult = runCatching { GoogleKit.billing.queryEntitlement() }
            setLoading(false)
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

    private fun bindEntitlement(entitlement: GoogleBillingEntitlement) {
        val activePlanId = entitlement.activePlanId
        binding.tvStatusTitle.text = when {
            entitlement.hasLifetimeUnlock -> getString(R.string.google_billing_status_lifetime)
            entitlement.hasSubscription -> getString(R.string.google_billing_status_active)
            else -> getString(R.string.google_billing_status_inactive)
        }
        binding.tvStatusDesc.text = when {
            activePlanId.isNotBlank() -> activePlanId
            else -> getString(R.string.google_billing_status_free_desc)
        }
    }

    private fun openRedeemCode() {
        val code = binding.etRedeemCode.text?.toString().orEmpty().trim()
        val redeemUri = buildRedeemUri(code)
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

    private fun buildRedeemUri(code: String): Uri {
        return if (code.isBlank()) {
            Uri.parse(GOOGLE_PLAY_REDEEM_URL)
        } else {
            Uri.parse(GOOGLE_PLAY_REDEEM_URL)
                .buildUpon()
                .appendQueryParameter("code", code)
                .build()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRedeem.isEnabled = !loading
        binding.btnRefresh.isEnabled = !loading
    }

    private fun showMessage(message: String) {
        if (message.isBlank()) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun Throwable.userMessageOrFallback(fallbackRes: Int): String {
        val raw = message.orEmpty().trim()
        return if (raw.isBlank() || raw.contains("BillingClient", ignoreCase = true)) {
            getString(fallbackRes)
        } else {
            raw
        }
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
        const val RESULT_KEY = "google_billing_redeem_result"
        const val KEY_EVENT = "event"
        const val KEY_MESSAGE = "message"
        const val KEY_IS_PRO = "is_pro"
        const val KEY_ACTIVE_PLAN_ID = "active_plan_id"

        const val EVENT_ERROR = "error"
        const val EVENT_STATUS_CHANGED = "status_changed"

        private const val GOOGLE_PLAY_PACKAGE = "com.android.vending"
        private const val GOOGLE_PLAY_REDEEM_URL = "https://play.google.com/redeem"

        fun newInstance(): GoogleBillingRedeemFragment {
            return GoogleBillingRedeemFragment()
        }
    }
}
