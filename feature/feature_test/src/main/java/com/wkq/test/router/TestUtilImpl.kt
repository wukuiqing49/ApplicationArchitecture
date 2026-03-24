package com.wkq.test.router

import com.wkq.router.annotation.ProvideService

@ProvideService(api = ITestUtil::class)
class TestUtilImpl : ITestUtil {
    override fun doSomething(input: String): String {
        return "Util 处理结果: [${input.uppercase()}]"
    }
}
