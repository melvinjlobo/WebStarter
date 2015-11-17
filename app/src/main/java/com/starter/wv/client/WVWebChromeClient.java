package com.starter.wv.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.starter.wv.R;
import com.starter.wv.ui.WVActivity;
import com.starter.wv.utils.Constants;

import java.io.File;

/**
 * Created by Melvin Lobo on 8/13/2015.
 *
 * Web Chrome client to handle stuff like JS Alerts, Camera uploads, etc.
 * Note that Lollipop onwards, instead of openFileChooser, we can use onShowFileChooser which is
 * simpler, but we need to also use openFileChooser because we need to support JellyBean and above
 */
public class WVWebChromeClient extends WebChromeClient implements WVActivity.WVWebChromeClientDataListener {

    //////////////////////////////////// CLASS MEMBERS /////////////////////////////////////////////
    /**
     * Static variables
     */
    public static final int CAPTURE_RESULTCODE = 1101;
    private static final String  IMAGE_MIME_TYPE = "image/*";

    /**
     * The upload message to pass on the captured image in case of Android < 5.0
     */
    private ValueCallback<Uri> mFileUploadCallbackFirst = null;

    /**
     * The upload message to pass on the captured image in case of Android > 5.0
     */
    private ValueCallback<Uri []> mFileUploadCallbackSecond = null;

    /**
     * The stored Image URI
     */
    private Uri mCapturedFileImageURI;

    /**
     * Calling activity. We sadly need this to call startActivityForResult.
     * We'll make sure we'll null it out once we are done with it
     */
    private Activity mCallingActivity = null;

    /**
     * The created file path
     */
    private String mCaptureFilePath = null;

    //////////////////////////////////// CLASS METHODS /////////////////////////////////////////////

    /**
     * Constructor
     *
     * @param activity
     *      The calling activity. We need to be sure to destroy it after we're done
     *
     * @author Melvin Lobo
     */
    public WVWebChromeClient(Activity activity) {
        mCallingActivity = activity;
    }

    /**
     * openFileChooser for Android 2.2 - Android 2.3 [Hidden Method]
     *
     * @param uploadMsg
     *      The upload message to upload the media
     *
     * @author Melvin Lobo
     */
    @SuppressWarnings("unused")
    public void openFileChooser(ValueCallback<Uri> uploadMsg){
        openFileChooser(uploadMsg, null);
    }

    /**
     * openFileChooser for Android 3.0 - Android 4.0 [Hidden Method]
     *
     * @param uploadMsg
     *      The upload message to upload the media
     * @param acceptType
     *      The accept Type
     *
     * @author Melvin Lobo
     */
    @SuppressWarnings("unused")
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        openFileChooser(uploadMsg, acceptType, null);
    }

    /**
     * openFileChooser for other Android 4.1 - Android 4.3 [Hidden Method]
     *
     * @param uploadMsg
     *      The upload message to upload the media
     * @param acceptType
     *      The accept Type
     * @param capture
     *      The capture path
     *
     *  @author Melvin Lobo
     */
    @SuppressWarnings("unused")
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        openFileInput(uploadMsg, null);
    }

    /**
     * File Chooser for Android  > 5.0 [Public method]
     * @param webView
     *      The webview that throws the event
     * @param filePathCallback
     *      Invoke this callback to supply the list of paths to files to upload, or NULL to cancel.
     *      Must only be called if the showFileChooser implementations returns true.
     * @param fileChooserParams
     *      Describes the mode of file chooser to be opened, and options to be used with it.
     * @return
     *
     */
    @SuppressWarnings("all")
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        openFileInput(null, filePathCallback);
        return true;
    }

    /**
     * Function to show file chooser. This chooser function would accept files from all Android versions.
     * Thanks to Android-AdvancedWebView for showing how to handle situations from all versions
     * (https://github.com/delight-im/Android-AdvancedWebView/blob/master/Source/src/im/delight/android/webview/AdvancedWebView.java)
     *
     * The logic is that we need two options, Camera and File system. So we create a chooser intent with these
     * two options and extract as necessary
     *
     * @param fileUploadCallbackFirst
     *      Value is not null if it comes from hidden methods < 5.0
     *
     * @param fileUploadCallbackSecond
     *      Value is not null if it comes fom public methods >= 5.0
     *
     * @author Melvin Lobo
     */
    private void openFileInput(final ValueCallback<Uri> fileUploadCallbackFirst, final ValueCallback<Uri[]> fileUploadCallbackSecond) {
        // Store the Update message, for later to upload the captured image. In case upload Messages are not found to be null
        //(previous existence), set their onReceive to null to cancel them and allocate the current upload message
        if (mFileUploadCallbackFirst != null) {
            mFileUploadCallbackFirst.onReceiveValue(null);
        }
        mFileUploadCallbackFirst = fileUploadCallbackFirst;

        if (mFileUploadCallbackSecond != null) {
            mFileUploadCallbackSecond.onReceiveValue(null);
        }
        mFileUploadCallbackSecond = fileUploadCallbackSecond;

        try {

            /*
             * Start the file chooser by creating the chooser intent. Its a complicated decision making
             * process, but separating the rest of the code, I'm following a basic logic from
             * AOSP UploadHandler.Java
             * http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android-apps/4.1.2_r1/com/android/browser/UploadHandler.java
             *
             * Create a chooser intent with the camera intent and image file selection intent
             */
            Intent chooserIntent = createChooserIntent(createCameraIntent());
            chooserIntent.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(IMAGE_MIME_TYPE));

            //Call the activity for result. Note that the result will be called in the calling activity
            mCallingActivity.startActivityForResult(chooserIntent, CAPTURE_RESULTCODE);

        } catch (Exception e) {
            Log.d("WV", "Exception when trying to open intents - " + e.getMessage());
        }
    }

    /**
     * Create the chooser intents from the list of intents sent out as a parameter
     * Reference, AOSP
     *
     * @author Melvin Lobo
     */
    private Intent createChooserIntent(Intent... intents) {
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
        chooser.putExtra(Intent.EXTRA_TITLE, mCallingActivity.getString(R.string.intent_chooser_title));
        return chooser;
    }

    /**
     * Create the camera Intent. Create a file to store the captured image
     * Reference, AOSP
     *
     * @author Melvin Lobo
     */
    private Intent createCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        /*
         * Create the file and path to store the captured image
         */
        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                , Constants.EXTERNAL_DIRECTORY_NAME);

        if (!imageStorageDir.exists())
            imageStorageDir.mkdirs();

        //Get the image path
        mCaptureFilePath = imageStorageDir.getAbsolutePath() + File.separator + "IMG_"
                + String.valueOf(System.currentTimeMillis())
                + ".jpg";

        // Create an image file to store the captured file
        File captureFile = new File(mCaptureFilePath);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(captureFile));

        return cameraIntent;
    }

    /**
     * Create the filesystem or openable intent.
     * Reference, AOSP
     *
     * @param mimeType
     *      The mime type of the openable content
     *
     * @author Melvin Lobo
     */
    private Intent createOpenableIntent(String mimeType) {
        Intent openableIntent = new Intent(Intent.ACTION_GET_CONTENT);
        openableIntent.addCategory(Intent.CATEGORY_OPENABLE);
        openableIntent.setType(mimeType);
        return openableIntent;
    }


    /**
     * Set up the upload message. We have two cases to handle
     * 1. Version based (<5.0 vs >=5.0 )
     * 2. Picture taken by camera or selected from gallery
     *
     * @param onResultIntent
     *      The intent of onActivityResult
     * @param bIsSuccessful
     *      The result code
     *
     * @author Melvin Lobo
     */
    @Override
    public void setUploadResult(Intent onResultIntent, boolean bIsSuccessful) {
        /*
         * Since we have asked the camera to store the result of taking a picture, it returns a null
         * intent with RESULT_OK. In this case, we check whether the file was actually
         * stored on the disk with the filepath value that we have stored with us.
         */
       boolean bWasCameraPictureTaken = false;
        Uri sCameraFileURI = null;
        if(bIsSuccessful && (onResultIntent == null)) {
            File imageFile = new File(mCaptureFilePath);
            if(imageFile.exists()) {
                bWasCameraPictureTaken = true;
                sCameraFileURI = Uri.fromFile(imageFile);
            }
        }

        /*
         * Version based segregation
         */
        //< 5.0
        if (mFileUploadCallbackFirst != null) {
            Uri result = (bWasCameraPictureTaken) ? sCameraFileURI : onResultIntent.getData();
            mFileUploadCallbackFirst.onReceiveValue((bIsSuccessful) ?  result : null);
            mFileUploadCallbackFirst = null;
        }
        //>= 5.0
        else if (mFileUploadCallbackSecond != null) {
            Uri[] result;
            try {
                result = new Uri[] { (bWasCameraPictureTaken) ? sCameraFileURI : Uri.parse( onResultIntent.getDataString()) };
            }
            catch (Exception e) {
                result = null;
            }

            mFileUploadCallbackSecond.onReceiveValue((bIsSuccessful) ? result : null);
            mFileUploadCallbackSecond = null;
        }
    }

    /**
     * Reset all values
     *
     * @author Melvin Lobo
     */
    @Override
    public void resetValues() {
        mCaptureFilePath = null;
        mFileUploadCallbackFirst = null;
        mFileUploadCallbackSecond = null;
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message,
                             final android.webkit.JsResult result) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("javaScript dialog")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                result.confirm();
                            }
                        }).setCancelable(false).create().show();

        return true;
    }

    @Override
    public void clearReferences() {
        mCallingActivity = null;
    }

    //////////////////////////////////// INNER CLASSES /////////////////////////////////////////////

}
