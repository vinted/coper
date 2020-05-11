package com.vinted.coper

/**
 * To create coper instance use [CoperBuilder].
 * ```
 * val coper = CoperBuilder()
 *      .setFragmentManager(fragmentManager)
 *      .build()
 * ```
 */
interface Coper {

    /**
     * Request for permissions at runtime and get permission result, when user submits all permissions.
     * Request could only be done on [kotlinx.coroutines.Dispatchers.Main], otherwise will throw [IllegalStateException].
     * Example:
     * ```
     * coper.request(Manifest.permission.CAMERA)
     *      .onGranted { grantedResult ->
     *          doSomethingOnGrant()
     *      }.onDenied {
     *          doSomethingOnDeny()
     *      }
     * ```
     * @return [PermissionResult] which could be [PermissionResult.Granted] or [PermissionResult.Denied]
     * @throws IllegalStateException if [permissions] will be empty, [withPermissions]
     * @throws PermissionRequestCancelException to [withPermissions] job, if fragment will be destroyed while [withPermissions] is pending
     */
    suspend fun request(vararg permissions: String): PermissionResult

    /**
     * Request permissions [withPermissions] and execute [onSuccess] if all permissions granted.
     * [onSuccess] will have [PermissionResult.Granted] in body.
     * Request could only be done on [kotlinx.coroutines.Dispatchers.Main], otherwise will throw [IllegalStateException].
     * Example:
     * ```
     * coper.withPermissions(Manifest.permission.CAMERA) { grantedResult ->
     *      doSomethingOnGranted(grantedResult)
     * }
     * ```
     * @throws PermissionsRequestFailedException if permission were denied
     * @throws IllegalStateException if [permissions] will be empty, [withPermissions]
     * @throws PermissionRequestCancelException to [withPermissions] job, if fragment will be destroyed while [withPermissions] is pending
     */
    suspend fun withPermissions(
        vararg permissions: String,
        onSuccess: suspend (PermissionResult.Granted) -> Unit
    )

    fun isRequestPending(): Boolean

    /**
     * @return true if all [permissions] is granted, false if at least one denied.
     */
    fun isPermissionsGranted(vararg permissions: String): Boolean
}
