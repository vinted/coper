package com.vinted.coper

import androidx.fragment.app.FragmentActivity

class CoperTestActivity : FragmentActivity() {

    private val permissionResult = mutableMapOf<String, Int>()
    private val shouldShowRequestPermissionRationaleResult = mutableMapOf<String, Boolean>()

    fun setPermissionResult(permission: String, result: Int) {
        permissionResult[permission] = result
    }

    fun setShouldShowRequestPermissionRationale(permission: String, rationale: Boolean) {
        shouldShowRequestPermissionRationaleResult[permission] = rationale
    }

    override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
        return permissionResult[permission] ?: super.checkPermission(permission, pid, uid)
    }

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return shouldShowRequestPermissionRationaleResult[permission]
            ?: super.shouldShowRequestPermissionRationale(permission)
    }
}
