package com.vinted.coper

import android.content.pm.PackageManager
import android.os.Looper.getMainLooper
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.vinted.coper.CoperImpl.Companion.FRAGMENT_TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.anyArray
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(manifest = Config.NONE, sdk = [23, 27])
class CoperImplTest {

    private val activityController = Robolectric.buildActivity(CoperTestActivity::class.java)
    private val activity = activityController.setup().get()
    private val fixture: CoperImpl = getCoperInstance()

    @Before
    fun setup() = runTest {
        runIdlingMainThread { fixture.mockGetFragmentWithStub() }
        mockCheckPermissions("test", PackageManager.PERMISSION_GRANTED)
    }

    @Test
    @Config(sdk = [23, 27])
    fun request_responseIsSuccessful() = runTest {
        val response = fixture.request("test")

        assertTrue(response.isGranted())
    }

    @Test
    @Config(sdk = [23, 27])
    fun request_permissionsIsGranted_grantedListConsistOfThisPermission() = runTest {
        val permissionName = "granted"
        mockCheckPermissions(permissionName, PackageManager.PERMISSION_GRANTED)

        val response = fixture.request(permissionName)

        assertTrue(response is PermissionResult.Granted)
        assertEquals(permissionName, response.grantedPermissions.first())
    }

    @Test
    @Config(sdk = [23, 27])
    fun request_twoPermissionsIsGranted_grantedListConsistOfThisPermissions() = runTest {
        val firstPermission = "granted"
        val secondPermission = "granted2"
        mockCheckPermissions(firstPermission, PackageManager.PERMISSION_GRANTED)
        mockCheckPermissions(secondPermission, PackageManager.PERMISSION_GRANTED)

        val response = fixture.request(firstPermission, secondPermission)

        assertTrue(response is PermissionResult.Granted)
        assertEquals(listOf(firstPermission, secondPermission), response.grantedPermissions)
    }

    @Test
    fun request_permissionsIsDeniedRationale_permissionDeniedRationaleResult() = runTest {
        val permissionName = "denied"
        mockCheckPermissions(permissionName, PackageManager.PERMISSION_DENIED)
        activity.setShouldShowRequestPermissionRationale(
            permission = permissionName,
            rationale = true
        )

        val response = executePermissionRequest(
            listOf(permissionName),
            listOf(PermissionChecker.PERMISSION_DENIED)
        )

        assertTrue(response is PermissionResult.Denied)
        assertTrue { response.isRationale() }
        assertTrue { response.getDeniedRationale().isNotEmpty() }
    }

    @Test
    fun request_permissionsIsDeniedPermanently_permissionDeniedPermanentlyResult() = runTest {
        val permissionName = "denied_perm"
        mockCheckPermissions(permissionName, PackageManager.PERMISSION_DENIED)
        activity.setShouldShowRequestPermissionRationale(
            permission = permissionName,
            rationale = false
        )

        val response = executePermissionRequest(
            permissionsToRequest = listOf(permissionName),
            permissionResult = listOf(PermissionChecker.PERMISSION_DENIED)
        )

        assertTrue(response is PermissionResult.Denied)
        assertTrue { response.isPermanentlyDenied() }
        assertTrue { response.getDeniedPermanently().isNotEmpty() }
    }

    @Test
    fun request_permissionsIsDeniedPermanentlyAndOtherRationale_bothPermissionsExist() = runTest {
        val permissionPermanently = "denied_perm"
        val permissionRationale = "denied_rat"
        mockCheckPermissions(permissionPermanently, PackageManager.PERMISSION_DENIED)
        activity.setShouldShowRequestPermissionRationale(
            permission = permissionPermanently,
            rationale = false
        )
        mockCheckPermissions(permissionRationale, PackageManager.PERMISSION_DENIED)
        activity.setShouldShowRequestPermissionRationale(
            permission = permissionRationale,
            rationale = true
        )

        val response = executePermissionRequest(
            permissionsToRequest = listOf(permissionPermanently, permissionRationale),
            permissionResult = listOf(
                PermissionChecker.PERMISSION_DENIED,
                PermissionChecker.PERMISSION_DENIED
            )
        )

        assertTrue(response is PermissionResult.Denied)
        assertTrue(response.isPermanentlyDenied())
        assertTrue(response.getDeniedPermanently().contains(permissionPermanently))
        assertTrue(response.isRationale())
        assertTrue(response.getDeniedRationale().contains(permissionRationale))
        assertTrue(
            response.getAllDeniedPermissions().containsAll(
                listOf(
                    permissionPermanently,
                    permissionRationale
                )
            )
        )
    }

    @Test
    fun request_twoPermissionsOneGrantedAndOneDenied_onePermissionEmitsToOnGrantedOtherToOnDenied() {
        return runTest {
            val permissionDenied = "denied"
            mockCheckPermissions(permissionDenied, PackageManager.PERMISSION_DENIED)
            val permissionGranted = "granted"
            mockCheckPermissions(permissionGranted, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequest(
                permissionsToRequest = listOf(permissionGranted, permissionDenied),
                permissionResult = listOf(
                    PermissionChecker.PERMISSION_GRANTED,
                    PermissionChecker.PERMISSION_DENIED
                )
            )

            response
                .onGranted {
                    assertTrue(it.grantedPermissions.contains(permissionGranted))
                }.onDenied {
                    assertTrue(it.getAllDeniedPermissions().contains(permissionDenied))
                }
        }
    }

    @Test
    fun request_permissionsIsDeniedButOnrequestGranted_permissionGrantedResult() = runTest {
        val permissionName = "denied_and_granted"
        mockCheckPermissions(permissionName, PackageManager.PERMISSION_DENIED)

        val response = executePermissionRequest(
            permissionsToRequest = listOf(permissionName),
            permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
        )

        assertTrue(response is PermissionResult.Granted)
        assertEquals(listOf(permissionName), response.grantedPermissions)
    }

    @Test
    fun request_twoPermissionsRequestBothGrantedOnRequest_permissionResultIsGrantedAndHasAllPermissions() {
        return runTest {
            val firstPermission = "first"
            val secondPermission = "second"
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequest(
                permissionsToRequest = listOf(
                    firstPermission,
                    secondPermission
                ),
                permissionResult = listOf(
                    PermissionChecker.PERMISSION_GRANTED,
                    PermissionChecker.PERMISSION_GRANTED
                )
            )

            assertTrue(response is PermissionResult.Granted)
            assertEquals(listOf(firstPermission, secondPermission), response.grantedPermissions)
        }
    }

    @Test
    fun request_twoPermissionsRequestOneGrantedOneNotOnRequest_permissionResultIsDeniedAndHasThatPermission() {
        return runTest {
            val firstPermission = "first"
            val secondPermission = "second"
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequest(
                permissionsToRequest = listOf(
                    firstPermission,
                    secondPermission
                ),
                permissionResult = listOf(
                    PermissionChecker.PERMISSION_GRANTED,
                    PermissionChecker.PERMISSION_DENIED
                )
            )

            assertTrue(response is PermissionResult.Denied)
            assertEquals(listOf(secondPermission), response.getAllDeniedPermissions())
        }
    }

    @Test
    fun request_twoPermissionsRequestOneDeniedPermOneRationale_permissionResultIsDeniedWithBoth() {
        return runTest {
            val firstPermission = "first"
            val secondPermission = "second"
            activity.setShouldShowRequestPermissionRationale(
                permission = firstPermission,
                rationale = false
            )
            activity.setShouldShowRequestPermissionRationale(
                permission = secondPermission,
                rationale = true
            )
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequest(
                permissionsToRequest = listOf(
                    firstPermission,
                    secondPermission
                ),
                permissionResult = listOf(
                    PermissionChecker.PERMISSION_DENIED,
                    PermissionChecker.PERMISSION_DENIED
                )
            )

            assertTrue(response is PermissionResult.Denied)
            assertEquals(
                listOf(firstPermission, secondPermission),
                response.getAllDeniedPermissions()
            )
        }
    }

    @Test
    fun request_twoParallelPermissionsRequestBothGranted_bothGotGranted() = runTest {
        val firstPermission = "first"
        val secondPermission = "second"
        mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
        mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

        val response1Job = async {
            executePermissionRequest(
                permissionsToRequest = listOf(firstPermission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
        }
        val response2Job = async {
            executePermissionRequest(
                permissionsToRequest = listOf(secondPermission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
        }
        val response1 = response1Job.await()
        val response2 = response2Job.await()

        assertTrue(response1 is PermissionResult.Granted)
        assertTrue(response2 is PermissionResult.Granted)
        assertNotEquals(response1.grantedPermissions, response2.grantedPermissions)
    }

    @Test
    fun request_twoParallelPermissionsRequestFromDiffReferences_bothGotGrantedSynchronously() {
        return runTest {
            val firstPermission = "first"
            val secondPermission = "second"
            val firstCoperReference = getCoperInstance()
            firstCoperReference.mockGetFragmentWithStub()
            val secondCoperReference = getCoperInstance()
            secondCoperReference.mockGetFragmentWithStub()
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response1Job = async {
                executePermissionRequest(
                    permissionsToRequest = listOf(firstPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED),
                    coperImplReference = firstCoperReference
                )
            }
            val response2Job = async {
                executePermissionRequest(
                    permissionsToRequest = listOf(secondPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED),
                    coperImplReference = secondCoperReference
                )
            }
            val response1 = response1Job.await()
            val response2 = response2Job.await()

            assertTrue(response1 is PermissionResult.Granted)
            assertTrue(response2 is PermissionResult.Granted)
            assertNotEquals(response1.grantedPermissions, response2.grantedPermissions)
        }
    }

    @Test
    fun request_twoSynchronousPermissionsRequestBothGranted_bothGotGranted() = runTest {
        val firstPermission = "first"
        val secondPermission = "second"
        mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
        mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

        val response1 = executePermissionRequest(
            permissionsToRequest = listOf(firstPermission),
            permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
        )
        val response2 = executePermissionRequest(
            permissionsToRequest = listOf(secondPermission),
            permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
        )

        assertTrue(response1 is PermissionResult.Granted)
        assertTrue(response2 is PermissionResult.Granted)
        assertNotEquals(response1.grantedPermissions, response2.grantedPermissions)
    }

    @Test
    fun getFragment_twoRequest_sameInstance() = runTest {
        val fixture = getCoperInstance()
        val firstFragment = fixture.getFragmentSafely()
        val secondFragment = fixture.getFragmentSafely()

        assertTrue { firstFragment === secondFragment }
    }

    @Test
    fun getFragment_manyRequestAsync_onlyOneFragmentCreated() = runTest {
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        val activity: FragmentActivity = activityController.setup().get()
        val fragmentManager = activity.supportFragmentManager
        val fixture = getCoperInstance(
            lifecycle = activity.lifecycle,
            fragmentManager = fragmentManager
        )
        val fragmentPromises = mutableListOf<Deferred<CoperFragment?>>()

        repeat(10) {
            fragmentPromises.add(async(Dispatchers.IO) {
                runCatching {
                    fixture.getFragmentSafely()
                }.getOrNull()
            })
        }

        fixture.latestCommittedFragmentFlow.filterNotNull().first()
        shadowOf(getMainLooper()).idle()
        val fragments = fragmentPromises.awaitAll().toSet()
        assertEquals(1, fragments.size)
        assertEquals(fragments.first(), fragmentManager.findFragmentByTag(FRAGMENT_TAG))
    }

    @Test
    fun getFragment_lifecycleAlreadyAfterCreated_fragmentTransactionMade() = runTest {
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        val activity = activityController.setup().pause().get()
        val fixture = getCoperInstance(
            lifecycle = activity.lifecycle,
            fragmentManager = activity.supportFragmentManager
        )

        val fragment = withTimeout(1000) {
            runIdlingMainThread { fixture.getFragmentSafely() }
        }

        assertEquals(fragment, activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG))
    }

    @Test
    fun request_onIoThread_shouldNotCrash() = runTest {
        val fixture = getCoperInstance()
        fixture.mockGetFragmentWithStub()

        withContext(Dispatchers.IO) {
            executePermissionRequest(
                coperImplReference = fixture,
                permissionsToRequest = listOf("test"),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun request_resultPermissionsEmptyList_throwException() = runTest {
        val crashPermission = "crash"
        mockCheckPermissions(crashPermission, PackageManager.PERMISSION_DENIED)


        executePermissionRequest(
            permissionsToRequest = emptyList(),
            permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
        )
    }

    @Test(expected = IllegalStateException::class)
    fun request_resultPermissionResultEmptyList_throwException() = runTest {
        val crashPermission = "crash"
        mockCheckPermissions(crashPermission, PackageManager.PERMISSION_DENIED)

        executePermissionRequest(listOf(crashPermission), emptyList())
    }

    @Test(expected = PermissionRequestCancelException::class)
    fun request_onDestroy_throwCancelException() = runTest {
        val permission = "onDestroy"

        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        val activity = activityController.setup().get()

        val fixture = getCoperInstance(
            fragmentManager = activity.supportFragmentManager,
            lifecycle = activity.lifecycle,
        )

        val fragment = runIdlingMainThread { fixture.getFragmentSafely() }

        val job = async {
            fixture.request(permission)
        }
        fragment.permissionRequestStateFlow.filterNotNull().first()
        activityController.destroy()
        job.await()
    }

    @Test
    fun request_onConfigurationChange_requestDone() = runTest {
        val activityController = Robolectric.buildActivity(CoperTestActivity::class.java)
        val activity = activityController.setup().get()
        val fragmentManager = activity.supportFragmentManager
        val fixture = getCoperInstance(
            lifecycle = activity.lifecycle,
            fragmentManager = fragmentManager
        )
        val fragment = runIdlingMainThread { fixture.getFragmentSafely() }
        val permission = "onConfigurationChange"
        activity.setPermissionResult(permission, PackageManager.PERMISSION_DENIED)

        val job = async {
            fixture.request(permission)
        }
        fragment.permissionRequestStateFlow.filterNotNull().first()
        activity.recreate()
        fragment.onRequestPermissionResult(
            permissions = listOf(permission),
            permissionsResult = listOf(PermissionChecker.PERMISSION_GRANTED),
            requestCode = CoperFragment.REQUEST_CODE
        )
        val result = job.await()

        assertEquals(PermissionResult.Granted::class, result::class)
    }

    @Test
    fun withPermissions_requestGranted_bodyRun() = runTest {
        val permission = "permission"
        mockCheckPermissions(permission, PackageManager.PERMISSION_DENIED)

        executeWithPermission(listOf(permission), listOf(PermissionChecker.PERMISSION_GRANTED)) {
            assertTrue { it.grantedPermissions.isNotEmpty() }
        }
    }

    @Test(expected = PermissionsRequestFailedException::class)
    fun withPermissions_requestDenied_throwException() = runTest {
        val permission = "permission"
        mockCheckPermissions(permission, PackageManager.PERMISSION_DENIED)

        executeWithPermission(listOf(permission), listOf(PermissionChecker.PERMISSION_DENIED)) {
        }
    }

    @Test
    fun request_permissionResultCameWithDifferentPermissions_jobIsNotCompleted() = runTest {
        val permission = "permission"
        mockCheckPermissions(permission, PackageManager.PERMISSION_DENIED)
        val fragment = fixture.getFragmentSafely()
        whenever(
            fragment.requestPermissions(
                eq(listOf(permission).toTypedArray()),
                anyOrNull()
            )
        ).then {
            fragment.onRequestPermissionResult(
                permissions = listOf("test"),
                permissionsResult = listOf(PermissionChecker.PERMISSION_GRANTED),
                requestCode = CoperFragment.REQUEST_CODE
            )
        }

        val responseAsync = async {
            fixture.request(permission)
        }

        assertFalse { responseAsync.isCompleted }
        responseAsync.cancel()
    }

    @Test
    fun request_onResumeCalledDuringRequest_permissionRequestStarted() = runTest {
        val permission = "onResume"
        val fragment = fixture.getFragmentSafely()
        mockCheckPermissions(permission, PermissionChecker.PERMISSION_DENIED)

        val responseAsync = async {
            fixture.request(permission)
        }
        fragment.permissionRequestStateFlow.filterNotNull().first()
        fragment.onResume()

        verify(fragment, times(2)).requestPermissions(anyArray(), anyOrNull())
        responseAsync.cancel()
    }

    @Test
    fun request_twoIdenticalRequest_twoRequestCompleted() = runTest {
        val permission = "sameRequest"
        mockCheckPermissions(permission, PermissionChecker.PERMISSION_DENIED)
        val responseAsync1 = async {
            executePermissionRequest(
                permissionsToRequest = listOf(permission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
        }
        val responseAsync2 = async {
            executePermissionRequest(
                permissionsToRequest = listOf(permission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
        }
        val result2 = responseAsync2.await()
        val result1 = responseAsync1.await()

        assertTrue(result1.isGranted())
        assertTrue(result2.isGranted())
    }

    @Test
    fun request_twoIdenticalRequestButOrderIsDifferent_twoRequestCompleted() = runTest {
        val firstPermission = "firstPermission"
        val secondPermission = "secondPermission"
        mockCheckPermissions(firstPermission, PermissionChecker.PERMISSION_DENIED)
        mockCheckPermissions(secondPermission, PermissionChecker.PERMISSION_DENIED)

        val responseAsync1 = async {
            executePermissionRequest(
                permissionsToRequest = listOf(firstPermission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
        }
        val responseAsync2 = async {
            executePermissionRequest(
                permissionsToRequest = listOf(secondPermission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
        }
        val result2 = responseAsync2.await()
        val result1 = responseAsync1.await()

        assertTrue(result1.isGranted())
        assertTrue(result2.isGranted())
    }

    @Test
    fun request_permissionsRequestedButDifferentOrderPermissionsCameAsResult_permissionsGranted() {
        return runTest {
            val firstPermission = "firstPermission"
            val secondPermission = "secondPermission"
            mockCheckPermissions(firstPermission, PermissionChecker.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PermissionChecker.PERMISSION_DENIED)
            val coperFragment = fixture.getFragmentSafely()
            whenever(
                coperFragment.requestPermissions(
                    eq(arrayOf(firstPermission, secondPermission)),
                    anyOrNull()
                )
            ).then {
                coperFragment.onRequestPermissionResult(
                    permissions = listOf(secondPermission, firstPermission),
                    permissionsResult = listOf(
                        PermissionChecker.PERMISSION_GRANTED,
                        PermissionChecker.PERMISSION_GRANTED
                    ),
                    requestCode = CoperFragment.REQUEST_CODE
                )
            }

            val response = fixture.request(firstPermission, secondPermission)

            assertTrue(response.isGranted())
        }
    }

    @Test
    fun isRequestPending_requestNotPending_returnsFalse() = runTest {
        val isPending = fixture.isRequestPendingSafe()

        assertFalse(isPending)
    }

    @Test
    fun isRequestPending_requestIsPending_returnsTrue() = runTest {
        mockCheckPermissions("test", PermissionChecker.PERMISSION_DENIED)
        val responseAsync = async {
            fixture.request("test")
        }
        fixture.getFragmentSafely().permissionRequestStateFlow.filterNotNull().first()

        val isPending = fixture.isRequestPendingSafe()

        assertTrue(isPending)
        responseAsync.cancel()
    }

    @Test
    @Config(sdk = [23, 27])
    fun isPermissionsGranted_permissionsNotGranted_returnsFalse() = runTest {
        mockCheckPermissions("not_granted", PermissionChecker.PERMISSION_DENIED)

        val isGranted = fixture.isPermissionsGrantedSafe("not_granted")

        assertFalse(isGranted)
    }

    @Test
    @Config(sdk = [23, 27])
    fun isPermissionsGranted_permissionsNotGrantedByOp_returnsFalse() = runTest {
        mockCheckPermissions("not_granted_op", PermissionChecker.PERMISSION_DENIED_APP_OP)

        val isGranted = fixture.isPermissionsGrantedSafe("not_granted_op")

        assertFalse(isGranted)
    }

    @Test
    @Config(sdk = [23, 27])
    fun isPermissionsGranted_permissionsGranted_returnsTrue() = runTest {
        mockCheckPermissions("granted", PermissionChecker.PERMISSION_GRANTED)

        val isGranted = fixture.isPermissionsGrantedSafe("granted")

        assertTrue(isGranted)
    }

    @Test
    fun request_manyParallelRequestsAllGranted_allGranted() = runTest {
        val testCount = 1000
        val requests = mutableListOf<Deferred<PermissionResult>>()
        for (i in 0 until testCount) {
            val permission = "test-$i"
            mockCheckPermissions(permission, PermissionChecker.PERMISSION_DENIED)

            requests.add(async {
                executePermissionRequest(
                    permissionsToRequest = listOf(permission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
                )
            })
        }
        val requestResults = requests.awaitAll()

        assertEquals(testCount, requestResults.size)
        requestResults.forEachIndexed { index, permissionResult ->
            assertTrue(permissionResult.isGranted())
            assertEquals("test-$index", permissionResult.grantedPermissions.first())
        }
    }

    @Test(expected = TimeoutCancellationException::class)
    fun getFragmentSafely_lifecycleNotReady_crashWithTimeout() = runTest {
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        val activity = activityController.get()

        val fixture = getCoperInstance(
            fragmentManager = activity.supportFragmentManager,
            lifecycle = activity.lifecycle,
        )

        withTimeout(100) {
            fixture.getFragmentSafely()
        }
    }

    @Test
    @Config(sdk = [23, 27])
    fun request_runOnPausingDispatcher_responseIsSuccessful() = runTest {
        val isCancelled = fixture.getFragmentSafely().lifecycleScope.launchWhenCreated {
            fixture.request("test")
        }.isCancelled
        assertFalse(isCancelled)
    }

    @Test(expected = IllegalStateException::class)
    fun request_permissionRequestReturnsEmptyResults_throwException() = runTest {
        val permissionName = "interrupted_permission"

        executePermissionRequest(
            permissionsToRequest = listOf(permissionName),
            permissionsInResults = emptyList(),
            permissionResult = emptyList()
        )
    }

    @Test(expected = IllegalStateException::class)
    fun request_permissionRequestReturnsDifferentPermissions_throwException() = runTest {
        val requestedPermission = "requested_permission"

        executePermissionRequest(
            permissionsToRequest = listOf(requestedPermission),
            permissionsInResults = listOf(requestedPermission),
            permissionResult = emptyList()
        )
    }

    @Test(expected = IllegalStateException::class)
    fun request_requestCodeIsNotOfCoperFragment_throwException() = runTest {
        val requestedPermission = "requested_permission"

        executePermissionRequest(
            permissionsToRequest = listOf(requestedPermission),
            permissionResult = listOf(PermissionChecker.PERMISSION_DENIED),
            requestCode = 0
        )
    }

    private suspend fun executePermissionRequest(
        permissionsToRequest: List<String>,
        permissionResult: List<Int>,
        coperImplReference: CoperImpl = fixture,
        permissionsInResults: List<String>? = null,
        requestCode: Int = CoperFragment.REQUEST_CODE,
    ): PermissionResult {
        return coroutineScope {
            stubRequestPermission(
                coperFragment = coperImplReference.getFragmentSafely(),
                permissions = permissionsToRequest,
                permissionResults = permissionResult,
                permissionsInResults = permissionsInResults,
                requestCode = requestCode
            )
            coperImplReference.request(*permissionsToRequest.toTypedArray())
        }
    }

    private suspend fun executeWithPermission(
        permissions: List<String>,
        permissionResult: List<Int>,
        coperImplReference: CoperImpl = fixture,
        onSuccess: (PermissionResult.Granted) -> Unit
    ) {
        coroutineScope {
            stubRequestPermission(
                coperFragment = coperImplReference.getFragmentSafely(),
                permissions = permissions,
                permissionResults = permissionResult
            )
            coperImplReference.withPermissions(*permissions.toTypedArray()) {
                onSuccess(it)
            }
        }
    }

    private fun stubRequestPermission(
        coperFragment: CoperFragment,
        permissions: List<String>,
        permissionResults: List<Int>,
        permissionsInResults: List<String>? = null,
        requestCode: Int = CoperFragment.REQUEST_CODE
    ) {
        whenever(
            coperFragment.requestPermissions(
                eq(permissions.toTypedArray()),
                anyOrNull()
            )
        ).then {
            coperFragment.onRequestPermissionResult(
                permissions = permissionsInResults ?: permissions,
                permissionsResult = permissionResults,
                requestCode = requestCode
            )
        }
    }

    private fun mockCheckPermissions(permission: String, result: Int) {
        activity.setPermissionResult(permission, result)
    }

    private fun getCoperInstance(
        fragmentManager: FragmentManager = activity.supportFragmentManager,
        lifecycle: Lifecycle? = activity.lifecycle,
    ): CoperImpl {
        return CoperImpl(fragmentManager, lifecycle)
    }

    private suspend fun CoperImpl.mockGetFragmentWithStub() {
        val coperFragment = getFragmentSafely()
        val spyCoperFragment = spy(coperFragment)
        // This is needed because spy creates new instance and stubbing needs exact reference
        coperFragment.requireActivity().supportFragmentManager.beginTransaction()
            .add(spyCoperFragment, FRAGMENT_TAG)
            .commitNow()
    }

    private suspend fun <T> runIdlingMainThread(block: suspend () -> T): T {
        return coroutineScope {
            val asyncBlock = async { block() }
            val asyncMainThreadIdle = async { shadowOf(getMainLooper()).idle() }
            awaitAll(asyncBlock, asyncMainThreadIdle).first() as T
        }
    }
}
