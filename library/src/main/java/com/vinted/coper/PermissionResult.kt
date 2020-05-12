package com.vinted.coper

/**
 * [PermissionResult] class is for [Coper.request] or [Coper.withPermissions] result handling.
 * Also for [PermissionResult] handling there is convenient contracts:
 * [isGranted] -> true if all permissions are granted
 * [isDenied] -> true if at least one permission were denied
 */
sealed class PermissionResult {

    /**
     * [PermissionResult] container if all permissions were granted.
     */
    class Granted internal constructor(
        val grantedPermissions: Collection<String>
    ) : PermissionResult()

    /**
     * [PermissionResult] container if at least one permission were denied.
     */
    class Denied internal constructor(
        private val deniedPermissions: List<DeniedPermission>
    ) : PermissionResult() {

        /**
         * Is denied permissions consists of denied rationale permission.
         */
        fun isRationale(): Boolean {
            return deniedPermissions
                .filterIsInstance<DeniedPermission.NeedsRationale>()
                .isNotEmpty()
        }

        /**
         * Is denied permissions consists of permanently denied permissions.
         */
        fun isPermanentlyDenied(): Boolean {
            return deniedPermissions
                .filterIsInstance<DeniedPermission.DeniedPermanently>()
                .isNotEmpty()
        }

        /**
         * Get all denied rationale permissions.
         */
        fun getDeniedRationale(): List<String> {
            return deniedPermissions
                .filterIsInstance<DeniedPermission.NeedsRationale>()
                .map { it.permission }
        }

        /**
         * Get all denied permanently permissions.
         */
        fun getDeniedPermanently(): List<String> {
            return deniedPermissions
                .filterIsInstance<DeniedPermission.DeniedPermanently>()
                .map { it.permission }
        }

        /**
         * Get all denied permissions.
         */
        fun getAllDeniedPermissions(): List<String> {
            return deniedPermissions.map { it.permission }
        }

        internal sealed class DeniedPermission {
            abstract val permission: String

            data class NeedsRationale(override val permission: String) : DeniedPermission()
            data class DeniedPermanently(override val permission: String) : DeniedPermission()
        }
    }

    /**
     * Execute [body] if all permissions were [Granted].
     */
    suspend fun onGranted(body: suspend (Granted) -> Unit): PermissionResult {
        if (this.isGranted()) {
            body(this)
        }
        return this
    }

    /**
     * Execute [body] if at least one permission were [Denied].
     */
    suspend fun onDenied(body: suspend (Denied) -> Unit): PermissionResult {
        if (this.isDenied()) {
            body(this)
        }
        return this
    }
}
