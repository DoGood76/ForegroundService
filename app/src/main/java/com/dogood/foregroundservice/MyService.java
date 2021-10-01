package com.dogood.foregroundservice;

import static com.dogood.foregroundservice.MyApp.CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.VolumeProviderCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyService extends Service {
    // Binder given to clients
    private MediaSessionCompat mediaSession;

    private final IBinder mBinder = new LocalBinder();
    private boolean mIsRunning;
    public static final String TAG = MyService.class.getSimpleName();
    private final Logger mLogger;
    private static EventsHandler mEventsHandler;


    public static final String SERVICE_INTENT_ACTION = "inputExtra";
    private KeyEventReceiver mKeyEventReceiver;
    private PowerManager.WakeLock mWakeLock;
    private HeadsetIntentReceiver mHeadsetIntentReceiver;

    public MyService() {
        mIsRunning = false;
        mKeyEventReceiver = null;
        mWakeLock = null;
        mLogger = LoggerFactory.getLogger(MyService.class);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
        mHeadsetIntentReceiver = new HeadsetIntentReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadsetIntentReceiver, filter);

        mEventsHandler = new EventsHandler();
        mEventsHandler.start();
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra(SERVICE_INTENT_ACTION);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Example Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        //do heavy work on a background thread
        //stopSelf();
        PrimeThread p = new PrimeThread(143);
        p.start();

        IntentFilter intentFilter = new IntentFilter(ACTION);
        mKeyEventReceiver = new KeyEventReceiver();
        registerReceiver(mKeyEventReceiver, intentFilter);

        mediaSession.setActive(true);
        mIsRunning = true;

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "foregroundservice::MyService");
        mWakeLock.acquire();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
        }

        if (mHeadsetIntentReceiver != null) {
            unregisterReceiver(mHeadsetIntentReceiver);
        }

        cleanMeUp();
        if (mEventsHandler != null) {
            mEventsHandler.quit();
            mEventsHandler = null;
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        MyService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyService.this;
        }
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void selfStop() {
        cleanMeUp();
        stopForeground(true);
        stopSelf();
    }

    private void cleanMeUp() {
        mediaSession.setActive(false);
        mIsRunning = false;
        unRegKeyEventReceiver();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private synchronized void unRegKeyEventReceiver() {
        if (mKeyEventReceiver != null) {
            unregisterReceiver(mKeyEventReceiver);
            mKeyEventReceiver = null;
        }
    }

    class PrimeThread extends Thread {
        private final long mMinWait;

        PrimeThread(long minPrime) {
            mMinWait = minPrime;
        }

        public void run() {
            // compute primes larger than minPrime
            while (mIsRunning) {
                try {
                    Thread.sleep(mMinWait);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mLogger.debug("Test");
            }

        }
    }

    private void initMediaPlayer() {
        mediaSession = new MediaSessionCompat(this, "PlayerService");
//        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
//                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0) //you simulate a player which plays something.
                .build());

        final ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        //this will only work on Lollipop and up, see https://code.google.com/p/android/issues/detail?id=224134
        VolumeProviderCompat myVolumeProvider =
                new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, /*max volume*/100, /*initial volume level*/50) {
                    @Override
                    public void onAdjustVolume(int direction) {

                /*
                -1 -- volume down
                1 -- volume up
                0 -- volume button released
                 */
                        mLogger.debug("Test2");

//                        switch (direction) {
//                            case -1:
//                                Toast.makeText(MyService.this, "Volume Down", Toast.LENGTH_LONG).show();
//                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
//                                break;
//                            case 1:
//                                Toast.makeText(MyService.this, "Volume Up", Toast.LENGTH_LONG).show();
//                                toneGen1.startTone(ToneGenerator.TONE_CDMA_ANSWER, 150);
//                                break;
//                            default:
//                                toneGen1.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 150);
                        switch (direction) {
                            case -1:
                                mEventsHandler.sendEvent(EventsHandler.ACCESSIBILITY_DOWN_KEY_EVENT);
                                break;
                            case 1:
                                mEventsHandler.sendEvent(EventsHandler.ACCESSIBILITY_UP_KEY_EVENT);
                                break;
                            default:
                                mLogger.debug("Key released");
                                //mEventsHandler.sendEvent(EventsHandler.UNKNOWN_EVENT);
                        }
                    }
                };

        mediaSession.setPlaybackToRemote(myVolumeProvider);
    }

    ///-------------------------------------------------
    public static void startService(Context context, String notificationMessage) {
        Intent serviceIntent = new Intent(context, MyService.class);
        serviceIntent.putExtra("inputExtra", notificationMessage);
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    public static final String ACTION = "com.dogood.foregroundservice.KeyEventReceiver";

    public static class KeyEventReceiver extends BroadcastReceiver {
        private static final String EXTRA_DATA = "EXTRA_DATA";
        private static final String EXTRA_SOUND = "EXTRA_SOUND";

        public KeyEventReceiver() {
        }

        public static void reportKeyEvent(@NonNull Context context, @NonNull String msg, int soundType) {
            Intent intent = new Intent();
            intent.setAction(ACTION);
            intent.putExtra(EXTRA_DATA, msg);
            intent.putExtra(KeyEventReceiver.EXTRA_SOUND, soundType);
            context.sendBroadcast(intent);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: This method is called when the BroadcastReceiver is receiving
            // an Intent broadcast.
            if (!intent.getAction().isEmpty() && intent.getAction().equals(ACTION)) {
                String msg = intent.getStringExtra(EXTRA_DATA);
                if (msg != null) {
                    int soundType = intent.getIntExtra(EXTRA_SOUND, -1);
                    switch (soundType) {
                        case 0:
                            mEventsHandler.sendEvent(EventsHandler.ACCESSIBILITY_DOWN_KEY_EVENT);
                            break;
                        case 1:
                            mEventsHandler.sendEvent(EventsHandler.ACCESSIBILITY_UP_KEY_EVENT);
                            break;
                        default:
                            mEventsHandler.sendEvent(EventsHandler.UNKNOWN_EVENT);
                    }
                }
            }
        }
    }

    private class EventsHandler extends HandlerThread {
        public static final int UNKNOWN_EVENT = -1;
        public static final int ACCESSIBILITY_DOWN_KEY_EVENT = 1;
        public static final int ACCESSIBILITY_UP_KEY_EVENT = 2;
        public static final int MEDIA_SESSION_DOWN_KEY_EVENT = 3;
        public static final int MEDIA_SESSION_UP_KEY_EVENT = 4;
        public static final int HEADSET_PLUGGED_IN = 5;
        public static final int HEADSET_PLUGGED_OUT = 6;
        public static final int HEADSET_UNKNOWN_EVENT = 7;


        private Handler mHandler;

        public EventsHandler() {
            super("EventsHandler");
        }

        protected void onLooperPrepared() {
            mHandler = new Handler(Looper.myLooper()) {
                final ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case ACCESSIBILITY_DOWN_KEY_EVENT:
                            mLogger.debug("ACCESSIBILITY_DOWN_KEY_EVENT");
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 10);
                            break;

                        case ACCESSIBILITY_UP_KEY_EVENT:
                            mLogger.debug("ACCESSIBILITY_UP_KEY_EVENT");
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 10);
                            break;

                        case MEDIA_SESSION_DOWN_KEY_EVENT:
                            mLogger.debug("MEDIA_SESSION_DOWN_KEY_EVENT");
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_NETWORK_BUSY, 10);
                            break;

                        case MEDIA_SESSION_UP_KEY_EVENT:
                            mLogger.debug("MEDIA_SESSION_UP_KEY_EVENT");
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_NETWORK_CALLWAITING, 10);
                            break;

                        case HEADSET_PLUGGED_IN:
                            mLogger.debug("HEADSET_PLUGGED_IN");
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SS, 15);
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SSL, 15);
                            break;

                        case HEADSET_PLUGGED_OUT:
                            mLogger.debug("HEADSET_PLUGGED_OUT");
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_L, 15);
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_S_X4, 15);
                            break;
                        case HEADSET_UNKNOWN_EVENT:
                            mLogger.debug("HEADSET_UNKNOWN_EVENT");
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_PING_RING, 5);
                            break;
                        default:
                            mLogger.debug("Unknown key event");
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_INTERGROUP, 10);

                    }
                }
            };
        }

        public void sendEvent(int eventType) {
            if (mIsRunning) {
                Message msg = mHandler.obtainMessage(eventType);
                mHandler.sendMessage(msg);
            } else {
                mLogger.debug("We are not ready to process events {}", eventType);
            }
        }
    }

    private class HeadsetIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        mLogger.debug("Headset is unplugged");
                        mEventsHandler.sendEvent(EventsHandler.HEADSET_PLUGGED_OUT);
                        break;
                    case 1:
                        mLogger.debug("Headset is plugged");
                        mEventsHandler.sendEvent(EventsHandler.HEADSET_PLUGGED_IN);
                        break;
                    default:
                        mLogger.debug("I have no idea what the headset state is");
                        mEventsHandler.sendEvent(EventsHandler.HEADSET_UNKNOWN_EVENT);
                }
            }
        }
    }
}