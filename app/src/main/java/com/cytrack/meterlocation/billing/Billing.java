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

package com.cytrack.meterlocation.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.squareup.otto.Bus;

import com.cytrack.meterlocation.R;
import com.cytrack.meterlocation.billing.event.BecomePremiumEvent;
import com.cytrack.meterlocation.data.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages connection with Google IaB API v3.
 *
 * @author David Vávra (david@vavra.me)
 */
@Singleton
public class Billing {

    private final Context mContext;
    private final Bus mBus;
    private final Preferences mPreferences;
    private BillingProcessor mBillingProcessor;
    private boolean mFullVersion = false;

    @Inject
    public Billing(Context context, Bus bus, Preferences preferences) {
        mContext = context;
        mBus = bus;
        mPreferences = preferences;
        mFullVersion = preferences.isFullVersion();
    }

    public void activityOnCreate() {
        mBillingProcessor = new BillingProcessor(mContext, mContext.getString(R.string.google_play_license_key), new BillingProcessor.IBillingHandler() {
            @Override
            public void onProductPurchased(String s, TransactionDetails transactionDetails) {
                fullVersion();
            }

            @Override
            public void onPurchaseHistoryRestored() {
                onBillingInitialized();
            }

            @Override
            public void onBillingError(int i, Throwable throwable) {
                checkLegacyPremium();
            }

            @Override
            public void onBillingInitialized() {
                if (mBillingProcessor.isPurchased(mContext.getString(R.string.google_play_product_id))) {
                    fullVersion();
                } else {
                    checkLegacyPremium();
                }
            }
        });
        mBillingProcessor.loadOwnedPurchasesFromGoogle();
    }

    public void activityOnDestroy() {
        if (mBillingProcessor != null) {
            mBillingProcessor.release();
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mBillingProcessor.handleActivityResult(requestCode, resultCode, data);
    }

    public void purchase(Activity activity) {
        mBillingProcessor.purchase(activity, mContext.getString(R.string.google_play_product_id));
    }

    private void fullVersion() {
        mFullVersion = true;
        mBus.post(new BecomePremiumEvent());
        mPreferences.setFullVersion(true);
    }

    private void checkLegacyPremium() {
        mFullVersion = false;
        if (mContext.getPackageManager().checkSignatures("com.cytrack.meterlocation", "com.cytrack.meterlocation") == PackageManager.SIGNATURE_MATCH) {
            fullVersion();
            return;
        }
        mPreferences.setFullVersion(false);
    }

    public boolean isFullVersion() {
        return mFullVersion;
    }
}
