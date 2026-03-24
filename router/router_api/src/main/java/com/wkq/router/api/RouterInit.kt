package com.wkq.router.api

import java.util.ServiceLoader

/**
 *
 * @ Author: wkq
 *
 * @ Time: 2026/3/24 9:22
 *
 * @ Desc:

 */
class RouterInit {
    fun init() {
        ServiceLoader.load(IRouteInit::class.java).forEach {
            it.init()
        }
    }
}