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
###### Note:
Calling permission on a background thread or request with 0 permissions will throw `IllegalStateException`
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
        Manifest.permission.CAMERA, // granted
        Manifest.permission.READ_EXTERNAL_STORAGE, // granted
        Manifest.permission.BLUETOOTH //granted
)
permissionResult.isGranted() // returns true
```
```
val permissionResult = coper.request(
        Manifest.permission.CAMERA, // granted
        Manifest.permission.READ_EXTERNAL_STORAGE, // denied
        Manifest.permission.BLUETOOTH //granted
)
permissionResult.isGranted() // returns false
```
###### Note:
If any of the requests fail, then request returns `PermissionsResult.Denied` with all denied permissions and its deny value (denied rationale or denied permanently). 
##### Error handling:
```
if(permissionResult.isDenied()) {
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
##### Permission result callbacks: 
```
coper.request(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.CAMERA
).onSuccess { grantedResult ->
    handleGrantedPermission(grantedResult)
}.onDeny { deniedResult ->
    handleDeniedPermission(deniedResult)
}
```
##### Also thinking to add api like this:
```
coper.withPermissions(Manifest.permission.CAMERA) {
    launchCamera()
}
```
###### Note:
If permission will not be granted, then request crash with `PermissionsRequestFailedException`
### Cancelation
If you cancel job, request will be left until user will submit, but client will not get response.
### State recreation
Sometimes after application killed (for example temporally killing to preserve memory), the dialog could be still visible, but reference to request will be lost.
Putting application to background or `onConfigurationChange` will not affect request.
