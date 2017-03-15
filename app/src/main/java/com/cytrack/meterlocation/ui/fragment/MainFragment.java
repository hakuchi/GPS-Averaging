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

import android.Manifest;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import com.cytrack.meterlocation.App;
import com.cytrack.meterlocation.R;
import com.cytrack.meterlocation.base.BaseFragment;
import com.cytrack.meterlocation.billing.Billing;
import com.cytrack.meterlocation.data.Intents;
import com.cytrack.meterlocation.data.Preferences;
import com.cytrack.meterlocation.databinding.FragmentMainBinding;
import com.cytrack.meterlocation.location.GpsObserver;
import com.cytrack.meterlocation.location.event.CurrentLocationEvent;
import com.cytrack.meterlocation.location.event.FirstFixEvent;
import com.cytrack.meterlocation.location.event.GpsNotAvailableEvent;
import com.cytrack.meterlocation.location.event.SatellitesEvent;
import com.cytrack.meterlocation.measure.LocationAverager;
import com.cytrack.meterlocation.measure.Measurements;
import com.cytrack.meterlocation.measure.event.AveragedLocationEvent;
import com.cytrack.meterlocation.ui.Animations;
import com.cytrack.meterlocation.ui.view.Snackbar;
import com.cytrack.meterlocation.ui.viewmodel.MainFragmentViewModel;

import javax.inject.Inject;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;
import permissions.dispatcher.ShowsRationale;

/**
 * Fragment containing most of the UI. It listens to events and triggers related components.
 *
 * @author David Vávra (david@vavra.me)
 */
@RuntimePermissions
public class MainFragment extends BaseFragment implements MainFragmentViewModel.FabListener {

    @Inject
    Bus mBus;
    @Inject
    GpsObserver mGps;
    @Inject
    Measurements mMeasurements;
    @Inject
    LocationAverager mAverager;
    @Inject
    Intents mIntents;
    @Inject
    Billing mBilling;

    private MainFragmentViewModel mViewModel;
    private FragmentMainBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        App.component().injectToMainFragment(this);
        if (savedInstanceState == null) {
            mViewModel = new MainFragmentViewModel();
        } else {
            mViewModel = (MainFragmentViewModel) savedInstanceState.getSerializable("VIEW_MODEL");
        }
        if (mViewModel != null) {
            mViewModel.setClickListener(this);
            // if averaging is running in background and android doesn't keep the activity
            if (mAverager.isRunning()) {
                mViewModel.hasFix = true;
                mViewModel.isAveraging = true;
                mViewModel.stopIcon.set(true);
            }
        }
        mBinding = FragmentMainBinding.inflate(inflater, container, false);
        mBinding.setViewModel(mViewModel);
        mBus.register(this);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        MainFragmentPermissionsDispatcher.observeGpsWithCheck(this);
    }

    @Override
    public void onStop() {
        mGps.stop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mBus.unregister(this);

        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("VIEW_MODEL", mViewModel);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MainFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Subscribe
    public void onFirstFix(FirstFixEvent e) {
        if (!mViewModel.hasFix) {
            mViewModel.hasFix = true;
            Animations.hide(mBinding.progress);
            startAveraging();
            Animations.showFromBottom(mBinding.fab);
        }
    }

    @Subscribe
    public void onGpsNotAvailable(GpsNotAvailableEvent e) {
        mViewModel.hasFix = false;
        Snackbar.show(mBinding.coordinator, R.string.gps_not_available);
    }

    @Subscribe
    public void onSatellites(SatellitesEvent e) {
        mViewModel.satelliteInfo.set(getString(R.string.satellites_info, e.getCount()));
    }

    @Subscribe
    public void onCurrentLocation(CurrentLocationEvent e) {
        mBinding.currentLocation.updateLocation(e.getLocation());
    }

    @Subscribe
    public void onAverageLocation(AveragedLocationEvent e) {
        mBinding.averageLocation.updateLocation(e.getLocation());
        // if the precision is btn 0.5-1m, stop averaging
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        String units = prefs.getString(Preferences.UNITS, Preferences.UNITS_DEFAULT_VALUE);
        double accuracy = units.equals(Preferences.UNITS_METRIC) ? e.getLocation().getAccuracy() : e.getLocation().getAccuracy() * 3.28132739;
        if ((accuracy > 0.5) && (accuracy < 1.0)){
                stopAveraging();
        }
    }

    @Override
    public void onFabClicked() {
        if (mAverager.isRunning()) {
            stopAveraging();
        } else {
            startAveraging();
        }
    }


    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void observeGps() {
        mGps.start();
    }

    @ShowsRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showRationaleForLocation() {
        Snackbar.show(mBinding.coordinator, R.string.location_permission_rationale);
    }

    private void startAveraging() {
        mAverager.start();
        mViewModel.isAveraging = true;
        mViewModel.stopIcon.set(true);
        if (mViewModel.isReadyForSharing) {
            mViewModel.isReadyForSharing = false;
            mBinding.averageLocation.collapse(new Animations.AnimationEndCallback() {
                @Override
                public void onAnimationEnd() {
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        mBinding.averageLocation.setVisibility(View.VISIBLE);
                    }
                }
            });
        } else {
            Animations.showFromBottom(mBinding.averageLocation);
        }
    }

    private void stopAveraging() {
        mAverager.stop();
        mViewModel.isAveraging = false;
        mViewModel.isReadyForSharing = true;
        mViewModel.stopIcon.set(false);
        mIntents.answerToThirdParty(getActivity());
    }
}
