package com.njupt.multibiometricauth.main;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.njupt.multibiometricauth.Constants;
import com.njupt.multibiometricauth.MMAApplication;
import com.njupt.multibiometricauth.R;

public class MainActivity extends AppCompatActivity {

    TextView logoTextView;
    ConstraintLayout multiLayout,scenarioLayout;
    LottieAnimationView lottiLogo;
    ImageButton reminderImageButton,moreImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        requestPermissions();
        Intent intent = getIntent();
        if (intent != null) {
            saveLoginInfo(intent);
        }
    }

    private void initUI() {
        reminderImageButton = findViewById(R.id.imageButton_reminder);
        logoTextView =findViewById(R.id.textView_logo);
        multiLayout = findViewById(R.id.multiLayout) ;
        scenarioLayout = findViewById(R.id.scenarioLayout);
        lottiLogo = findViewById(R.id.lottie_cartoon);

        logoTextView.setTranslationY(-1000);
        multiLayout.setTranslationX(1000);
        scenarioLayout.setTranslationX(1500);
        lottiLogo.setTranslationY(-1500);

        multiLayout.animate().translationX(0).alpha(1).setDuration(1000).setStartDelay(10).start();
        scenarioLayout.animate().translationX(0).alpha(1).setDuration(1000).setStartDelay(10).start();
        logoTextView.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(10).start();
        lottiLogo.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(10).start();


    }

    public void selectMultiClicked(View view) {
        Intent intent = new Intent(this, BiometricSelectionActivity.class);
        startActivity(intent);
    }
    public void selectScenarioClicked(View view) {
        Intent intent = new Intent(this, ScenarioSelectActivity.class);
        startActivity(intent);
    }

    public void reminderClicked(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("本系统可支持多模生物特征的录入和多场景下身份验证演练");
        builder.setPositiveButton("我知道了", null);
        builder.create() .show();
    }

    private void requestPermissions(){
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                int permission = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permission!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[]
                            {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.LOCATION_HARDWARE,Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.WRITE_SETTINGS,Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS},0x0010);
                }

                if(permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},0x0010);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
