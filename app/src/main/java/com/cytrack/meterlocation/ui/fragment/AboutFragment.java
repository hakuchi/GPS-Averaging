/*
 * Copyright 2015 David Vávra
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cytrack.meterlocation.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cytrack.meterlocation.App;
import com.cytrack.meterlocation.R;
import com.cytrack.meterlocation.base.BaseFragment;
import com.cytrack.meterlocation.databinding.FragmentAboutBinding;
import com.cytrack.meterlocation.ui.viewmodel.AboutViewModel;
import com.cytrack.meterlocation.util.PackageUtils;

import java.util.Locale;

/**
 * Fragment displaying about information.
 *
 * @author David Vávra (david@vavra.me)
 */
public class AboutFragment extends BaseFragment implements AboutViewModel.ClickListener {

    private AboutViewModel mViewModel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        App.component().injectToAboutFragment(this);
        FragmentAboutBinding binding = FragmentAboutBinding.inflate(inflater, container, false);
        mViewModel = new AboutViewModel();
        mViewModel.setClickListener(this);
        binding.setViewModel(mViewModel);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel.version.set(PackageUtils.getAppVersion(getContext()));
    }

    @Override
    public void onMailClicked() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"gps-averaging-app@googlegroups.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.problem_report));
        String usersPhone = Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ") " + "v"
                + PackageUtils.getAppVersion(getContext()) + "-" + Locale.getDefault();
        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.problem_report_body, usersPhone));
        startActivity(i);
    }

    @Override
    public void onRateClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.cytrack.meterlocation"));
        startActivity(intent);
    }

    @Override
    public void onGithubClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/destil/GPS-Averaging"));
        startActivity(intent);
    }
}
