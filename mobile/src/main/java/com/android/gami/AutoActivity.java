package com.android.gami;


import android.accessibilityservice.AccessibilityService;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.car.Car;
import android.support.car.CarConnectionCallback;
import android.support.car.media.CarAudioManager;
import android.support.v4.widget.TextViewCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.slashmax.aamirror.BrightnessService;
import com.github.slashmax.aamirror.MinitouchSocket;
import com.github.slashmax.aamirror.OrientationService;
import com.github.slashmax.aamirror.ResultRequestActivity;
import com.github.slashmax.aamirror.TwoFingerGestureDetector;
import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.DayNightStyle;
import com.android.gami.util.SettingUtil;
import com.android.gami.util.ShellManager;
import com.android.gami.util.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;
import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.content.Intent.ACTION_USER_PRESENT;
import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.ON_AFTER_RELEASE;
import static android.os.PowerManager.SCREEN_DIM_WAKE_LOCK;
import static android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION;
import static android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS;
import static android.support.car.media.CarAudioManager.CAR_AUDIO_USAGE_DEFAULT;
import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;

public class AutoActivity extends CarActivity implements
        Handler.Callback, View.OnTouchListener, TwoFingerGestureDetector.OnTwoFingerGestureListener
{
    private static final String         TAG = "AutoActivity";
    public static final int            REQUEST_MEDIA_PROJECTION_PERMISSION = 1;

    private void shellExec(final String cmd)
    {
        ShellManager.runSU(cmd);
        //ShellManager.runAsync(cmd);
        Log.d("Shell", cmd);
    }

    //////////////////////////////////////////////////////////////////////////////

    MinitouchAsyncTask _minitouchTask;
    MinitouchSocket _minitouchSocket;

    //////////////////////////////////////////////////////////////////////////////

    private Car                         m_Car;

    private boolean                     m_ScreenResized;

    private Handler m_RequestHandler;
    private PowerManager.WakeLock       m_WakeLock;

    private SurfaceView m_SurfaceView;
    private Surface m_Surface;

    private TwoFingerGestureDetector    m_TwoFingerDetector;

    private VirtualDisplay m_VirtualDisplay;
    private MediaProjection m_MediaProjection;

    private AutoCarReceiver _carReceiver;

    private int                         m_ProjectionCode;
    private Intent                      m_ProjectionIntent;

    private int                         m_ScreenRotation;
    private int                         m_ScreenWidth;
    private int                         m_ScreenHeight;
    private double                      m_ProjectionOffsetX;
    private double                      m_ProjectionOffsetY;
    private double                      m_ProjectionWidth;
    private double                      m_ProjectionHeight;

    private class AutoCarReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(TAG, "AutoCarReceiver.onReceive: " + (intent != null ? intent.toString() : "null"));

            if (intent != null && intent.getAction() != null)
            {
                if (intent.getAction().equals(ACTION_USER_PRESENT))
                    OnUnlock();
                else if (intent.getAction().equals(ACTION_SCREEN_ON))
                    OnScreenOn();
                else if (intent.getAction().equals(ACTION_SCREEN_OFF))
                    OnScreenOff();
                else if (intent.getAction().equals(ACTION_POWER_CONNECTED))
                {
                    OnScreenOn();
                }
                else if (intent.getAction().equals(ACTION_POWER_DISCONNECTED))
                {
                    OnScreenOff();
                    handleHome();
                }
                /*else if (intent.getAction().equals(UiModeManager.ACTION_EXIT_CAR_MODE))
                {
                    OnScreenOff();
                    handleHome();
                }*/
            }
        }
    }

    private class MinitouchAsyncTask extends AsyncTask<Void, Void, Void>
    {
        MinitouchExecutor _minitouchExecutor;

        MinitouchAsyncTask(Context context)
        {
            _minitouchExecutor = new MinitouchExecutor(context);
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            Log.d(TAG, "MinitouchTask.doInBackground");
            _minitouchExecutor.start();
            return null;
        }

        @Override
        protected void onCancelled()
        {
            super.onCancelled();
        }

        public void stop()
        {
            _minitouchExecutor.stop();
        }
    }

    GestureDetector _doubleTapDetector;
    final GestureDetector.SimpleOnGestureListener _simpleGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            //handleKeyAction(getDefaultSharedPreferences("action_double_tap", 2));
            return true;
        }
    };

    public static boolean isPlugged(Context context)
    {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return isPlugged(intent);
    }

    public static boolean isPlugged(Intent intent)
    {
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        boolean isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
                || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;

        return isPlugged;
    }


    @Override
    public void onCreate(Bundle bundle)
    {
        setTheme(R.style.AppTheme);

        super.onCreate(bundle);

        setContentView(R.layout.activity_car_main);

        _doubleTapDetector = new GestureDetector(this, _simpleGestureListener);
        _doubleTapDetector.setOnDoubleTapListener(_simpleGestureListener);
        _doubleTapDetector.setIsLongpressEnabled(false);

        if(ShellManager.available())
        {
            _minitouchTask = new MinitouchAsyncTask(getApplicationContext());
            _minitouchTask.execute(null, null, null);

            ShellManager.createAsyncShell();
        }

        _minitouchSocket = new MinitouchSocket();

        new Handler().post(new Runnable()
        {
            @Override
            public void run()
            {
                if(!_minitouchSocket.isConnected())
                {
                    _minitouchSocket.connect(true);
                    UpdateTouchTransformations(true);
                }
            }
        });

        InitCarUiController(getCarUiController());
        setIgnoreConfigChanges(0xFFFF);

        _carReceiver = new AutoCarReceiver();
        m_RequestHandler = new Handler(this);

        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (powerManager != null)
            m_WakeLock = powerManager.newWakeLock(SCREEN_DIM_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP, "__WakeLock__");

        m_SurfaceView = (SurfaceView)findViewById(R.id.m_SurfaceView);

        m_SurfaceView.setOnTouchListener(this);
        m_Surface = m_SurfaceView.getHolder().getSurface();

        m_TwoFingerDetector = new TwoFingerGestureDetector(this, this);

        UpdateConfiguration(getResources().getConfiguration());

        UpdateTouchTransformations(true);

        m_ScreenResized = false;

        m_Car = Car.createCar(this, new CarConnectionCallback()
        {
            @Override
            public void onConnected(Car car)
            {
                Log.d(TAG, "onConnected");
                RequestAudioFocus();
            }

            @Override
            public void onDisconnected(Car car)
            {
                Log.d(TAG, "onDisconnected");
                AbandonAudioFocus();
            }
        });
        m_Car.connect();

        RequestProjectionPermission();
        test();
    }

    @Override
    public void onTwoFingerTapUp()
    {
        handleKeyAction(getDefaultSharedPreferences("action_2finger_tap", 2));
    }

    @Override
    public void onDestroy()
    {
        stopToolbarHideTimer();

        if(_minitouchSocket != null)
            _minitouchSocket.disconnect();

        if(_minitouchTask != null)
        {
            _minitouchTask.cancel(true);
            _minitouchTask.stop();
        }

        if (m_Car.isConnected())
        {
            AbandonAudioFocus();
            m_Car.disconnect();
        }

        stopTimer();

        if(getDefaultSharedPreferences("goto_home_at_stop", true))
        {
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    killCurrentApp();
                    launchHome(AutoActivity.this);
                }
            }, 1000);
        }

        super.onDestroy();
    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "onStart");
        //Util.toast(getApplicationContext(), "onStart");

        super.onStart();
        AutoApplication.EnableOrientationListener();

        OnScreenOn();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USER_PRESENT);
        filter.addAction(ACTION_SCREEN_ON);
        filter.addAction(ACTION_SCREEN_OFF);
        filter.addAction(ACTION_POWER_CONNECTED);
        filter.addAction(ACTION_POWER_DISCONNECTED);
        filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
        filter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);
        registerReceiver(_carReceiver, filter);
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "onStop");

        //Util.toast(getApplicationContext(), "onStop");

        super.onStop();
        AutoApplication.DisableOrientationListener();

        unregisterReceiver(_carReceiver);
        OnScreenOff();
    }

    @Override
    public void onWindowFocusChanged(boolean focus, boolean b1)
    {
        Log.d(TAG, "onWindowFocusChanged: " + focus);

        //Util.toast(getApplicationContext(), "onWindowFocusChanged: " + focus);

        super.onWindowFocusChanged(focus, b1);

        if(focus)
        {
            startScreenCapture();
            SetScreenSize();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration)
    {
        Log.d(TAG, "onConfigurationChanged: " + (configuration != null ? configuration.toString() : "null"));

        //Util.toast(getApplicationContext(), "onConfigurationChanged: " + (configuration != null ? configuration.toString() : "null"));

        super.onConfigurationChanged(configuration);
        UpdateConfiguration(configuration);
        UpdateTouchTransformations(true);
    }

    private void InitCarUiController(CarUiController controller)
    {
        Log.d(TAG, "InitCarUiController");
        controller.getStatusBarController().setTitle("");
        controller.getStatusBarController().hideAppHeader();
        controller.getStatusBarController().setAppBarAlpha(0.0f);
        controller.getStatusBarController().setAppBarBackgroundColor(Color.WHITE);
        controller.getStatusBarController().setDayNightStyle(DayNightStyle.AUTO);
        controller.getMenuController().hideMenuButton();
    }

    @ColorInt
    private int getColorCompat(@ColorRes int id)
    {
        if (Build.VERSION.SDK_INT >= 23)
            return getColor(id);
        else
            return getResources().getColor(id);
    }

    private void UpdateConfiguration(Configuration configuration)
    {
        /*if (configuration == null)
            return;

        Log.d(TAG, "UpdateConfiguration: " + configuration.toString());
        int backgroundColor;
        if ((configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            backgroundColor = getColorCompat(R.color.colorCarBackgroundNight);
        else
            backgroundColor = getColorCompat(R.color.colorCarBackgroundDay);*/

    }

    private void OnUnlock()
    {
        Log.d(TAG, "OnUnlock");
        m_SurfaceView.setKeepScreenOn(false);
        startBrightnessService();
        startOrientationService();
        SetScreenSize();
        SetImmersiveMode();
    }

    private void OnScreenOn()
    {
        if (m_WakeLock != null)
            m_WakeLock.acquire();

        Log.d(TAG, "OnScreenOn");

        //Util.toast(getApplicationContext(), "onScreenOn");

        m_SurfaceView.setKeepScreenOn(true);
        startScreenCapture();
        if (!IsLocked())
            OnUnlock();

        updateLeftToolbar();
    }

    private  void OnScreenOff()
    {
        Log.d(TAG, "OnScreenOff");
        //Util.toast(getApplicationContext(), "OnScreenOff");

        m_SurfaceView.setKeepScreenOn(false);
        stopBrightnessService();

        if (getDefaultSharedPreferences("reset_screen_rotation_on_stop", true))
            stopOrientationService();

        //if (getDefaultSharedPreferences("reset_screen_size_on_stop", true))
            ResetScreenSize();

        ResetImmersiveMode();
        stopScreenCapture();

        if (m_WakeLock != null && m_WakeLock.isHeld())
            m_WakeLock.release(ON_AFTER_RELEASE);
    }

    private boolean IsLocked()
    {
        Log.d(TAG, "IsLocked");
        if (Build.VERSION.SDK_INT < 22)
            return false;

        KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        return (km != null && km.isDeviceLocked());
    }

    private void startOrientationService()
    {
        int method = getDefaultSharedPreferences("orientation_method", 2);
        int rotation = getDefaultSharedPreferences("orientation_rotation", 1);

        startService(new Intent(this, OrientationService.class)
                .putExtra(OrientationService.METHOD, method)
                .putExtra(OrientationService.ROTATION, rotation));
    }

    private void stopOrientationService()
    {
        stopService(new Intent(this, OrientationService.class));
    }

    private void startBrightnessService()
    {
        boolean do_it = getDefaultSharedPreferences("overwrite_brightness", false);
        if (do_it)
        {
            int brightness = getDefaultSharedPreferences("overwrite_brightness_value", 0);
            startService(new Intent(this, BrightnessService.class)
                    .putExtra(BrightnessService.BRIGHTNESS, brightness)
                    .putExtra(BrightnessService.BRIGHTNESS_MODE, BrightnessService.SCREEN_BRIGHTNESS_MODE_MANUAL));
        }
    }

    private void stopBrightnessService()
    {
        stopService(new Intent(this, BrightnessService.class));
    }


    private void GenerateKeyEvent(final int keyCode, final boolean longPress)
    {
        Log.d(TAG, "GenerateKeyEvent");

        ShellManager.sendKey(keyCode, longPress);
    }

    private void SetScreenSize()
    {
        boolean do_it = true;//getDefaultSharedPreferences("set_screen_size_on_start", true);

        double c_width = m_SurfaceView.getWidth();
        double c_height = m_SurfaceView.getHeight();
        if (do_it && !IsLocked() && c_width > 0 && c_height > 0)
        {
            double ratio = c_width / c_height;
            double s_width = AutoApplication.DisplaySize.x;
            if (s_width > 0)
            {
                int height = (int)(s_width * ratio);
                if(height % 2 == 1)
                    height += 1;

                SetScreenSize((int)s_width, height);
            }
        }
    }
    private void SetScreenSize(int width, int height)
    {
        if (!m_ScreenResized)
        {
            Log.d(TAG, "SetScreenSize: " + width + " x " + height);
            m_ScreenResized = true;
            shellExec("wm size " + width + "x" + height);
        }
    }

    private void ResetScreenSize()
    {
        m_ScreenResized = false;
        shellExec("wm size reset");
    }

    private void SetImmersiveMode()
    {
        String immersiveMode = getDefaultSharedPreferences("immersive_mode", "immersive.full=*");
        if (immersiveMode.contains("immersive"))
        {
            shellExec("settings put global policy_control " + immersiveMode);
        }
    }

    private void ResetImmersiveMode()
    {
        shellExec("settings put global policy_control none*");
    }

    private void startScreenCapture()
    {
        stopScreenCapture();

        UpdateTouchTransformations(true);

        DisplayMetrics metrics = new DisplayMetrics();
        c().getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int ScreenDensity = metrics.densityDpi;

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null)
            m_MediaProjection = mediaProjectionManager.getMediaProjection(m_ProjectionCode, m_ProjectionIntent);

        if (m_MediaProjection != null && m_SurfaceView!= null)
        {
            int c_width = m_SurfaceView.getWidth();
            int c_height = m_SurfaceView.getHeight();

            Log.d(TAG, "c_width: " + c_width);
            Log.d(TAG, "c_height: " + c_height);
            Log.d(TAG, "ScreenDensity: " + ScreenDensity);

            if (c_width > 0 && c_height > 0)
            {
                m_VirtualDisplay = m_MediaProjection.createVirtualDisplay("ScreenCapture",
                        c_width, c_height, ScreenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        m_Surface, null, null);

                Log.d(TAG, "startScreenCapture started !!");
            }
        }
    }

    private void stopScreenCapture()
    {
        Log.d(TAG, "stopScreenCapture");

        if (m_VirtualDisplay != null)
        {
            m_VirtualDisplay.release();
            m_VirtualDisplay = null;
        }

        if (m_MediaProjection != null)
        {
            m_MediaProjection.stop();
            m_MediaProjection = null;
        }
    }

    private void UpdateTouchTransformations(boolean force)
    {
        if ((AutoApplication.ScreenRotation == m_ScreenRotation) &&
                AutoApplication.ScreenSize.equals(m_ScreenWidth, m_ScreenHeight) &&
                !force)
            return;

        if (m_SurfaceView == null)
            return;

        m_ScreenRotation = AutoApplication.ScreenRotation;
        m_ScreenWidth = AutoApplication.ScreenSize.x;
        m_ScreenHeight = AutoApplication.ScreenSize.y;
        double ScreenWidth = m_ScreenWidth;
        double ScreenHeight = m_ScreenHeight;

        double SurfaceWidth = m_SurfaceView.getWidth();
        double SurfaceHeight = m_SurfaceView.getHeight();

        double factX = SurfaceWidth / ScreenWidth;
        double factY = SurfaceHeight / ScreenHeight;

        double fact = (factX < factY ? factX : factY);

        m_ProjectionWidth = fact * ScreenWidth;
        m_ProjectionHeight = fact * ScreenHeight;

        m_ProjectionOffsetX = (SurfaceWidth - m_ProjectionWidth) / 2.0;
        m_ProjectionOffsetY = (SurfaceHeight - m_ProjectionHeight) / 2.0;

        if (m_ScreenRotation == ROTATION_0 || m_ScreenRotation == ROTATION_180)
            _minitouchSocket.UpdateTouchTransformations(m_ScreenWidth, m_ScreenHeight, AutoApplication.DisplaySize);
        else
            _minitouchSocket.UpdateTouchTransformations(m_ScreenHeight, m_ScreenWidth, AutoApplication.DisplaySize);
    }

    Queue<MotionEvent> _eventDown = new LinkedList<>();

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (_minitouchSocket != null && event != null)
        {
            if (!_minitouchSocket.isConnected())
            {
                _minitouchSocket.connect(true);
                UpdateTouchTransformations(true);
            }
            else
            {
                UpdateTouchTransformations(false);
            }

            boolean ok = _minitouchSocket.isConnected();
            int action = event.getActionMasked();
            for (int i = 0; i < event.getPointerCount() && ok; i++)
            {
                int id = event.getPointerId(i);
                double x = (event.getX(i) - m_ProjectionOffsetX) / m_ProjectionWidth;
                double y = (event.getY(i) - m_ProjectionOffsetY) / m_ProjectionHeight;
                double pressure = event.getPressure(i);

                double rx = x;
                double ry = y;
                switch (m_ScreenRotation)
                {
                    case ROTATION_0:
                    {
                        rx = x;
                        ry = y;
                        break;
                    }
                    case ROTATION_90:
                    {
                        rx = 1.0 - y;
                        ry = x;
                        break;
                    }
                    case ROTATION_180:
                    {
                        rx = 1.0 - x;
                        ry = 1.0 - y;
                        break;
                    }
                    case ROTATION_270:
                    {
                        rx = y;
                        ry = 1.0 - x;
                        break;
                    }
                }
                switch (action)
                {
                    case ACTION_DOWN:
                    case ACTION_POINTER_DOWN:
                        ok = ok && _minitouchSocket.TouchDown(id, rx, ry, pressure);
                        break;
                    case ACTION_MOVE:
                        ok = ok && _minitouchSocket.TouchMove(id, rx, ry, pressure);
                        break;
                    case ACTION_UP:
                    case ACTION_CANCEL:
                        ok = ok && _minitouchSocket.TouchUpAll();
                        break;
                    case ACTION_POINTER_UP:
                        ok = ok && _minitouchSocket.TouchUp(id);
                        break;
                }
            }

            if(m_TwoFingerDetector.onTouchEvent(event))
            {
                _minitouchSocket.TouchReset();
            }

            if(ok)
            {
                _minitouchSocket.TouchCommit();
            }
        }
        else
            m_TwoFingerDetector.onTouchEvent(event);

        _doubleTapDetector.onTouchEvent(event);

        resetToolbarHideTimer();

        return true;
    }

    @Override
    public boolean handleMessage(Message msg)
    {
        Log.d(TAG, "handleMessage: " + (msg != null ? msg.toString() : "null"));

        if (msg != null)
        {
            if (msg.what == REQUEST_MEDIA_PROJECTION_PERMISSION)
            {
                m_ProjectionCode = msg.arg2;
                m_ProjectionIntent = (Intent)msg.obj;

                startScreenCapture();
                requestPermissions();

                if(getDefaultSharedPreferences("goto_home_at_start", true))
                {
                    new Handler().postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            AutoActivity.launchHome(getApplicationContext());

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    String launch_app_at_start = SettingUtil.getString(getApplicationContext(), "launch_app_at_start");
                                    if(!TextUtils.isEmpty(launch_app_at_start))
                                    {
                                        Util.openApp(getApplicationContext(), launch_app_at_start, false);
                                    }
                                }
                            }, 500);

                        }
                    }, 1000);
                }
                else
                {
                    String launch_app_at_start = SettingUtil.getString(getApplicationContext(), "launch_app_at_start");
                    if(!TextUtils.isEmpty(launch_app_at_start))
                    {
                        Util.openApp(getApplicationContext(), launch_app_at_start, false);
                    }
                }
            }
        }
        return false;
    }

    private void startActivityForResult(int what, Intent intent)
    {
        Log.d(TAG, "startActivityForResult");
        ResultRequestActivity.startActivityForResult(this, m_RequestHandler, what, intent, what);
    }

    private void RequestProjectionPermission()
    {
        Log.d(TAG, "RequestProjectionPermission");
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if(mediaProjectionManager != null)
            startActivityForResult(REQUEST_MEDIA_PROJECTION_PERMISSION, mediaProjectionManager.createScreenCaptureIntent());
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

    private void RequestAudioFocus()
    {
        if(getDefaultSharedPreferences("request_audio_focus_at_start", false))
        {
            Log.d(TAG, "RequestAudioFocus");
            try
            {
                CarAudioManager carAM = m_Car.getCarManager(CarAudioManager.class);
                carAM.requestAudioFocus(null, carAM.getAudioAttributesForCarUsage(CAR_AUDIO_USAGE_DEFAULT), AUDIOFOCUS_GAIN, 0);
            }
            catch (Exception e)
            {
                Log.d(TAG, "RequestAudioFocus exception: " + e.toString());
            }
        }
    }

    private void AbandonAudioFocus()
    {
        Log.d(TAG, "AbandonAudioFocus");
        try
        {
            CarAudioManager carAM = m_Car.getCarManager(CarAudioManager.class);
            carAM.abandonAudioFocus(null, carAM.getAudioAttributesForCarUsage(CAR_AUDIO_USAGE_DEFAULT));
        }
        catch (Exception e)
        {
            Log.d(TAG, "AbandonAudioFocus exception: " + e.toString());
        }
    }

    private String getDefaultSharedPreferences(String key, @Nullable String defValue)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString(key, defValue);
    }

    private int getDefaultSharedPreferences(String key, int defValue)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String result = sharedPref.getString(key, Integer.toString(defValue));

        try
        {
            return Integer.parseInt(result);
        }
        catch (Exception e)
        {
            return  defValue;
        }
    }

    private boolean getDefaultSharedPreferences(String key, boolean defValue)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean(key, defValue);
    }

    ///////////////////////////////////////////////////////////////

    private void killCurrentApp()
    {
        try
        {
            final String packageName = Util.getCurrentApp();

            Log.d(TAG, "killCurrentApp: " + packageName);

            Intent intent= new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo defaultLauncher= getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            String nameOfLauncherPkg= defaultLauncher.activityInfo.packageName;

            Log.d(TAG, nameOfLauncherPkg + ", " + packageName);

            if(packageName.equals(getPackageName()) || packageName.equals(nameOfLauncherPkg))
            {
                return;
            }

            ShellManager.runSU("am force-stop " + packageName);
            //ShellManager.runSU("kill " + pid);
            //Util.killProcess(packageName);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
                        am.killBackgroundProcesses(packageName);
                    }
                    catch(Exception e){}
                }
            }, 1000);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private String getHomeApp()
    {
        try
        {
            PackageManager localPackageManager = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            return localPackageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName;
        }
        catch(Exception e){}
        return null;
    }

    public static void launchHome(Context context)
    {
        try
        {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        catch(Exception e){}
    }

    private void handleHome()
    {
        killCurrentApp();
        launchHome(getApplicationContext());
    }


    TextView _textClock;
    View _toolBar;
    boolean _toolBarVisible;

    TextView _textBattery;
    ImageView _imageBattery;
    Intent _batteryStatus;

    BroadcastReceiver _batteryReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            _batteryStatus = intent;
            //Log.d(TAG, "batteryReceiver onReceive: " + intent);
        }
    };

    //Timer _timerToolbarHide;
    Handler _handlerToolbarHide = new Handler();
    Runnable _runnableToolbarHide = new Runnable() {
        @Override
        public void run()
        {
            stopToolbarHideTimer();

            if(_toolBarVisible)
                toggleToolbar();
        }
    };

    long _left_toolbar_auto_hide_time = -100;
    boolean _postedHideToolbar = false;
    Handler _handlerPostDelay = new Handler();

    private void resetToolbarHideTimer()
    {
        if(!_toolBarVisible)
            return;

        if(_left_toolbar_auto_hide_time == -100)
            _left_toolbar_auto_hide_time = getDefaultSharedPreferences("left_toolbar_auto_hide_time", 5) * 1000;

        if(_left_toolbar_auto_hide_time < 1000)
            return;

        stopToolbarHideTimer();

        if(_postedHideToolbar)
            return;

        _postedHideToolbar = true;

        _handlerPostDelay.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                _postedHideToolbar = false;
                resetToolbarHideTimerImpl();
            }
        }, 300);
    }

    private void resetToolbarHideTimerImpl()
    {
        Log.d(TAG, "resetToolbarHideTimerImpl");
        _handlerToolbarHide.postDelayed(_runnableToolbarHide, _left_toolbar_auto_hide_time);
    }

    private void stopToolbarHideTimer()
    {
        try
        {
            _handlerToolbarHide.removeCallbacks(_runnableToolbarHide);
        }
        catch (Exception e){}
    }

    private void updateLeftToolbar()
    {
        _toolBar = findViewById(R.id.toolBar);
        //MySurfaceView surfaceView = (MySurfaceView)findViewById(R.id.m_SurfaceView);
        MyFrameLayout layoutSurface = (MyFrameLayout)findViewById(R.id.layoutSurface);
        View btnOpenMenu = findViewById(R.id.btnOpenMenu);
        btnOpenMenu.setVisibility(View.GONE);

        btnOpenMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleToolbar();
            }
        });

        _toolBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleToolbar();
            }
        });

        _toolBar.bringToFront();

        {
            int left_toolbar_size = getDefaultSharedPreferences("left_toolbar_size", 70);
            if(left_toolbar_size < 50 || left_toolbar_size > 100)
                left_toolbar_size = 70;

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)_toolBar.getLayoutParams();
            params.width = Util.DP2PX(this, left_toolbar_size);
            _toolBar.setLayoutParams(params);
        }

        View layoutBattery = findViewById(R.id.layoutBattery);

        if(getDefaultSharedPreferences("left_toolbar_show_battery_level", true))
            layoutBattery.setVisibility(View.VISIBLE);
        else
            layoutBattery.setVisibility(View.GONE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)layoutSurface.getLayoutParams();
        params.removeRule(RelativeLayout.END_OF);

        _textClock = (TextView)findViewById(R.id.textClock);
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Regular.ttf");
        _textClock.setTypeface(tf);

        TextViewCompat.setAutoSizeTextTypeWithDefaults(_textClock, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);

        _textBattery = (TextView)findViewById(R.id.textBattery);
        tf = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Regular.ttf");
        _textBattery.setTypeface(tf);

        _imageBattery = (ImageView)findViewById(R.id.imageBattery);

        startTimer();

        View btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
                if(!KeyEventService.sendGlobaleKey(AccessibilityService.GLOBAL_ACTION_BACK))
                    GenerateKeyEvent(KeyEvent.KEYCODE_BACK, false);

                resetToolbarHideTimer();
            }
        });

        if(getDefaultSharedPreferences("show_left_toolbar", false))
        {
            if(SettingUtil.getBoolean(this, "left_toolbar_floating", true))
            {
                btnOpenMenu.setVisibility(View.VISIBLE);
                _toolBar.setVisibility(View.VISIBLE);
                _toolBarVisible = true;
                toggleToolbar();
            }
            else
            {
                params.addRule(RelativeLayout.END_OF, _toolBar.getId());
                _toolBar.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            _toolBar.setVisibility(View.GONE);
            layoutSurface.setRatio(-1);
        }

        layoutSurface.setLayoutParams(params);

        if(getDefaultSharedPreferences("show_left_toolbar_use_system_key", true))
            buildSystemKeys();
        else
            buildFavorites();
    }

    private void toggleToolbar()
    {
        _toolBarVisible = !_toolBarVisible;

        int diff = _toolBar.getLayoutParams().width;

        if(_toolBarVisible)
        {
            ObjectAnimator.ofFloat(_toolBar, "translationX", -diff, 0)
                    .start();

            resetToolbarHideTimer();
        }
        else
        {
            ObjectAnimator.ofFloat(_toolBar, "translationX", 0, -diff)
                    .start();

            stopToolbarHideTimer();
        }
    }

    Timer _timer;
    Handler _timerHandler;

    private void startTimer()
    {
        stopTimer();

        registerReceiver(_batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        _timerHandler = new Handler(Looper.getMainLooper());

        _timer = new Timer();
        _timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                _timerHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        handleTimer();
                    }
                });
            }
        }, 0, 2000);
    }

    private void stopTimer()
    {
        if(_timer != null)
        {
            _timer.cancel();
            _timer = null;
        }

        try
        {
            unregisterReceiver(_batteryReceiver);
        }
        catch(Exception e){}
    }

    SimpleDateFormat _formatClock = new SimpleDateFormat("h:mm", Locale.getDefault());

    private void handleTimer()
    {
        _textClock.setText(_formatClock.format(new Date()));

        if(_batteryStatus != null)
        {
            int level = _batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = _batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level / (float)scale;

            _textBattery.setText(String.format(Locale.ENGLISH, "%.0f%%", batteryPct*100.f));

            int status = _batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            _imageBattery.setImageResource(isCharging ? R.drawable.ic_battery_charging_full_white_18dp : R.drawable.ic_battery_std_white_18dp);
        }

    }





    //////////////////////////////////////////////////////////////////////


    ListAdapter _adapter;

    private void buildSystemKeys()
    {
        findViewById(R.id.layoutSystemKeys).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutFavorites).setVisibility(View.GONE);


        findViewById(R.id.btnSystemNotification).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                v.playSoundEffect(android.view.SoundEffectConstants.CLICK);

                if(!KeyEventService.sendGlobaleKey(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS))
                    GenerateKeyEvent(KeyEvent.KEYCODE_NOTIFICATION, false);

                resetToolbarHideTimer();
            }
        });

        findViewById(R.id.btnSystemAppSwitch).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                v.playSoundEffect(android.view.SoundEffectConstants.CLICK);

                if(!KeyEventService.sendGlobaleKey(AccessibilityService.GLOBAL_ACTION_RECENTS))
                    GenerateKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, false);

                resetToolbarHideTimer();
            }
        });

        findViewById(R.id.btnSystemHome).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                v.playSoundEffect(android.view.SoundEffectConstants.CLICK);

                launchHome(AutoActivity.this);

                resetToolbarHideTimer();
            }
        });

        findViewById(R.id.btnSystemBack).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                v.playSoundEffect(android.view.SoundEffectConstants.CLICK);

                if(!KeyEventService.sendGlobaleKey(AccessibilityService.GLOBAL_ACTION_BACK))
                    GenerateKeyEvent(KeyEvent.KEYCODE_BACK, false);

                resetToolbarHideTimer();
            }
        });
    }

    private void buildFavorites()
    {
        findViewById(R.id.layoutSystemKeys).setVisibility(View.GONE);
        findViewById(R.id.layoutFavorites).setVisibility(View.VISIBLE);

        ListView listView = (ListView)findViewById(R.id.listView);
        //listView.setVerticalSpacing(Util.DP2PX(this, 8));
        //listView.setNumColumns(1);

        //listView.setSelector(new PaintDrawable(0xffff0000 ));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Animation animation = new AlphaAnimation(0.3f, 0.9f);
                animation.setDuration(500);
                view.startAnimation(animation);

                final String packageName = FavoritesLoader.instance().getItems(getApplicationContext()).get(position);
                handleAppClick(packageName);

                resetToolbarHideTimer();
            }
        });

        FavoritesLoader.instance().check(this);
        listView.setAdapter(_adapter = new ListAdapter());
    }


    private class ListAdapter extends BaseAdapter
    {
        @Override
        public Object getItem(int position)
        {
            return null;
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public int getCount()
        {
            return FavoritesLoader.instance().getItems(getApplicationContext()).size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent)
        {
            View v = convertView;

            if(v == null)
            {
                v = LayoutInflater.from(AutoActivity.this).inflate(R.layout.app_favorites_cell, parent, false);
            }

            v.setAlpha(0.9f);

            ImageView imageIcon = v.findViewById(R.id.imageIcon);

            final String packageName = FavoritesLoader.instance().getItems(getApplicationContext()).get(position);

            try
            {
                imageIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
            }
            catch(Exception e){}

            return v;
        }
    }

    private void handleAppClick(String packageName)
    {
        try
        {
            startActivity(getPackageManager().getLaunchIntentForPackage(packageName));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void handleKeyAction(int action)
    {
        switch(action)
        {
            case 1:

                if(!KeyEventService.sendGlobaleKey(AccessibilityService.GLOBAL_ACTION_BACK))
                    GenerateKeyEvent(KeyEvent.KEYCODE_BACK, false);

                resetToolbarHideTimer();

                break;
            case 2:
                handleHome();
                resetToolbarHideTimer();
                break;

            case 3:
                launchHome(getApplicationContext());
                resetToolbarHideTimer();
                break;

            case 4:
                toggleToolbar();
                break;
        }
    }

    private void test()
    {
    }
}
