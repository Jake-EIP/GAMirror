package com.neokii.androidautomirror.util;

import android.util.Log;
import android.util.SparseIntArray;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class ShellManager
{
    private static boolean _enabled = false;
    private static Shell.Interactive _shell = null;

    public static void createAsyncShell()
    {
        if(_shell == null)
        {
            _shell = new Shell.Builder().useSU().open(new Shell.OnCommandResultListener()
            {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output)
                {
                    _enabled = exitCode == 0;
                }
            });

            //_shell.addCommand("echo test");
            //_shell.waitForIdle();
        }
    }

    public static void runAsync(String cmd)
    {
        createAsyncShell();
        _shell.addCommand(cmd);
    }

    private static SparseIntArray _pendingCountKey = new SparseIntArray();

    public static void sendKey(final int keyCode, boolean londPress)
    {
        int count = _pendingCountKey.get(keyCode);

        if(count > 2)
        {
            Log.d("ShellManager", "sendKey return !!!");
            return;
        }
        _pendingCountKey.put(keyCode, count+1);

        createAsyncShell();

        String cmd;

        if(londPress)
            cmd = "input keyevent --longpress " + keyCode;
        else
            cmd = "input keyevent " + keyCode;

        _shell.addCommand(cmd, 0, new Shell.OnCommandResultListener()
        {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output)
            {
                int count = _pendingCountKey.get(keyCode);
                _pendingCountKey.put(keyCode, count-1);
            }
        });
    }

    public static boolean available()
    {
        return Shell.SU.available();
    }

    public static List<String> runSU(String cmd)
    {
        return Shell.SU.run(cmd);
    }

    public static List<String> runSH(String cmd)
    {
        return Shell.SH.run(cmd);
    }
}
