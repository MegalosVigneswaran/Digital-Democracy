package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private ConstraintLayout addelection, getresult;
    private TextView bt_status,cred;
    private BluetoothService bluetoothService;
    private Handler handler = new Handler();
    private static final String DEVICE_NAME = "ESP32_BT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bt_status = findViewById(R.id.bt_status);
        addelection = findViewById(R.id.addelection);
        getresult = findViewById(R.id.viewresult);
        cred = findViewById(R.id.cred);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bluetoothService = BluetoothService.getInstance(this);

        if (checkPermissions()) {
            connectBluetooth();
        } else {
            requestPermissions();
        }

        addelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, add_election.class);
                startActivity(myIntent);
            }
        });
        getresult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, ViewResult.class);
                startActivity(myIntent);
            }
        });

        cred.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, projectinfo.class);
                startActivity(myIntent);
            }
        });
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectBluetooth();
            } else {
                ShowButton(false);
            }
        }
    }

    private void connectBluetooth() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!bluetoothService.isConnected()) {
                    if (bluetoothService.connect(DEVICE_NAME)) {
                        ShowButton(true);
                    } else {
                        ShowButton(false);
                        connectBluetooth();
                    }
                } else {
                    ShowButton(true);
                }
            }
        }, 1250);
    }

    private void ShowButton(boolean show_button) {
        if (show_button) {
            addelection.setVisibility(View.VISIBLE);
            getresult.setVisibility(View.VISIBLE);
            bt_status.setVisibility(View.INVISIBLE);
        } else {
            addelection.setVisibility(View.INVISIBLE);
            getresult.setVisibility(View.INVISIBLE);
            bt_status.setText("EVM is not connected!\nCheck your Bluetooth\nDisconnect other connected devices");
            bt_status.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
