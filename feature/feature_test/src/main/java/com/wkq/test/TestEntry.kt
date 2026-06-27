package com.wkq.test

/**
 * 测试中心入口配置。
 */
data class TestEntry(
    val group: String,
    val icon: String,
    val title: String,
    val desc: String,
    val action: () -> Unit
)
