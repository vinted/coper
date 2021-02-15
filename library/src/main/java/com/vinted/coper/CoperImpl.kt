package com.vinted.coper

import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume


internal class CoperImpl(
    private val fragmentManager: FragmentManager,
    private val lifecycle: Lifecycle?,
) : Coper {

    private val fragmentInitializationMutex = Mutex()

    @VisibleForTesting
    internal val fragmentTransactionFlow = MutableStateFlow<CoperFragment?>(null)

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
        getFragmentSafely().requestPermission(permissions)
    }

    override suspend fun isRequestPendingSafe(): Boolean {
        return getFragmentSafely().isRequestPending()
    }

    override suspend fun isPermissionsGrantedSafe(vararg permissions: String): Boolean {
        return getFragmentSafely().isPermissionsGranted(permissions)
    }

    override fun isRequestPending(): Boolean {
        return getFragment().isRequestPending()
    }

    override fun isPermissionsGranted(vararg permissions: String): Boolean {
        return getFragment().isPermissionsGranted(permissions)
    }

    @Synchronized
    @Deprecated("Use getFragmentSafely")
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

    internal suspend fun getFragmentSafely(): CoperFragment {
        val fragment = getAttachedFragment()
        fragment.lifecycle.waitOnCreate()
        return fragment
    }

    private suspend fun getAttachedFragment(): CoperFragment {
        return fragmentInitializationMutex.withLock {
            val fragment = fragmentManager
                .findFragmentByTag(FRAGMENT_TAG) as? CoperFragment
            if (fragment == null) {
                val newFragment = CoperFragment()
                if (lifecycle != null) {
                    lifecycle.waitOnCreate()
                    newFragment.attachTo(fragmentManager)
                } else {
                    newFragment.attachTo(fragmentManager)
                }
            } else {
                fragment
            }
        }
    }

    private suspend fun CoperFragment.attachTo(
        fragmentManager: FragmentManager
    ): CoperFragment {
        return suspendCancellableCoroutine { continuation ->
            fragmentManager.beginTransaction()
                .add(this, FRAGMENT_TAG)
                .runOnCommit {
                    continuation.resume(this)
                }
                .commit()
            fragmentTransactionFlow.value = this
        }
    }

    private suspend fun Lifecycle.waitOnCreate() {
        return suspendCancellableCoroutine {
            addObserver(object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    this@waitOnCreate.removeObserver(this)
                    it.resume(Unit)
                }
            })
        }
    }

    companion object {
        internal const val FRAGMENT_TAG = "COPER_FRAGMENT"
    }
}
