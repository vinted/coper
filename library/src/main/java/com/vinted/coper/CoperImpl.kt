package com.vinted.coper

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume


internal class CoperImpl(
    private val fragmentManager: FragmentManager,
    private val lifecycle: Lifecycle?,
    private val fragmentAwaitingTimeout: Long?,
) : Coper {

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

    @Synchronized
    internal suspend fun getFragmentSafely(): CoperFragment {
        return withTimeout(fragmentAwaitingTimeout ?: Long.MAX_VALUE) {
            val deferred = CompletableDeferred<CoperFragment>()
            val fragment = getAttachedFragment()
            fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    deferred.complete(fragment)
                }
            })
            deferred.await()
        }
    }

    private suspend fun getAttachedFragment(): CoperFragment {
        return suspendCancellableCoroutine { continuation ->
            val fragment = fragmentManager
                .findFragmentByTag(FRAGMENT_TAG) as? CoperFragment
            if (fragment == null) {
                val newFragment = CoperFragment()
                if (lifecycle != null) {
                    lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onCreate(owner: LifecycleOwner) {
                            attachFragment(newFragment, continuation::resume)
                        }
                    })
                } else {
                    attachFragment(newFragment, continuation::resume)
                }
            } else {
                continuation.resume(fragment)
            }
        }
    }

    private fun attachFragment(
        fragment: CoperFragment,
        onAttached: (CoperFragment) -> Unit
    ) {
        fragmentManager.beginTransaction()
            .add(fragment, FRAGMENT_TAG)
            .runOnCommit {
                onAttached(fragment)
            }
            .commit()
    }

    companion object {
        internal const val FRAGMENT_TAG = "COPER_FRAGMENT"
    }
}
