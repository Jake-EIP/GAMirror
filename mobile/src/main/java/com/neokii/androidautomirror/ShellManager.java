package com.neokii.androidautomirror;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

class ShellManager
{
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
