package com.wkq.base.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wkq.core.res.R

import com.wkq.util.pop.CommonPopupListener
import com.wkq.util.pop.PopupUtil
import com.wkq.util.showToast


/**
 * 专门处理权限申请的基类 Activity
 */
open class PermissionsActivity : AppCompatActivity() {

    private var permissionType = -1
    private var permissionList = mutableListOf<String>()

    // 权限请求启动器
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedPermissions = mutableListOf<String>()
        val deniedPermissions = mutableListOf<String>()
        val permanentlyDeniedPermissions = mutableListOf<String>()

        permissions.entries.forEach { entry ->
            if (entry.value) {
                grantedPermissions.add(entry.key)
            } else {
                deniedPermissions.add(entry.key)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (isPermissionPermanentlyDenied(entry.key)) {
                        permanentlyDeniedPermissions.add(entry.key)
                    }
                }
            }
        }

        if (deniedPermissions.isEmpty()) {
            authorized(permissionType, grantedPermissions)
        } else {
            if (permanentlyDeniedPermissions.isNotEmpty()) {
                // 如果权限被永久拒绝，弹窗引导用户去设置页面
                PopupUtil.createCommonPopupView(
                    this,
                    getString(R.string.permission_request_title),
                    getString(R.string.permission_permanently_denied),
                    getString(R.string.go_to_settings),
                    object : CommonPopupListener {
                        override fun sureClick() {
                            openAppDetailsSettings()
                        }

                        override fun cancelClick() {}
                    })
            } else {
                // 部分权限被拒绝，给个提示
                showToast(getString(R.string.partial_permission_denied, deniedPermissions.joinToString()))
            }
        }
    }

    // 设置页面返回后的监听
    private val openSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val allPermissionsGranted = permissionList.all { isGrantedOne(it) }
            if (allPermissionsGranted) {
                showToast(getString(R.string.permissions_granted))
            } else {
                showToast(getString(R.string.permissions_not_granted))
            }
        }

    /**
     * 请求应用权限
     * @param type 权限业务类型标识
     * @param permissions 权限列表
     */
    fun requestAppPermissions(type: Int, permissions: List<String>) {
        permissionType = type
        permissionList = permissions.toMutableList()
        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    /**
     * 检查是否已拥有指定权限
     */
    fun isGranted(permissions: List<String>?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return permissions?.all { isGrantedOne(it) } ?: false
    }

    private fun isGrantedOne(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 判断权限是否被永久拒绝
     */
    private fun isPermissionPermanentlyDenied(permission: String): Boolean {
        return !shouldShowRequestPermissionRationale(permission) &&
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取当前系统适配的媒体权限（自动兼容 Android 13/14）
     */
    fun getMediaPermissions(): MutableList<String> {
        return when {
            Build.VERSION.SDK_INT >= 34 -> mutableListOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> mutableListOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> mutableListOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * 跳转至系统设置页面
     */
    fun openSettingsPage(action: String, configure: (Intent.() -> Unit)? = null) {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            configure?.invoke(this)
        }
        startActivity(intent)
    }

    /**
     * 跳转至应用设置详情页
     */
    fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        openSettingsLauncher.launch(intent)
    }

    /**
     * 跳转通知设置页面
     */
    fun openNotificationSettings() { 
        PopupUtil.createCommonPopupView(
            this,
            getString(R.string.permission_request_setting_title),
            getString(R.string.permissions_granted_setting),
            getString(R.string.go_to_settings),
            object : CommonPopupListener {
                override fun sureClick() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        openSettingsPage(Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        }
                    } else {
                        openAppDetailsSettings()
                    }
                }

                override fun cancelClick() {}
            })
    }

    /**
     * 权限请求成功回调
     * @param permissionType 请求时传入的类型标识
     * @param permissionList 已授权的权限列表
     */
    open fun authorized(permissionType: Int, permissionList: MutableList<String>) {}
}
