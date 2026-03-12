package com.wkq.test

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.wkq.base.activity.BaseActivity
import com.wkq.test.databinding.ActivityMagicIndicatorTestBinding
import com.wkq.ui.helper.ViewPager2Helper
import com.wkq.ui.widget.GradientRoundPagerIndicator
import com.wkq.ui.widget.GradientRoundPagerTitleView
import com.wkq.ui.widget.ScaleTransitionPagerTitleView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator

class MagicIndicatorTestActivity : BaseActivity<ActivityMagicIndicatorTestBinding>() {

    private val mDataList = listOf("推荐", "音乐", "游戏", "生活", "科技", "娱乐", "萌宠")

    override fun initView() {
        initMagicIndicator1() // GradientRoundPagerTitleView
        initMagicIndicator2() // ScaleTransitionPagerTitleView
        initViewPager()
    }

    private fun initViewPager() {
        binding.viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    gravity = Gravity.CENTER
                    textSize = 30f
                    setTextColor(Color.BLACK)
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as TextView).text = mDataList[position]
            }

            override fun getItemCount(): Int = mDataList.size
        }

        ViewPager2Helper.bind(binding.magicIndicator, binding.viewPager)
        ViewPager2Helper.bind(binding.magicIndicator2, binding.viewPager)
    }

    private fun initMagicIndicator1() {
        val commonNavigator = CommonNavigator(this)
        commonNavigator.isFollowTouch = true
        commonNavigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount(): Int = mDataList.size

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                return GradientRoundPagerTitleView(context).apply {
                    normalTextColor = Color.parseColor("#999999")
                    selectedTextColor = Color.parseColor("#FFFFFF")
                    normalTextSizeSp = 14f
                    selectedTextSizeSp = 16f
                    selectedBgColors = intArrayOf(Color.parseColor("#FF4081"), Color.parseColor("#FF80AB"))
                    normalBgColors = intArrayOf(Color.parseColor("#EEEEEE"), Color.parseColor("#EEEEEE"))
                    viewMarginHorizontal = dp(10f).toInt() 
                    viewMarginVertical = dp(4f).toInt()
                    textPaddingHorizontal = dp(18f).toInt()
                    textPaddingVertical = dp(6f).toInt()
                    cornerRadiusPx = dp(20f)
                    text = mDataList[index]
                    setOnClickListener { binding.viewPager.currentItem = index }
                    refresh()
                }
            }

            override fun getIndicator(context: Context): IPagerIndicator? = null
        }
        binding.magicIndicator.navigator = commonNavigator
    }

    private fun initMagicIndicator2() {
        val commonNavigator = CommonNavigator(this)
        commonNavigator.isFollowTouch = true
        commonNavigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount(): Int = mDataList.size

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                return ScaleTransitionPagerTitleView(context).apply {
                    normalColor = Color.parseColor("#999999")
                    selectedColor = Color.parseColor("#333333")
                    normalTextSize = 15f
                    selectedTextSize = 18f
                    text = mDataList[index]
                    setOnClickListener { binding.viewPager.currentItem = index }
                }
            }

            override fun getIndicator(context: Context): IPagerIndicator {
                return GradientRoundPagerIndicator(context).apply {
                    indicatorWidth = dp(24f)
                    indicatorHeight = dp(6f)
                    cornerRadius = dp(3f)
                    startColor = Color.parseColor("#FF4081")
                    endColor = Color.parseColor("#FF80AB")
                }
            }
        }
        binding.magicIndicator2.navigator = commonNavigator
    }

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    override fun initData() {}
}
