package com.wkq.test.router

import com.wkq.router.annotation.ProvideService

@ProvideService(api = ITestService::class)
class TestServiceImpl : ITestService {
    override fun sayHello(name: String): String = "Hello KSP Routing, $name!"
}
