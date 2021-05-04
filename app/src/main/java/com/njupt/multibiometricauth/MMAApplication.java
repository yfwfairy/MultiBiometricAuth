package com.njupt.multibiometricauth;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

import java.util.HashMap;
import java.util.Map;

public class MMAApplication extends Application {

    private Map<String,String> propMap;

    @Override
    public void onCreate() {
        super.onCreate();
        propMap = new HashMap<>();
        initXf();
    }

    private void initXf() {
        SpeechUtility.createUtility(this, "appid=" + getString(R.string.xf_app_id));
    }

    public synchronized String getProp(String key) {
        return propMap.get(key);
    }

    public synchronized void setProp(String key, String value) {
        propMap.put(key, value);
    }


}


