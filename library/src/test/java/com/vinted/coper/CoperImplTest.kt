package com.vinted.coper

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentActivity
import com.nhaarman.mockitokotlin2.*
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
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1, Build.VERSION_CODES.LOLLIPOP])
class CoperImplTest {

    private val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
    private val activity: FragmentActivity = spy(activityController.setup().get())
    private val shadowActivity = shadowOf(activity)
    private val fixture: CoperImpl = spy(CoperImpl(activity.supportFragmentManager))

    @Before
    fun setup() = runBlocking {
        whenever(fixture.isTestDispatcher).thenReturn(true)
        fixture.mockGetFragmentWithStub()
        mockCheckPermissions("test", PackageManager.PERMISSION_GRANTED)
    }

    @Test
    fun request_responseIsSuccessful() = runBlocking {
        val response = fixture.request("test")

        assertTrue(response.isGranted())
    }

    @Test
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
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
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
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
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
            assertFalse { response.isRationale() }
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
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
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
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

    // Paralel tests are run on version above or equal to 23 sdk, because, they can be stubbed
    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
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
            delay(1)
            val response2Job = async {
                executePermissionRequest(
                    permissions = listOf(secondPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
                )
            }
            delay(100)
            val response1 = response1Job.await()
            val response2 = response2Job.await()

            assertTrue(response1 is PermissionResult.Granted)
            assertTrue(response2 is PermissionResult.Granted)
            assertNotEquals(response1.grantedPermissions, response2.grantedPermissions)
        }
    }

    // Paralel tests are run on version above or equal to 23 sdk, because, they can be stubbed
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
    @Test(timeout = 10000)
    fun request_twoParallelPermissionsRequestFromDiffReferences_bothGotGrantedSynchronously() {
        runBlocking {
            val firstPermission = "first"
            val secondPermission = "second"
            val firstCoperReference = spy(CoperImpl(activity.supportFragmentManager))
            whenever(firstCoperReference.isTestDispatcher).thenReturn(true)
            firstCoperReference.mockGetFragmentWithStub()
            val secondCoperReference = spy(CoperImpl(activity.supportFragmentManager))
            whenever(secondCoperReference.isTestDispatcher).thenReturn(true)
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
            delay(1)
            val response2Job = async {
                executePermissionRequest(
                    permissions = listOf(secondPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED),
                    coperImplReference = secondCoperReference
                )
            }
            delay(100)
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
            val fixture = CoperImpl(activity.supportFragmentManager)
            val firstFragment = fixture.getFragment()
            val secondFragment = fixture.getFragment()

            assertTrue { firstFragment === secondFragment }
        }
    }

    @Test(expected = IllegalStateException::class, timeout = 10000)
    fun request_onIoThread_shouldCrash() {
        val fixture = CoperImpl(activity.supportFragmentManager)
        runBlocking(Dispatchers.IO) {
            fixture.request("test")
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

    @Test(expected = PermissionRequestCancelException::class, timeout = 1000)
    fun request_onDestroy_throwCancelException() {
        val permission = "onDestroy"
        mockCheckPermissions(permission, PackageManager.PERMISSION_DENIED)

        runBlocking {
            val job = async {
                fixture.request(permission)
            }
            delay(5)
            activity.supportFragmentManager
                .beginTransaction()
                .remove(fixture.getFragment())
                .commitNow()
            job.await()
        }
    }

    // On api 21 permission request result with shadowActivity is returned by its initial deny
    // permission value, because permission were handled differently on api 21.
    // So this test will run on api 23 and api 27
    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1, Build.VERSION_CODES.M])
    fun request_onConfigurationChange_requestDone() {
        runBlocking {
            val fixture = spy(CoperImpl(activity.supportFragmentManager))
            whenever(fixture.isTestDispatcher).thenReturn(true)
            val fragment = fixture.getFragment()
            val permission = "onConfigurationChange"
            shadowActivity.denyPermissions(permission)

            val job = async {
                fixture.request(permission)
            }
            delay(5)
            activity.recreate()
            postRequestResult(
                coperFragment = fragment,
                permissions = listOf(permission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
            val result = job.await()

            assertEquals(PermissionResult.Granted::class, result::class)
        }
    }

    @Test
    fun withPermissions_requestGranted_bodyRun() = runBlocking {
        val permission = "permission"
        shadowActivity.denyPermissions(permission)

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
        val fragment = fixture.getFragment()

        val responseAsync = async {
            fixture.request(permission)
        }
        postRequestResult(
            fragment,
            listOf("test"),
            listOf(PermissionChecker.PERMISSION_GRANTED)
        )
        delay(5)

        assertFalse { responseAsync.isCompleted }
        responseAsync.cancel()
    }

    @Test
    fun request_onResumeCalledDuringRequest_permissionRequestStarted() {
        runBlocking {
            val permission = "onResume"
            val fragment = fixture.getFragment()
            mockCheckPermissions(permission, PermissionChecker.PERMISSION_DENIED)

            val responseAsync = async {
                fixture.request(permission)
            }
            delay(5)
            fragment.onResume()

            verify(fragment, times(2)).requestPermissions(anyArray(), anyOrNull())
            responseAsync.cancel()
        }
    }

    // Paralel tests are run on version above or equal to 23 sdk, because, they can be stubbed
    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
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
            delay(5)
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

    // Paralel tests are run on version above or equal to 23 sdk, because, they can be stubbed
    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
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
            delay(5)
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
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
    fun request_permissionsRequestedButDifferentOrderPermissionsCameAsResult_permissionsGranted_version27() {
        runBlocking {
            val firstPermission = "firstPermission"
            val secondPermission = "secondPermission"
            mockCheckPermissions(firstPermission, PermissionChecker.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PermissionChecker.PERMISSION_DENIED)
            val coperFragment = fixture.getFragment()
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
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun request_permissionsRequestedButDifferentOrderPermissionsCameAsResult_permissionsGranted_version21() {
        runBlocking {
            val firstPermission = "firstPermission"
            val secondPermission = "secondPermission"
            mockCheckPermissions(firstPermission, PermissionChecker.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PermissionChecker.PERMISSION_DENIED)
            val coperFragment = fixture.getFragment()

            val responseAsync = async {
                fixture.request(firstPermission, secondPermission)
            }
            postRequestResult(
                coperFragment = coperFragment,
                permissions = listOf(secondPermission, firstPermission),
                permissionResult = listOf(
                    PermissionChecker.PERMISSION_GRANTED,
                    PermissionChecker.PERMISSION_GRANTED
                )
            )
            val response = responseAsync.await()

            assertTrue(response.isGranted())
        }
    }

    private suspend fun executePermissionRequest(
        permissions: List<String>,
        permissionResult: List<Int>,
        coperImplReference: CoperImpl = fixture
    ): PermissionResult {
        return coroutineScope {
            if (Build.VERSION.SDK_INT >= 23) {
                stubRequestPermission(
                    coperFragment = coperImplReference.getFragment(),
                    permissions = permissions,
                    permissionResults = permissionResult
                )
                coperImplReference.request(*permissions.toTypedArray())
            } else {
                val responseAsync = async {
                    coperImplReference.request(*permissions.toTypedArray())
                }
                postRequestResult(coperImplReference.getFragment(), permissions, permissionResult)
                responseAsync.await()
            }
        }
    }

    private suspend fun executeWithPermission(
        permissions: List<String>,
        permissionResult: List<Int>,
        coperImplReference: CoperImpl = fixture,
        onSuccess: (PermissionResult.Granted) -> Unit
    ) {
        coroutineScope {
            if (Build.VERSION.SDK_INT >= 23) {
                stubRequestPermission(
                    coperFragment = coperImplReference.getFragment(),
                    permissions = permissions,
                    permissionResults = permissionResult
                )
                coperImplReference.withPermissions(*permissions.toTypedArray()) {
                    onSuccess(it)
                }
            } else {
                val responseAsync = async {
                    coperImplReference.withPermissions(*permissions.toTypedArray()) {
                        onSuccess(it)
                    }
                }
                postRequestResult(coperImplReference.getFragment(), permissions, permissionResult)
                responseAsync.await()
            }
        }
    }

    private suspend fun postRequestResult(
        coperFragment: CoperFragment,
        permissions: List<String>,
        permissionResult: List<Int>
    ) = coroutineScope {
        delay(5)
        coperFragment.onRequestPermissionResult(
            permissions = permissions,
            permissionsResult = permissionResult
        )
    }

    // Only available above api 23
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

    private fun CoperImpl.mockGetFragmentWithStub() {
        val coperFragment = spy(this.getFragment())
        whenever(coperFragment.activity).thenReturn(activity)
        whenever(this.getFragment()).thenReturn(coperFragment)
    }
}
