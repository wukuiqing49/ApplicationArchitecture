package com.wkq.test

import com.wkq.core.router.Route


object TestRoutes {
    val routes = listOf(
        Route.activity("/test/main", TestActivity::class),
        Route.activity("/test/loader_image", ImageLoaderDemoActivity::class),
        Route.activity("/test/gradient_label", GradientLabelTestActivity::class),
        Route.activity("/test/magic_indicator", MagicIndicatorTestActivity::class)
    )
}
