package com.neokii.androidautomirror;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.neokii.androidautomirror.util.SettingUtil;
import com.neokii.androidautomirror.util.Util;

/**
 * Created by Legend on 2018-04-03.
 */

public class AppSelectDialog extends DialogFragment
{
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.select_app_layout, container, false);

        v.findViewById(R.id.btnDoNotUse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                SettingUtil.setString(getActivity(), "launch_app_at_start", "");
                dismiss();
            }
        });

        AppSingleSelectFragment fragment = new AppSingleSelectFragment();

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit();

        return v;
    }
}
