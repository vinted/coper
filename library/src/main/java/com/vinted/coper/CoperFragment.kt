package com.vinted.coper

import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal class CoperFragment : Fragment() {

    private var requestDeferred: CompletableDeferred<List<PermissionCheckResult>>? = null

    internal suspend fun requestPermission(permissions: Array<out String>): PermissionResult {
        val checkPermissionResults = checkPermissions(permissions)
        return getPermissionResult(checkPermissionResults)
    }

    private fun checkPermissions(permissions: Array<out String>): List<PermissionCheckResult> {
        val permissionResults = mutableListOf<PermissionCheckResult>()
        permissions.forEach { permission ->
            val permissionResult = PermissionCheckResult.of(
                permissionResult = ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    permission
                ),
                permission = permission
            )
            permissionResults.add(permissionResult)
        }
        return permissionResults
    }

    private suspend fun getPermissionResult(
        permissionChecks: List<PermissionCheckResult>,
        granted: List<String> = emptyList(),
        isSecondTime: Boolean = false
    ): PermissionResult {
        val grantedPermissions = permissionChecks.filterGrantedPermissions()
        val deniedPermissions = permissionChecks.filterDeniedPermissions()
        return if (deniedPermissions.isEmpty()) {
            PermissionResult.Granted(granted + grantedPermissions.toList())
        } else {
            if (isSecondTime) {
                getDeniedPermissionResult(deniedPermissions)
            } else {
                val result = requestPermissionAsync(deniedPermissions.toTypedArray()).await()
                getPermissionResult(
                    permissionChecks = result,
                    granted = grantedPermissions,
                    isSecondTime = true
                )
            }
        }
    }

    private fun getDeniedPermissionResult(permissions: List<String>): PermissionResult {
        val deniedPermissions = permissions.map { permission ->
            val needsRationale = ActivityCompat
                .shouldShowRequestPermissionRationale(requireActivity(), permission)
            if (needsRationale) {
                PermissionResult.Denied.DeniedPermission.NeedsRationale(permission)
            } else {
                PermissionResult.Denied.DeniedPermission.DeniedPermanently(permission)
            }
        }
        return PermissionResult.Denied(deniedPermissions)
    }

    private fun requestPermissionAsync(
        permissions: Array<out String>
    ): Deferred<List<PermissionCheckResult>> {
        val requestDeferred = CompletableDeferred<List<PermissionCheckResult>>()
        this.requestDeferred = requestDeferred
        requestPermissions(permissions, REQUEST_CODE)
        return requestDeferred
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE) return
        onRequestPermissionResult(permissions.toList(), grantResults.toList())
    }

    internal fun onRequestPermissionResult(
        permission: List<String>,
        permissionResult: List<Int>
    ) {
        val requestDeferred = requestDeferred ?: return
        if (permissionResult.isEmpty() ||
            permission.isEmpty() ||
            permissionResult.size != permission.size
        ) {
            requestDeferred.completeExceptionally(
                IllegalStateException("One of result is empty: permission: $permission, permissionResult: $permissionResult")
            )
            return
        }
        val checkPermissionResult = permissionResult.mapIndexed { index, result ->
            PermissionCheckResult.of(
                permissionResult = result,
                permission = permission[index]
            )
        }
        requestDeferred.complete(checkPermissionResult)
    }

    private fun List<PermissionCheckResult>.filterGrantedPermissions(): List<String> {
        return this.filterIsInstance<PermissionCheckResult.Granted>()
            .map { it.permission }
    }

    private fun List<PermissionCheckResult>.filterDeniedPermissions(): List<String> {
        return this.filterIsInstance<PermissionCheckResult.Denied>()
            .map { it.permission }
    }

    private sealed class PermissionCheckResult {
        data class Granted(val permission: String) : PermissionCheckResult()
        data class Denied(val permission: String) : PermissionCheckResult()

        companion object {
            fun of(permissionResult: Int, permission: String): PermissionCheckResult {
                return when (permissionResult) {
                    PermissionChecker.PERMISSION_GRANTED -> Granted(permission)
                    PermissionChecker.PERMISSION_DENIED,
                    PermissionChecker.PERMISSION_DENIED_APP_OP -> Denied(permission)
                    else -> throw IllegalStateException("Not expected permission result: $permissionResult for $permission")
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 11111
    }
}
