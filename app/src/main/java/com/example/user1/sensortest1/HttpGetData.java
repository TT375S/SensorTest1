package com.example.user1.sensortest1;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by user1 on 2017/06/27.
 * データベースサーバにクエリを送信して、結果をJSONで受け取ります
 * 受信が終わったら呼び出し元のコールバックメソッドを呼び出して結果を渡します
 */

public class HttpGetData extends AsyncTask<Double, Void, JSONArray> {

    public interface HttpGetDataListner {
        //データの取得が終わったら呼ぶコールバックメソッド
        void finishedFetchingHttpGetData(JSONArray jsonObject);
    }

    private HttpGetDataListner caller;
    public HttpGetData(HttpGetDataListner caller) {
        super();
        this.caller = caller;
    }

    // doInBackgroundの事前準備処理（UIスレッド）
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    // 別スレッド処理
    @Override
    protected JSONArray doInBackground(Double... param) {
        JSONArray jsonObject = new JSONArray();

        double latitudeNW = param[0];   //右上の角の座標
        double longtitudeNW = param[1];
        double latitudeSE = param[2];   //左下の角の座標
        double longtitudeSE = param[3];

        try {
            StringBuilder urlStrBuilder = new StringBuilder("http://taiyakon.xyz:3000/dots/api/search/rectangle/json");
            urlStrBuilder.append("?langtitudeNW=" + latitudeNW + "&longtitudeNW=" + longtitudeNW);  //左上と右上の座標を指定してその矩形の囲む領域から検索
            urlStrBuilder.append("&langtitudeSE=" + latitudeSE + "&longtitudeSE=" + longtitudeSE);
            URL u = new URL(urlStrBuilder.toString());

            Log.i("HttpGetData", u.toString());

            // HTTP request
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            BufferedInputStream is = new BufferedInputStream(con.getInputStream());


            int bytesRead = -1;
            byte[] buffer = new byte[1024];
            String jsonResult="";
            while ((bytesRead = is.read(buffer)) != -1) {
                String buf = new String(buffer, 0, bytesRead);
                jsonResult += buf;
            }

            is.close();

            jsonObject = new JSONArray(jsonResult);
            //datas = jsonObject.getJSONArray("results");

        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            Log.i("HttpGetData", jsonObject.toString(4));
        }catch (JSONException e){
            e.printStackTrace();
        }
        Log.i("HttpGetData", jsonObject.toString());
        return jsonObject;
    }


    // doInBackgroundの事後処理(UIスレッド)
    protected void onPostExecute(JSONArray result) {
        //呼び出し元のコールバックメソッドを呼び出す
        this.caller.finishedFetchingHttpGetData(result);
    }

    // 進捗状況をUIに反映するための処理(UIスレッド)
    @Override
    protected void onProgressUpdate(Void... values) {
        // progressDialogなどで進捗表示したりする
    }

    // 非同期処理がキャンセルされた場合の処理
    @Override
    protected void onCancelled(JSONArray s) {
        super.onCancelled(s);
    }
}
