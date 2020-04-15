# Coper (Coroutines permissions) 

[![Build Status](https://travis-ci.com/vinted/coper.svg?token=jJbXr9K9ZKMgFDkycBtv&branch=master)](https://travis-ci.com/vinted/coper)

Library, which lets you to request android runtime permissions using coroutines. 
This library will create separate fragment in fragment manager and handle all permission request there.
Fragment lifecycle is covered by this library, so you should not worry about request cancellation.
It is designed to use in any place like ViewModel as in fragment or activity.

### Api
```
val coper: Coper = CoperBuilder()
    .setFragmentManager(fragmentManager)
    .build()
```
###### Note:
You must provide fragment manager to build coper.
### Usage
##### Request:
```
launch {
    val permissionResult: PermissionResult = coper.request(Manifest.permission.CAMERA)
}
```
##### Example:
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
val permissionResult = coper.request(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

```
###### Note:
If any of the requests fail, then request returns `PermissionsResult.Denied` with all denied permissions and its deny value (denied rationale or denied permanently). 
E.g. you request 3 permissions (`CAMERA`, `READ_EXTERNAL_STORAGE` and `BLUETOOTH`), in case `CAMERA` is successful, `READ_EXTERNAL_STORAGE` is failure, response will be failure for `READ_EXTERNAL_STORAGE`.
##### Error handling:
```
if(permissionResult is Denied) {
    if(permissionResult.isRationale()) {
        showRationale(permissionResult.getDeniedRationale())
    } else {
        showPermanent(permissionResult.getDeniedPermanently())
    }
}
```
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
