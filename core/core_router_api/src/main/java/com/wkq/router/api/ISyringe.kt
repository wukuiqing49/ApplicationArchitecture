package com.wkq.router.api

/**
 * 注入器接口，用于自动给 Activity/Fragment 的变量赋值。
 */
interface ISyringe {
    /**
     * 执行注入逻辑。
     *
     * @param target 目标对象，通常是 Activity 或 Fragment
     */
    fun inject(target: Any)
}
