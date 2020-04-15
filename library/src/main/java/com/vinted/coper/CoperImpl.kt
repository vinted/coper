package com.vinted.coper

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.ContinuationInterceptor


internal class CoperImpl(private val fragmentManager: FragmentManager) : Coper {

    private val mutex = Mutex()

    // Created because coroutines in tests are run on `BlockingEventLoop` or
    // `TestCoroutineDispatcher` dispatchers
    internal val isTestDispatcher = false

    override suspend fun request(vararg permissions: String): PermissionResult = coroutineScope {
        check(permissions.isNotEmpty()) { "Cant request 0 permissions" }
        check(isTestDispatcher || coroutineContext[ContinuationInterceptor] == Dispatchers.Main) {
            "Request handled on ${coroutineContext[ContinuationInterceptor]}, should be done on main dispatcher"
        }
        mutex.withLock {
            getFragment().requestPermission(permissions)
        }
    }

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
