package com.example.user1.sensortest1;

import android.*;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Created by user1 on 2017/06/29.
 */

public class SensingService extends Service implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        HttpGetData.HttpGetDataListner{

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        reportSpeed();
        checkDODGE();
        Log.d("SensingService", "STARTSERVICE!");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        sendReport();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private SensorManager sensorManager;
    private float sensorX;
    private float sensorY;
    private float sensorZ;
    private boolean flg = true;

    private float speed_rotate = 0; //ジャイロセンサの加速度から計算した回転速度(Javaの命名規約におとなしく従ってキャメルケースにした方がよかった？)
    private float speed_foward = 0; //GPSから取得した前進速度
    private float speed_right = 0;  //加速度センサから計算した横移動速度
    private float currentLatitude = 0;      //現在の位置
    private float currentLongtitude = 0;

    private LocationManager locationManager;

    // LocationClient の代わりにGoogleApiClientを使います
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private FusedLocationProviderApi fusedLocationProviderApi;

    private LocationRequest locationRequest;
    private Location location;
    private long lastLocationTime = 0;

    @Override
    public void onCreate()  {
        Log.d("SensingService", "STARTSERVICE!");
        super.onCreate();

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Listenerの登録
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); //TYPE_LINEAR_ACCELERATIONは重力の影響を除いた加速度を得る
        Sensor magne = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);  //ドリフトを考慮したジャイロセンサー値を得る
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magne, SensorManager.SENSOR_DELAY_FASTEST);

        locationStart();

        //test(); //テスト用！本番では外す
    }

    //テスト用
    void test(){
        HttpGetData aaaa = new HttpGetData(this);
        aaaa.execute(36.333, 32.0, 2323.0, 222.2);  //データベースからフェッチ

        //報告用JSONオブジェクト作成
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("typename", "DODGE");
            jsonObject.put("speed", 32.1);
            jsonObject.put("degree", 10.1);
            jsonObject.put("latitude", 164.2323232);
            jsonObject.put("longtitude", 36.7474737);
            Timestamp timestamp = new Timestamp(Calendar.getInstance().getTimeInMillis() - 1000*60*60*24);
            jsonObject.put("time", timestamp);

            JSONArray jsonArray = new JSONArray();
            jsonArray.put(jsonObject);

            jsonObject = new JSONObject();

            jsonObject.put("typename", "SUDDEN-BREAKING");
            jsonObject.put("speed", 42.1);
            jsonObject.put("degree", 39.1);
            jsonObject.put("latitude", 163.2323232);
            jsonObject.put("longtitude", 36.7874737);
            timestamp = new Timestamp(Calendar.getInstance().getTimeInMillis() - 1000*60*60*24);
            jsonObject.put("time", timestamp);
            jsonArray.put(jsonObject);

            Log.d("HttpPostData", jsonArray.toString());
            //POSTする
            HttpPostData httpPostData = new HttpPostData();
            httpPostData.execute(jsonArray);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void locationStart(){
        Log.d("debug","locationStart()");

        // LocationRequest を生成して精度、インターバルを設定
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(16);

        fusedLocationProviderApi = LocationServices.FusedLocationApi;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        startFusedLocation();
    }

    private void startFusedLocation(){
        Log.d("LocationActivity", "onStart");

        // Connect the client.
        if (!mResolvingError) {
            // Connect the client.
            mGoogleApiClient.connect();
        } else {

        }
    }

    private void stopFusedLocation(){
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    //-----センサー値取得関係-----
    final private float k = 0.1f;   //値が大きいほどローパスフィルタの効きが強くなる
    private float lowPassX = 0,lowPassY = 0,lowPassZ = 0; //ローパスフィルタ通過後の値

    private float rawAx = 0, rawAy=0, rawAz = 0;    //ハイパスフィルタを通した値

    long oldTime = 0;   //前回、センサの値が変更されたとき

    float vx = 0, vy = 0, vz = 0;   //速度だけど端末から見た座標(ローカル座標)なことに注意
    float x=0, y=0, z=0;    //位置。精度悪くてあてにならない。

    long oldTime_rotate = 0;    //前回更新時刻
    final private float k_rotate = 0.1f;    //回転センサーの値へのローパスフィルターの効きの強さ
    private float vx_rotate =0, vy_rotate =0, vz_rotate =0;    //回転センサーの変化量
    private float lowPassX_rotate =0, lowPassY_rotate =0, lowPassZ_rotate =0;

    //このメソッドは、リスナーとして登録してあるどのセンサーの値が変化しても呼ばれる
    //ので中でどのセンサーか場合分けしなくてはならない
    @Override
    public void onSensorChanged(SensorEvent event) {
        //このメソッドが呼ばれた理由となる、値の変わったセンサのタイプを確かめる必要がある

        //重力の影響を除いた加速度センサの場合
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //センサーの生の値
            sensorX = event.values[0];
            sensorY = event.values[1];
            sensorZ = event.values[2];

            //LPF
            lowPassX += (event.values[0] - lowPassX) * k;
            lowPassY += (event.values[1] - lowPassY) * k;
            lowPassZ += (event.values[2] - lowPassZ) * k;

            // High Pass Filter
            rawAx = event.values[0] - lowPassX;
            rawAy = event.values[1] - lowPassY;
            rawAz = event.values[2] - lowPassZ;

            String strTmp = "加速度センサー\n"
                    + " X: " + sensorX + "\n"
                    + " Y: " + sensorY + "\n"
                    + " Z: " + sensorZ;

            strTmp += "\nLP加速度センサー\n"
                    + " X: " + lowPassX + "\n"
                    + " Y: " + lowPassY + "\n"
                    + " Z: " + lowPassZ;

            strTmp += "\nLP速度加速度センサー\n"
                    + " vX: " + vx + "\n"
                    + " vY: " + vy + "\n"
                    + " vZ: " + vz;
            strTmp += "\nLP位置加速度センサー\n"
                    + " X: " + x + "\n"
                    + " Y: " + y + "\n"
                    + " Z: " + z;
            strTmp += "\nHP磁気センサー\n"
                    + " X: " + lowPassX_rotate + "\n"
                    + " Y: " + lowPassY_rotate + "\n"
                    + " Z: " + lowPassZ_rotate;
            strTmp += "\nHP磁気センサー速度\n"
                    + " X: " + vx_rotate + "\n"
                    + " Y: " + vy_rotate + "\n"
                    + " Z: " + vz_rotate;

            if(flg){
                showInfo(event);
            }

            if(oldTime == 0) oldTime = System.currentTimeMillis();
            long nowTime = System.currentTimeMillis();
            long interval = nowTime - oldTime;
            oldTime = nowTime;

            vx += rawAx * interval / 10; // [cm/s] にする
            vy += rawAy * interval / 10;
            vz += rawAz * interval / 10;
            x += vx * interval / 1000; // [cm] にする
            y += vy * interval / 1000;
            z += vz * interval / 1000;

            //スピード更新
            speed_right = vx;
            //必然性がないのにここでDODGE判定
            checkDODGE();
            //スピードレポート
            reportSpeed();
        }

        //回転センサーの場合
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if(oldTime_rotate == 0) oldTime_rotate = System.currentTimeMillis();
            long nowTime = System.currentTimeMillis();
            long interval = nowTime - oldTime_rotate;
            oldTime_rotate = nowTime;

            float tempX = lowPassX_rotate, tempY = lowPassY_rotate, tempZ = lowPassZ_rotate;

            //LPF
            lowPassX_rotate += (event.values[0] - lowPassX_rotate) * k_rotate;
            lowPassY_rotate += (event.values[1] - lowPassY_rotate) * k_rotate;
            lowPassZ_rotate += (event.values[2] - lowPassZ_rotate) * k_rotate;

            // High Pass Filter
            float hiPassX_rotate = event.values[0] - lowPassX_rotate;
            float hiPassY_rotate = event.values[1] - lowPassY_rotate;
            float hiPassZ_rotate = event.values[2] - lowPassZ_rotate;

            //向きの変化速度
            vx_rotate += hiPassX_rotate * interval / 10;
            vy_rotate += hiPassY_rotate * interval / 10;
            vz_rotate += hiPassZ_rotate * interval / 10;

            //スピード更新
            speed_rotate = vx;
        }
    }

    // 加速度センサーの各種情報を表示する
    private void showInfo(SensorEvent event){
        String info = "Name: " + event.sensor.getName() + "\n";
        info += "Vendor: " + event.sensor.getVendor() + "\n";
        info += "Type: " + event.sensor.getType() + "\n";
        info += "StringType: " + event.sensor.getStringType()+ "\n";

        int data = event.sensor.getMinDelay();
        info += "Mindelay: "+String.valueOf(data) +" usec\n";

        data = event.sensor.getMaxDelay();
        info += "Maxdelay: "+String.valueOf(data) +" usec\n";

        data = event.sensor.getReportingMode();
        String stinfo = "unknown";
        if(data == 0){
            stinfo = "REPORTING_MODE_CONTINUOUS";
        }else if(data == 1){
            stinfo = "REPORTING_MODE_ON_CHANGE";
        }else if(data == 2){
            stinfo = "REPORTING_MODE_ONE_SHOT";
        }
        info += "ReportingMode: "+stinfo +" \n";

        float fData = event.sensor.getMaximumRange();
        info += "MaxRange: "+String.valueOf(fData) +" \n";

        fData = event.sensor.getResolution();
        info += "Resolution: "+String.valueOf(fData) +" m/s^2 \n";

        fData = event.sensor.getPower();
        info += "Power: "+String.valueOf(fData) +" mA\n";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {

        lastLocationTime = location.getTime() - lastLocationTime;

        //スピード更新
        speed_foward = location.getSpeed();
        //位置更新
        currentLatitude = (float) location.getLatitude();
        currentLongtitude = (float)location.getLongitude();


        //必然性がないのにここでDODGE判定
        checkDODGE();
        //スピードレポート
        reportSpeed();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("LocationClient", "ConnectionFailed!");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            Log.d("", "Already attempting to resolve an error");

            return;
        } else if (connectionResult.hasResolution()) {

        } else {
            mResolvingError = true;
        }
    }

    long lastDodgeTime = 0;
    final long dodgeInterval = 1000;    //ms
    private String logInfo = "";
    final float fowardThreshold = 10;   //km/h
    final float sideThreshold = 1;  //km/h
    final float rotateThreshold = 0.8f; //cm/s ?
    public void checkDODGE(){
        long nowTime = System.currentTimeMillis();
        long interval = nowTime - lastDodgeTime;

        if(interval < dodgeInterval) return;

        //時速10km/h以上で
        if(speed_foward * 60 * 60 / 1000 > fowardThreshold){
            //横移動速度が時速1km/h以上で
            if(Math.abs(speed_right * 60 * 60 / 1000/1000 )> sideThreshold){
                //回転速度が0.8cm/s以下、つまり回転していないとき
                if(speed_rotate < rotateThreshold){
                    lastDodgeTime = nowTime;    //最終更新時刻を更新

                    JSONObject jsonObject = new JSONObject();
                    try {
                        //結果を追加
                        jsonObject.put("typename", "DODGE");
                        jsonObject.put("speed", speed_foward  );
                        jsonObject.put("degree", speed_right );
                        jsonObject.put("latitude", currentLatitude);
                        jsonObject.put("longtitude", currentLongtitude);

                        Timestamp timestamp = new Timestamp(Calendar.getInstance().getTimeInMillis() - 1000*60*60*24);
                        jsonObject.put("time", timestamp);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    DataHoldSingleton.getInstance().report.put(jsonObject);
                    Log.d("SensingService", jsonObject.toString());
                }
            }
        }
    }

    long lastSpeedReportTime = 0;
    final long speedReportInterval = 3000;    //ms

    //定期スピードレポート
    public void reportSpeed(){
        long nowTime = System.currentTimeMillis();
        long interval = nowTime - lastSpeedReportTime;

        if(interval < speedReportInterval) return;

        lastSpeedReportTime = nowTime;    //最終更新時刻を更新

        JSONObject jsonObject = new JSONObject();
        try {
            //結果を追加
            jsonObject.put("typename", "SPEEDREPORT");
            jsonObject.put("speed", speed_foward );
            jsonObject.put("degree", speed_right );
            jsonObject.put("latitude", currentLatitude);
            jsonObject.put("longtitude", currentLongtitude);

            Timestamp timestamp = new Timestamp(Calendar.getInstance().getTimeInMillis() - 1000*60*60*24);
            jsonObject.put("time", timestamp);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        DataHoldSingleton.getInstance().report.put(jsonObject); //レポートに追加
        Log.d("SensingService", jsonObject.toString());
    }

    @Override
    public void finishedFetchingHttpGetData(JSONArray jsonObject) {
        return ;
    }

    public void sendReport(){
        Log.d("SensingService", "REPORT:"+DataHoldSingleton.getInstance().report);
        //レポート送信
        HttpPostData post = new HttpPostData();
        post.execute(DataHoldSingleton.getInstance().report);

        DataHoldSingleton.getInstance().report = new JSONArray();
    }
}