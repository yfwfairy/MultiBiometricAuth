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
import com.njupt.multibiometricauth.MyDialog;
import com.njupt.multibiometricauth.R;
import com.njupt.multibiometricauth.face.FaceConfigActivity;
import com.njupt.multibiometricauth.finger.FingerConfigActivity;
import com.njupt.multibiometricauth.voice.VoiceConfigActivity;

public class BiometricSelectionActivity extends AppCompatActivity {
    TextView titleTextView;
    Button backBut;
    ConstraintLayout voiceLayout,fingerLayout,faceLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biometric_selection);
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
        fingerLayout = findViewById(R.id.constraintLayout);
        voiceLayout= findViewById(R.id.constraintLayout2);
        faceLayout= findViewById(R.id.constraintLayout3);

        titleTextView.setTranslationY(-1000);
        voiceLayout.setTranslationX(1000);
        faceLayout.setTranslationX(1500);
        fingerLayout.setTranslationX(2000);

        titleTextView.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(10).start();
        voiceLayout.animate().translationX(0).alpha(1).setDuration(1000).setStartDelay(10).start();
        faceLayout.animate().translationX(0).alpha(1).setDuration(1000).setStartDelay(10).start();
        fingerLayout.animate().translationX(0).alpha(1).setDuration(1000).setStartDelay(10).start();

    }

    public void voiceRecClicked(View view) {
        Intent intent = new Intent(this, VoiceConfigActivity.class);
        startActivity(intent);
    }

    public void faceRegClicked(View view){
        MyDialog myDialog = new MyDialog(this, MyDialog.PICK_AVATAR);
        myDialog.show();
    }

    public void fingerRegClicked(View view){
        Intent intent = new Intent(this, FingerConfigActivity.class);
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
