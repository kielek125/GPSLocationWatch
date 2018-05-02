package com.malavero.trackyourchild.watchgps.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;

/**
 * Created by Kiełson on 01.05.2018.
 */

public class Utils {
    private static String TAG = "GPS_TAG";
    // Delay mechanism

    public interface DelayCallback{
        void afterDelay();
    }

    public static void delay(int secs, final DelayCallback delayCallback){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                delayCallback.afterDelay();
            }
        }, secs * 1000); // afterDelay will be executed after (secs*1000) milliseconds.
    }
    public static boolean isMyServiceRunning(Class<?> serviceClass, Activity activity){
        ActivityManager manager = (ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(serviceClass.getName().equals(service.service.getClassName())){
                return true;
            }
        }
        return false;
    }
}
