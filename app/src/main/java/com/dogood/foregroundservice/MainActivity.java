package com.dogood.foregroundservice;

import androidx.appcompat.app.AppCompatActivity;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.dogood.foregroundservice.databinding.ActivityMainBinding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBiding;
    private MyService mService;
    private boolean mBound = false;
    private Logger mLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBiding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBiding.getRoot());
        mLogger = LoggerFactory.getLogger(getClass());
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean enabled = isAccessibilityServiceEnabled(MyAccessibilityService.class);
        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        }

    }

    public void startService(View v) {
        MyService.startService(this, "Starting");
        setStartBtnVisibility(false);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, MyService.class);
        mService.selfStop();
        stopService(serviceIntent);
        setStartBtnVisibility(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, MyService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        mBound = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBiding = null;
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MyService.LocalBinder binder = (MyService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            boolean isServiceRunning = mService.isRunning();
            setStartBtnVisibility(!isServiceRunning);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void setStartBtnVisibility(boolean isServiceRunning) {
        mBiding.startBtn.setEnabled(isServiceRunning);
        mBiding.stopBtn.setEnabled(!isServiceRunning);
    }

    private boolean isAccessibilityServiceEnabled(Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }

        return false;
    }
}