import { NativeModules, PermissionsAndroid } from 'react-native'

export async function captureImage(returnBase64 = false) {
    var cPermission = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.CAMERA)
    if (cPermission)
        return NativeModules.CameraModule.openCamera(returnBase64)
    else {
        let result = await requestCameraPermission()
        if (result === PermissionsAndroid.RESULTS.GRANTED) {
            return NativeModules.CameraModule.openCamera(returnBase64)
        } else {
            return Promise.reject("Permission Denied", result)
        }
    }
}

export function openImagePicker() { return NativeModules.CameraModule.pickImage() }

function requestCameraPermission() {
    return PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.CAMERA
    );

}