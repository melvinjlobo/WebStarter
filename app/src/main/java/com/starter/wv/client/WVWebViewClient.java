package com.starter.wv.client;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.starter.wv.application.WVApplication;

/**
 * Implementation class for the WebViewClient
 *
 * @author Schubert Gomes.
 */
public class WVWebViewClient extends WebViewClient {

    /***************************************** CLASS MEMBERS **************************************/
    /**
     * Progress Dialog to be shown when the page is loading
     */
    private ProgressDialog mProgressDialog;


    /***************************************** CLASS METHODS **************************************/

    public WVWebViewClient(Context context) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setTitle("Please wait");
        mProgressDialog.setMessage("Connecting..");
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        if(WVApplication._DEBUG)
            Log.d("WV", "onReceivedSslError - " + error.toString());
        handler.proceed();
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if(WVApplication._DEBUG)
            Log.d("WV", "onPageStarted - " + url);
        mProgressDialog.show();
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        if(WVApplication._DEBUG) {
            Log.v("Error in URL", description);
            Log.v("Error code", "" + errorCode);
            Log.v("Error URL", failingUrl);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if(WVApplication._DEBUG)
            Log.d("WV", "Loading URL - " + url);
        view.loadUrl(url);
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mProgressDialog.dismiss();
        mProgressDialog.cancel();
        if (mProgressDialog.isShowing()) {
            try {
                Thread.sleep(2000);
                mProgressDialog.dismiss();
                mProgressDialog.cancel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /***************************************** INNER CLASSES **************************************/

}
