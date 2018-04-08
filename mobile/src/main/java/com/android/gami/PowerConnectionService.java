package com.android.gami;

import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;

public class PowerConnectionService extends Service
{
    BroadcastReceiver _receiver = new PowerConnectionReceiver();

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        IntentFilter filter = new IntentFilter();

        filter.addAction(ACTION_POWER_CONNECTED);
        filter.addAction(ACTION_POWER_DISCONNECTED);
        filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
        //filter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);

        registerReceiver(_receiver, filter);
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        unregisterReceiver(_receiver);
        super.onDestroy();
    }
}
