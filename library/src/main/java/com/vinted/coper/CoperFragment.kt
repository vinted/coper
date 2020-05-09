package com.vinted.coper

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class CoperFragment : Fragment() {

    private val mutex = Mutex()

    // If job is not completed, then it is cancelled, when changed
    private var permissionRequestState: PermissionRequestState? = null
        set(value) {
            if (field?.deferred?.isCompleted == false) {
                val message =
                    "Request with: ${value?.permissions?.joinToString(", ")} created while other request with: ${field?.permissions?.joinToString(
                        ", "
                    )} were running"
                field?.deferred?.cancel(
                    PermissionRequestCancelException(message)
                )
            }
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    // This request makes sure that permissions will be handled after getting back from background
    override fun onResume() {
        super.onResume()
        val permissionRequest = permissionRequestState ?: return
        if (mutex.isLocked && !permissionRequest.deferred.isCompleted) {
            requestPermissions(permissionRequest.permissions.toTypedArray(), REQUEST_CODE)
        }
    }

    internal fun isPermissionsGranted(permissions: Array<out String>): Boolean {
        val permissionCheckResult = checkPermissions(permissions)
        val grantedPermissions = permissionCheckResult.filterGrantedPermissions()
        return grantedPermissions.isNotEmpty()
    }

    internal fun isRequestPending(): Boolean {
        val permissionRequestState = permissionRequestState
        return permissionRequestState != null && !permissionRequestState.deferred.isCompleted
    }

    internal suspend fun requestPermission(permissions: Array<out String>): PermissionResult {
        return mutex.withLock {
            val checkPermissionResults = checkPermissions(permissions)
            val result = getPermissionsResult(checkPermissionResults)
            permissionRequestState = null
            result
        }
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

    private suspend fun getPermissionsResult(
        permissionChecks: List<PermissionCheckResult>,
        grantedPermissions: List<String> = emptyList(),
        isSecondTime: Boolean = false
    ): PermissionResult {
        val granted = permissionChecks.filterGrantedPermissions()
        val denied = permissionChecks.filterDeniedPermissions()
        return if (denied.isEmpty()) {
            PermissionResult.Granted(grantedPermissions + granted)
        } else {
            if (isSecondTime) {
                getDeniedPermissionsResult(denied)
            } else {
                getPermissionsResult(
                    permissionChecks = requestPermissionsByVersion(denied.toTypedArray()),
                    grantedPermissions = granted,
                    isSecondTime = true
                )
            }
        }
    }

    private fun getDeniedPermissionsResult(permissions: List<String>): PermissionResult {
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

    // On devices with lower sdk than 23, there is no requesting permissions.
    private suspend fun requestPermissionsByVersion(
        permissions: Array<out String>
    ): List<PermissionCheckResult> {
        return if (isDeviceSdkAtLeast23()) {
            requestPermissionsAsync(permissions).await()
        } else {
            permissions.map { PermissionCheckResult.Denied(it) }
        }
    }

    private fun isDeviceSdkAtLeast23(): Boolean {
        return Build.VERSION.SDK_INT >= 23
    }

    private fun requestPermissionsAsync(
        permissions: Array<out String>
    ): Deferred<List<PermissionCheckResult>> {
        val requestDeferred = CompletableDeferred<List<PermissionCheckResult>>()
        this.permissionRequestState = PermissionRequestState(
            permissions = permissions.toList(),
            deferred = requestDeferred
        )
        requestPermissions(permissions, REQUEST_CODE)
        return requestDeferred
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Permissions result came with not Coper request code: $requestCode")
            return
        }
        if (permissions.isEmpty() && grantResults.isEmpty()) {
            Log.i(TAG, "Permissions result and permissions came empty")
            return
        }
        onRequestPermissionResult(permissions.toList(), grantResults.toList())
    }

    internal fun onRequestPermissionResult(
        permissions: List<String>,
        permissionsResult: List<Int>
    ) {
        val permissionRequestState = permissionRequestState
        if (permissionRequestState == null) {
            Log.e(TAG, "Something went wrong with permission request state")
            return
        }
        if (permissionRequestState.permissions.toSet() != permissions.toSet()) {
            Log.e(
                TAG,
                "Permissions (${permissions.joinToString(", ")}) result came not as requested (${permissionRequestState.permissions.joinToString(
                    ", "
                )})"
            )
            return
        }
        if (permissionsResult.size != permissions.size) {
            permissionRequestState.deferred.completeExceptionally(
                PermissionRequestCancelException("Permissions ${permissions.size} is not same size as permissionResult: ${permissionsResult.size}")
            )
            return
        }
        val checkPermissionResult = permissionsResult.mapIndexed { index, result ->
            PermissionCheckResult.of(
                permissionResult = result,
                permission = permissions[index]
            )
        }
        permissionRequestState.deferred.complete(checkPermissionResult)
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionRequestState = null
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

    private data class PermissionRequestState(
        val permissions: List<String>,
        val deferred: CompletableDeferred<List<PermissionCheckResult>>
    )

    companion object {
        private const val TAG = "CoperFragment"
        private const val REQUEST_CODE = 11111
    }
}
