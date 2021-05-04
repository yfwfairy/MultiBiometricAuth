package com.njupt.multibiometricauth;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.njupt.multibiometricauth.face.CameraRegisterAndRecognizeActivity;
import com.njupt.multibiometricauth.face.PhotoRegisterAndRecognizeActivity;

public class MyDialog {
    public final static int PICK_AVATAR = 1;

    public Dialog       mDialog;

    public TextView 	mSelectCamera;
    public TextView		mSelectPicture;
    public TextView 	mCancel;
    public View         mView;
    private Context mContext;

    public MyDialog(Context context, int type) {
        mContext = context;
        if(PICK_AVATAR==type) {
            LayoutInflater inflater = LayoutInflater.from(context);
            mView = inflater.inflate(R.layout.dialog_pick_avatar, null);
            mDialog = new Dialog(context, R.style.dialog);
            mDialog.setContentView(mView);

            Window dialogWindow = mDialog.getWindow();
            dialogWindow.setGravity(Gravity.BOTTOM);
            dialogWindow.setWindowAnimations(R.style.dialogWindowAnim); // 添加动画
            WindowManager.LayoutParams lp = dialogWindow.getAttributes(); // 获取对话框当前的参数值
            lp.width = context.getResources().getDisplayMetrics().widthPixels; // 宽度
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT; // 高度
            dialogWindow.setAttributes(lp);

            mDialog.setCanceledOnTouchOutside(true);
            mCancel         = mView.findViewById(R.id.select_dismiss);
            mSelectCamera   = mView.findViewById(R.id.select_camera);
            mSelectPicture  = mView.findViewById(R.id.select_picture);
            mCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            mSelectCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, CameraRegisterAndRecognizeActivity.class);
                    mContext.startActivity(intent);
                }
            });
            mSelectPicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, PhotoRegisterAndRecognizeActivity.class);
                    mContext.startActivity(intent);
                }
            });
        }
    }
    public void show() {
        mDialog.show();
    }

    public void dismiss() {
        mDialog.dismiss();
    }
}
