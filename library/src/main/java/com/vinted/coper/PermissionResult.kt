package com.vinted.coper

sealed class PermissionResult {
    fun isSuccessful(): Boolean = this is Granted

    data class Granted(val grantedPermissions: Collection<String>) : PermissionResult()
    sealed class Denied : PermissionResult() {
        abstract val deniedPermission: String
        data class JustDenied(override val deniedPermission: String) : Denied()
        data class NeedsRationale(override val deniedPermission: String) : Denied()
        data class DeniedPermanently(override val deniedPermission: String) : Denied()
    }
}
