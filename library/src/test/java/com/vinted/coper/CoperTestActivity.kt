package com.vinted.coper

import androidx.fragment.app.FragmentActivity

class CoperTestActivity : FragmentActivity() {
    private val shouldShowRequestPermissionRationaleResult = mutableMapOf<String, Boolean>()

    fun setShouldShowRequestPermissionRationale(permission: String, rationale: Boolean) {
        shouldShowRequestPermissionRationaleResult[permission] = rationale
    }

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return shouldShowRequestPermissionRationaleResult[permission]
            ?: super.shouldShowRequestPermissionRationale(permission)
    }
}
