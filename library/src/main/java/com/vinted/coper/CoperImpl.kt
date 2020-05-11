package com.vinted.coper

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.ContinuationInterceptor


internal class CoperImpl(private val fragmentManager: FragmentManager) : Coper {

    // Created because coroutines in tests are run on `BlockingEventLoop` or
    // `TestCoroutineDispatcher` dispatchers
    internal val isTestDispatcher = false

    override suspend fun withPermissions(
        vararg permissions: String,
        onSuccess: suspend (PermissionResult.Granted) -> Unit
    ) {
        val result = requestPermissions(permissions)
        if (result.isGranted()) {
            onSuccess(result)
        } else {
            throw PermissionsRequestFailedException("One of ${permissions.joinToString(", ")} were not granted")
        }

    }

    override suspend fun request(vararg permissions: String): PermissionResult {
        return requestPermissions(permissions)
    }

    private suspend fun requestPermissions(permissions: Array<out String>) = coroutineScope {
        check(permissions.isNotEmpty()) { "Cant request 0 permissions" }
        check(isTestDispatcher || coroutineContext[ContinuationInterceptor] == Dispatchers.Main) {
            "Request handled on ${coroutineContext[ContinuationInterceptor]}, should be done on main dispatcher"
        }
        getFragment().requestPermission(permissions)
    }

    override fun isRequestPending(): Boolean {
        return getFragment().isRequestPending()
    }

    override fun isPermissionsGranted(vararg permissions: String): Boolean {
        return getFragment().isPermissionsGranted(permissions)
    }

    @Synchronized
    internal fun getFragment(): CoperFragment {
        val fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? CoperFragment
        return if (fragment == null) {
            val newFragment = CoperFragment()
            fragmentManager.beginTransaction()
                .add(newFragment, FRAGMENT_TAG)
                .commitNow()
            newFragment
        } else {
            fragment
        }
    }

    companion object {
        internal const val FRAGMENT_TAG = "COPER_FRAGMENT"
    }
}
