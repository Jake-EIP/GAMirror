package com.neokii.androidautomirror;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.neokii.androidautomirror.util.SettingUtil;
import com.neokii.androidautomirror.util.ShellManager;

/**
 * Created by Legend on 2018-03-23.
 */

public class PowerConnectionReceiver extends BroadcastReceiver
{
    private static String FLIGHT_MODE_COMMAND_ENABLE = "settings put global airplane_mode_on 1\n" +
            "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true\n";

    private static String FLIGHT_MODE_COMMAND_DISABLE = "settings put global airplane_mode_on 0\n" +
            "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false\n";

    private static String LOW_POWER_COMMAND_ENABLE = "settings put global low_power 1\n" +
            "am broadcast -a android.os.action.POWER_SAVE_MODE_CHANGED --ez mode true\n";

    private static String LOW_POWER_COMMAND_DISABLE = "settings put global low_power 0\n" +
            "am broadcast -a android.os.action.POWER_SAVE_MODE_CHANGED --ez mode false\n";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED))
        {
            int action = Integer.valueOf(SettingUtil.getString(context, "action_when_power_plugged", "0"));

            if(action == 1)
            {
                ShellManager.runAsync(FLIGHT_MODE_COMMAND_DISABLE);
            }
            else if(action == 2)
            {
                ShellManager.runAsync(LOW_POWER_COMMAND_DISABLE);
            }
            else if(action == 3)
            {
                ShellManager.runAsync(FLIGHT_MODE_COMMAND_DISABLE);
                ShellManager.runAsync(LOW_POWER_COMMAND_DISABLE);
            }
        }
        else
        {
            int action = Integer.valueOf(SettingUtil.getString(context, "action_when_power_unplugged", "0"));

            if(action == 1)
            {
                ShellManager.runAsync(FLIGHT_MODE_COMMAND_ENABLE);
            }
            else if(action == 2)
            {
                ShellManager.runAsync(LOW_POWER_COMMAND_ENABLE);
            }
            else if(action == 3)
            {
                ShellManager.runAsync(FLIGHT_MODE_COMMAND_ENABLE);
                ShellManager.runAsync(LOW_POWER_COMMAND_ENABLE);
            }
        }

    }
}
