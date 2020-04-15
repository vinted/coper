package com.vinted.coper

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentActivity
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.eq
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1, Build.VERSION_CODES.LOLLIPOP])
class CoperImplTest {

    private val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
    private lateinit var fixture: CoperImpl
    private lateinit var activity: FragmentActivity

    @Before
    fun setup() = runBlocking {
        activity = spy(activityController.setup().get())
        fixture = spy(CoperImpl(activity.supportFragmentManager))
        whenever(fixture.isTestDispatcher).thenReturn(true)
        val coperFragment = spy(fixture.getFragment())
        whenever(coperFragment.activity).thenReturn(activity)
        whenever(fixture.getFragment()).thenReturn(coperFragment)
        mockCheckPermissions("test", PackageManager.PERMISSION_GRANTED)
    }

    @Test
    fun request_responseIsSuccessful() = runBlocking {
        val response = fixture.request("test")

        assertTrue(response.isSuccessful())
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

            val response = executePermissionRequestAsync(
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

            val response = executePermissionRequestAsync(
                permissions = listOf(permissionName),
                permissionResult = listOf(PermissionChecker.PERMISSION_DENIED)
            )

            assertTrue(response is PermissionResult.Denied)
            assertFalse { response.isRationale() }
        }
    }

    @Test
    fun request_permissionsIsDeniedButOnrequestGranted_permissionGrantedResult() {
        runBlocking {
            val permissionName = "denied_and_granted"
            mockCheckPermissions(permissionName, PackageManager.PERMISSION_DENIED)

            val response = executePermissionRequestAsync(
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

            val response = executePermissionRequestAsync(
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

            val response = executePermissionRequestAsync(
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

            val response = executePermissionRequestAsync(
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

    // Because this is parallel request execution, it is safer to leave this with timeout
    @Ignore("This parallel fails unexpected, because it is hard to make sure, that every execution will be handled as expected")
    @Test(timeout = 10000)
    fun request_twoParallelPermissionsRequestBothGranted_bothGotGranted() {
        runBlocking {
            val firstPermission = "first"
            val secondPermission = "second"
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response1Job = async {
                executePermissionRequestAsync(
                    permissions = listOf(firstPermission),
                    permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
                )
            }
            delay(1)
            val response2Job = async {
                executePermissionRequestAsync(
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

    @Test
    fun request_twoSynchronousPermissionsRequestBothGranted_bothGotGranted() {
        runBlocking {
            val firstPermission = "first"
            val secondPermission = "second"
            mockCheckPermissions(firstPermission, PackageManager.PERMISSION_DENIED)
            mockCheckPermissions(secondPermission, PackageManager.PERMISSION_DENIED)

            val response1 = executePermissionRequestAsync(
                permissions = listOf(firstPermission),
                permissionResult = listOf(PermissionChecker.PERMISSION_GRANTED)
            )
            val response2 = executePermissionRequestAsync(
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
            executePermissionRequestAsync(
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
            executePermissionRequestAsync(listOf(crashPermission), emptyList())
        }
    }

    private suspend fun executePermissionRequestAsync(
        permissions: List<String>,
        permissionResult: List<Int>
    ): PermissionResult {
        return coroutineScope {
            val responseAsync = async {
                fixture.request(*permissions.toTypedArray())
            }
            delay(5)
            fixture.getFragment().onRequestPermissionResult(
                permission = permissions,
                permissionResult = permissionResult
            )
            responseAsync.await()
        }
    }

    private fun mockCheckPermissions(permission: String, result: Int) {
        whenever(activity.checkPermission(eq(permission), anyOrNull(), anyOrNull()))
            .thenReturn(result)
    }
}
