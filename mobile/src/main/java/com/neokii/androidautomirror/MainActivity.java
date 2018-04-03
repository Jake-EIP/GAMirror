package com.neokii.androidautomirror;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityManager;

import com.github.slashmax.aamirror.AppCompatPreferenceActivity;
import com.neokii.androidautomirror.util.SettingUtil;
import com.neokii.androidautomirror.util.ShellManager;

import java.util.ArrayList;
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

        SharedPreferences.OnSharedPreferenceChangeListener _listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

                if(key.equals("launch_app_at_start"))
                {
                    Preference launch_app_at_start = findPreference(key);

                    String packageName = SettingUtil.getString(getActivity(), launch_app_at_start.getKey(), "");
                    launch_app_at_start.setSummary(getSummary(packageName));
                }
            }
        };

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

            bindPreferenceSummaryToValue(findPreference("action_when_power_plugged"));
            bindPreferenceSummaryToValue(findPreference("action_when_power_unplugged"));

            EditTextPreference left_toolbar_size = (EditTextPreference)findPreference("left_toolbar_size");
            bindPreferenceSummaryToValue(left_toolbar_size);

            left_toolbar_size.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    try
                    {
                        int val = Integer.valueOf((String)newValue);
                        if(val < 50 || val > 100)
                            return false;

                        preference.setSummary((String)newValue);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                    return true;
                }
            });

            Preference launch_app_at_start = findPreference("launch_app_at_start");

            //bindPreferenceSummaryToValue(launch_app_at_start);
            launch_app_at_start.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    AppSelectDialog dialog = new AppSelectDialog();
                    dialog.show(getFragmentManager(), null);

                    return false;
                }
            });

            String packageName = SettingUtil.getString(getActivity(), launch_app_at_start.getKey(), "");
            launch_app_at_start.setSummary(getSummary(packageName));
            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(_listener);
        }

        @Override
        public void onDestroy()
        {
            super.onDestroy();

            PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(_listener);
        }

        private String getSummary(String packageName)
        {
            if(TextUtils.isEmpty(packageName))
                return getString(R.string.do_not_use);

            String appName = getAppName(packageName);
            return "" + appName + " (" + packageName + ")";
        }

        private String getAppName(String packageName)
        {
            try
            {
                final PackageManager pm = getActivity().getPackageManager();

                return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
            }
            catch (Exception e)
            {}

            return null;
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

        if(ShellManager.available())
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

        RequestProjectionPermission();

        test();
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
        if(key.equals("show_left_toolbar"))
        {
            if(SettingUtil.getBoolean(this, "show_left_toolbar", false))
            {
                if(!checkAccessibilityPermissions())
                {
                    requestAccessibilityPermissions();
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

    public void requestAccessibilityPermissions()
    {
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
                .show();
    }

    private void RequestProjectionPermission()
    {
        Log.d(TAG, "RequestProjectionPermission");
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if(mediaProjectionManager != null)
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), AutoActivity.REQUEST_MEDIA_PROJECTION_PERMISSION);
    }

    private void requestPermissions()
    {
        if(Build.VERSION.SDK_INT >= 23)
        {
            ArrayList<Intent> intents = new ArrayList<>();

            if(!Settings.System.canWrite(this))
            {
                Intent intent = new Intent(ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                intents.add(intent);
            }

            if(!Settings.canDrawOverlays(this))
            {
                Intent intent = new Intent(ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                intents.add(intent);
            }

            if(intents.size() > 0)
                startActivities(intents.toArray(new Intent[intents.size()]));
        }
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
                        requestPermissions();
                    }
                }
            });
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void test()
    {
    }
}
