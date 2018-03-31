package com.neokii.androidautomirror.util;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.widget.Toast;


import java.util.List;

public class Util
{
    public static int DP2PX(Context context, int DP)
    {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int)((float)DP * scale);
    }

    public static int PX2DP(Context context, int PX)
    {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int)((float)PX / scale);
    }

    public static boolean killProcess(String name)
    {
        List<String> output = ShellManager.runSU("ps | grep " + name);

        try
        {
            String[] tokens = output.get(0).split(" ");

            int i = 0;
            for(String token : tokens)
            {
                if(token == null || token.isEmpty())
                    continue;

                if(i == 1)
                {
                    ShellManager.runSU("kill " + token);
                    return true;
                }

                i++;
            }
        }
        catch(Exception e)
        {
        }

        return false;
    }

    public static void toast(Context context, String text)
    {
        try
        {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
        catch(Exception e){}
    }

    public static boolean setBluetooth(boolean enable)
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        boolean isEnabled = bluetoothAdapter.isEnabled();
        if(enable && !isEnabled)
        {
            return bluetoothAdapter.enable();
        }
        else if(!enable && isEnabled)
        {
            return bluetoothAdapter.disable();
        }

        return true;
    }
}
