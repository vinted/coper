package com.vinted.coper

import android.content.pm.PackageManager
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.nhaarman.mockitokotlin2.*
import com.vinted.coper.CoperBuilder.Companion.DEFAULT_FRAGMENT_PREPARATION_TIMEOUT
import kotlinx.coroutines.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.eq
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [23, 27])
class CoperImplTest {

    private val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
    private val activity: FragmentActivity = spy(activityController.setup().get())
    private val shadowActivity = shadowOf(activity)
    private val fixture: CoperImpl = spy(getCoperInstance())

    @Before
    fun setup() = runBlocking {
        fixture.mockGetFragmentWithStub()
        mockCheckPermissions("test", PackageManager.PERMISSION_GRANTED)
    }

    @Test
    @Config(sdk = [21, 23, 27])
    fun request_responseIsSuccessful() = runBlocking {
        val response = fixture.request("test")

        assertTrue(response.isGranted())
    }

    @Test
    @Config(sdk = [21, 23, 27])
    fun request_permissionsIsGranted_grantedListConsistOfThisPermission() {
        runBlocking {
            val permissionName = "granted"
            mockCheckPermissions(permissionName, PackageManager.PERMISSION_GRANTED)

            val response = fixture.request(permissionName)

            assertTrue(response is PermissionResult.Granted)
            assertEquals(permissionName, response.grantedPermissions.first())
        }
    }

    @Test
    @Config(sdk = [21, 23, 27])
    fun request_twoPermissionsIsGranted_grantedListConsistOfThisPermissions() {
        runBlocking {
            val firstPermission = "granted"
            val secondPermission = "granted2"
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_GRANTED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_GRANTED)

            val response = fixture.request(firstPermission, secondPermission)

            assertTrue(response is PermissionResult.Granted)
            assertEquals(listOf(firstPermission, secondPermission), response.grantedPermissions)
        }
    }

    @Test
    @Config(sdk = [21])
    fun request_sdkUnder23AndPermissionsDenied_permissionResultDeniedWaitingResult() {
        runBlocking {
            val permission = "denied"
            mockCheckPermissions(permission, PackageManager.PERMISSION_DENIED)

            val response = fixture.request(permission)

            assertTrue(response.isDenied())
            assertEquals(listOf(permission), response.getAllDeniedPermissions())
        }
    }

    @Test
    fun request_permissionsIsDeniedRationale_permissionDeniedRationaleResult() {
        runBlocking {
            val permissionName = "denied"
            mockCheckPermissions(permissionName, PackageManager.PERMISSION_DENIED)
            whenever(activity.shouldShowRequestPermissionRationale(permissionName)).thenReturn(true)

            val response = executePermissionRequest(
                listOf(permissionName),
                listOf(PermissionChecker.PERMISSION_DENIED)
            )

            assertTrue(response is PermissionResult.Denied)
            assertTrue { response.isRationale() }
            assertTrue { response.getDeniedRationale().isNotEmpty() }
        }
    }

    @Test
    fun request_permissionsIsDeniedPermanently_permissionDeniedPermanentlyResult() {
        runBlocking {
            val permissionName = "denied_perm"
            mockCheckPermissions(permissionName, PackageManager.PERMISSION_DENIED)
            whenever(activity.shouldShowRequestPermissionRationale(permissionName)).thenReturn(false)

            val response = executePermissionRequest(
                permissions = listOf(permissionName),
                permissionResult = listOf(PermissionChecker.PERMISSION_DENIED)
            )

            assertTrue(response is PermissionResult.Denied)
            assertTrue { response.isPermanentlyDenied() }
            assertTrue { response.getDeniedPermanently().isNotEmpty() }
        }
    }

    @Test
    fun request_permissionsIsDeniedPermanentlyAndOtherRationale_bothPermissionsExist() {
        runBlocking {
            val permissionPermanently = "denied_perm"
            val permissionRationale = "denied_rat"
            mockCheckPermissions(permissionPermanently, PackageManager.PERMISSION_DENIED)
            whenever(activity.shouldShowRequestPermissionRationale(permissionPermanently)).thenReturn(
                false
            )
            mockCheckPermissions(permissionRationale, PackageManager.PERMISSION_DENIED)
            whenever(activity.shouldShowRequestPermissionRationale(permissionRationale)).thenReturn(
                true
            )

            val response = executePermissionRequest(
                permissions = listOf(permissionPermanently, permissionRationale),
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
    }

    @Test
    fun request_twoPermissionsOneGrantedAndOneDenied_onePermissionEmitsToOnGrantedOtherToOnDenied() {
        runBlocking {
            val permissionDenied = "denied"
            mockCheckPermissions(permissionDenied, PackageManager.PERMISSION_DENIED)
            val permissionGranted = "granted"
            mockCheckPermissions(permissionGranted, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequest(
                permissions = listOf(permissionGranted, permissionDenied),
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
    fun request_permissionsIsDeniedButOnrequestGranted_permissionGrantedResult() {
        runBlocking {
            val permissionName = "denied_and_granted"
            mockCheckPermissions(permissionName, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequest(
                permissions = listOf(permissionName),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )

            assertTrue(response is PermissionResult.Granted)
            assertEquals(listOf(permissionName), response.grantedPermissions)
        }
    }

    @Test
    fun request_twoPermissionsRequestBothGrantedOnRequest_permissionResultIsGrantedAndHasAllPermissions() {
        runBlocking {
            val firstPermission = "first"
            val secondPermission = "second"
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequest(
                permissions = listOf(
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
        runBlocking {
            val firstPermission = "first"
            val secondPermission = "second"
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequest(
                permissions = listOf(
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
        runBlocking {
            val firstPermission = "first"
            val secondPermission = "second"
            whenever(activity.shouldShowRequestPermissionRationale("first")).thenReturn(false)
            whenever(activity.shouldShowRequestPermissionRationale("second")).thenReturn(true)
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequest(
                permissions = listOf(
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
    fun request_twoParallelPermissionsRequestBothGranted_bothGotGranted() {
        runBlocking {
            val firstPermission = "first"
            val secondPermission = "second"
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response1Job = async {
                executePermissionRequest(
                    permissions = listOf(firstPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
                )
            }
            val response2Job = async {
                executePermissionRequest(
                    permissions = listOf(secondPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
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
    fun request_twoParallelPermissionsRequestFromDiffReferences_bothGotGrantedSynchronously() {
        runBlocking {
            val firstPermission = "first"
            val secondPermission = "second"
            val firstCoperReference = spy(getCoperInstance())
            firstCoperReference.mockGetFragmentWithStub()
            val secondCoperReference = spy(getCoperInstance())
            secondCoperReference.mockGetFragmentWithStub()
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response1Job = async {
                executePermissionRequest(
                    permissions = listOf(firstPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED),
                    coperImplReference = firstCoperReference
                )
            }
            val response2Job = async {
                executePermissionRequest(
                    permissions = listOf(secondPermission),
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
    fun request_twoSynchronousPermissionsRequestBothGranted_bothGotGranted() {
        runBlocking {
            val firstPermission = "first"
            val secondPermission = "second"
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response1 = executePermissionRequest(
                permissions = listOf(firstPermission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
            val response2 = executePermissionRequest(
                permissions = listOf(secondPermission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )

            assertTrue(response1 is PermissionResult.Granted)
            assertTrue(response2 is PermissionResult.Granted)
            assertNotEquals(response1.grantedPermissions, response2.grantedPermissions)
        }
    }

    @Test
    fun getFragment_twoRequest_sameInstance() {
        runBlocking {
            val fixture = getCoperInstance()
            val firstFragment = fixture.getFragmentSafely()
            val secondFragment = fixture.getFragmentSafely()

            assertTrue { firstFragment === secondFragment }
        }
    }

    @Test
    fun request_onIoThread_shouldNotCrash() {
        runBlocking {
            val fixture = spy(getCoperInstance())
            fixture.mockGetFragmentWithStub()

            withContext(Dispatchers.IO) {
                executePermissionRequest(
                    coperImplReference = fixture,
                    permissions = listOf("test"),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
                )
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun request_resultPermissionsEmptyList_throwException() {
        val crashPermission = "crash"
        mockCheckPermissions(crashPermission, PackageManager.PERMISSION_DENIED)

        runBlocking {
            executePermissionRequest(
                permissions = emptyList(),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun request_resultPermissionResultEmptyList_throwException() {
        val crashPermission = "crash"
        mockCheckPermissions(crashPermission, PackageManager.PERMISSION_DENIED)

        runBlocking {
            executePermissionRequest(listOf(crashPermission), emptyList())
        }
    }

    @Test(expected = PermissionRequestCancelException::class)
    fun request_onDestroy_throwCancelException() {
        runBlocking {
            val permission = "onDestroy"

            val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
            val activity = spy(activityController.setup().get())

            val fixture = getCoperInstance(
                fragmentManager = activity.supportFragmentManager,
                lifecycle = activity.lifecycle,
                timeout = DEFAULT_FRAGMENT_PREPARATION_TIMEOUT
            )

            val fragment = fixture.getFragmentSafely()

            val job = async {
                fixture.request(permission)
            }
            fragment.waitUntilRequestStart()
            activityController.destroy()
            job.await()
        }
    }

    @Test
    fun request_onConfigurationChange_requestDone() {
        runBlocking {
            val fixture = spy(getCoperInstance())
            val fragment = fixture.getFragmentSafely()
            val permission = "onConfigurationChange"
            shadowActivity.denyPermissions(permission)

            val job = async {
                fixture.request(permission)
            }
            fixture.getFragmentSafely().waitUntilRequestStart()
            activity.recreate()
            fragment.onRequestPermissionResult(
                permissions = listOf(permission),
                permissionsResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
            val result = job.await()

            assertEquals(PermissionResult.Granted::class, result::class)
        }
    }

    @Test
    fun withPermissions_requestGranted_bodyRun() = runBlocking {
        val permission = "permission"
        mockCheckPermissions(permission, PackageManager.PERMISSION_DENIED)

        executeWithPermission(listOf(permission), listOf(PermissionChecker.PERMISSION_GRANTED)) {
            assertTrue { it.grantedPermissions.isNotEmpty() }
        }
    }

    @Test(expected = PermissionsRequestFailedException::class)
    fun withPermissions_requestDenied_throwException() = runBlocking {
        val permission = "permission"
        mockCheckPermissions(permission, PackageManager.PERMISSION_DENIED)

        executeWithPermission(listOf(permission), listOf(PermissionChecker.PERMISSION_DENIED)) {
        }
    }

    @Test
    fun request_permissionResultCameWithDifferentPermissions_jobIsNotCompleted() = runBlocking {
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
                permissionsResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
        }

        val responseAsync = async {
            fixture.request(permission)
        }

        assertFalse { responseAsync.isCompleted }
        responseAsync.cancel()
    }

    @Test
    fun request_onResumeCalledDuringRequest_permissionRequestStarted() {
        runBlocking {
            val permission = "onResume"
            val fragment = fixture.getFragmentSafely()
            mockCheckPermissions(permission, PermissionChecker.PERMISSION_DENIED)

            val responseAsync = async {
                fixture.request(permission)
            }
            fragment.waitUntilRequestStart()
            fragment.onResume()

            verify(fragment, times(2)).requestPermissions(anyArray(), anyOrNull())
            responseAsync.cancel()
        }
    }

    @Test
    fun request_twoIdenticalRequest_twoRequestCompleted() {
        runBlocking {
            val permission = "sameRequest"
            mockCheckPermissions(permission, PermissionChecker.PERMISSION_DENIED)
            val responseAsync1 = async {
                executePermissionRequest(
                    permissions = listOf(permission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
                )
            }
            val responseAsync2 = async {
                executePermissionRequest(
                    permissions = listOf(permission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
                )
            }
            val result2 = responseAsync2.await()
            val result1 = responseAsync1.await()

            assertTrue(result1.isGranted())
            assertTrue(result2.isGranted())
        }
    }

    @Test
    fun request_twoIdenticalRequestButOrderIsDifferent_twoRequestCompleted() {
        runBlocking {
            val firstPermission = "firstPermission"
            val secondPermission = "secondPermission"
            mockCheckPermissions(firstPermission, PermissionChecker.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PermissionChecker.PERMISSION_DENIED)

            val responseAsync1 = async {
                executePermissionRequest(
                    permissions = listOf(firstPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
                )
            }
            val responseAsync2 = async {
                executePermissionRequest(
                    permissions = listOf(secondPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
                )
            }
            val result2 = responseAsync2.await()
            val result1 = responseAsync1.await()

            assertTrue(result1.isGranted())
            assertTrue(result2.isGranted())
        }
    }

    @Test
    fun request_permissionsRequestedButDifferentOrderPermissionsCameAsResult_permissionsGranted() {
        runBlocking {
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
                    )
                )
            }

            val response = fixture.request(firstPermission, secondPermission)

            assertTrue(response.isGranted())
        }
    }

    @Test
    fun isRequestPending_requestNotPending_returnsFalse() {
        runBlocking {
            val isPending = fixture.isRequestPendingSafe()

            assertFalse(isPending)
        }
    }

    @Test
    fun isRequestPending_requestIsPending_returnsTrue() {
        mockCheckPermissions("test", PermissionChecker.PERMISSION_DENIED)
        runBlocking {
            val responseAsync = async {
                fixture.request("test")
            }
            fixture.getFragmentSafely().waitUntilRequestStart()

            val isPending = fixture.isRequestPendingSafe()

            assertTrue(isPending)
            responseAsync.cancel()
        }
    }

    @Test
    @Config(sdk = [21, 23, 27])
    fun isPermissionsGranted_permissionsNotGranted_returnsFalse() {
        runBlocking {
            mockCheckPermissions("not_granted", PermissionChecker.PERMISSION_DENIED)

            val isGranted = fixture.isPermissionsGrantedSafe("not_granted")

            assertFalse(isGranted)
        }
    }

    @Test
    @Config(sdk = [21, 23, 27])
    fun isPermissionsGranted_permissionsNotGrantedByOp_returnsFalse() {
        runBlocking {
            mockCheckPermissions("not_granted_op", PermissionChecker.PERMISSION_DENIED_APP_OP)

            val isGranted = fixture.isPermissionsGrantedSafe("not_granted_op")

            assertFalse(isGranted)
        }
    }

    @Test
    @Config(sdk = [21, 23, 27])
    fun isPermissionsGranted_permissionsGranted_returnsTrue() {
        runBlocking {
            mockCheckPermissions("granted", PermissionChecker.PERMISSION_GRANTED)

            val isGranted = fixture.isPermissionsGrantedSafe("granted")

            assertTrue(isGranted)
        }
    }

    @Test
    fun request_manyParallelRequestsAllGranted_allGranted() {
        runBlocking {
            val testCount = 1000
            val requests = mutableListOf<Deferred<PermissionResult>>()
            for (i in 0 until testCount) {
                val permission = "test-$i"
                mockCheckPermissions(permission, PermissionChecker.PERMISSION_DENIED)

                requests.add(async {
                    executePermissionRequest(
                        permissions = listOf(permission),
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
    }

    @Test(expected = TimeoutCancellationException::class)
    fun getFragmentSafely_lifecycleNotReady_crashWithTimeout() {
        runBlocking {
            val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
            val activity = spy(activityController.get())

            val fixture = getCoperInstance(
                fragmentManager = activity.supportFragmentManager,
                lifecycle = activity.lifecycle,
                timeout = DEFAULT_FRAGMENT_PREPARATION_TIMEOUT
            )

            fixture.getFragmentSafely()
        }
    }

    @Test
    @Config(sdk = [21, 23, 27])
    fun request_runOnPausingDispatcher_responseIsSuccessful() = runBlocking {
        val isCancelled = fixture.getFragmentSafely().lifecycleScope.launchWhenCreated {
            fixture.request("test")
        }.isCancelled
        assertFalse(isCancelled)
    }

    private suspend fun executePermissionRequest(
        permissions: List<String>,
        permissionResult: List<Int>,
        coperImplReference: CoperImpl = fixture
    ): PermissionResult {
        return coroutineScope {
            stubRequestPermission(
                coperFragment = coperImplReference.getFragmentSafely(),
                permissions = permissions,
                permissionResults = permissionResult
            )
            coperImplReference.request(*permissions.toTypedArray())
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
        permissionResults: List<Int>
    ) {
        whenever(
            coperFragment.requestPermissions(
                eq(permissions.toTypedArray()),
                anyOrNull()
            )
        ).then {
            coperFragment.onRequestPermissionResult(
                permissions = permissions,
                permissionsResult = permissionResults
            )
        }
    }

    private fun mockCheckPermissions(permission: String, result: Int) {
        whenever(activity.checkPermission(eq(permission), anyOrNull(), anyOrNull()))
            .thenReturn(result)
    }

    private fun getCoperInstance(
        fragmentManager: FragmentManager = activity.supportFragmentManager,
        lifecycle: Lifecycle = activity.lifecycle,
        timeout: Long? = null
    ): CoperImpl {
        return CoperImpl(fragmentManager, lifecycle, timeout)
    }

    private suspend fun CoperImpl.mockGetFragmentWithStub() {
        val coperFragment = spy(this.getFragmentSafely())
        whenever(coperFragment.activity).doReturn(activity)
        whenever(this.getFragmentSafely()).doReturn(coperFragment)
        whenever(this.getFragment()).doReturn(coperFragment)
    }
}
