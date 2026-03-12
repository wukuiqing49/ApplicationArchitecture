package com.wkq.test

import com.wkq.base.activity.BaseActivity
import com.wkq.test.databinding.ActivityImageLoaderDemoBinding
import com.wkq.util.coil.ImageLoaderUtil
import com.wkq.util.coil.loadUrl

/**
 * ImageLoaderUtil 优化测试 Demo
 */
class ImageLoaderDemoActivity : BaseActivity<ActivityImageLoaderDemoBinding>() {

    private val testImg = "https://bpic.588ku.com/element_pic/23/07/25/ac0eff9c8e8ed3780d194bd48d106d70.jpg!/fw/300/unsharp/true" // 稳定图片源
    private val avatarImg = "https://www.bing.com/th/id/OIP.IJZgTNx1vp9EML_1wV5p2gHaEo?w=257&h=211&c=8&rs=1&qlt=90&o=6&pid=3.1&rm=2" // 稳定图片源
    private val gifUrl = "https://th.bing.com/th/id/R.3562db2b97df44ad0a2913367e923bc9?rik=F0gPEgEKbhChlw&riu=http%3a%2f%2fpic.962.net%2fup%2f2016-10%2f14760807179493223.gif&ehk=1QnaRGwQ3oUxW2TtYQHyQM5fm9qkakLLshGCLtGyzE0%3d&risl=&pid=ImgRaw&r=0" // 稳定 GIF 源


    override fun initView() {
        // 1. 普通加载 (使用扩展函数)
        binding.ivNormal.loadUrl(testImg)

        // 2. 圆形裁剪
        binding.ivCircle.loadUrl(avatarImg, isCircle = true)

        // 3. 圆角加载 (30dp)
        binding.ivRounded.loadUrl(testImg, radius = 60f) // 这里的单位是 px，demo 使用 60 示意

        // 4. 灰度模式
        binding.ivGrayscale.loadUrl(testImg, isGrayscale = true)

        // 5. GIF 动图 (自动支持，也可以显式调用以禁用内存缓存防 OOM)
        ImageLoaderUtil.loadGif(binding.ivGif, gifUrl)

//         6. 永久缓存加载 (Pinned Cache)
//         适合高频显示的头像、小图标，即使应用清理缓存也不会丢失（除非手动清空永久缓存）
      ImageLoaderUtil.loadPinned(binding.ivPinned, avatarImg, isCircle = true)

    }

    override fun initData() {
        // 预加载测试
//        ImageLoaderUtil.preload(this, "https://th.bing.com/th/id/OIP.-yGmmcWjg_xXUfcODuhafAHaE7?w=343&h=187&c=7&r=0&o=7&pid=1.7&rm=3")
    }
}