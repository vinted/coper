package com.vinted.coper

import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class CoperFragment : Fragment() {

    private val mutex = Mutex()

    // If job is not completed, then it is cancelled, when changed
    private var permissionRequestState: PermissionRequestState? = null
        set(value) {
            if (field?.deferred?.isCompleted == false) {
                val requestedPermissions = value?.permissions?.joinToString(", ")
                val currentPermissions = field?.permissions?.joinToString(", ")
                val message = "Request with: $requestedPermissions + created while other request" +
                        " with: $currentPermissions were running"
                field?.deferred?.cancel(
                    PermissionRequestCancelException(message)
                )
            }
            field = value
            _permissionRequestStateFlow.value = value
        }

    private val _permissionRequestStateFlow = MutableStateFlow(permissionRequestState)
    @VisibleForTesting
    internal val permissionRequestStateFlow = _permissionRequestStateFlow.asStateFlow()

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
                    permissionChecks = requestPermissionsAsync(denied.toTypedArray()).await(),
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
        onRequestPermissionResult(permissions.toList(), grantResults.toList(), requestCode)
    }

    internal fun onRequestPermissionResult(
        permissions: List<String>,
        permissionsResult: List<Int>,
        requestCode: Int
    ) {
        if (requestCode != REQUEST_CODE) {
            val message = "Permissions result came with not Coper request code: $requestCode"
            Log.e(TAG, message)
            permissionRequestState?.deferred?.completeExceptionally(
                PermissionRequestCancelException(message)
            )
            return
        }

        if (permissions.isEmpty() && permissionsResult.isEmpty()) {
            val message = "Permissions result and permissions came empty"
            Log.i(TAG, message)
            permissionRequestState?.deferred?.completeExceptionally(
                PermissionRequestCancelException(message)
            )
            return
        }

        val permissionRequestState = permissionRequestState
        if (permissionRequestState == null) {
            Log.e(TAG, "Something went wrong with permission request state")
            return
        }
        if (permissionRequestState.permissions.toSet() != permissions.toSet()) {
            val permissionsInResult = permissions.joinToString(", ")
            val currentRequestedPermissions = permissionRequestState.permissions.joinToString(", ")
            val message = "Permissions ($permissionsInResult) result came not as requested " +
                    "($currentRequestedPermissions)"
            Log.e(TAG, message)
            permissionRequestState.deferred.completeExceptionally(
                PermissionRequestCancelException(message)
            )
            return
        }
        if (permissionsResult.size != permissions.size) {
            val message = "Permissions ${permissions.size} is not same size as " +
                    "permissionResult: ${permissionsResult.size}"
            permissionRequestState.deferred.completeExceptionally(
                PermissionRequestCancelException(message)
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

    internal sealed class PermissionCheckResult {
        data class Granted(val permission: String) : PermissionCheckResult()
        data class Denied(val permission: String) : PermissionCheckResult()

        companion object {
            fun of(permissionResult: Int, permission: String): PermissionCheckResult {
                return when (permissionResult) {
                    PermissionChecker.PERMISSION_GRANTED -> Granted(permission)
                    else -> Denied(permission)
                }
            }
        }
    }

    internal data class PermissionRequestState(
        val permissions: List<String>,
        val deferred: CompletableDeferred<List<PermissionCheckResult>>
    )

    companion object {
        private const val TAG = "CoperFragment"
        internal const val REQUEST_CODE = 11111
    }
}
