package com.example.user1.sensortest1;

import android.app.Application;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user1 on 2017/06/29.
 */

public class DataHoldSingleton extends Application {

    private static DataHoldSingleton sInstance = null;   //唯一のインスタンスを保持する

    public JSONArray report = new JSONArray();

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
    }

    //シングルトンパターンでは、インスタンスはこのクラスメソッドを介して行う
    public static DataHoldSingleton getInstance() {
        if (sInstance == null) {
            Log.e("FusedLocationClient", "Singleton instance is not generated.");
        }
        return sInstance;
    }



}
