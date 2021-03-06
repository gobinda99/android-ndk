/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.sample.echo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

/**
 * Fragment that handles the runtime permission checking required for "M" and later.
 */
public class PermissionRequestFragment extends Fragment {
    private static final String TAG = "PermReqFragment";
    private static final int RC_REQUEST_PERMISSION = 123;

    // These are error codes
    public static final int Error_FragmentNotAttached = -2;
    public static final int Error_Busy = -3;

    private static PermissionRequestFragment theInstance;

    private long callbackPtr;

    /**
     * Called when a fragment is first attached to its context.
     *
     * @param context - the context for this fragment.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        theInstance = this;
        callbackPtr = 0L;
    }

    /**
     * Static method callable from native code.  This method checks the permission, and then
     * invokes the callback with the results.  This callback may happen immediately if the permission
     * state is already known, or it may need to go through the requesting permission and onRequestPermissionsResult
     * before the callback is called.
     * <p>If there is already a pending permission check, the callback is invoked immediately  with
     * and error.</p>
     * <p>This is marked with API 23 since the permissions are new. The ContextCompat and activity
     * are aware of the API level and perform the correct behavior for SDKs < 23</p>
     *
     * @param permission  - the permission to check.
     * @param callbackPtr - the pointer to the callback function which has the signature (jint), the
     *                    value is either Packagemanager.PERMISSION_GRANTED (0),
     *                    Packagemanager.PERMISSION_DENIED (-1), or another negative number indicating
     *                    an error state.
     */
    @TargetApi(23)
    public static void checkPermission(final String permission, String rationale, long callbackPtr) {
        Log.d(TAG,"Checking permission for: " + permission);
        if (theInstance == null) {
            Log.e(TAG, "An instance of this fragment has not been attached to the activity.");

            handlePermissionResult(Error_FragmentNotAttached, callbackPtr);
            return;
        }
        if (theInstance.callbackPtr != 0L) {
            Log.e(TAG, "Permission check already in progress");
            handlePermissionResult(Error_Busy, callbackPtr);
            return;
        }

        theInstance.callbackPtr = callbackPtr;

        int permissionCheck = ContextCompat.checkSelfPermission(theInstance.getActivity(), permission);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            Log.d(TAG,"self permission is denied, requesting");
            // request runtime permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(theInstance.getActivity(),
                    permission)) {

                final Activity thisActivity = theInstance.getActivity();
                View view = thisActivity.getCurrentFocus();
                if (view == null) {
                    view = thisActivity.getWindow().getDecorView();
                }
                Snackbar.make(view,
                        rationale,
                        Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view1) {
                        theInstance.requestPermissions(new String[]{permission}, RC_REQUEST_PERMISSION);
                    }
                }).show();
            } else {
                theInstance.requestPermissions(new String[]{permission}, RC_REQUEST_PERMISSION);
            }
        } else {
            long ptr = theInstance.callbackPtr;
            theInstance.callbackPtr = 0L;
            Log.d(TAG,permission + " granted!");
            handlePermissionResult(PackageManager.PERMISSION_GRANTED, ptr);
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == RC_REQUEST_PERMISSION) {
            long ptr = callbackPtr;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // finish starting
                Log.d(TAG,permissions[0] + " granted!");
                callbackPtr = 0L;
                handlePermissionResult(PackageManager.PERMISSION_GRANTED, ptr);
            } else {
                Log.w(TAG,permissions[0] + " denied");
                callbackPtr = 0L;
                handlePermissionResult(PackageManager.PERMISSION_DENIED, ptr);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /*
     * Loading our Libs
     */
    static {
        System.loadLibrary("echo");
    }

    public static native void handlePermissionResult(int resultCode, long callbackPtr);

}
