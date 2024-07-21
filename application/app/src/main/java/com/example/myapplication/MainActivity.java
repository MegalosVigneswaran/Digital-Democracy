package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String DEVICE_NAME = "ESP32_BT";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private TextView tvReceived;
    private Handler handler = new Handler();
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvReceived = findViewById(R.id.btnConnect);
        Button btnConnect = findViewById(R.id.btnConnect);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!connected) {
                    connectToBluetoothDevice();
                    if (bluetoothSocket != null) {
                        connected = true;
                        btnConnect.setText("Disconnect");
                        startSendingData();
                        receiveData();
                    }
                } else {
                    connected = false;
                    btnConnect.setText("Connect");
                    stopSendingData();
                    disconnectBluetoothDevice();
                }
            }
        });
    }

    private void connectToBluetoothDevice() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions not granted, handle this case gracefully
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        // Proceed with Bluetooth connection
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (DEVICE_NAME.equals(device.getName())) {
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    Log.d(TAG, "Connected to " + DEVICE_NAME);
                } catch (IOException e) {
                    Log.e(TAG, "Error connecting to Bluetooth device", e);
                    Toast.makeText(this, "Error connecting to Bluetooth device", Toast.LENGTH_SHORT).show();
                    return; // Exit method if connection fails
                } catch (SecurityException se) {
                    Log.e(TAG, "SecurityException: Permission denied", se);
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    return; // Exit method if permission denied
                }
                break;
            }
        }

        if (bluetoothSocket == null) {
            Log.e(TAG, "Bluetooth socket is null");
            Toast.makeText(this, "Bluetooth socket is null", Toast.LENGTH_SHORT).show();
            return; // Exit method if socket is null
        }
    }

    private void sendData(String data) {
        try {
            outputStream.write(data.getBytes());
            Log.d(TAG, "Data sent: " + data);
        } catch (IOException e) {
            Log.e(TAG, "Error sending data", e);
            Toast.makeText(this, "Error sending data", Toast.LENGTH_SHORT).show();
        }
    }

    // Inside receiveData() method in MainActivity.java
    private void receiveData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;
                while (connected) {
                    try {
                        bytes = inputStream.read(buffer);
                        final String receivedData = new String(buffer, 0, bytes);
                        Log.d(TAG, "Data received from ESP32: " + receivedData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvReceived.setText("Received Data: " + receivedData);
                            }
                        });
                    } catch (IOException e) {
                        Log.e(TAG, "Error receiving data from ESP32", e);
                        break; // Exit loop on IO error
                    }
                }
            }
        }).start();
    }

    private Runnable sendDataRunnable = new Runnable() {
        @Override
        public void run() {
            if (connected) {
                sendData("hi");
                handler.postDelayed(this, 1000); // Send data every 1 second
            }
        }
    };

    private void startSendingData() {
        handler.postDelayed(sendDataRunnable, 1000);
    }

    private void stopSendingData() {
        handler.removeCallbacks(sendDataRunnable);
    }

    private void disconnectBluetoothDevice() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing Bluetooth socket", e);
            Toast.makeText(this, "Error closing Bluetooth socket", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetoothDevice();
    }
}
