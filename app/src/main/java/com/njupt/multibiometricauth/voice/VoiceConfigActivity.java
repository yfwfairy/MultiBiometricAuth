package com.njupt.multibiometricauth.voice;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.njupt.multibiometricauth.Constants;
import com.njupt.multibiometricauth.MMAApplication;
import com.njupt.multibiometricauth.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.IdentityListener;
import com.iflytek.cloud.IdentityResult;
import com.iflytek.cloud.IdentityVerifier;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VoiceConfigActivity extends AppCompatActivity {

    private static final String TAG = VoiceConfigActivity.class.getSimpleName();

    private static final int PWD_TYPE_NUM = 3;

    // 会话类型
    private int mSST = 0;
    // 注册
    private static final int SST_ENROLL = 0;
    // 验证
    private static final int SST_VERIFY = 1;

    // 默认为数字密码
    private int mPwdType = 3;
    // 模型操作类型
    private int mModelCmd;
    // 查询模型
    private static final int MODEL_QUE = 0;
    // 删除模型
    private static final int MODEL_DEL = 1;

    private FloatingActionButton speakButton;
    private TextView tipTxv;
    private TextView resultTxv;
    private Button regButton;
    // 身份验证对象
    private IdentityVerifier mIdVerifier;
    private Toast mToast;
    // 进度对话框
    private ProgressDialog mProDialog;

    private String phoneNumber;
    private VoiceRegStatus mRegStatus = VoiceRegStatus.UNINIT;

    // 数字声纹密码
    private String mNumPwd = "";
    // 数字声纹密码段，默认有5段
    private String[] mNumPwdSegs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_voice_config);
        initUI();
    }

    @Override
    protected void onDestroy() {
        if (null != mIdVerifier) {
            mIdVerifier.destroy();
            mIdVerifier = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initEngine();
    }

    private void initEngine() {
        if (mIdVerifier != null) {
            checkUserDataAndStates();
        }
        mIdVerifier = IdentityVerifier.createVerifier(this, new InitListener() {

            @Override
            public void onInit(int errorCode) {
                if (ErrorCode.SUCCESS == errorCode) {
                    showTip("引擎初始化成功");
                    checkUserDataAndStates();
                } else {
                    showTip("引擎初始化失败，错误码：" + errorCode+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
                }
            }
        });
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    private void initUI() {
        speakButton = findViewById(R.id.speak_floatbutton);
        tipTxv = findViewById(R.id.tip_txv);
        resultTxv = findViewById(R.id.result_txv);
        regButton = findViewById(R.id.reg_button);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
//        mToast.setGravity(Gravity.CLIP_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);

        mProDialog = new ProgressDialog(this);
        mProDialog.setCancelable(true);
        mProDialog.setTitle("请稍候");
        // cancel进度框时，取消正在进行的操作
        mProDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                if (null != mIdVerifier) {
                    mIdVerifier.cancel();
                }
            }
        });

    }

    //检测当前用户是否已注册过声纹
    public void checkUserDataAndStates() {
        executeModelCommand("query");
    }

    public void voiceRegClicked(View view) {
        switch (mRegStatus) {
            case UNINIT:
                initEngine();
                break;
            case REG:
                executeModelCommand("delete");
                break;
            case UNREG:
                downloadPwd();
                break;


        }
        String userId = getPhoneNumber();

    }

    public void voiceVrfClicked(View view) {

    }

    public void voiceDelClicked(View view) {

    }

    public String getPhoneNumber() {
        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumber = ((MMAApplication)getApplication()).getProp(Constants.PHONE);
        }
        return phoneNumber;
    }

    /**
     * 模型操作
     * @param cmd 命令
     */
    private void executeModelCommand(String cmd) {
        if ("query".equals(cmd)) {
            mProDialog.setMessage("查询中...");
        } else if ("delete".equals(cmd)) {
            mProDialog.setMessage("删除中...");
        }
        mProDialog.show();
        // 设置声纹模型参数
        // 清空参数
        mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
        // 设置会话场景
        mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ivp");
        // 用户id
        mIdVerifier.setParameter(SpeechConstant.AUTH_ID, getPhoneNumber());

        // 子业务执行参数，若无可以传空字符传
        StringBuffer params3 = new StringBuffer();
        // 设置模型操作的密码类型
        params3.append("pwdt=" + mPwdType + ",");
        // 执行模型操作
        mIdVerifier.execute("ivp", cmd, params3.toString(), mModelListener);
    }

    /**
     * 声纹模型操作监听器
     */
    private IdentityListener mModelListener = new IdentityListener() {
        @Override
        public void onResult(IdentityResult result, boolean islast) {
            Log.d(TAG, "model operation:" + result.getResultString());
            mProDialog.dismiss();
            JSONObject jsonResult = null;
            int ret = ErrorCode.SUCCESS;
            try {
                jsonResult = new JSONObject(result.getResultString());
                ret = jsonResult.getInt("ret");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            switch (mModelCmd) {
                case MODEL_QUE:
                    if (ErrorCode.SUCCESS == ret) {
                        showTip("模型存在");
                        mRegStatus = VoiceRegStatus.REG;
                    } else {
                        showTip("模型不存在");
                        mRegStatus = VoiceRegStatus.UNREG;
                    }
                    break;
                case MODEL_DEL:
                    if (ErrorCode.SUCCESS == ret) {
                        showTip("模型已删除");
                        mRegStatus = VoiceRegStatus.UNREG;
                    } else {
                        showTip("模型删除失败");
                    }
                    break;
                default:
                    break;
            }
            updateRegStatus();
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }

        @Override
        public void onError(SpeechError error) {
            mProDialog.dismiss();
            showTip(error.getPlainDescription(true));
            mRegStatus = VoiceRegStatus.UNREG;
            updateRegStatus();
        }
    };

    private void updateRegStatus() {
        int resId = 0;
        switch (mRegStatus) {
            case REG:
                resId = R.string.unreg_tip;
                break;
            case UNREG:
                resId = R.string.reg_tip;
                break;
            case REGERROR:
                resId = R.string.reg_error;
                break;
        }
        final int finalResId = resId;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                regButton.setText(finalResId);
            }
        });
    }

    private void downloadPwd() {
        // 获取密码之前先终止之前的操作
        mIdVerifier.cancel();

        mNumPwd = null;
        // 下载密码时，按住说话触摸无效
        speakButton.setClickable(false);

        mProDialog.setMessage("下载密码中...");
        mProDialog.show();

        // 设置下载密码参数
        // 清空参数
        mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
        // 设置会话场景
        mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ivp");

        // 子业务执行参数，若无可以传空字符传
        StringBuffer params = new StringBuffer();
        // 设置模型操作的密码类型
        params.append("pwdt=" + mPwdType + ",");
        // 执行密码下载操作
        int ret = mIdVerifier.execute("ivp", "download", params.toString(), mDownloadPwdListener);
        if (ret!= 0)
            mProDialog.dismiss();
    }

    /**
     * 下载密码监听器
     */
    private IdentityListener mDownloadPwdListener = new IdentityListener() {

        @Override
        public void onResult(IdentityResult result, boolean islast) {
            Log.d(TAG, result.getResultString());
            mProDialog.dismiss();
            speakButton.setClickable(true);
            switch (mPwdType) {
                case PWD_TYPE_NUM:
                    StringBuffer numberString = new StringBuffer();
                    try {
                        JSONObject object = new JSONObject(result.getResultString());
                        if (!object.has("num_pwd")) {
                            mNumPwd = null;
                            return;
                        }

                        JSONArray pwdArray = object.optJSONArray("num_pwd");
                        numberString.append(pwdArray.get(0));
                        for (int i = 1; i < pwdArray.length(); i++) {
                            numberString.append("-" + pwdArray.get(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mNumPwd = numberString.toString();
                    mNumPwdSegs = mNumPwd.split("-");

                    tipTxv.setText("您的注册密码：\n" + mNumPwd + "\n请长按“按住说话”按钮进行注册\n");
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }

        @Override
        public void onError(SpeechError error) {
            mProDialog.dismiss();
            // 下载密码时，恢复按住说话触摸
            // 下载密码时，恢复按住说话触摸
            speakButton.setClickable(true);
            tipTxv.setText("密码下载失败！" + error.getPlainDescription(true));
        }
    };


}
