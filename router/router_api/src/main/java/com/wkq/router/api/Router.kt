package com.wkq.router.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.fragment.app.FragmentActivity
import kotlin.reflect.KClass

/**
 * 路由门面类
 */
object Router {

    /**
     * 初始化路由（扫描所有模块的注册类）
     */
    fun init(context: Context) {
        try {
            val loader = java.util.ServiceLoader.load(IRouteInit::class.java)
            for (init in loader) {
                init.init()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 构建路由请求
     */
    fun build(path: String): Postcard {
        return Postcard(path)
    }

    /**
     * 执行拦截器并导航
     */
    fun navigate(context: Context, postcard: Postcard) {
        val interceptors = RouteTable.interceptors.sortedByDescending { it.priority }.map { it.interceptor as IInterceptor }
        
        executeInterceptors(interceptors, 0, postcard) { p ->
            realNavigate(context, p)
        }
    }

    /**
     * 执行拦截器并导航（带 Result）
     */
    fun navigateWithResult(activity: FragmentActivity, postcard: Postcard, callback: (ActivityResult) -> Unit) {
        val interceptors = RouteTable.interceptors.sortedByDescending { it.priority }.map { it.interceptor as IInterceptor }
        
        executeInterceptors(interceptors, 0, postcard) { p ->
            realNavigateWithResult(activity, p, callback)
        }
    }

    private fun extractGroup(path: String): String {
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        val group = cleanPath.substringBefore("/")
        return if (group.isEmpty()) "default" else group
    }

    private fun checkAndLoadGroup(path: String) {
        if (RouteTable.routes[path] == null) {
            val group = extractGroup(path)
            val groupClass = RouteTable.groups[group]
            if (groupClass != null) {
                try {
                    val groupInstance = groupClass.getConstructor().newInstance()
                    groupInstance.load()
                    // 移除以防重复加载
                    RouteTable.groups.remove(group)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun executeInterceptors(interceptors: List<IInterceptor>, index: Int, postcard: Postcard, finish: (Postcard) -> Unit) {
        if (index >= interceptors.size) {
            finish(postcard)
            return
        }

        val interceptor = interceptors[index]
        interceptor.process(postcard, object : InterceptorCallback {
            override fun onContinue(postcard: Postcard) {
                executeInterceptors(interceptors, index + 1, postcard, finish)
            }

            override fun onInterrupt(exception: Throwable?) {
                // 中断不执行后续
            }
        })
    }

    private fun realNavigate(context: Context, postcard: Postcard) {
        checkAndLoadGroup(postcard.path)
        val meta = RouteTable.routes[postcard.path]
        if (meta == null) {
            val degradationService = getService(IDegradationService::class)
            if (degradationService != null) {
                degradationService.onLost(context, postcard)
            } else {
                throw RuntimeException("Route not found: ${postcard.path}")
            }
            return
        }

        val intent = Intent(context, meta.clazz)
        intent.putExtras(postcard.getExtras())
        if (postcard.getFlags() != -1) {
            intent.setFlags(postcard.getFlags())
        }
        
        context.startActivity(intent)
        
        // 处理动画
        handleAnimation(context, postcard, meta)
    }

    private fun realNavigateWithResult(activity: FragmentActivity, postcard: Postcard, callback: (ActivityResult) -> Unit) {
        checkAndLoadGroup(postcard.path)
        val meta = RouteTable.routes[postcard.path]
        if (meta == null) {
            val degradationService = getService(IDegradationService::class)
            if (degradationService != null) {
                degradationService.onLost(activity, postcard)
            } else {
                throw RuntimeException("Route not found: ${postcard.path}")
            }
            return
        }

        val intent = Intent(activity, meta.clazz)
        intent.putExtras(postcard.getExtras())
        
        val proxy = RouterResultProxyFragment()
        proxy.setParams(intent, callback)
        
        activity.supportFragmentManager.beginTransaction()
            .add(proxy, "RouterResultProxy")
            .commit()
    }

    private fun handleAnimation(context: Context, postcard: Postcard, meta: RouteMeta) {
        if (context is Activity) {
            val enterId = if (postcard.getEnterAnim() != 0) postcard.getEnterAnim() else meta.enterAnim
            val exitId = if (postcard.getExitAnim() != 0) postcard.getExitAnim() else meta.exitAnim
            if (enterId != 0 || exitId != 0) {
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    context.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, enterId, exitId)
                } else {
                    @Suppress("DEPRECATION")
                    context.overridePendingTransition(enterId, exitId)
                }
            }
        }
    }

    /**
     * 获取 Fragment 实例
     */
    fun getFragment(path: String, bundle: Bundle? = null): androidx.fragment.app.Fragment? {
        checkAndLoadGroup(path)
        val meta = RouteTable.routes[path] ?: return null
        return try {
            val fragment = meta.clazz.getConstructor().newInstance() as? androidx.fragment.app.Fragment
            fragment?.arguments = bundle
            fragment
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取 View 实例
     */
    fun getView(path: String, context: Context): android.view.View? {
        checkAndLoadGroup(path)
        val meta = RouteTable.routes[path] ?: return null
        return try {
            meta.clazz.getConstructor(Context::class.java).newInstance(context) as? android.view.View
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val syringeCache = java.util.concurrent.ConcurrentHashMap<String, ISyringe>()

    /**
     * 自动注入参数
     * @param target Activity 或 Fragment 实例
     */
    fun inject(target: Any) {
        val className = target.javaClass.name + "_Syringe"
        var syringe = syringeCache[className]
        if (syringe == null) {
            try {
                val syringeClass = Class.forName(className)
                syringe = syringeClass.getConstructor().newInstance() as ISyringe
                syringeCache[className] = syringe
            } catch (e: Exception) {
                // 如果没找到 Syringe 类，说明该类不需要注入
                return
            }
        }
        syringe?.inject(target)
    }

    /**
     * 获取服务实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getService(api: KClass<T>): T? {
        return RouteTable.services[api.java] as? T
    }
    
    // 兼容旧代码的 open 方法
    fun open(path: String, context: Context, block: (Intent.() -> Unit)? = null) {
        val postcard = build(path)
        checkAndLoadGroup(path)
        val meta = RouteTable.routes[path]
        if (meta == null) {
            val degradationService = getService(IDegradationService::class)
            if (degradationService != null) {
                degradationService.onLost(context, postcard)
            } else {
                throw RuntimeException("Route not found: $path")
            }
            return
        }
        val intent = Intent(context, meta.clazz)
        block?.invoke(intent)
        postcard.withBundle(intent.extras ?: Bundle())
        // 注意：原先 open 方法如果 block 设置了 flags，无法传给 postcard
        // 这里尽可能将 flags 也提取进 postcard，保证 navigate 正确执行
        postcard.withFlags(intent.flags)
        navigate(context, postcard)
    }
}