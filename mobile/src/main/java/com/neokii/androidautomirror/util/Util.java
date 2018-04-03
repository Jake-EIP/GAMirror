package com.neokii.androidautomirror.util;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.chainfire.libsuperuser.Shell;

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

    public static String getCurrentApp()
    {
        try
        {
            List<String> output = Shell.SU.run("dumpsys activity processes | grep top-activity");

            Pattern pattern = Pattern.compile(".* \\d+:(.*)/.*");
            Matcher matcher = pattern.matcher(output.get(0));
            if (matcher.find())
            {
                String packageName = matcher.group(1).trim();
                if(!TextUtils.isEmpty(packageName))
                    return packageName;
            }
        }
        catch (Exception e){}

        try
        {
            List<String> output = Shell.SU.run("dumpsys activity activities | grep mFocusedActivity");
            Log.d("qqqqqq", "" + output);

            Pattern pattern = Pattern.compile(".*[:blank:](.*)/.*");
            Matcher matcher = pattern.matcher(output.get(0));
            if (matcher.find())
            {
                String packageName = matcher.group(1).trim();
                if(!TextUtils.isEmpty(packageName))
                    return packageName;
            }
        }
        catch (Exception e){}

        try
        {
            List<String> output = Shell.SU.run("dumpsys window | grep mCurrentFocus");

            Pattern pattern = Pattern.compile(".*[:blank:](.*)/.*");
            Matcher matcher = pattern.matcher(output.get(0));
            if (matcher.find())
            {
                String packageName = matcher.group(1).trim();
                if(!TextUtils.isEmpty(packageName))
                    return packageName;
            }
        }
        catch (Exception e){}

        return null;
    }

    private static final AtomicInteger _nextGeneratedId = new AtomicInteger(1);

    public static void generateViewId(View v)
    {
        for(;;)
        {
            final int result = _nextGeneratedId.get();
            int newValue = result + 1;
            if(newValue > 0x00FF0000) newValue = 1;
            if(_nextGeneratedId.compareAndSet(result, newValue))
            {
                v.setId(result);
                return;
            }
        }
    }

    public static boolean isInstalled(Context context, String packageName)
    {
        try
        {
            context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return true;
        }
        catch(Exception e)
        {}
        return false;
    }

    public static void openApp(Context context, String packageName, boolean openStoreIfNotInstalled)
    {
        try
        {
            if(isInstalled(context, packageName))
            {
                try
                {
                    Intent i = context.getPackageManager().getLaunchIntentForPackage(packageName);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                }
                catch(Exception e)
                {
                }
            }
            else if(openStoreIfNotInstalled)
            {
                try
                {
                    Intent browse = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id="+ packageName));
                    browse.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(browse);
                }
                catch(Exception e)
                {
                }
            }
        }
        catch(Exception e)
        {
        }
    }
}
