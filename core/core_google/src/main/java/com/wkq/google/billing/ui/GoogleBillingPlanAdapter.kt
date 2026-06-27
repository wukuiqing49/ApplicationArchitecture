package com.wkq.google.billing.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wkq.google.R
import com.wkq.google.billing.GoogleProduct
import com.wkq.google.databinding.ItemGoogleBillingPlanBinding

internal data class GoogleBillingPlanItem(
    val config: GoogleBillingPlanConfig,
    val product: GoogleProduct?
)

internal class GoogleBillingPlanAdapter(
    private val purchaseButtonText: String,
    private val onPlanClick: (GoogleBillingPlanItem) -> Unit
) : ListAdapter<GoogleBillingPlanItem, GoogleBillingPlanAdapter.PlanViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemGoogleBillingPlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlanViewHolder(
        private val binding: ItemGoogleBillingPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GoogleBillingPlanItem) {
            val context = binding.root.context
            val product = item.product
            binding.tvPlanTitle.text = item.config.title
            binding.tvPlanDesc.text = item.config.description
            binding.tvPlanBadge.text = item.config.badge
            binding.tvPlanBadge.visibility = if (item.config.badge.isBlank()) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
            binding.tvPlanPrice.text = product?.formattedPrice?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.google_billing_plan_unavailable)
            binding.btnPurchase.text = purchaseButtonText.ifBlank {
                context.getString(R.string.google_billing_purchase)
            }
            binding.btnPurchase.isEnabled = product != null
            binding.btnPurchase.setOnClickListener {
                onPlanClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GoogleBillingPlanItem>() {
        override fun areItemsTheSame(
            oldItem: GoogleBillingPlanItem,
            newItem: GoogleBillingPlanItem
        ): Boolean {
            return oldItem.config.planId == newItem.config.planId
        }

        override fun areContentsTheSame(
            oldItem: GoogleBillingPlanItem,
            newItem: GoogleBillingPlanItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
