package com.cytrack.meterlocation.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cytrack.meterlocation.R;
import com.cytrack.meterlocation.base.BaseActivity;
import com.cytrack.meterlocation.ui.fragment.AboutFragment;
import com.cytrack.meterlocation.ui.fragment.HomeFragment;
import com.cytrack.meterlocation.ui.view.ColoredSnackBar;
import com.cytrack.meterlocation.data.Keys;

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

public class HomeActivity extends BaseActivity {
    @Override
    public Fragment getFragment() {
        return new HomeFragment();
    }

    @Override
    public boolean shouldShowUpArrow() {
        return false;
    }
}

