package com.vinted.coper

sealed class PermissionResult {
    fun isSuccessful(): Boolean = this is Granted

    class Granted internal constructor(
        val grantedPermissions: Collection<String>
    ) : PermissionResult()

    class Denied internal constructor(
        private val deniedPermissions: List<DeniedPermission>
    ) : PermissionResult() {

        /*
        * Is denied permissions consists of denied rationale permission
        */
        fun isRationale(): Boolean {
            return deniedPermissions
                .filterIsInstance<DeniedPermission.NeedsRationale>()
                .isNotEmpty()
        }

        /*
        * Is denied permissions consists of permanently denied permissions
        */
        fun isPermanentlyDenied(): Boolean {
            return deniedPermissions
                .filterIsInstance<DeniedPermission.DeniedPermanently>()
                .isNotEmpty()
        }

        /*
        * Get all denied rationale permissions
        */
        fun getDeniedRationale(): List<String> {
            return deniedPermissions
                .filterIsInstance<DeniedPermission.NeedsRationale>()
                .map { it.deniedPermission }
        }

        /*
        * Get all denied permanently permissions
        */
        fun getDeniedPermanently(): List<String> {
            return deniedPermissions
                .filterIsInstance<DeniedPermission.DeniedPermanently>()
                .map { it.deniedPermission }
        }

        /*
        * Get all denied permissions
        */
        fun getAllDeniedPermissions(): List<String> {
            return deniedPermissions.map { it.deniedPermission }
        }

        internal sealed class DeniedPermission {
            abstract val deniedPermission: String

            data class NeedsRationale(override val deniedPermission: String) : DeniedPermission()
            data class DeniedPermanently(override val deniedPermission: String) : DeniedPermission()
        }
    }
}
