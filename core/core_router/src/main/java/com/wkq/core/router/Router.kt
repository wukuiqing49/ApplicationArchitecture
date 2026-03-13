package com.wkq.core.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

sealed class RouteEntry(val path: String) {
    class ActivityEntry(path: String, val clazz: KClass<out Activity>) : RouteEntry(path)
    class FragmentEntry(path: String, val factory: () -> Fragment) : RouteEntry(path)
    class ComposeEntry(path: String, val factory: @Composable () -> Unit) : RouteEntry(path)
}

/**
 * 路由功能核心实现
 */

object Router {
    // 采用 ConcurrentHashMap 避免多模块异步并发初始化注册路由时发生 ConcurrentModificationException
    private val routes = ConcurrentHashMap<String, RouteEntry>()

    fun registerRouterInit(context: Context) {
        try {
            // 1️⃣ 获取 app 的包名
            val basePackage = context.packageName // 比如 com.qianrun.voice

            // 2️⃣ 拼接 RouterInit 完整类名
            val routerInitClass = "$basePackage.core.router.RouterInit"

            // 3️⃣ 反射获取对象并调用方法
            val clazz = Class.forName(routerInitClass)
            val instance = clazz.getField("INSTANCE").get(null)
            val method = clazz.getMethod("registerAll")
            method.invoke(instance)
        } catch (e: ClassNotFoundException) {
            // 类不存在，可能没有生成 RouterInit
            Log.w("RouterInit", "RouterInit class not found, skipping registration")
        } catch (e: Exception) {
            // 其他异常，打印日志
            e.printStackTrace()
        }
    }



    /**
     * 注册路由节点。
     * 建议在各业务模块初始化（如 Application 或 Startup 组件）时调用。
     * @param entry 具体的路由实体，包含路径与对应的目标 (Activity/Fragment/Compose)。
     */
    fun add(entry: RouteEntry) { 
        routes[entry.path] = entry 
    }
    
    /**
     * 跨模块打开一个 Activity 页面。
     * @param path 目标 Activity 提前注册的路由路径。
     * @param context 发起跳转的上下文。如果不是 Activity Context，会自动追加 FLAG_ACTIVITY_NEW_TASK 标志以防止崩溃。
     * @param block 可选的 DSL 配置块，用于在跳转前向 Intent 中装配额外的参数信息。
     */
    fun open(path: String, context: Context, block: (Intent.() -> Unit)? = null) {
        val entry = routes[path]
        if (entry == null) {
            Log.e("Router", "Target path not found: $path")
            return
        }
        
        if (entry is RouteEntry.ActivityEntry) {
            val intent = Intent(context, entry.clazz.java)
            
            // 兜底处理：如果传入的 context 不是 Activity，必须加上 NEW_TASK 标志，否则会引发 AndroidRuntimeException Crash
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            block?.invoke(intent)
            context.startActivity(intent)
        } else {
            // 健壮性保障：找不到路由时，不静默失败，提供错误日志方便开发排查
            Log.e("Router", "Target path exists but is not an Activity Route: $path (Actual type: ${entry::class.simpleName})")
        }
    }
    
    /**
     * 跨模块获取一个具有明确路由路径的 Fragment 实例。
     * @param path 目标 Fragment 提前注册的路由路径。
     * @return 成功时返回新创建的 Fragment 实例；如果路径不存在或类型不匹配则返回 null。
     */
    fun getFragment(path: String): Fragment? {
        val entry = routes[path]
        if (entry == null) {
            Log.e("Router", "Target path not found: $path")
            return null
        }
        
        if (entry is RouteEntry.FragmentEntry) {
            return entry.factory()
        }
        Log.e("Router", "Target path exists but is not a Fragment Route: $path (Actual type: ${entry::class.simpleName})")
        return null
    }

    /**
     * 跨模块获取一套具有明确路由路径的 Compose UI 组合函数。
     * @param path 目标 Compose UI 块提前注册的路由路径。
     * @return 成功时返回 @Composable 的 Lambda 组合函数体；如果路径不存在或类型不匹配则返回 null。
     */
    fun getCompose(path: String): (@Composable () -> Unit)? {
        val entry = routes[path]
        if (entry == null) {
            Log.e("Router", "Target path not found: $path")
            return null
        }
        
        if (entry is RouteEntry.ComposeEntry) {
            return entry.factory
        }
        Log.e("Router", "Target path exists but is not a Compose Route: $path (Actual type: ${entry::class.simpleName})")
        return null
    }
}

class Route {
    companion object {
        fun activity(path: String, clazz: KClass<out Activity>) = RouteEntry.ActivityEntry(path, clazz)
        fun fragment(path: String, factory: () -> Fragment) = RouteEntry.FragmentEntry(path, factory)
        fun compose(path: String, factory: @Composable () -> Unit) = RouteEntry.ComposeEntry(path, factory)
    }
}
