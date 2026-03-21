package com.wkq.common

import com.wkq.core.router.Route
import com.wkq.core.router.RouteEntry
import com.wkq.common.web.CommonWebActivity


object CommonRoutes {

    val routes = listOf<RouteEntry.ActivityEntry>(
        RouteEntry.ActivityEntry("/common/webview", CommonWebActivity::class)
    )
}
