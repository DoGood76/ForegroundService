package com.dogood.foregroundservice;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAccessibilityService extends AccessibilityService {

    private Logger mLogger;
    private boolean mBound = false;
    public static final String TAG = MyAccessibilityService.class.getSimpleName();

    public MyAccessibilityService() {
        mLogger = LoggerFactory.getLogger(MyAccessibilityService.class);
    }

    @Override
    protected void onServiceConnected() {
        mLogger.debug("Service connected");
        MyService.startService(this, "Starting");

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        mLogger.debug("Got event:{}", event);
        Log.d(TAG, "Got event:" + event);

    }

    @Override
    public void onInterrupt() {
        mLogger.debug("onInterrupt");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        mLogger.debug("Got onKeyEvent:{}", event);
        Log.d(TAG, "Got onKeyEvent:" + event);
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            mLogger.debug("Key down");
            if (isServiceRunningInForeground(this,MyService.class)){
                MyService.KeyEventReceiver.reportKeyEvent(this, "Key Down", 0);
                return true;
            }
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            mLogger.debug("Key up");
            if (isServiceRunningInForeground(this,MyService.class)){
                MyService.KeyEventReceiver.reportKeyEvent(this, "Key up", 1);
                return true;
            }
        }
        return false;
    }

    private boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return service.foreground;
            }
        }
        return false;
    }
}
