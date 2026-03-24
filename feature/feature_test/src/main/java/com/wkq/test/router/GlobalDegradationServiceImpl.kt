package com.wkq.test.router

import android.content.Context
import android.widget.Toast
import com.wkq.router.annotation.ProvideService
import com.wkq.router.api.IDegradationService
import com.wkq.router.api.Postcard

/**
 * 路由全局容错降级服务
 */
@ProvideService(IDegradationService::class)
class GlobalDegradationServiceImpl : IDegradationService {
    override fun onLost(context: Context, postcard: Postcard) {
        // 当路由找不到时，不再崩溃，而是给出友好提示
        // 实际开发中，这里可以跳转到统一下发的一个动态 H5 页面进行降级补偿
        Toast.makeText(context, "全局路由降级触发：\n未找到路径 ${postcard.path}", Toast.LENGTH_LONG).show()
    }
}
