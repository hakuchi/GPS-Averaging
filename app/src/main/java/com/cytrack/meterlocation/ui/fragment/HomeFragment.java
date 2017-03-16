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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cytrack.meterlocation.R;
import com.cytrack.meterlocation.base.BaseFragment;
import com.cytrack.meterlocation.data.Keys;
import com.cytrack.meterlocation.ui.view.ColoredSnackBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment containing most of the UI. It listens to events and triggers related components.
 *
 * @author David Vávra (david@vavra.me)
 */
public class HomeFragment extends BaseFragment {

    public static final String TAG = "TAG";
    private Button submitLocationBtn;
    private EditText assetId;
    private OkHttpClient client;
    private String androidId;
    private String url = "http://www.cy-track.com/uFind/";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_home, container, false);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        assetId = (EditText) view.findViewById(R.id.assetid);
        submitLocationBtn = (Button) view.findViewById(R.id.submitLocationBtn);

        final TextInputLayout assetIdLayout = (TextInputLayout) view.findViewById(R.id.assetidLayout);

        client = new OkHttpClient();

        androidId = Settings.Secure.getString(getActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID);


        final Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), "Device ID: " + androidId, Snackbar.LENGTH_INDEFINITE);
        ColoredSnackBar.info(snackbar).show();

        submitLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                snackbar.dismiss();

                if (assetId.getText().length() > 0) {
                    assetIdLayout.setErrorEnabled(false);
                    if (isNetworkAvailable()) {
                        try {
                            Intent intent = new Intent("com.cytrack.meterlocation.AVERAGED_LOCATION");
                            startActivityForResult(intent, 0);
                        } catch (ActivityNotFoundException e) {
                            //GPS Averaging is not installed, you can redirect user to Play like this:
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.cytrack.meterlocation"));
                            startActivity(intent);
                        }
                    } else {
                        showSnackBarAlert("Check the network connection...");
                    }
                } else {
                    assetIdLayout.setErrorEnabled(true);
                    assetIdLayout.setError("Please enter an account number");
                }

            }
        });

        return view;
    }


    private void checkAccount(String url) {
        final Request request = new Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSnackBarAlert("Request failed!");
                    }
                });

            }

            @Override
            public void onResponse(Call call, final Response response) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseBody);
                            String error_code = jsonObject.getString(Keys.KEY_ERROR_CODE);
                            String account_name = jsonObject.getString(Keys.KEY_ACCOUNT_NAME);
                            // Check the response code and show the appropriate dialog
                            Log.d(TAG, responseBody);
                            if (error_code.equalsIgnoreCase("02")) {
                                submitAccountDialog(account_name);
                            } else {
                                msgDialog("Invalid Account",
                                        "The account number you submitted does not exist.");
                            }

                        } catch (IOException ioe) {
                            showSnackBarAlert("Invalid response!");
                        } catch (JSONException e) {
                            showSnackBarAlert("Invalid response!");
                        }

                    }
                });
            }
        });
    }


    private void submitAccount(String url) {
        final Request request = new Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSnackBarAlert("Request failed!");
                    }
                });

            }

            @Override
            public void onResponse(Call call, final Response response) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseBody);
                            String error_code = jsonObject.getString(Keys.KEY_ERROR_CODE);
                            Log.d(TAG, responseBody);
                            if (error_code.equalsIgnoreCase("02")) {
                                msgDialog("Account submitted",
                                        "The account has been successfully updated.");
                                assetId.setText("");
                            } else {
                                msgDialog("Failed",
                                        "An error occured while submitting the account.");
                            }

                        } catch (IOException ioe) {
                            showSnackBarAlert("Invalid response!");
                        } catch (JSONException e) {
                            showSnackBarAlert("Invalid response!");
                        }

                    }
                });
            }
        });
    }

    private void showSnackBarAlert(String message) {
        Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        ColoredSnackBar.alert(snackbar).show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            Bundle locationBundle = intent.getExtras();
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            urlBuilder.addQueryParameter("acc_number", assetId.getText().toString());
            urlBuilder.addQueryParameter("lat", String.valueOf(locationBundle.getDouble("latitude")));
            urlBuilder.addQueryParameter("lng", String.valueOf(locationBundle.getDouble("longitude")));
            urlBuilder.addQueryParameter("alt", String.valueOf(locationBundle.getDouble("altitude")));
            urlBuilder.addQueryParameter("acc", String.valueOf(locationBundle.getDouble("accuracy")));
            urlBuilder.addQueryParameter("device_id", androidId);
            urlBuilder.addQueryParameter("cmd", "numValidity");
            checkAccount(urlBuilder.build().toString());
        } else {
            //handle cancel
        }
    }

    public void submitAccountDialog(String account_name) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.alert_dialog, null);
        final String acc_name = account_name;
        dialogBuilder.setView(dialogView);
        final TextView tv = (TextView) dialogView.findViewById(R.id.alertMessage);
        tv.setText("Account: " + assetId.getText().toString() + "\nName: "+ account_name);
        dialogBuilder.setTitle("Valid account");
        dialogBuilder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Submit the validated account
                HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
                urlBuilder.addQueryParameter("acc_number", assetId.getText().toString());
                urlBuilder.addQueryParameter("acc_name", acc_name);
                urlBuilder.addQueryParameter("device_id", androidId);
                urlBuilder.addQueryParameter("cmd", "submitLoc");

                submitAccount(urlBuilder.build().toString());
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }


    public void msgDialog(String title, String message) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.alert_dialog, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //do something with edt.getText().toString();
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
        /// if no network is available networkInfo will be null
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }
}

