package com.njupt.multibiometricauth.guide;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.njupt.multibiometricauth.R;
import com.njupt.multibiometricauth.loginsign.LoginActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.njupt.multibiometricauth.main.MainActivity;

public class OnBoardingFragment3 extends Fragment {
    TextView nextText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_on_boarding3, container, false);

        nextText = root.findViewById(R.id.next);
        nextText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
            }
        });
        return root;
    }
}
