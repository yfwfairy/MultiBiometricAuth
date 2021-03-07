package com.njupt.multibiometricauth.guide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.njupt.multibiometricauth.R;
import com.njupt.multibiometricauth.loginsign.LoginActivity;

public class IntroductoryActivity extends AppCompatActivity {
    ImageView splashImage;
    LottieAnimationView lottiLogo, lottiCartoon;
    boolean isFirstIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_introductory);

        splashImage = findViewById(R.id.intro_img);
        lottiLogo = findViewById(R.id.lottie_logo);
        lottiCartoon = findViewById(R.id.lottie_cartoon);

        splashImage.animate().translationY(-3000).setDuration(900).setStartDelay(3000);
        lottiLogo.animate().translationY(-1800).setDuration(900).setStartDelay(3000);
        lottiCartoon.animate().translationY(1600).setDuration(900).setStartDelay(3000);
        final SharedPreferences sharedPreferences = getSharedPreferences("is_first_in_data", MODE_PRIVATE);
        isFirstIn = sharedPreferences.getBoolean("isFirstIn", true);
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                if (isFirstIn) {
//          Toast.makeText(TransitionActivity.this, "First log", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(IntroductoryActivity.this, GuideActivity.class);
                    IntroductoryActivity.this.startActivity(intent);
                    IntroductoryActivity.this.finish();
                } else {
                    Intent intent = new Intent(IntroductoryActivity.this, LoginActivity.class);
                    IntroductoryActivity.this.startActivity(intent);
                    IntroductoryActivity.this.finish();
                }
            }
        }, 4000);
    }


}
