package com.example.wificonnector;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends Activity {
    private final static String TAG = "WifiConnector_sxf";
    private WifiConnector mWifiConnector;
    private String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET
    };
    private final static int REQUEST_PERMISSION_CODE = 101;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayList<String> targetPermissions = new ArrayList<>();
        for (String permission : permissions){
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED){
                targetPermissions.add(permission);
            }
        }
        if (!targetPermissions.isEmpty()){
            Log.d(TAG, "onCreate permission not enough");
            requestPermissions(targetPermissions.toArray(new String[0]),REQUEST_PERMISSION_CODE);
        }else {
            connect();
        }

    }
    private void connect(){
        mWifiConnector = new WifiConnector(this);
        mWifiConnector.register();
        mWifiConnector.tryEnableWifi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWifiConnector.unregister();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE){
            for (int i = 0; i < permissions.length;++i){
                Log.d(TAG, "onRequestPermissionsResult permissions: " + permissions[i] +" result:"+ grantResults[i]);
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "未得到授权：" + permissions[i]+",无法正常使用", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            connect();
        }
    }
}