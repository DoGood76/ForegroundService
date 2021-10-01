package com.dogood.foregroundservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Objects;

//public class KeyEventReceiver extends BroadcastReceiver {
//    public static final String ACTION = "com.dogood.foregroundservice.KeyEventReceiver";
//    private static final String EXTRA_DATA = "EXTRA_DATA";
//    private static final String EXTRA_SOUND = "EXTRA_SOUND";
//
//    public KeyEventReceiver() {
//    }
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        // TODO: This method is called when the BroadcastReceiver is receiving
//        // an Intent broadcast.
//        if (!intent.getAction().isEmpty() && intent.getAction().equals(ACTION)) {
//            String msg = intent.getStringExtra(EXTRA_DATA);
//            if (msg != null) {
//                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
//            }
//            int soundType = intent.getIntExtra(EXTRA_SOUND,-1);
//            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
//            switch (soundType){
//                case 0:
//                    toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 150);
//                    break;
//                case 1:
//                    toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 150);
//                    break;
//                default:
//                    toneGen1.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 150);
//
//            }
//        }
//    }
//
//
//    public static void reportKeyEvent(@NonNull Context context, @NonNull String msg, int soundType) {
//        Intent intent = new Intent();
//        intent.setAction(ACTION);
//        intent.putExtra(EXTRA_DATA, msg);
//        intent.putExtra(EXTRA_SOUND,soundType);
//        context.sendBroadcast(intent);
//    }
//}