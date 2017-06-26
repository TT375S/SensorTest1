package com.example.user1.sensortest1;


import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private TextView textView, textInfo;
    private float sensorX;
    private float sensorY;
    private float sensorZ;
    private boolean flg = true;


    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        textInfo = (TextView) findViewById(R.id.text_info);

        // Get an instance of the TextView
        textView = (TextView) findViewById(R.id.text_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
//        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
    }

    // 解除するコードも入れる!
    @Override
    protected void onPause() {
        super.onPause();
        // Listenerを解除
        sensorManager.unregisterListener(this);
    }

    final private float k = 0.1f;
    private float lowPassX = 0;
    private float lowPassY = 0;
    private float lowPassZ = 0;

    long oldTime = 0;

    float vx = 0, vy = 0, vz = 0;
    float x=0, y=0, z=0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(oldTime == 0) oldTime = System.currentTimeMillis();
        // ax, ay, az が求まった後で
        long nowTime = System.currentTimeMillis();
        long interval = nowTime - oldTime;
        oldTime = nowTime;

        vx += lowPassX * interval / 10; // [cm/s] にする
        vy += lowPassY * interval / 10;
        vz += lowPassZ * interval / 10;
        x += vx * interval / 1000; // [cm] にする
        y += vy * interval / 1000;
        z += vz * interval / 1000;



        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorX = event.values[0];
            sensorY = event.values[1];
            sensorZ = event.values[2];


            lowPassX += (event.values[0] - lowPassX) * k; // event.values[0から2] はそれぞれ端末座標系での生の加速度です
            lowPassY += (event.values[1] - lowPassY) * k;
            lowPassZ += (event.values[2] - lowPassZ) * k;

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
            textView.setText(strTmp);

            if(flg){
                showInfo(event);
            }
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

        textInfo.setText(info);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
