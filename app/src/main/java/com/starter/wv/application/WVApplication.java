package com.starter.wv.application;

import android.app.Application;
import android.content.Context;

/**
 * Created by Melvin Lobo on 8/13/2015.
 *
 * The application class for WV
 *
 * @author Melvin Lobo
 *
 */
public class WVApplication extends Application {

    //////////////////////////////////////// CLASS MEMBERS /////////////////////////////////////////
    private static Context mApplicationContext = null;

    public static boolean _DEBUG = true;

    //////////////////////////////////////// CLASS METHODS /////////////////////////////////////////
    @Override
    public void onCreate() {

        super.onCreate();
        setGlobalContext(); // Set the application context
    }

    /**
     * Set the application context for later use
     *
     * @author Melvin Lobo
     */
    public void setGlobalContext() {
        mApplicationContext = getApplicationContext();
    }

    /**
     * Get the application context stored with the application class
     *
     * @author Melvin Lobo
     */
    public static Context getGlobalContext() {
        return mApplicationContext;
    }


    //////////////////////////////////////// INNER CLASSES /////////////////////////////////////////
}
