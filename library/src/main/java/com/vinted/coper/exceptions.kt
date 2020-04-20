package com.vinted.coper

import kotlinx.coroutines.CancellationException

class PermissionRequestCancelException(
    override val message: String?
) : CancellationException()

class PermissionsRequestFailedException(
    override val message: String?
) : Exception()
