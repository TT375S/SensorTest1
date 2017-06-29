package com.example.user1.sensortest1;

/**
 * Created by user1 on 2017/06/29.
 */

import java.util.Timer;
import java.util.TimerTask;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ServiceTimer extends Service {

    private Timer timer = null;
    private int count = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("service", "onStartCommand");

        timer = new Timer();
        timer.schedule( new TimerTask(){
            @Override
            public void run(){
                Log.d( "TestService" , "count = "+ count );
                count++;
            }
        }, 0, 1000);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("service", "onDestroy");
        super.onDestroy();
        // timer cancel
        if( timer != null ){
            timer.cancel();
            timer = null;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        // エラーになるので、とりあえず入れてありますが使いません
        return null;
    }
}