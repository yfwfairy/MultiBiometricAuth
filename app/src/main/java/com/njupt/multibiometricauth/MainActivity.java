package com.njupt.multibiometricauth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.njupt.multibiometricauth.voice.VoiceConfigActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        if (intent != null) {
            saveLoginInfo(intent);
        }
    }


    public void voiceRecClicked(View view) {
        Intent intent = new Intent(this, VoiceConfigActivity.class);
        startActivity(intent);
    }

    public void saveLoginInfo(Intent intent) {
        String phoneNumber = intent.getStringExtra(Constants.PHONE);
        String userName = intent.getStringExtra(Constants.USERNAME);
        if (!TextUtils.isEmpty(phoneNumber)) {
            ((MMAApplication)getApplication()).setProp(Constants.PHONE, phoneNumber);
        }
        if (!TextUtils.isEmpty(userName)) {
            ((MMAApplication)getApplication()).setProp(Constants.USERNAME, userName);
        }
    }
}
