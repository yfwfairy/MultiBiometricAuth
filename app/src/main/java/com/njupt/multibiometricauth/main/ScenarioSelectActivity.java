package com.njupt.multibiometricauth.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.njupt.multibiometricauth.Constants;
import com.njupt.multibiometricauth.MMAApplication;
import com.njupt.multibiometricauth.R;

public class ScenarioSelectActivity extends AppCompatActivity {
    TextView titleTextView;
    Button backBut;
    ConstraintLayout gestureLayout,multiLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenario_select);
        initUI();
        Intent intent = getIntent();
        if (intent != null) {
            saveLoginInfo(intent);
        }
    }

    private void initUI() {
        backBut = findViewById(R.id.back_button);
        backBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        titleTextView = findViewById(R.id.textView_logo);
        gestureLayout = findViewById(R.id.constraintLayout2);
        multiLayout= findViewById(R.id.constraintLayout1);


        titleTextView.setTranslationY(-1000);
        multiLayout.setTranslationX(1000);
        gestureLayout.setTranslationX(1500);

        titleTextView.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(10).start();
        multiLayout.animate().translationX(0).alpha(1).setDuration(1000).setStartDelay(10).start();
        gestureLayout.animate().translationX(0).alpha(1).setDuration(1000).setStartDelay(10).start();
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
