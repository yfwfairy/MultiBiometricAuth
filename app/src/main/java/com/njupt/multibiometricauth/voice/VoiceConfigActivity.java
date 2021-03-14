package com.njupt.multibiometricauth.voice;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.IdentityListener;
import com.iflytek.cloud.IdentityResult;
import com.iflytek.cloud.IdentityVerifier;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.record.PcmRecorder;
import com.iflytek.cloud.util.VerifierUtil;
import com.njupt.multibiometricauth.Constants;
import com.njupt.multibiometricauth.MMAApplication;
import com.njupt.multibiometricauth.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VoiceConfigActivity extends AppCompatActivity {

    private int BASE = 600;
    private int SPACE = 200;// 间隔取样时间
    private Drawable[] micImages;
    //话筒的图片
    private ImageView micImage;
    private static final String TAG = VoiceConfigActivity.class.getSimpleName();
    private static final int PWD_TYPE_NUM = 3;
    private TextView statusTitleTxv;
    private TextView statusDescTxv;
    private TextView tipTxv;
    private Button backButton;
    private Button unRegBtn;
    private ConstraintLayout recordingContainer;
    private TextView recordingHint;
    private Button recordImv;
    private MediaRecorder recorder=null;
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
    // 身份验证对象
    private IdentityVerifier mIdVerifier;
    private Toast mToast;
    // 是否可以录音
    private boolean mCanStartRecord = false;
    // 是否可以录音
    private boolean isStartWork = false;
    // pcm录音机
    private PcmRecorder mPcmRecorder;
    // 录音采样率
    private final int SAMPLE_RATE = 16000;
    // 进度对话框
    private ProgressDialog mProDialog;
    private String phoneNumber;
    private VoiceRegStatus mRegStatus = VoiceRegStatus.UNINIT;
    // 数字声纹密码
    private String mNumPwd = "";
    // 数字声纹密码段，默认有5段
    private String[] mNumPwdSegs;
    //用于验证的声纹密码
    private String mVerifyNumPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    showTip("引擎初始化失败，错误码：" + errorCode + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
                }
            }
        });
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    private void initUI() {
        recordImv = findViewById(R.id.record_button);
        tipTxv = findViewById(R.id.tip_txv);
        statusTitleTxv = findViewById(R.id.status_txv);
        statusDescTxv = findViewById(R.id.status_desc);
        backButton = findViewById(R.id.back_button);
        unRegBtn = findViewById(R.id.unreg_btn);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        unRegBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRegStatus == VoiceRegStatus.REG) {
                    executeModelCommand("delete");
                }
            }
        });
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

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

        recordImv.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mRegStatus == VoiceRegStatus.UNINIT || mRegStatus == VoiceRegStatus.REGERROR) {
                    showTip("init error can't record");
                    return false;
                }
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!isStartWork) {
//                            recordImv.setImageResource(R.drawable.micro);
                            if (mSST == SST_ENROLL) {
                                if (mNumPwdSegs == null) {
                                    downloadPwd();
                                    break;
                                }
                                vocalEnroll();
                            } else if (mSST == SST_VERIFY) {
                                vocalVerify();
                            } else {
                                showTip("mSST invalid!");
                                break;
                            }
                            isStartWork = true;
                            mCanStartRecord = true;
                        }
                        if (mCanStartRecord) {
                            try {
                                mPcmRecorder = new PcmRecorder(SAMPLE_RATE, 40);
                                mPcmRecorder.startRecording(mPcmRecordListener);
                            } catch (SpeechError e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        mIdVerifier.stopWrite("ivp");
                        if (null != mPcmRecorder) {
                            mPcmRecorder.stopRecord(true);
                        }
//                        recordImv.setImageResource(R.drawable.ic_recorder);
                        break;
                }
                return false;
            }
        });
    }


    private void vocalVerify() {

        StringBuffer strBuffer = new StringBuffer();
        mVerifyNumPwd = VerifierUtil.generateNumberPassword(8);
        strBuffer.append("您的验证密码：" + mVerifyNumPwd + "\n");
        strBuffer.append("请长按“按住说话”按钮进行验证！\n");
        tipTxv.setText(strBuffer.toString());
        // 设置声纹验证参数
        // 清空参数
        mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
        // 设置会话场景
        mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ivp");
        // 设置会话类型
        mIdVerifier.setParameter(SpeechConstant.MFV_SST, "verify");
        // 验证模式，单一验证模式：sin
        mIdVerifier.setParameter(SpeechConstant.MFV_VCM, "sin");
        // 用户的唯一标识，在声纹业务获取注册、验证、查询和删除模型时都要填写，不能为空
        mIdVerifier.setParameter(SpeechConstant.AUTH_ID, getPhoneNumber());
        // 设置监听器，开始会话
        mIdVerifier.startWorking(mVerifyListener);
    }

    /**
     * 声纹验证监听器
     */
    private IdentityListener mVerifyListener = new IdentityListener() {

        @Override
        public void onResult(IdentityResult result, boolean islast) {
            Log.d(TAG, "verify:" + result.getResultString());

            try {
                JSONObject object = new JSONObject(result.getResultString());
                String decision = object.getString("decision");

                if ("accepted".equalsIgnoreCase(decision)) {
                    tipTxv.setText("验证通过");
                } else {
                    tipTxv.setText("验证失败");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            isStartWork = false;
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            if (SpeechEvent.EVENT_VOLUME == eventType) {
                showTip("音量：" + arg1);
            } else if (SpeechEvent.EVENT_VAD_EOS == eventType) {
                showTip("录音结束");
            }
        }

        @Override
        public void onError(SpeechError error) {
            isStartWork = false;
            mCanStartRecord = false;

            StringBuffer errorResult = new StringBuffer();
            errorResult.append("验证失败！\n");
            errorResult.append("错误信息：" + error.getPlainDescription(true) + "\n");
            errorResult.append("请长按“按住说话”重新验证!");
            tipTxv.setText(errorResult.toString());
        }
    };

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

    public void reminderClicked(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("提供语音录入和语音验证功能，目前只支持数字密码！");
        builder.setPositiveButton("我知道了", null);
        builder.create() .show();
    }



    //录音点击事件
    public void recodeClicked(View view){

    }

    private void cancelOperation() {
        isStartWork = false;
        mIdVerifier.cancel();

        if (null != mPcmRecorder) {
            mPcmRecorder.stopRecord(true);
        }
    }

    //删除点击事件
    public void voiceDelClicked(View view) {

    }

    public String getPhoneNumber() {
        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumber = ((MMAApplication) getApplication()).getProp(Constants.PHONE);
        }
        return phoneNumber;
    }

    /**
     * 模型操作
     *
     * @param cmd 命令
     */
    private void executeModelCommand(String cmd) {
        if ("query".equals(cmd)) {
            mProDialog.setMessage("查询中...");
            mModelCmd = MODEL_QUE;
        } else if ("delete".equals(cmd)) {
            mProDialog.setMessage("删除中...");
            mModelCmd = MODEL_DEL;
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
                        downloadPwd();
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
            downloadPwd();
        }
    };

    private void updateRegStatus() {
        int titleId = 0;
        int descId = 0;
        boolean canUnreg = false;
        switch (mRegStatus) {
            case REG:
                titleId = R.string.regged_title;
                descId = R.string.regged_description;
                mSST = SST_VERIFY;
                canUnreg = true;
                break;
            case UNREG:
                titleId = R.string.unuregged_title;
                descId = R.string.unregged_description;
                mSST = SST_ENROLL;
                break;
            case REGERROR:
                titleId = R.string.initfailed_title;
                descId = R.string.initfailed_description;
                mSST = -1;
                break;
        }
        final int finalTitleId = titleId;
        final int finalDescId = descId;
        final boolean unRegBtnVisible = canUnreg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusTitleTxv.setText(finalTitleId);
                statusDescTxv.setText(finalDescId);
                if (unRegBtnVisible) {
                    unRegBtn.setVisibility(View.VISIBLE);
                } else {
                    unRegBtn.setVisibility(View.GONE);
                }
            }
        });
    }

    //密码下载
    private void downloadPwd() {
        // 获取密码之前先终止之前的操作
        mIdVerifier.cancel();

        mNumPwd = null;
        // 下载密码时，按住说话触摸无效
        recordImv.setClickable(false);

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
        if (ret != 0)
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
            recordImv.setClickable(true);
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
                    tipTxv.setText("您的注册密码：\n" + mNumPwd + "\n请长按“长按讲话”按钮进行注册\n");
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
            recordImv.setClickable(true);
            tipTxv.setText("密码下载失败！" + error.getPlainDescription(true));
        }
    };

    /**
     * 注册
     */
    private void vocalEnroll() {
        StringBuffer strBuffer = new StringBuffer();
        strBuffer.append("请长按“按住说话”按钮！\n");
        strBuffer.append("请读出：" + mNumPwdSegs[0] + "\n");
        strBuffer.append("训练 第" + 1 + "遍，剩余4遍\n");
        tipTxv.setText(strBuffer.toString());

        // 设置声纹注册参数
        // 清空参数
        mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
        // 设置会话场景
        mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ivp");
        // 设置会话类型
        mIdVerifier.setParameter(SpeechConstant.MFV_SST, "enroll");
//     // 用户id
//      mIdVerifier.setParameter(SpeechConstant.AUTH_ID, authid);
        // 设置监听器，开始会话
        mIdVerifier.startWorking(mEnrollListener);
    }

    /**
     * 声纹注册监听器
     */
    private IdentityListener mEnrollListener = new IdentityListener() {

        @Override
        public void onResult(IdentityResult result, boolean islast) {
            Log.d(TAG, result.getResultString());

            JSONObject jsonResult = null;
            try {
                jsonResult = new JSONObject(result.getResultString());
                int ret = jsonResult.getInt("ret");

                if (ErrorCode.SUCCESS == ret) {

                    final int suc = Integer.parseInt(jsonResult.optString("suc"));
                    final int rgn = Integer.parseInt(jsonResult.optString("rgn"));

                    if (suc == rgn) {
                        tipTxv.setText("注册成功");

                        mCanStartRecord = false;
                        isStartWork = false;
                        if (mPcmRecorder != null) {
                            mPcmRecorder.stopRecord(true);
                        }
                        mRegStatus = VoiceRegStatus.REG;
                        updateRegStatus();
                    } else {
                        int nowTimes = suc + 1;
                        int leftTimes = 5 - nowTimes;

                        StringBuffer strBuffer = new StringBuffer();
                        strBuffer.append("请长按“按住说话”按钮！\n");
                        strBuffer.append("请读出：" + mNumPwdSegs[nowTimes - 1] + "\n");
                        strBuffer.append("训练 第" + nowTimes + "遍，剩余" + leftTimes + "遍");
                        tipTxv.setText(strBuffer.toString());
                    }

                } else {
                    showTip(new SpeechError(ret).getPlainDescription(true));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle bundle) {
            if (SpeechEvent.EVENT_VOLUME == eventType) {
                showTip("音量：" + arg1);
            } else if (SpeechEvent.EVENT_VAD_EOS == eventType) {
                showTip("录音结束");
            }

        }

        @Override
        public void onError(SpeechError error) {
            isStartWork = false;

            StringBuffer errorResult = new StringBuffer();
            errorResult.append("注册失败！\n");
            errorResult.append("错误信息：" + error.getPlainDescription(true) + "\n");
            errorResult.append("请长按“按住说话”重新注册!");
            tipTxv.setText(errorResult.toString());
        }

    };

    /**
     * 录音机监听器
     */
    private PcmRecorder.PcmRecordListener mPcmRecordListener = new PcmRecorder.PcmRecordListener() {

        @Override
        public void onRecordStarted(boolean success) {
        }

        @Override
        public void onRecordReleased() {
        }

        @Override
        public void onRecordBuffer(byte[] data, int offset, int length) {
            StringBuffer params = new StringBuffer();

            switch (mSST) {
                case SST_ENROLL:
                    params.append("rgn=5,");
                    params.append("ptxt=" + mNumPwd + ",");
                    params.append("pwdt=" + mPwdType + ",");
                    mIdVerifier.writeData("ivp", params.toString(), data, 0, length);
                    break;
                case SST_VERIFY:
                    params.append("ptxt=" + mVerifyNumPwd + ",");
                    params.append("pwdt=" + mPwdType + ",");
                    mIdVerifier.writeData("ivp", params.toString(), data, 0, length);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onError(SpeechError e) {
        }
    };


}
