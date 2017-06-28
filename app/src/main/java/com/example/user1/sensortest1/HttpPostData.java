package com.example.user1.sensortest1;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by user1 on 2017/06/28.
 */

public class HttpPostData {
    //deprecatedな古いクラスたちを使うにはbuild.gradleにuseLibrary 'org.apache.http.legacy'を追加せねばならない
    public static void makeRequest(String path, JSONArray params) throws Exception
    {
        //instantiates httpclient to make request
        DefaultHttpClient httpclient = new DefaultHttpClient();

        //url with the post data
        HttpPost httpost = new HttpPost(path);

        //passes the results to a string builder/entity
        StringEntity se = new StringEntity(params.toString());

        //sets the post request as the resulting string
        httpost.setEntity(se);
        //sets a request header so the page receving the request
        //will know what to do with it
        httpost.setHeader("Accept", "application/json");
        httpost.setHeader("Content-type", "application/json");

        //Handles what is returned from the page
        ResponseHandler responseHandler = new BasicResponseHandler();
        httpclient.execute(httpost, responseHandler);
        //return httpclient.execute(httpost, responseHandler);
    }
}
