package com.wkq.test

import com.wkq.core.router.Route


object TestRoutes {
    val routes = listOf(
        Route.activity("/test/main", TestActivity::class),
        Route.activity("/test/loader_image", ImageLoaderDemoActivity::class),
        Route.activity("/test/gradient_label", GradientLabelTestActivity::class),
        Route.activity("/test/magic_indicator", MagicIndicatorTestActivity::class),
        Route.activity("/test/photo_picker", PhotoPickerTestActivity::class),
        Route.activity("/test/particle_loading", ParticleLoadingDemoActivity::class),
        Route.activity("/test/protocol_demo", ProtocolDemoActivity::class)
    )
}
