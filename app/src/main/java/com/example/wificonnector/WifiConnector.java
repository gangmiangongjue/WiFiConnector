package com.example.wificonnector;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.PatternMatcher;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class WifiConnector {
    private final static String TAG = "WifiConnector_sxf";

    private WifiReceiver mWifiReceiver;
    private Context mContext;
    private WifiManager mWifiManager;
    private ArrayList<WifiNetworkSuggestion> suggestions = new ArrayList<>();

    private WifiScanListener wifiScanListener;

    private boolean isMscan = false;//标识是否由自身发出的scan请求

    WifiConnector(Context context) {
        mContext = context;
    }
    public interface WifiScanListener{
        void onScanFinish(List<ScanResult> scanResultList);
    }
    //注册系统监听
    void register() {
        //注册WiFi扫描结果、WiFi状态变化的广播接收
        mWifiReceiver = new WifiReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION); //监听wifi扫描结果
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION); //监听wifi是开关变化的状态
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION); // 监听wifi是否连接成功的广播
        mContext.registerReceiver(mWifiReceiver, intentFilter);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    }

    /***
     * 扫描wifi信号，receiver会收到
     */
    void scan(){
        isMscan = true;
        mWifiManager.startScan();
    }

    boolean isWifiEnabled(){
        return mWifiManager.isWifiEnabled();
    }

    //反注册系统监听
    void unregister() {
        mContext.unregisterReceiver(mWifiReceiver);
        if (!suggestions.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "unregister remove suggestion status: " + mWifiManager.removeNetworkSuggestions(suggestions));
            }
        }
    }


    public void setWifiScanListener(WifiScanListener wifiScanListener){
        this.wifiScanListener = wifiScanListener;
    }

    public void sortResult(List<ScanResult> scanResultList){
        for (int i=0; i<scanResultList.size();++i){
            for (int j=i+1; j<scanResultList.size();++j){
                int levelI = WifiManager.calculateSignalLevel(scanResultList.get(i).level,100);
                int levelJ = WifiManager.calculateSignalLevel(scanResultList.get(j).level,100);
                if (levelJ>levelI){
                    ScanResult tmp = scanResultList.get(i);
                    scanResultList.set(i,scanResultList.get(j));
                    scanResultList.set(j,tmp);
                }
            }
        }
    }
    private class WifiReceiver extends BroadcastReceiver {

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {

            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "onReceive action: " + intent.getAction());
                //扫描结果广播，查找扫描列表是否存在指定SSID的WiFi，如果存在则进行连接
                List<ScanResult> scanResultList = mWifiManager.getScanResults();//需要手工申请一下fine_location权限
                sortResult(scanResultList);
                Log.d(TAG, "onReceive action RESULTS size: " + scanResultList.size());
                if (wifiScanListener != null) {
                    if (isMscan)
                    wifiScanListener.onScanFinish(scanResultList);
                }
                isMscan = false;
            }
        }

    }

    public String getTargetWifi(String tag,List<ScanResult> scanResultList){
        for (ScanResult item :scanResultList){
            if (item.SSID.startsWith(tag))
                return item.SSID;
        }
        return null;
    }



}
