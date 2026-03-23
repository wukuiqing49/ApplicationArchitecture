package com.wkq.common

import com.wkq.core.router.RouteEntry
import com.wkq.common.web.CommonWebActivity

/**
 * 路由你注册  页面跳转
 */
object CommonRoutes {

    val routes = listOf<RouteEntry.ActivityEntry>(
        RouteEntry.ActivityEntry("/common/webview", CommonWebActivity::class)
    )
}
