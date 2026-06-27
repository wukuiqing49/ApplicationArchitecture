package com.wkq.google.rate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.animation.Animation
import android.view.animation.CycleInterpolator
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.Toast
import com.wkq.base.dialog.CommonDialog
import com.wkq.base.dialog.PopupHandle
import com.wkq.google.R
import com.wkq.google.databinding.DialogRateAppBinding

/**
 * 引导好评与反馈弹窗辅助类。
 */
object RateAppDialogHelper {

    private const val COLOR_GOLD = "#FFB300"
    private const val COLOR_GRAY = "#BDBDBD"

    fun showRateDialog(
        activity: Activity,
        appName: String,
        feedbackEmail: String,
        force: Boolean = false,
        onDismiss: (() -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val binding = DialogRateAppBinding.inflate(activity.layoutInflater)
        var popup: PopupHandle? = null

        binding.tvRateTitle.text = activity.getString(R.string.rate_dialog_default_title, appName)
        binding.tvRateDesc.text = activity.getString(R.string.rate_dialog_default_desc)

        val stars = listOf(
            binding.ivStar1,
            binding.ivStar2,
            binding.ivStar3,
            binding.ivStar4,
            binding.ivStar5
        )
        var selectedRating = 0

        stars.forEachIndexed { index, starView ->
            starView.setOnClickListener {
                selectedRating = index + 1
                updateStarsUI(stars, selectedRating)
                playStarAnimation(starView)

                binding.btnRateAction.isEnabled = true
                if (selectedRating >= 4) {
                    binding.tvRateDesc.text = activity.getString(R.string.rate_dialog_desc_good)
                    binding.btnRateAction.text = activity.getString(R.string.rate_dialog_btn_action_good)
                } else {
                    binding.tvRateDesc.text = activity.getString(R.string.rate_dialog_desc_bad)
                    binding.btnRateAction.text = activity.getString(R.string.rate_dialog_btn_action_bad)
                }
            }
        }

        binding.btnRateCancel.setOnClickListener {
            popup?.dismiss()
        }

        binding.btnRateAction.setOnClickListener {
            if (selectedRating >= 4) {
                launchGooglePlayReview(activity, binding, popup)
            } else {
                if (sendFeedbackEmail(activity, appName, feedbackEmail)) {
                    RateAppController.markRated(activity)
                    popup?.dismiss()
                }
            }
        }

        popup = CommonDialog.showRawCenter(
            context = activity,
            contentView = binding.root,
            onDismiss = onDismiss
        )
    }

    private fun updateStarsUI(stars: List<ImageView>, rating: Int) {
        stars.forEachIndexed { index, starView ->
            if (index < rating) {
                starView.setColorFilter(Color.parseColor(COLOR_GOLD))
            } else {
                starView.setColorFilter(Color.parseColor(COLOR_GRAY))
            }
        }
    }

    private fun playStarAnimation(starView: ImageView) {
        val anim = ScaleAnimation(
            0.8f, 1.15f, 0.8f, 1.15f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
            interpolator = CycleInterpolator(0.5f)
        }
        starView.startAnimation(anim)
    }

    private fun launchGooglePlayReview(
        activity: Activity,
        binding: DialogRateAppBinding,
        popup: PopupHandle?
    ) {
        binding.btnRateAction.isEnabled = false
        GooglePlayReviewLauncher.launch(activity) { success ->
            if (!success) {
                val openStoreSuccess = launchAppRating(activity)
                if (!openStoreSuccess) {
                    Toast.makeText(activity, R.string.rate_feedback_store_failed, Toast.LENGTH_SHORT).show()
                }
            }
            if (!activity.isFinishing && !activity.isDestroyed && popup?.isShowing() == true) {
                popup.dismiss()
            }
        }
    }

    private fun launchAppRating(context: Context): Boolean {
        val packageName = context.packageName
        val uri = Uri.parse("market://details?id=$packageName")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun sendFeedbackEmail(context: Context, appName: String, emailAddress: String): Boolean {
        if (emailAddress.isBlank()) {
            Toast.makeText(context, R.string.rate_feedback_email_empty, Toast.LENGTH_SHORT).show()
            return false
        }
        val subject = context.getString(R.string.rate_feedback_email_subject, appName)
        val body = context.getString(R.string.rate_feedback_email_body, appName)
        val emailUri = Uri.parse("mailto:$emailAddress").buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()
        val intent = Intent(Intent.ACTION_SENDTO, emailUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            CommonDialog.showConfirm(
                context = context,
                title = context.getString(R.string.rate_feedback_email_empty),
                message = context.getString(R.string.rate_feedback_no_email_client, emailAddress),
                confirmText = context.getString(android.R.string.ok),
                cancelText = context.getString(android.R.string.cancel),
                onConfirm = {}
            )
            return false
        }
    }
}
