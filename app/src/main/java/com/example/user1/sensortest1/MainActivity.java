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
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); //TYPE_LINEAR_ACCELERATIONは重力の影響を除いた加速度を得る
        Sensor magne = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);  //ドリフトを考慮したジャイロセンサー値を得る
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magne, SensorManager.SENSOR_DELAY_FASTEST);
    }

    // 解除するコード
    @Override
    protected void onPause() {
        super.onPause();
        // Listenerを解除
        sensorManager.unregisterListener(this);
    }

    final private float k = 0.1f;   //値が大きいほどローパスフィルタの効きが強くなる
    private float lowPassX = 0;
    private float lowPassY = 0;
    private float lowPassZ = 0;

    private float rawAx = 0, rawAy=0, rawAz = 0;    //ハイパスフィルタを通した値

    long oldTime = 0;   //前回、センサの値が変更されたとき

    float vx = 0, vy = 0, vz = 0;   //現在こいつらは端末から見た座標(ローカル座標)なことに注意
    float x=0, y=0, z=0;


    long oldTime_rotate = 0;    //前回更新時刻
    final private float k_rotate = 0.1f;    //回転センサーの値へのローパスフィルターの効きの強さ
    //private float magX=0, magY=0, magZ=0;   //回転センサーの値
    private float vx_rotate =0, vy_rotate =0, vz_rotate =0;    //回転センサーの変化量
    private float lowPassX_rotate =0, lowPassY_rotate =0, lowPassZ_rotate =0;

    //このメソッドは、リスナーとして登録してあるどのセンサーの値が変化しても呼ばれる
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
            textView.setText(strTmp);

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

//            vx_rotate = (lowPassX_rotate - tempX)/interval;
//            vy_rotate = (lowPassY_rotate - tempY)/interval;
//            vz_rotate = (lowPassZ_rotate - tempZ)/interval;

            // High Pass Filter
            float magRawAX = event.values[0] - lowPassX_rotate;
            float magRawAY = event.values[1] - lowPassY_rotate;
            float magRawAZ = event.values[2] - lowPassZ_rotate;

            //向きの変化速度
            vx_rotate += magRawAX * interval / 10;
            vy_rotate += magRawAY * interval / 10;
            vz_rotate += magRawAZ * interval / 10;
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
