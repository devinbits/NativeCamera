package com.nativecamera;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static java.io.File.createTempFile;

public class CameraModule extends ReactContextBaseJavaModule {

    ReactApplicationContext rnContext;
    Promise mCameraPromise = null;

    private Uri photoURI = null;

    public CameraModule(@Nullable ReactApplicationContext reactContext) {
        super(reactContext);
        this.rnContext = reactContext;
        if (reactContext != null) {
            reactContext.addActivityEventListener(listener);
        }
    }

    @NonNull
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @ReactMethod
    public void openCamera(boolean getThumbNail, Promise promise) {
        mCameraPromise = promise;
        Activity currentActivity = getCurrentActivity();
        if (getThumbNail) dispatchThumbnailIntent(currentActivity);
        else dispatchFullPictureIntent(currentActivity);
    }

    ActivityEventListener listener = new BaseActivityEventListener() {

        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_CANCELED) {
                mCameraPromise.reject(Constants.E_REQUEST_CANCELLED, "Request was cancelled");
                return;
            }

            if (requestCode == Constants.IMAGE_PICKER_REQUEST) {
                if (mCameraPromise != null && resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    if (uri == null) {
                        mCameraPromise.reject(Constants.E_NO_IMAGE_DATA_FOUND, "No image data found");
                    } else {
                        mCameraPromise.resolve(sendResponse(uri));
                    }
                }
            } else if (requestCode == Constants.CAMERA_REQUEST_THUMBNAIL && resultCode == Activity.RESULT_OK) {
                if (data.getExtras() == null) {
                    mCameraPromise.reject(Constants.E_NO_IMAGE_DATA_FOUND, "No image data found");
                    return;
                }
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                if (photo != null) {
                    photo.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                }
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                mCameraPromise.resolve(sendResponse(encoded));
            } else if (requestCode == Constants.CAMERA_REQUEST_FULLSIZE && resultCode == Activity.RESULT_OK) {
                if (photoURI == null) {
                    mCameraPromise.reject(Constants.E_NO_IMAGE_DATA_FOUND, "No image data found");
                } else {
                    mCameraPromise.resolve(sendResponse(photoURI));
                }
            } else mCameraPromise.reject(Constants.E_REQUEST_FAILED, "No image data found");
            mCameraPromise = null;
            photoURI = null;
        }
    };

    @ReactMethod
    public void pickImage(final Promise promise) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            promise.reject(Constants.E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
            return;
        }
        mCameraPromise = promise;

        try {
            final Intent galleryIntent = new Intent(Intent.ACTION_PICK);
            galleryIntent.setType("image/*");
            final Intent chooserIntent = Intent.createChooser(galleryIntent, "Pick an image");
            currentActivity.startActivityForResult(chooserIntent, Constants.IMAGE_PICKER_REQUEST);
        } catch (Exception e) {
            mCameraPromise.reject(Constants.E_FAILED_TO_SHOW_PICKER, e);
            mCameraPromise = null;
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        File storageDir = rnContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return createTempFile(
                "temp_jpg",  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    private void dispatchFullPictureIntent(Activity currentActivity) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile;
        try {
            photoFile = createImageFile();
            photoURI = FileProvider.getUriForFile(rnContext,
                    currentActivity.getPackageName() + ".fileprovider",
                    photoFile);
//            rnContext.grantUriPermission(getReactApplicationContext().getPackageName()
//                    , photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            currentActivity.startActivityForResult(takePictureIntent, Constants.CAMERA_REQUEST_FULLSIZE, null);
        } catch (Exception ex) {
            mCameraPromise.reject(Constants.E_FAILED_TO_START_CAMERA, ex.getMessage());
        }
    }

    public void dispatchThumbnailIntent(Activity currentActivity) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            currentActivity.startActivityForResult(takePictureIntent, Constants.CAMERA_REQUEST_THUMBNAIL, null);
        } catch (Exception ex) {
            mCameraPromise.reject(Constants.E_FAILED_TO_START_CAMERA, ex.getMessage());
        }
    }

    public WritableMap sendResponse(Uri uri) {
        ContentResolver contentResolver = rnContext.getContentResolver();
        Cursor cursor =
                contentResolver.query(uri, null, null, null, null);

        if (cursor == null)
            return new PromiseResolve(uri.toString(), "", "").getObject();
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String name = cursor.getString(nameIndex);
        String type = contentResolver.getType(uri);
        cursor.close();
        return new PromiseResolve(uri.toString(), name, type).getObject();
    }

    public WritableMap sendResponse(String data) {
        return new PromiseResolve(data).getObject();
    }

}
