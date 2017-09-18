package com.retrocode.xsecapp.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.retrocode.xsecapp.R;
import com.retrocode.xsecapp.model.User;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Đorđe Milutinović on 9/13/17.
 * Copyright (c) 2017 retrocode. All rights reserved.
 */

public class MainActivity extends AppCompatActivity {

    private DatabaseReference firebaseDatabase;
    private FirebaseAuth auth;
    private String batteryLevel;

    private BroadcastReceiver batteryInfoReceiver;
    private Timer timer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        findViewById(R.id.submit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateData();

                TextView connectivityTextView = (TextView) findViewById(R.id.connectivity_info);
                String connection = getConnections().toString()
                        .replace("{", "").replace("}", "").replace("=", ": ");
                connectivityTextView.setText(connection);

                updateLayout();
            }
        });

        findViewById(R.id.logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOut();
            }
        });

        batteryInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {

                int level = intent.getIntExtra("level", 0);
                batteryLevel = (String.valueOf(level) + "%");

                TextView batteryLevelTextView = (TextView) findViewById(R.id.battery_level);
                batteryLevelTextView.setText("Battery level is: " + batteryLevel);
            }
        };

        registerReceiver(batteryInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle("X Second App");
    }

    public NetworkInfo getNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    private void updateData() {
        EditText secondsEditText = (EditText) findViewById(R.id.seconds_edit_text);
        String seconds = secondsEditText.getText().toString();

        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                writeData();
            }
        }, 0, Long.parseLong(seconds) * 1000);
    }

    private void writeData() {
        User user = new User();
        String id = firebaseDatabase.push().getKey();

        if (auth.getCurrentUser() != null) {
            for (UserInfo profile : auth.getCurrentUser().getProviderData()) {
                user.username = profile.getDisplayName();
                user.id = auth.getCurrentUser().getUid();
            }
        }

        user.batteryLevel = batteryLevel;
        user.connectivity = getConnections();
        firebaseDatabase.child(id).setValue(user);
    }

    private HashMap<String, String> getConnections() {
        HashMap<String, String> connections = new HashMap<>();

        connections.put("Connection info", getNetworkInfo().getDetailedState().toString());
        connections.put("Internet", getNetworkInfo().getTypeName());

        return connections;
    }

    private void logOut() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                timer.cancel();
                MainActivity.this.unregisterReceiver(MainActivity.this.batteryInfoReceiver);
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void updateLayout() {
        findViewById(R.id.submit_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.seconds_edit_text).setVisibility(View.INVISIBLE);
        findViewById(R.id.text_view).setVisibility(View.VISIBLE);
        findViewById(R.id.battery_level).setVisibility(View.VISIBLE);
        hideKeyboard();
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(MainActivity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

}
