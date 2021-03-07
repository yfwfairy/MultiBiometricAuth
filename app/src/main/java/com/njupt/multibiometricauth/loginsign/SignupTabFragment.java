package com.njupt.multibiometricauth.loginsign;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.njupt.multibiometricauth.R;
import com.njupt.multibiometricauth.SQLite.User;
import com.njupt.multibiometricauth.SQLite.UserDatabaseHelper;

import java.util.List;

public class SignupTabFragment extends Fragment {


    UserDatabaseHelper db;
    SwitchCompat mSwitchCompat1, mSwitchCompat2;
    UserDatabaseHelper mUserDatabaseHelper;
    EditText EditPhone, EditUsername, EditPassword, EditConfirm;
    Button signup;
    float v = 0;
    SignCallback mSignCallback;
    View mView1, mView2, mView3;
    TextView textViewSth;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.signup_fragment, container, false);

        initView(root);

        mUserDatabaseHelper = new UserDatabaseHelper(getActivity());
        return root;
    }

    public void setSignCallback(SignCallback callback) {
        mSignCallback = callback;
    }

    private void initView(ViewGroup root) {
        textViewSth = root.findViewById(R.id.textView);
        mView1 = root.findViewById(R.id.viewColor1);
        mView2 = root.findViewById(R.id.viewColor2);
        mView3 = root.findViewById(R.id.viewColor3);
        EditUsername = root.findViewById(R.id.username);
        EditPassword = root.findViewById(R.id.pass);
        EditPhone = root.findViewById(R.id.email);
        EditConfirm = root.findViewById(R.id.confirm);
        signup = root.findViewById(R.id.button_sig);
        mSwitchCompat1 = root.findViewById(R.id.switch_passSee);
        mSwitchCompat2 = root.findViewById(R.id.switch_confirmSee);

        EditPhone.setTranslationY(800);
        EditPassword.setTranslationY(800);
        EditUsername.setTranslationY(800);
        EditConfirm.setTranslationY(800);
        signup.setTranslationY(800);
        mSwitchCompat1.setTranslationY(800);
        mSwitchCompat2.setTranslationY(800);

        EditPhone.setAlpha(v);
        EditPassword.setAlpha(v);
        EditUsername.setAlpha(v);
        EditConfirm.setAlpha(v);
        signup.setAlpha(v);
        mSwitchCompat1.setAlpha(v);
        mSwitchCompat2.setAlpha(v);

        EditPhone.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        EditUsername.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        EditPassword.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        EditConfirm.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        signup.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        mSwitchCompat1.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        mSwitchCompat2.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();

        mSwitchCompat1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    EditPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    EditPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });
        mSwitchCompat2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    EditConfirm.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    EditConfirm.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });

        EditPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length() > 0) {
                    if (editable.toString().length() == 1)
                        textViewSth.setVisibility(View.VISIBLE);
                    mView1.setVisibility(View.VISIBLE);
                    switch (checkPassWord(editable.toString())) {
                        case 1:
                            mView2.setVisibility(View.GONE);
                            mView3.setVisibility(View.GONE);
                            break;
                        case 2:
                            mView2.setVisibility(View.VISIBLE);
                            mView3.setVisibility(View.GONE);
                            break;
                        case 3:
                            mView2.setVisibility(View.VISIBLE);
                            mView3.setVisibility(View.VISIBLE);
                            break;
                    }
                } else {
                    textViewSth.setVisibility(View.GONE);
                    mView1.setVisibility(View.GONE);
                    mView2.setVisibility(View.GONE);
                    mView3.setVisibility(View.GONE);
                }

            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phoneCode = EditPhone.getText().toString().trim();
                String username = EditUsername.getText().toString().trim();
                String password = EditPassword.getText().toString().trim();
                String passwordConfirm = EditConfirm.getText().toString().trim();
                //注册验证
                if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(phoneCode) && !TextUtils.isEmpty(passwordConfirm)) {
                    List<User> data = mUserDatabaseHelper.queryUser(phoneCode);
                    if (data != null && data.size() > 0) {
                        Toast.makeText(getActivity(), "手机号已注册", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (phoneCode.length() < 11) {
                        Toast.makeText(getActivity(), "手机号不合法", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!password.equals(passwordConfirm)) {
                        Toast.makeText(getActivity(), "两次密码输入不同", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mUserDatabaseHelper.addUser(phoneCode, username, password);
                    Toast.makeText(getActivity(), "注册成功", Toast.LENGTH_SHORT).show();
                    if (mSignCallback != null) {
                        mSignCallback.signSuccess(phoneCode, password);
                    }
                } else {
                    Toast.makeText(getActivity(), "未完善信息，注册失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private int checkPassWord(String passwordStr) {
        String regexZ = "\\d*";
        String regexS = "[a-zA-Z]+";
        String regexT = "\\W+$";
        String regexZT = "\\D*";
        String regexST = "[\\d\\W]*";
        String regexZS = "\\w*";
        String regexZST = "[\\w\\W]*";

        int pass = 0;
        if (passwordStr.matches(regexZ)) {
            return 1;
        }
        if (passwordStr.matches(regexS)) {
            return 1;
        }
        if (passwordStr.matches(regexT)) {
            return 1;
        }
        if (passwordStr.matches(regexZT)) {
            return 2;
        }
        if (passwordStr.matches(regexST)) {
            return 2;
        }
        if (passwordStr.matches(regexZS)) {
            return 2;
        }
        if (passwordStr.matches(regexZST)) {
            return 3;
        }
        return pass;
    }

}
