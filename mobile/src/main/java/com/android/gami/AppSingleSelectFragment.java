package com.android.gami;

import android.app.DialogFragment;

import com.github.slashmax.aamirror.AppEntry;
import com.android.gami.util.SettingUtil;

/**
 * Created by Legend on 2018-04-03.
 */

public class AppSingleSelectFragment extends BaseAppSelectFragment
{
    @Override
    protected void handleClick(AppEntry entry)
    {
        String packageName = entry.getApplicationInfo().packageName;

        SettingUtil.setString(getActivity(), "launch_app_at_start", packageName);

        if(getParentFragment() != null && getParentFragment() instanceof DialogFragment)
        {
            ((DialogFragment)getParentFragment()).dismiss();
        }
        else
        {
            getFragmentManager().popBackStack();
        }
    }
}
