package com.wkq.test.corebase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.wkq.base.activity.BaseActivity
import com.wkq.base.adapter.BaseRecyclerViewAdapter
import com.wkq.base.fragment.BaseFragment
import com.wkq.base.fragment.BaseListFragment
import com.wkq.base.fragment.BaseVMFragment
import com.wkq.base.fragment.BaseVMListFragment
import com.wkq.base.insets.SystemBarInsets
import com.wkq.router.annotation.Route
import com.wkq.test.R
import com.wkq.test.databinding.ActivityCoreBaseFragmentHostBinding
import com.wkq.test.databinding.ActivityCoreBaseSimplePageBinding

@Route(path = "/test/core_base/base_fragment")
class CoreBaseFragmentHostActivity : BaseActivity<ActivityCoreBaseFragmentHostBinding>() {

    override fun initView() {
        SystemBarInsets.applyBottomInset(binding.rootContainer, extraBottom = dp(12))
        binding.topBar.onLeftClickListener = { finish() }
        binding.btnFragment.setOnClickListener { showFragment(CoreBaseFragmentDemo()) }
        binding.btnVmFragment.setOnClickListener { showFragment(CoreBaseVMFragmentDemo()) }
        binding.btnListFragment.setOnClickListener { showFragment(CoreBaseListFragmentDemo()) }
        binding.btnVmListFragment.setOnClickListener { showFragment(CoreBaseVMListFragmentDemo()) }
    }

    override fun initData() {
        showFragment(CoreBaseFragmentDemo())
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

class CoreBaseFragmentDemo : BaseFragment<ActivityCoreBaseSimplePageBinding>() {

    private val adapter by lazy { CoreBaseSampleAdapter(requireContext()) }

    override fun initView() {
        binding.bindFragmentPage(
            title = "BaseFragment",
            desc = "验证 BaseFragment 的 ViewBinding 生命周期释放。",
            state = "viewLifecycleOwner=${viewLifecycleOwner.lifecycle.currentState}"
        )
        binding.rvSamples.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSamples.adapter = adapter
    }

    override fun initData() {
        adapter.setData(
            listOf(
                CoreBaseSample("onCreateView", "通过 BaseFragment 泛型 inflate ViewBinding。"),
                CoreBaseSample("onDestroyView", "Fragment 销毁视图时会释放 _binding。")
            )
        )
    }
}

class CoreBaseVMFragmentDemo :
    BaseVMFragment<ActivityCoreBaseSimplePageBinding, CoreBaseDemoViewModel>() {

    override fun initView() {
        binding.bindFragmentPage(
            title = "BaseVMFragment",
            desc = "验证 BaseVMFragment 自动创建 ViewModel。",
            state = "等待 ViewModel 状态"
        )
        viewModel.state.observe(viewLifecycleOwner) {
            binding.tvState.text = it
        }
    }

    override fun initData() {
        viewModel.markLoaded("BaseVMFragment")
    }
}

class CoreBaseListFragmentDemo : BaseListFragment<CoreBaseSample>() {

    override fun createAdapter(): BaseRecyclerViewAdapter<*, CoreBaseSample> {
        return CoreBaseSampleAdapter(requireContext())
    }

    override fun loadListData(page: Int) {
        binding.recyclerView.postDelayed({
            finishLoad(createFragmentPageData("BaseListFragment", page), hasMore = page < 2)
        }, 250L)
    }
}

class CoreBaseVMListFragmentDemo :
    BaseVMListFragment<CoreBaseDemoViewModel, CoreBaseSample>() {

    override fun createAdapter(): BaseRecyclerViewAdapter<*, CoreBaseSample> {
        return CoreBaseSampleAdapter(requireContext())
    }

    override fun loadListData(page: Int) {
        viewModel.markLoaded("BaseVMListFragment page=$page")
        binding.recyclerView.postDelayed({
            finishLoad(createFragmentPageData("BaseVMListFragment", page), hasMore = page < 2)
        }, 250L)
    }
}

private fun ActivityCoreBaseSimplePageBinding.bindFragmentPage(
    title: String,
    desc: String,
    state: String
) {
    tvTitle.text = title
    tvDesc.text = desc
    tvState.text = state
}

private fun createFragmentPageData(prefix: String, page: Int): List<CoreBaseSample> {
    return List(8) { index ->
        CoreBaseSample(
            title = "$prefix item ${(page - 1) * 8 + index + 1}",
            desc = "分页 page=$page，验证 Fragment 列表刷新和加载更多。"
        )
    }
}
