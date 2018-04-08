package com.android.gami;

import android.content.Context;
import android.util.Log;

import com.android.gami.util.ShellManager;
import com.android.gami.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class MinitouchExecutor
{
    private static final String TAG = "MinitouchExecutor";

    public static final String NAME = "minitouch_ga";

    private Context m_Context;

    public MinitouchExecutor(Context context)
    {
        Log.d(TAG, "MinitouchExecutor");
        m_Context = context;
    }

    public void start()
    {
        Log.d(TAG, "start");

        final String path = install();
        if (path == null || path.isEmpty())
            return;

        stop();

        String pp = path + " -n " + NAME;
        Log.d(TAG, "run: " + pp);

        ShellManager.runSU(pp);
    }

    public void stop()
    {
        Util.killProcess(NAME);
    }

    private String install()
    {
        Log.d(TAG, "install");

        File file = m_Context.getFileStreamPath(NAME);
        if(file.exists())
            return file.getAbsolutePath();

        try
        {
            FileOutputStream fileOutputStream = m_Context.openFileOutput(NAME, 0);
            String assetName = getAssetFile();
            InputStream assetFile = m_Context.getAssets().open(assetName);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = assetFile.read(buffer)) != -1)
                fileOutputStream.write(buffer, 0, read);

            assetFile.close();
            fileOutputStream.close();

            ShellManager.runSU("chmod 755 " + file.getAbsolutePath());
        }
        catch (Exception e)
        {
            Log.d(TAG, "install exception: " + e.toString());

            try
            {
                file.delete();
            }
            catch(Exception e2){}

            return null;
        }

        return file.getAbsolutePath();
    }

    private String getAssetFile()
    {
        Log.d(TAG, "getAssetFile");
        return ("libs/" + detectAbi() + "/minitouch");
    }

    private String detectAbi()
    {
        Log.d(TAG, "detectAbi");
        List<String> result = Shell.SH.run("getprop ro.product.cpu.abi");

        if (result != null && !result.isEmpty())
            return result.get(0);

        return "armeabi";
    }
}
