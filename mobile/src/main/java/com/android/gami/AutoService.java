package com.android.gami;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

public class AutoService extends CarActivityService
{
    private static final String TAG = "AutoService";

    @Override
    public Class<? extends CarActivity> getCarActivity()
    {
        return AutoActivity.class;
    }
}
