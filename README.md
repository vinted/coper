# Coper (Coroutines permissions) 

[![Build Status](https://travis-ci.com/vinted/coper.svg?token=jJbXr9K9ZKMgFDkycBtv&branch=master)](https://travis-ci.com/vinted/coper)
[![](https://jitpack.io/v/vinted/coper.svg)](https://jitpack.io/#vinted/coper)

Library, which lets you to request android runtime permissions using coroutines. 
This library will create separate fragment in fragment manager and handle all permission request there.
Fragment lifecycle is covered by this library, so you should not worry about request cancellation.
It is designed to use in any place like ViewModel as in fragment or activity.

### Download
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
	
dependencies {
    implementation 'com.github.vinted:coper:1.0'
}
```

### Api
```
val coper: Coper = CoperBuilder()
    .setFragmentActivity(activity)
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
##### Execute body if all permissions enabled or throw error:
```
coper.withPermissions(Manifest.permission.CAMERA) {
    launchCamera()
}
```
###### Note:
If permission will not be granted, then request crash with `PermissionsRequestFailedException`
##### Additional functionality:
###### Request pending check:
```
coper.isRequestPendingSafe()
```
###### Granted permissions check:
```
// Returns true if all permissions granted
coper.isPermissionsGrantedSafe(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
```
### Cancelation
If you cancel job, request will be left until user will submit, but client will not get response.
### State recreation
Sometimes after application killed (for example temporally killing to preserve memory), the dialog could be still visible, but reference to request will be lost.
Putting application to background or `onConfigurationChange` will not affect request.
### License
```
MIT License

Copyright (c) 2020 Vinted UAB

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
