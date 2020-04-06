package com.vinted.coper

import androidx.fragment.app.FragmentManager

internal class CoperImpl(private val fragmentManager: FragmentManager) : Coper {
    override suspend fun request(vararg permissions: String): PermissionResult {
        // TODO Implement
        return PermissionResult.Granted(emptyList())
    }
}
