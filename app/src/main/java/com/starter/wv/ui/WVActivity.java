package com.starter.wv.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.starter.wv.R;
import com.starter.wv.client.WVWebChromeClient;
import com.starter.wv.client.WVWebViewClient;
import com.starter.wv.utils.Constants;



public class WVActivity extends Activity {

    /************************************ CLASS MEMBERS *******************************************/
    /**
     * Static definitions
     */
    public static final String CONTENT_DISPOSITION_FILE_NAME = "filename=";

    /**
     * The WebView to load the content
     */
    private WebView mWebView = null;

    /**
     * Broadcast receiver to handle clicks when a file is being downloaded
     */
    private BroadcastReceiver mNotificationClickReceiver = null;

    /**
     * Broadcast receiver to handle a file that has finished downloading
     */
    private BroadcastReceiver   mDownloadFinishedReceiver = null;

    /**
     * Reference Id for the download
     */
    private long mnDownloadReferenceID = 0;

    /**
     * The WV Data Interface
     */
    private WVWebChromeClientDataListener mWVDataListener = null;

    /************************************ CLASS METHODS *******************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wv);

        mWebView = (WebView) findViewById(R.id.webView_main);
        WVWebViewClient webClient = new WVWebViewClient(this);

        // Handling incoming activity requests
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        //WebView Settings
        mWebView.setClickable(true);
        mWebView.setFocusableInTouchMode(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setBuiltInZoomControls(false);
        mWebView.getSettings().setSupportZoom(false);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.requestFocus(View.FOCUS_DOWN);
        mWebView.getSettings().setAllowContentAccess(true);
        CookieManager.getInstance().setAcceptCookie(true);

        if(Build.VERSION.SDK_INT >= 21)
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);

        //setting a user agent to detect the application
        mWebView.getSettings().setUserAgentString(mWebView.getSettings().getUserAgentString()
                + " "
                + getString(R.string.user_agent_suffix));

        //Set the download listener to download
        mWebView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                //Test
                /*Log.d("WV", "onDownloadStart: UserAgent - " + userAgent + ", contentDisposition - " + contentDisposition
                        + ", mimetype - " + mimetype + ", contentLength - " + contentLength);*/

                //Extract the fiel name from the Content Disposition
                String sFilename = contentDisposition.substring(contentDisposition.indexOf(CONTENT_DISPOSITION_FILE_NAME) + (CONTENT_DISPOSITION_FILE_NAME.length()));

                //Set up the download in the download manager Queue and try to open the pdf
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.allowScanningByMediaScanner();
                request.setMimeType(mimetype);
                request.setTitle(sFilename);
                request.setDescription(sFilename);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                //Set up an external directory to store the download
                request.setDestinationInExternalPublicDir(Constants.EXTERNAL_DIRECTORY_NAME, sFilename);

                //Get the download manager and queue the request
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                mnDownloadReferenceID = downloadManager.enqueue(request);
            }
        });

        //Set the WebView client
        mWebView.setWebViewClient(webClient);

        //Create the webchrome client
        WebChromeClient webChromeClient = new WVWebChromeClient(this);
        mWVDataListener = (WVWebChromeClientDataListener) webChromeClient;

        mWebView.setWebChromeClient(webChromeClient);

        //Load the URL
        mWebView.loadUrl(Constants.WV_URL);
    }


    /**
     * Register the broadcast receivers to handle clicks when the file is being downloaded
     * and after the file is downloaded
     *
     * @author Melvin Lobo
     */
    public void registerBroadcastReceivers() {

        /*
         * Register Click Receiver
         */
        registerReceiver(mNotificationClickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String sExtraId = DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
                long[] references = intent.getLongArrayExtra(sExtraId);
                for (long reference : references) {
                    if (reference == mnDownloadReferenceID) {
                        validateDownload();
                    }

                }
            }
        }, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        /*
         * Register Download Notifications
         */
        registerReceiver(mDownloadFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if(reference == mnDownloadReferenceID) {
                    validateDownload();
                }
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    /**
     * Unregister the broadcast receivers
     *
     * @author Melvin Lobo
     */
    private void unregisterBroadcastReceivers() {
        unregisterReceiver(mNotificationClickReceiver);
        unregisterReceiver(mDownloadFinishedReceiver);
    }

    /**
     * Handle onResume to register receivers
     *
     * @author Melvin Lobo
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerBroadcastReceivers();
    }

    /**
     * Identifies if the download is indeed valid
     *
     * @author Melvin Lobo
     */
    protected void validateDownload() {
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Cursor c= downloadManager.query(new DownloadManager.Query().setFilterById(mnDownloadReferenceID));

        if(c.moveToFirst()){
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            Uri sFileURI = Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
            String sLocalFileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));

            switch(status) {
                case DownloadManager.STATUS_SUCCESSFUL:
                {
                    //Start our viewer activity which will load the PDF using MuPDF
                    Intent pdfIntent = new Intent(this, ActivityPDFViewer.class);
                    pdfIntent.putExtra(ActivityPDFViewer.PDF_URL, sFileURI.getPath());
                    startActivity(pdfIntent);
                }
                break;
                case DownloadManager.STATUS_FAILED:
                {
                    Toast.makeText(WVActivity.this, getString(R.string.download_failure), Toast.LENGTH_LONG).show();
                }
                break;
                case DownloadManager.STATUS_PAUSED:
                case DownloadManager.STATUS_PENDING:
                case DownloadManager.STATUS_RUNNING:
                {
                    Toast.makeText(WVActivity.this, getString(R.string.download_pending), Toast.LENGTH_LONG).show();
                }
                break;
                default:
                    break;
            }
        }
    }

    /**
     * Handle onPause to unregister receivers
     *
     * @author Melvin Lobo
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterBroadcastReceivers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wv, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.exit_title))
                    .setMessage(getString(R.string.exit_msg))
                    .setPositiveButton(getString(R.string.exit_positive),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    finish();
                                }

                            }).setNegativeButton(getString(R.string.exit_negative), null).show();
        }
    }

    /**
     * Check if this device is a Tablet or  Phone
     *
     * @author Melvin Lobo
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Handle onActivity Result for startActivityForResult
     *
     * @param requestCode
     *      The requrst code sent
     * @param resultCode
     *      The result code sent when calling startActivityForResult
     * @param intent
     *      The intent sent by the result activity
     *
     * @author Melvin Lobo
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == WVWebChromeClient.CAPTURE_RESULTCODE) {
            if(mWVDataListener != null) {
                mWVDataListener.setUploadResult(intent, (resultCode == RESULT_OK));
                mWVDataListener.resetValues();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mWVDataListener != null)
            mWVDataListener.clearReferences();
    }

    /////////////////////////////////// INNER CLASSES //////////////////////////////////////////////
    /**
     * Interface to get data from the Chrome Client
     *
     * @author Melvin Lobo
     */
    public interface WVWebChromeClientDataListener {

        /**
         * Set the upload message with the activity result
         *
         * @param onResultIntent
         *      The Result Intent
         * @param bIsSuccessful
         *      The result code
         *
         * @author Melvin Lobo
         */
        public void setUploadResult(Intent onResultIntent, boolean bIsSuccessful);


        /**
         * Reset all captured values
         *
         * @author Melvin Lobo
         */
        public void resetValues();

        /**
         * Clear all references
         *
         * @author Melvin Lobo
         */
        public void clearReferences();
    }
}
