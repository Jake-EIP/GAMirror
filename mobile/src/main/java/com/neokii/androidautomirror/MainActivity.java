package com.neokii.androidautomirror;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityManager;

import com.github.slashmax.aamirror.AppCompatPreferenceActivity;
import com.neokii.androidautomirror.util.SettingUtil;
import com.neokii.androidautomirror.util.ShellManager;

import java.util.List;

import static android.content.ContentValues.TAG;
import static android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION;
import static android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS;

public class MainActivity extends AppCompatPreferenceActivity
{
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value)
        {
            String stringValue = value.toString();

            if (preference instanceof ListPreference)
            {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

            }
            else if (preference instanceof EditTextPreference)
            {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    public static class GeneralPreferenceFragment extends PreferenceFragment
    {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general_settings);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("overwrite_brightness_value"));
            bindPreferenceSummaryToValue(findPreference("orientation_method"));
            bindPreferenceSummaryToValue(findPreference("immersive_mode"));
            bindPreferenceSummaryToValue(findPreference("orientation_rotation"));

            bindPreferenceSummaryToValue(findPreference("action_2finger_tap"));
            //bindPreferenceSummaryToValue(findPreference("action_double_tap"));

            Preference show_left_toolbar = findPreference("show_left_toolbar");
            Preference show_left_toolbar_use_system_key = findPreference("show_left_toolbar_use_system_key");


        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), MainActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target)
    {
        super.onBuildHeaders(target);
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return true;
    }

    private static void bindPreferenceSummaryToValue(Preference preference)
    {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        ShellManager.runSU("");
        ShellManager.createAsyncShell();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);

            try
            {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                actionBar.setTitle(getString(R.string.app_name) + " " + versionName);

            }
            catch(Exception e){}
        }

        try
        {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            float w = Math.max((float)metrics.widthPixels, (float)metrics.heightPixels);
            float h = Math.min((float)metrics.widthPixels, (float)metrics.heightPixels);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putFloat("device_landscape_width", w);
            editor.putFloat("device_landscape_height", h);
            editor.apply();
        }
        catch(Exception e){}

        RequestProjectionPermission();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(_sharedPreferenceListener);
    }

    @Override
    protected void onPause()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.unregisterOnSharedPreferenceChangeListener(_sharedPreferenceListener);

        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    SharedPreferences.OnSharedPreferenceChangeListener _sharedPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener()
    {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            updatePreference(key);
        }
    };

    private void updatePreference(String key)
    {
        if(key.equals("show_left_toolbar") || key.equals("show_left_toolbar_use_system_key"))
        {
            if(SettingUtil.getBoolean(this, "show_left_toolbar", false))
            {
                if(SettingUtil.getBoolean(this, "show_left_toolbar_use_system_key", false))
                {
                    if(!checkAccessibilityPermissions())
                    {
                        requestAccessibilityPermissions();
                    }
                }
            }
        }
    }

    public boolean checkAccessibilityPermissions()
    {
        try
        {
            AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.DEFAULT);

            for(int i = 0; i < list.size(); i++)
            {
                AccessibilityServiceInfo info = list.get(i);

                if(info.getResolveInfo().serviceInfo.packageName.equals(getApplication().getPackageName()))
                {
                    return true;
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    private boolean _pending_accessibility_dialog;
    public void requestAccessibilityPermissions()
    {
        if(_pending_accessibility_dialog)
            return;

        _pending_accessibility_dialog = true;

        new AlertDialog.Builder(this)
                .setMessage(R.string.required_accessibility_permissions)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        _pending_accessibility_dialog = false;
                    }
                })
                .show();
    }


    private void RequestProjectionPermission()
    {
        Log.d(TAG, "RequestProjectionPermission");
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if(mediaProjectionManager != null)
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), AutoActivity.REQUEST_MEDIA_PROJECTION_PERMISSION);
    }

    private void startActivity(String action)
    {
        Intent intent = new Intent(action);
        intent.setData(Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void RequestWriteSettingsPermission()
    {
        Log.d(TAG, "RequestWriteSettingsPermission");
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(this))
            startActivity(ACTION_MANAGE_WRITE_SETTINGS);
    }

    private void RequestOverlayPermission()
    {
        Log.d(TAG, "RequestOverlayPermission");
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this))
            startActivity(ACTION_MANAGE_OVERLAY_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data)
    {
        if(AutoActivity.REQUEST_MEDIA_PROJECTION_PERMISSION == requestCode)
        {
            new Handler().post(new Runnable()
            {
                @Override
                public void run()
                {
                    if(resultCode != RESULT_OK)
                    {
                        RequestProjectionPermission();
                    }
                    else
                    {
                        RequestWriteSettingsPermission();

                        new Handler().post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                RequestOverlayPermission();
                            }
                        });
                    }
                }
            });
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
