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
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.VolumeProviderCompat;


public class MyService extends Service {
    // Binder given to clients
    private MediaSessionCompat mediaSession;

    private final IBinder mBinder = new LocalBinder();
    private boolean mIsRunning;
    public static final String TAG = MyService.class.getSimpleName();


    public static final String SERVICE_INTENT_ACTION = "inputExtra";
    private KeyEventReceiver mKeyEventReceiver;
    private PowerManager.WakeLock mWakeLock;

    public MyService() {
        mIsRunning = false;
        mKeyEventReceiver = null;
        mWakeLock = null;
    }

//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }


    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
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
        cleanMeUp();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void volumeDownEvent() {
        Toast.makeText(this, "Volume Down event", Toast.LENGTH_LONG).show();
        KeyEventReceiver.reportKeyEvent(this, "Key Down", 0);
    }

    public void volumeUpEvent() {
        Toast.makeText(this, "Volume Up event", Toast.LENGTH_LONG).show();
        KeyEventReceiver.reportKeyEvent(this, "Key up", 1);
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
        long minPrime;

        PrimeThread(long minPrime) {
            this.minPrime = minPrime;
        }

        public void run() {
            // compute primes larger than minPrime
            while (mIsRunning) {
                try {
                    Thread.sleep(minPrime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Test");
            }

        }
    }

    private void initMediaPlayer() {
        mediaSession = new MediaSessionCompat(this, "PlayerService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
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
                        Log.d(TAG, "Test2");

                        switch (direction) {
                            case -1:
                                Toast.makeText(MyService.this, "Volume Down", Toast.LENGTH_LONG).show();
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                                break;
                            case 1:
                                Toast.makeText(MyService.this, "Volume Up", Toast.LENGTH_LONG).show();
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_ANSWER, 150);
                                break;
                            default:
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 150);

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
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                }
                int soundType = intent.getIntExtra(EXTRA_SOUND, -1);
                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                switch (soundType) {
                    case 0:
                        toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 150);
                        break;
                    case 1:
                        toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 150);
                        break;
                    default:
                        toneGen1.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 150);

                }
            }
        }
    }
}