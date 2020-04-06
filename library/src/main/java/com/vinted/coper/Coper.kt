package com.vinted.coper

interface Coper {

    suspend fun request(vararg permissions: String): PermissionResult
}
