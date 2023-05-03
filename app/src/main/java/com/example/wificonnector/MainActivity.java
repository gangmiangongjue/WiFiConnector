package com.example.wificonnector;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements WifiConnector.WifiScanListener {
    private final static String TAG = "WifiConnector_sxf";
    private WifiConnector mWifiConnector;
    private String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET
    };
    private final static int REQUEST_PERMISSION_CODE = 101;
    private RecyclerView wifiListView;
    private AlertDialog connectClockDialog,attentionDialog,commonDialog;
    private EditText wifiConnectName,wifiConnectPassword;
    private ProgressDialog progressDialog;

    private boolean connectClock =false;

    private AlertContent commonContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiListView = findViewById(R.id.wifi_list);
        wifiListView.setAdapter(new WifiAdapter());
        wifiListView.setLayoutManager(new LinearLayoutManager(this));


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("扫描中...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        AlertContent alertContent = new AlertContent(this);
        attentionDialog = new AlertDialog.Builder(this).setView(alertContent.setTitle("注意").root).create();

        commonContent = new AlertContent(this);
        View.OnClickListener onClickListener = v -> commonDialog.dismiss();
        commonContent.setPositiveOnClick(onClickListener).setNegativeOnClick(onClickListener);
        commonDialog = new AlertDialog.Builder(this).setView(commonContent.root).create();

        View connectRoot = LayoutInflater.from(this).inflate(R.layout.dialog_connect_wifi,null);
        wifiConnectName = connectRoot.findViewById(R.id.wifi_connect_name);
        wifiConnectPassword = connectRoot.findViewById(R.id.wifi_connect_password);
        Button connectPositive = connectRoot.findViewById(R.id.positive);
        Button connectNegative = connectRoot.findViewById(R.id.negative);
        connectNegative.setOnClickListener(v -> connectClockDialog.dismiss());
        connectPositive.setOnClickListener(v -> {
            connectClockDialog.dismiss();
            alertContent.setMsg(alertContent.getMsg()+wifiConnectPassword.getText().toString());
            attentionDialog.show();

        });
        alertContent.setNegativeOnClick(v -> attentionDialog.dismiss());
        alertContent.setPositiveOnClick(v -> {
            if (!mWifiConnector.isWifiEnabled()){
                Toast.makeText(this, "请打开wifi开关后重试", Toast.LENGTH_SHORT).show();
                return;
            }
            curTime = System.currentTimeMillis();
            if (curTime - lastTime > 4000) {
                lastTime = curTime;
                searchClockWifi();
            }else {
                Toast.makeText(this, "操作太频繁，请稍后", Toast.LENGTH_SHORT).show();
            }
        });
        connectClockDialog = new AlertDialog.Builder(this).setView(connectRoot).create();

    }

    private void scanWifi() {
        mWifiConnector = new WifiConnector(this);
        mWifiConnector.setWifiScanListener(this);
        mWifiConnector.register();
        mWifiConnector.scan();
        progressDialog.show();
    }
    private void searchClockWifi(){
        connectClock = true;
        mWifiConnector.scan();
        progressDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWifiConnector.unregister();
    }

    boolean isFirst = true;
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (isFirst){
            ArrayList<String> targetPermissions = new ArrayList<>();
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    targetPermissions.add(permission);
                }
            }
            if (!targetPermissions.isEmpty()) {
                Log.d(TAG, "onCreate permission not enough");
                requestPermissions(targetPermissions.toArray(new String[0]), REQUEST_PERMISSION_CODE);
            } else {
                scanWifi();
            }
            isFirst = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; ++i) {
                Log.d(TAG, "onRequestPermissionsResult permissions: " + permissions[i] + " result:" + grantResults[i]);
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "未得到授权：" + permissions[i] + ",无法正常使用", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            scanWifi();
        }
    }

    boolean isFirstLoad = true;
    @Override
    public void onScanFinish(List<ScanResult> scanResultList) {
        progressDialog.dismiss();
        if (isFirstLoad){
            commonContent.setTitle("注意").setMsg("请选择一个家用wifi输入密码以配置给钟表，注意，钟表不支持5G wifi信号");
            commonDialog.show();
            isFirstLoad = false;
        }
        if (connectClock){
            String clockName = mWifiConnector.getTargetWifi("WiFiClock",scanResultList);
            if (clockName != null){
                String name = wifiConnectName.getText().toString();
                String password = wifiConnectPassword.getText().toString();
                Bundle bundle = new Bundle();
                bundle.putString("name",name);
                bundle.putString("password",password);
                bundle.putString("clockName",clockName);
                Intent intent = new Intent(MainActivity.this,ConnectWifiActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                attentionDialog.dismiss();
            }else {
                Toast.makeText(this, "不存在以WiFiClock开头的wifi，请检查钟表设置！", Toast.LENGTH_LONG).show();
            }

        }else {
            if (scanResultList !=null){
                Log.d(TAG, "onScanFinish: refresh");
                WifiAdapter wifiAdapter = (WifiAdapter) wifiListView.getAdapter();
                wifiAdapter.setDataSet(scanResultList);
                wifiAdapter.notifyDataSetChanged();
            }else {
                Toast.makeText(this, "未发现Wifi信号", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private long lastTime=0 ,curTime=0;
    public void Scan(View view) {
        curTime = System.currentTimeMillis();
        if (curTime - lastTime > 4000){
            connectClock = false;
            mWifiConnector.scan();
            progressDialog.show();
            lastTime = curTime;
        }else {
            Toast.makeText(this, "操作太频繁，请稍后", Toast.LENGTH_SHORT).show();
        }
    }


    class WifiAdapter extends RecyclerView.Adapter<WiFiViewHolder> {
        private List<ScanResult> scanResultList;
        @NonNull
        public WiFiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_list, parent,false);
            return new WiFiViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WiFiViewHolder holder, int position) {
            ScanResult scanResult = scanResultList.get(position);
            holder.name.setText(scanResult.SSID.isEmpty()?"匿名":scanResult.SSID);

            int signalStrength = WifiManager.calculateSignalLevel(scanResult.level,5);
            switch (signalStrength){
                case 0:
                case 1:
                    holder.level.setImageResource(R.drawable.baseline_wifi_1_bar_24);
                    break;
                case 2:
                case 3:
                    holder.level.setImageResource(R.drawable.baseline_wifi_2_bar_24);
                    break;
                case 4:
                case 5:
                    holder.level.setImageResource(R.drawable.baseline_wifi_24);
                    break;
                default:
                    holder.level.setImageResource(R.drawable.baseline_wifi_1_bar_24);
                    break;
            }
            holder.operate.setText("设置");
            holder.item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    wifiConnectPassword.setText("");
                    wifiConnectName.setText(scanResult.SSID);
                    if (!connectClockDialog.isShowing()){
                        connectClockDialog.show();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return scanResultList ==null?0: scanResultList.size();
        }

        public void setDataSet(List<ScanResult> scanResultList){
            this.scanResultList = scanResultList;
        }
    }

    static class WiFiViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView operate;
        View item;
        ImageView level;

        public WiFiViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView;
            name = itemView.findViewById(R.id.wifi_name);
            level = itemView.findViewById(R.id.wifi_level);
            operate = itemView.findViewById(R.id.wifi_operate);
        }
    }
}