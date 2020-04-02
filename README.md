# Coper (Coroutines permissions) 

Library, which lets you to request android runtime permissions using coroutines. 
This library will create separate fragment in fragment manager and handle all permission request there.
Fragment lifecycle is covered by this library, so you should not worry about request cancellation.
It will only need Activity to create instance, which should be instance of FragmentActivity.
It is designed to use in any place like ViewModel as in fragment or activity.

### Api
```
val coper: Coper = CoperBuilder()
    .withActivity(activity) // activity must be instanceOf FragmentActivity
    .build()
```
```
val coper: Coper = CoperBuilder()
    .withFragment(fragment)
    .build()
```
###### Note:
You must provide either fragment or either activity.
##### Request example:
```
launch {
    val permissionResult: PermissionResult = coper.request(Manifest.permission.CAMERA)
}
```
### Result 
```
sealed class PermissionResult {
    /*
    * If all permission were granted it is stated, that request was succesfull
    */
    fun isSuccessful(): Boolean
    data class Granted(val grantedPermissions: Collection<String>) : PermissionResult()
    sealed class Denied(val deniedPermission: String) : PermissionResult() {
        data class JustDenied(deniedPermission: String) : Denied(deniedPermission)
        data class NeedsRationale(deniedPermission: String) : Denied(deniedPermission)
        data class DeniedPermanently(deniedPermission: String) : Denied(deniedPermission)
    }
}
```
### Usage
##### Result:
```
launch {
    val permissionResult = coper.request(Manifest.permission.CAMERA)
    if (permissionResult.isSuccessful()) {
        launchCamera()
    } else {
        showError(permissionResult)
    }
}
```
##### Support for multiple permissions request:
```
launch {
    val permissionResult = coper.request(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    if (permissionResult.isSuccesfull()) {
        launchCamera()
    } else {
        showError(permissionResult)
    }
}
```
###### Note:
If any of the requests fail, on the first failure, permission response emits failed item and request is over. 
E.g. you request 3 permissions (`CAMERA`, `READ_EXTERNAL_STORAGE` and `BLUETOOTH`), in case `CAMERA` is successful, `READ_EXTERNAL_STORAGE` is failure, response will be failure for `READ_EXTERNAL_STORAGE`.
##### If you have some optional permissions:
```
launch {
    if (coper.request(Manifest.permission.CAMERA).isSuccesfull()) {
        launchCamera()
    } else if (coper.request(Manifest.permission.READ_EXTERNAL_STORAGE).isSuccesfull()) {
        launchGallery()
    } else {
        goBackAndShowError()
    }
}
```
##### Also thinking to add api like this:
```
val permissionJob = coper.withPermissions(Manifest.permission.CAMERA) {
    launchCamera()
}.onDeny { denied -> // emits `PermissionResult.Denied`
    showInstructions(denied)
}
```
### Cancelation
Putting application to background or screen rotation will cancel all jobs with `PermissionCancelationException`. Getting back to application you should start this permission request one more time.

If you cancel job, it will cancel all permission requests.
