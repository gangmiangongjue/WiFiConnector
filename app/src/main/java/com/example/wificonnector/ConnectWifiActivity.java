package com.example.wificonnector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ConnectWifiActivity extends Activity {
    private final static String TAG = "ConnectWifiActivity";

    private BreatheView breatheView;
    private String wifiName, wifiPassword, clockName;

    private boolean isFirst = true;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        breatheView = findViewById(R.id.breathe);
        breatheView.startLightAnim();
        Intent intent = getIntent();
        wifiName = intent.getStringExtra("name");
        wifiPassword = intent.getStringExtra("password");
        clockName = intent.getStringExtra("clockName");
        Log.d(TAG, "wifiName: "+ wifiName +" wifiPassword:"+ wifiPassword +" clockName:"+ clockName);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (isFirst){
            connectClockWifi();
            isFirst = false;
        }
    }


    private void connectClockWifi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            final NetworkSpecifier wifiNetworkSpecifier;

            wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(clockName)
                    .build();

            NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
            networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
            networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED);
            networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);
            NetworkRequest networkRequest = networkRequestBuilder.build();
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            cm.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    //Use this network object to Send request.
                    //eg - Using OkHttp library to create a service request
                    cm.bindProcessToNetwork(network);
                    breatheView.setMessage("连接钟表成功");
                    Log.d(TAG, "ConnectivityManager requestNetwork onAvailable: ");
                    OkHttpClient okHttpClient = new OkHttpClient();
                    syncWifiConfig(okHttpClient);
                }

                @Override
                public void onUnavailable() {
                    Log.d(TAG, "ConnectivityManager requestNetwork onUnavailable: ");
                    breatheView.setMessage("连接钟表失败");
                    super.onUnavailable();
                }

                @Override
                public void onLost(Network network) {
                    Log.d(TAG, "ConnectivityManager requestNetwork onLost: ");
                    breatheView.setMessage("钟表连接断开");
                    super.onLost(network);
                }

                @Override
                public void onLosing(Network network, int maxMsToLive) {
                    Log.d(TAG, "ConnectivityManager requestNetwork onLosing: ");
                    breatheView.setMessage("钟表连接断开中");
                    super.onLosing(network, maxMsToLive);
                }
            });
        }
    }

    private int retryTimeGet = 0;
    private void syncWifiConfig(OkHttpClient okHttpClient) {
        if (retryTimeGet >5){
            breatheView.setMessage("意外断开，请重试");
            return;
        }
        retryTimeGet++;

        Request requestGet1 = new Request.Builder().url("http://10.10.10.1/_ac/set/Asia/Shanghai$-480$0$zh-CN").build();
        okHttpClient.newCall(requestGet1).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                breatheView.setMessage("连接钟表成功\n访问主页异常\n重试中"+retryTimeGet);
                syncWifiConfig(okHttpClient);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "get onResponse : " + response.code()+" call body:"+call.request().body()+" header:"+call.request().headers());
                if (response.isSuccessful()) {
                    breatheView.setMessage("连接钟表成功\n访问主页成功");
                    Log.d(TAG, "doGetAsync Successful: " + response.body().string());
                    postConfig(okHttpClient);
                }else {
                    breatheView.setMessage("连接钟表成功\n访问主页失败\n重试中"+retryTimeGet);
                    syncWifiConfig(okHttpClient);
                }
            }
        });

    }
    private int retryTimePost = 0;
    private void postConfig(OkHttpClient okHttpClient){
        if (retryTimePost >5){
            breatheView.setMessage("意外断开，请重试");
            return;
        }
        retryTimePost++;
        FormBody formBody = new FormBody.Builder().add("SSID", wifiName).add("Passphrase", wifiPassword).add("apply", "设置").build();
        int length = (int) formBody.contentLength();
        Request request = new Request.Builder().url("http://10.10.10.1/_ac/connect")
                .addHeader("Host", "10.10.10.1")
                .addHeader("Proxy-Connection", "keep-alive")
                .addHeader("Content-Length",String.valueOf(length))
                .addHeader("Cache-Control", "max-age=0")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("Origin", "http://10.10.10.1")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
//                                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36 Edg/112.0.1722.64")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .addHeader("Referer", "http://10.10.10.1/_ac/set/Asia/Shanghai$-480$0$zh-CN")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,zh-TW;q=0.5")
                .post(formBody).build();
        Log.d(TAG, "onAvailable: "+" call body length:"+length);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                breatheView.setMessage("连接钟表成功\n访问主页成功\n同步设置异常\n重试中"+retryTimePost);
                postConfig(okHttpClient);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "post onResponse : " + response.code());
                if (response.isSuccessful()) {
                    if (response.body() !=null){
                        String responseString = response.body().string();
                        Log.d(TAG, "doPostAsync: " + responseString);
                        if (responseString.contains("连接中")){
                            breatheView.setMessage("连接钟表成功\n访问主页成功\n同步设置顺利");
                        }else {
                            breatheView.setMessage("连接钟表成功\n访问主页成功\n同步设置失败1");
                        }
                    }
                    getResult(okHttpClient);
                }else {
                    breatheView.setMessage("连接钟表成功\n访问主页成功\n同步设置失败\n重试中"+retryTimePost);
                    postConfig(okHttpClient);
                }
            }
        });
    }
    private int retryTime = 0;
    private void getResult(OkHttpClient okHttpClient){
        if (retryTime >5){
            breatheView.setMessage("意外断开，请重试");
            return;
        }
        retryTime++;
            Request requestGet = new Request.Builder().url("http://10.10.10.1/_ac/result")
                    .addHeader("Host", "10.10.10.1")
                    .addHeader("Proxy-Connection", "keep-alive")
                    .addHeader("Upgrade-Insecure-Requests", "1")
//                                                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36 Edg/112.0.1722.64")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .addHeader("Referer", "http://10.10.10.1/_ac/connect")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6,zh-TW;q=0.5")
                    .build();
            okHttpClient.newCall(requestGet).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.d(TAG, "get onResponse final fail : " + e.getMessage());
                    breatheView.setMessage("连接钟表成功\n访问主页成功\n同步设置顺利\n访问结果异常\n重试中:"+retryTime);//是否成功未知
                    getResult(okHttpClient);//失败就不断获取
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Log.d(TAG, "get onResponse final : " + response.code());
                    if (response.isSuccessful()) {
                        if (response.body() !=null){
                            String responseString = response.body().string();
                            Log.d(TAG, "doGetAsync final: " + responseString);
                            if (responseString.contains("连接失败")){//连接失败会返回200，内容包含失败
                                breatheView.setMessage("连接钟表成功\n访问主页成功\n同步设置失败2");//密码不匹配
                            }else{
                                Log.d(TAG, "onResponse final code: "+response.code()+" header:"+response.headers());
                                breatheView.setMessage("连接钟表成功\n访问主页成功\n同步设置成功");
                            }
                        }else {
                            Log.d(TAG, "onResponse final 11: "+response.code()+" header:"+response.headers());
                            breatheView.setMessage("连接钟表成功\n访问主页成功\n同步设置成功");
                        }

                    }else {
                        breatheView.setMessage("连接钟表成功\n访问主页成功\n同步设置顺利\n访问结果失败\n重试中:"+retryTime);//是否成功未知
                        getResult(okHttpClient);//失败就不断获取
                    }
                }
            });
    }

}
