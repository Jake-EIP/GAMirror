package com.android.gami;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;


public class KeyEventService extends AccessibilityService
{
    private static final String TAG = "KeyEventService";

    private static KeyEventService _instance = null;

    public static boolean sendGlobaleKey(int action)
    {
        if(_instance != null)
            return _instance.performGlobalAction(action);

        return false;
    }

    @Override
    protected void onServiceConnected()
    {
        Log.d(TAG, "onServiceConnected");

        super.onServiceConnected();
        _instance = this;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d(TAG, "onUnbind");

        _instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event)
    {
        //Log.d(TAG, "onAccessibilityEvent: " + event);
    }

    @Override
    public void onInterrupt()
    {
        //Log.d(TAG, "onInterrupt");
    }
}
