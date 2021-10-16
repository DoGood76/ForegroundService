package com.dogood.foregroundservice;

import static androidx.core.app.NotificationCompat.PRIORITY_MAX;
import static com.dogood.foregroundservice.MyApp.CHANNEL_ID;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAccessibilityService extends AccessibilityService {

    private final Logger mLogger;
    private PowerManager.WakeLock mWakeLock;
//    private EventsHandler mEventsHandler;

    public MyAccessibilityService() {
        mLogger = LoggerFactory.getLogger(MyAccessibilityService.class);
    }

    @Override
    protected void onServiceConnected() {
        mLogger.debug("Service connected");
        MyService.startService(this, "Starting");
        startAccService(this, "");

    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        mWakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK, "AccessibilityForegroundService::MyAccessibilityService");
        mWakeLock.acquire();

//        mEventsHandler = new EventsHandler();
//        mEventsHandler.start();


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setPriority(PRIORITY_MAX)
                .setContentTitle(getString(R.string.notification_default_content_title))
                .setContentText(getString(R.string.notification_default_content_text))
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);


        //mEventsHandler.sendEvent();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = null;
        }
//        if (mEventsHandler != null) {
//            mEventsHandler.quit();
//            mEventsHandler = null;
//        }

        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        mLogger.debug("Got event:{}", event);

    }

    @Override
    public void onInterrupt() {
        mLogger.debug("onInterrupt");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        mLogger.debug("Got onKeyEvent:{}", event);
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                mLogger.debug("Key down");
                if (isServiceRunningInForeground(this, MyService.class)) {
                    MyService.KeyEventReceiver.reportKeyEvent(this, "Key Down", 0);
                    return true;
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                mLogger.debug("Key up");
                if (isServiceRunningInForeground(this, MyService.class)) {
                    MyService.KeyEventReceiver.reportKeyEvent(this, "Key up", 1);
                    return true;
                }
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

    public static void startAccService(Context context, String notificationMessage) {
        Intent serviceIntent = new Intent(context, MyAccessibilityService.class);
        serviceIntent.putExtra("inputExtra", notificationMessage);
        ContextCompat.startForegroundService(context, serviceIntent);
    }

//    private class EventsHandler extends HandlerThread {
//        public static final int UNKNOWN_EVENT = -1;
//
//
//        private Handler mHandler;
//
//        public EventsHandler() {
//            super("EventsHandler");
//        }
//
//        protected void onLooperPrepared() {
//            mHandler = new Handler(Looper.myLooper()) {
//                final ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
//
//                @Override
//                public void handleMessage(Message msg) {
//                    if (msg.what == UNKNOWN_EVENT) {
//                        mEventsHandler.mHandler.removeMessages(UNKNOWN_EVENT);
//                        mLogger.debug("Unknown key event");
//                        toneGen1.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_INTERGROUP, 1);
//                        mEventsHandler.sendEvent();
//                    }
//                }
//            };
//        }
//
//        public void sendEvent() {
//            if (mWakeLock != null) {
//                mHandler.sendEmptyMessageDelayed(UNKNOWN_EVENT, TimeUnit.SECONDS.toMillis(30));
//            }
//        }
//    }
}
