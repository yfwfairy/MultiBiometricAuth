package com.njupt.multibiometricauth.loginsign;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

public class LoginAddAdapter extends FragmentStatePagerAdapter {

    List<FragmentInfo> mFragmentInfoList;
    private Context context;

    public LoginAddAdapter(@NonNull FragmentManager fm, Context context, List<FragmentInfo> fragmentList) {
        super(fm);
        this.context = context;
        mFragmentInfoList = fragmentList;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position < mFragmentInfoList.size()) {
            Fragment fragment = mFragmentInfoList.get(position).mFragment;
            return fragment;
        }
        return null;
    }

    @Override
    public int getCount() {
        return (mFragmentInfoList == null) ? 0 : mFragmentInfoList.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (position < mFragmentInfoList.size()) {
            String title = mFragmentInfoList.get(position).title;
            return title;
        }
        return null;
    }

    public static class FragmentInfo {
        public Fragment mFragment;
        public String title;
        public FragmentInfo(Fragment fragment, String title) {
            mFragment = fragment;
            this.title = title;
        }
    }
}
