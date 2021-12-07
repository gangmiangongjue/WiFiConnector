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

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Url;

public class WifiConnector {
    private final static String TAG = "WifiConnector";

    private WifiReceiver mWifiReceiver;
    private Context mContext;
    private WifiManager mWifiManager;
    private ArrayList<WifiNetworkSuggestion> suggestions = new ArrayList<>();

    WifiConnector(Context context) {
        mContext = context;
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

    }

    //检测是否开启wifi，未开启情况下开启，以在receiver中接收到消息
    boolean tryEnableWifi() {
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager != null && !mWifiManager.isWifiEnabled()) {
            //WiFi未打开，开启wifi
            Log.d(TAG, "wifi not enable try open:");
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                Toast.makeText(mContext, "请先打开wifi开关,再启动本应用", Toast.LENGTH_SHORT).show();
//            } else {
                mWifiManager.setWifiEnabled(true);
//            }

            return true;
        }
        return false;
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

    public WifiConfiguration createWifiConfig(String ssid, String password, WifiCipherType type) {

        WifiConfiguration config = new WifiConfiguration();

//        config.allowedAuthAlgorithms.clear();
//        config.allowedGroupCiphers.clear();
//        config.allowedKeyManagement.clear();
//        config.allowedPairwiseCiphers.clear();
//        config.allowedProtocols.clear();

        config.SSID = String.format("\"%s\"", ssid);

        if (type == WifiCipherType.WIFICIPHER_NOPASS) {
            Log.d(TAG, "createWifiConfig: config no password ");
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        if (type == WifiCipherType.WIFICIPHER_WEP) {
            Log.d(TAG, "createWifiConfig: config wep ");
            config.wepKeys[0] = String.format("\"%s\"", password);
//            config.hiddenSSID = true;
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }

        if (type == WifiCipherType.WIFICIPHER_WPA) {
            Log.d(TAG, "createWifiConfig: config wpa ");
            config.preSharedKey = String.format("\"%s\"", password);
//            config.hiddenSSID = true;
//            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
//            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//            config.status = WifiConfiguration.Status.ENABLED;
        }

        return config;

    }

    /**
     * 根据已有配置信息接入某个wifi热点
     */
    public boolean addNetWorkAndConnect(WifiConfiguration config) {
        Log.d(TAG, "addNetWorkAndConnect config.networkId: " + config.networkId);
        WifiInfo wifiinfo = mWifiManager.getConnectionInfo();

        if (wifiinfo.getSSID() != null) {
            Log.d(TAG, "getConnectionInfo WifiInfo: " + wifiinfo.getSSID());
            mWifiManager.disableNetwork(wifiinfo.getNetworkId());
        }
        boolean result;

        if (config.networkId > 0) {

            result = mWifiManager.enableNetwork(config.networkId, true);
            mWifiManager.updateNetwork(config);
        } else {

            int networkId = mWifiManager.addNetwork(config);
            result = false;
            Log.d(TAG, "addNetWork result networkId: " + networkId);
            if (networkId != -1) {

                mWifiManager.saveConfiguration();
                result = mWifiManager.enableNetwork(networkId, true);
            }
        }
        Log.d(TAG, "addNetWorkAndConnect result:" + result);
        return result;

    }

    /**
     * 判断wifi热点支持的加密方式
     */
    public WifiCipherType getWifiCipher(String capabilities) {
        if (capabilities.isEmpty()) {
            return WifiCipherType.WIFICIPHER_INVALID;
        } else if (capabilities.contains("WEP")) {
            return WifiCipherType.WIFICIPHER_WEP;
        } else if (capabilities.contains("WPA") || capabilities.contains("WPA2")) {
            return WifiCipherType.WIFICIPHER_WPA;
        } else {
            return WifiCipherType.WIFICIPHER_NOPASS;
        }
    }


    private class WifiReceiver extends BroadcastReceiver {
        boolean isFrist = true;

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                    Log.d(TAG, "onReceive action: " + intent.getAction());
                    //WiFi状态变化广播：如果WiFi已经完成开启，即可进行WiFi扫描
                    int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                    switch (wifiState) {
                        case WifiManager.WIFI_STATE_ENABLED:
                            Log.d(TAG, "**********wifi开启成功，开始扫描周边网络***********");
                            // 这里可以自定义一个扫描等待对话框，待完成扫描结果后关闭
                            mWifiManager.startScan();
                            break;
                        case WifiManager.WIFI_STATE_DISABLED:
                            mWifiManager.startScan();
                            Toast.makeText(mContext, "wifi开关未打开", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    Log.d(TAG, "onReceive action: " + intent.getAction());
                    break;
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                    if (!isFrist) {
                        return;
                    } else {
                        isFrist = false;
                    }
                    Log.d(TAG, "onReceive action: " + intent.getAction());
                    //扫描结果广播，查找扫描列表是否存在指定SSID的WiFi，如果存在则进行连接
                    List<ScanResult> scanResultList = mWifiManager.getScanResults();//需要手工申请一下fine_location权限
                    Log.d(TAG, "onReceive action RESULTS size: " + scanResultList.size());
//                    ScanResult target = getTargetResult(scanResultList,"Wifi-Clock");
                    ScanResult target = getTargetResult(scanResultList, "jiigan-guest");

                    if (target != null) {
                        if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            boolean direct = true;
                            if (!direct) {
                                /*以下的内容是可以做出推荐，让系统推荐给用户*/
                                Log.d(TAG, "onReceive connect with WifiNetworkSuggestion: ");
                                WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                                        .setSsid("jiigan-guest")
                                        .setWpa2Passphrase("jiiganguest")
                                        .build();

                                suggestions.clear();
                                suggestions.add(suggestion);

                                int status = mWifiManager.addNetworkSuggestions(suggestions);

                                if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                                    //We have successfully added our wifi for the system to consider
                                    Log.d(TAG, "onReceive We have successfully added our wifi for the system to consider: ");
                                } else {
                                    Log.d(TAG, "onReceive addNetworkSuggestions failed: " + status);
                                }
                            } else {
                                /*以下的内容是可以做出直接选择，但只可以本应用使用*/
                                Log.d(TAG, "onReceive connect with wifiNetworkSpecifier: ");
                                final NetworkSpecifier wifiNetworkSpecifier =
                                        new WifiNetworkSpecifier.Builder()
                                                .setSsid("jiigan-guest")
                                                .setWpa2Passphrase("jiiganguest")
                                                .build();

                                NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
                                networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                                networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
                                networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED);
                                networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);
                                NetworkRequest networkRequest = networkRequestBuilder.build();
                                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                                cm.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                                    @Override
                                    public void onAvailable(Network network) {
                                        //Use this network object to Send request.
                                        //eg - Using OkHttp library to create a service request
                                        cm.bindProcessToNetwork(network);
                                        Log.d(TAG, "ConnectivityManager requestNetwork onAvailable: ");
                                        //最简单的Retrofit对象

                                        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://www.baidu.com/").build();
                                        //初始化IApi
                                        IApi api = retrofit.create(IApi.class);
                                        Call<ResponseBody> call = api.get("https://www.baidu.com/");
                                        call.enqueue(new Callback<>() {

                                            @Override
                                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                                Log.e(TAG, "success：" + response.body().source());
                                            }
                                            @Override
                                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                Log.e(TAG, "failed：" + t.toString());
                                            }

                                        });


                                        super.onAvailable(network);
                                    }

                                    @Override
                                    public void onUnavailable() {
                                        Log.d(TAG, "ConnectivityManager requestNetwork onUnavailable: ");
                                        super.onUnavailable();
                                    }

                                    @Override
                                    public void onLost(Network network) {
                                        Log.d(TAG, "ConnectivityManager requestNetwork onLost: ");
                                        super.onLost(network);
                                    }

                                    @Override
                                    public void onLosing(Network network, int maxMsToLive) {
                                        Log.d(TAG, "ConnectivityManager requestNetwork onLosing: ");
                                        super.onLosing(network, maxMsToLive);
                                    }
                                });
                            }

                        } else {
                            Log.d(TAG, "onReceive connect with wifiConfiguration: ");
                            WifiConfiguration config = createWifiConfig(target.SSID, "jiiganguest", getWifiCipher(target.capabilities));
                            addNetWorkAndConnect(config);
                        }

                    } else {
                        Log.d(TAG, "onReceive no target wifi");
                    }

                    break;
                default:
                    break;
            }


        }

        private ScanResult getTargetResult(List<ScanResult> results, String startTag) {
            ScanResult result = null;
            for (ScanResult scanResult : results) {
                if (scanResult.SSID.equals(startTag)) {
                    Log.d(TAG, "getTargetResult: " + scanResult.SSID + " target.capabilities：" + scanResult.capabilities);
                    result = scanResult;
                    break;
                }
            }
            return result;
        }
    }


    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }


    public interface IApi {

        @GET()
        Call<ResponseBody> get(@Url String url);
    }

}
