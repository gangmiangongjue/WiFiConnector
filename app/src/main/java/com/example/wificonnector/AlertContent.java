package com.example.wificonnector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AlertContent {

    private TextView title,msg;
    private Button positive,negative;
    public View root;
    public AlertContent(Context context){
        root = LayoutInflater.from(context).inflate(R.layout.dialog_attention_wifi,null);
        title = root.findViewById(R.id.alert_title);
        msg = root.findViewById(R.id.alert_msg);
        positive = root.findViewById(R.id.attention_positive);
        negative = root.findViewById(R.id.attention_negative);
    }
    public AlertContent setTitle(String tt){
        title.setText(tt);
        return this;
    }
    public AlertContent setMsg(String m){
        msg.setText(m);
        return this;
    }
    public CharSequence getMsg(){
        return msg.getText();
    }
    public AlertContent setNegativeOnClick(View.OnClickListener listener){
        negative.setOnClickListener(listener);
        return this;
    }
    public AlertContent setPositiveOnClick(View.OnClickListener listener){
        positive.setOnClickListener(listener);
        return this;
    }
}
