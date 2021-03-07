package com.njupt.multibiometricauth.loginsign;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.njupt.multibiometricauth.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager viewPager;
    TextView mTextView;
    FloatingActionButton email, qq, wechat;
    float v = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initUI();
    }

    private void initUI() {
        mTextView = findViewById(R.id.text_threeParty);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.viewPager);
        email = findViewById(R.id.mail);
        qq = findViewById(R.id.qq);
        wechat = findViewById(R.id.wechat);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        SignupTabFragment signupTabFragment = new SignupTabFragment();
        final LoginTabFragment loginTabFragment = new LoginTabFragment();
        signupTabFragment.setSignCallback(new SignCallback() {
            @Override
            public void signSuccess(final String phoneNumber, final String passward) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewPager.setCurrentItem(0, true);
                        loginTabFragment.fillInEdt(phoneNumber, passward);
                    }
                });
            }
        });

        LoginAddAdapter.FragmentInfo loginFragmentInfo = new LoginAddAdapter.FragmentInfo(loginTabFragment, "sign in");
        LoginAddAdapter.FragmentInfo signFragmentInfo = new LoginAddAdapter.FragmentInfo(signupTabFragment, "sign up");
        List<LoginAddAdapter.FragmentInfo> fragmentInfos = new ArrayList<>();
        fragmentInfos.add(loginFragmentInfo);
        fragmentInfos.add(signFragmentInfo);
        final LoginAddAdapter addAdapter = new LoginAddAdapter(getSupportFragmentManager(), this, fragmentInfos);
        viewPager.setAdapter(addAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        email.setTranslationY(300);
        mTextView.setTranslationY(300);
        qq.setTranslationY(300);
        wechat.setTranslationY(300);
        tabLayout.setTranslationY(300);

        email.setAlpha(v);
        mTextView.setAlpha(v);
        qq.setAlpha(v);
        wechat.setAlpha(v);
        tabLayout.setAlpha(v);

        email.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(400).start();
        mTextView.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(400).start();
        qq.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(400).start();
        wechat.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(400).start();
        tabLayout.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(400).start();

    }

}
