package com.vinted.coper

import kotlin.contracts.contract

/**
 * @return true -> if all permissions granted, otherwise false
 */
fun PermissionResult.isGranted(): Boolean {
    contract { returns() implies (this@isGranted is PermissionResult.Granted) }
    return this is PermissionResult.Granted
}

/**
 * @return true -> if at least one permission were denied, otherwise false
 */
fun PermissionResult.isDenied(): Boolean {
    contract { returns() implies (this@isDenied is PermissionResult.Denied) }
    return this is PermissionResult.Denied
}
