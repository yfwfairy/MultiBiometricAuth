package com.njupt.multibiometricauth.loginsign;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.njupt.multibiometricauth.Constants;
import com.njupt.multibiometricauth.main.BiometricSelectionActivity;
import com.njupt.multibiometricauth.main.MainActivity;
import com.njupt.multibiometricauth.R;
import com.njupt.multibiometricauth.SQLite.User;
import com.njupt.multibiometricauth.SQLite.UserDatabaseHelper;

import java.util.List;

public class LoginTabFragment extends Fragment {

    private static final String SP_NAME = "LOGIN_INFO";
    private static final String SP_PROP_REM = "rememberPwd";
    private static final String SP_PROP_PHONE = "phone";
    private static final String SP_PROP_PASS = "password";
    SwitchCompat mSwitchCompat;
    UserDatabaseHelper mUserDatabaseHelper;
    SharedPreferences preference;
    SharedPreferences.Editor editor;
    EditText phoneEdt, passwordEdt;
    Button login;
    CheckBox rem;
    float v = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.login_tab_fragment, container, false);
        preference = getActivity().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        mUserDatabaseHelper = new UserDatabaseHelper(getActivity());
        initView(root);
        return root;
    }

    public void fillInEdt(String phoneNumber, String password) {
        phoneEdt.setText(phoneNumber);
        passwordEdt.setText(password);
    }


    private void initView(ViewGroup root) {

        passwordEdt = root.findViewById(R.id.pass);
        phoneEdt = root.findViewById(R.id.email);
        login = root.findViewById(R.id.login);
        rem = root.findViewById(R.id.rem_cb);
        mSwitchCompat = root.findViewById(R.id.switch_passsee);

        rem.setTranslationY(800);
        passwordEdt.setTranslationY(800);
        phoneEdt.setTranslationY(800);
        login.setTranslationY(800);
        mSwitchCompat.setTranslationY(800);

        phoneEdt.setAlpha(v);
        passwordEdt.setAlpha(v);
        rem.setAlpha(v);
        login.setAlpha(v);
        mSwitchCompat.setAlpha(v);

        phoneEdt.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        rem.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        passwordEdt.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        login.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();
        mSwitchCompat.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(300).start();

        mSwitchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    passwordEdt.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    passwordEdt.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });
        boolean isRemember = preference.getBoolean(SP_PROP_REM, false);

        String strAccount = preference.getString(SP_PROP_PHONE, "");
        String strPassword = preference.getString(SP_PROP_PASS, "");
        if (isRemember) {
            phoneEdt.setText(strAccount);
            passwordEdt.setText(strPassword);
            rem.setChecked(true);
        } else {
            phoneEdt.setText(strAccount);
        }

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String account = phoneEdt.getText().toString();
                String pass = passwordEdt.getText().toString();
                if (TextUtils.isEmpty(account) || TextUtils.isEmpty(pass)) {
                    Toast.makeText(getActivity(), "请输入完整内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<User> res = mUserDatabaseHelper.queryUser(account);
                if (res.size() < 1) {
                    Toast.makeText(getActivity(), "该账户尚未注册", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (res.size() > 1) {
                    Toast.makeText(getActivity(), "内部错误", Toast.LENGTH_SHORT).show();
                    return;
                }

                User user = res.get(0);

                if (!pass.equals(user.getPassword())) {
                    Toast.makeText(getActivity(), "密码错误", Toast.LENGTH_SHORT).show();
                    return;
                }

                editor = preference.edit();
                if (rem.isChecked()) {//复选框被选中
                    editor.putBoolean(SP_PROP_REM, true);
                    editor.putString(SP_PROP_PHONE, account);
                    editor.putString(SP_PROP_PASS, pass);
                } else {
                    editor.putBoolean(SP_PROP_REM, false);
                    editor.remove(SP_PROP_PASS);
                }
                editor.commit();
                Toast.makeText(getActivity(), "登录成功", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), BiometricSelectionActivity.class);
                intent.putExtra(Constants.PHONE, user.getPhone());
                intent.putExtra(Constants.USERNAME, user.getName());
                startActivity(intent);
                getActivity().finish();//销毁此Activity
            }
        });
    }
}
