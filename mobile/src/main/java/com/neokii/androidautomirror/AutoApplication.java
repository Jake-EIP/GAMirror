package com.neokii.androidautomirror;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import static android.view.Surface.ROTATION_0;
import static com.github.slashmax.aamirror.OrientationService.ROTATION_180;


public class AutoApplication extends Application
{
    private static final String TAG = "AutoApplication";

    public static int               ScreenRotation = ROTATION_0;
    public static Point             ScreenSize = new Point();
    public static Point             DisplaySize = new Point();

    private static OrientationEventListener m_OrientationListener;
    private WindowManager m_WindowManager;

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        super.onCreate();

        m_WindowManager = (WindowManager)getApplicationContext().getSystemService(WINDOW_SERVICE);
        UpdateScreenSizeAndRotation();
        UpdateDisplaySize();
        m_OrientationListener = new OrientationEventListener(this)
        {
            @Override
            public void onOrientationChanged(int orientation)
            {
                UpdateScreenSizeAndRotation();
            }
        };

        Intent service = new Intent(this, PowerConnectionService.class);
        startService(service);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        Log.d(TAG, "onConfigurationChanged: " + (newConfig != null ? newConfig.toString() : "null"));
        super.onConfigurationChanged(newConfig);
        UpdateScreenSizeAndRotation();
    }

    public static void EnableOrientationListener()
    {
        Log.d(TAG, "EnableOrientationListener");
        if (m_OrientationListener != null)
            m_OrientationListener.enable();
    }

    public static void DisableOrientationListener()
    {
        Log.d(TAG, "DisableOrientationListener");
        if (m_OrientationListener != null)
            m_OrientationListener.disable();
    }

    private void UpdateScreenSizeAndRotation()
    {
        if (m_WindowManager != null)
        {
            ScreenRotation = m_WindowManager.getDefaultDisplay().getRotation();
            m_WindowManager.getDefaultDisplay().getRealSize(ScreenSize);
        }
    }

    private void UpdateDisplaySize()
    {
        if (ScreenRotation == ROTATION_0 || ScreenRotation == ROTATION_180)
        {
            DisplaySize.x = ScreenSize.x;
            DisplaySize.y = ScreenSize.y;
        }
        else
        {
            DisplaySize.x = ScreenSize.y;
            DisplaySize.y = ScreenSize.x;
        }
        Log.d(TAG, "UpdateDisplaySize: " + DisplaySize);
    }
}
