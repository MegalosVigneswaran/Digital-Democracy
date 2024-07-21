package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String deviceName = "";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private boolean connected = false;
    private Context context;

    private static BluetoothService instance;

    private BluetoothService(Context context) {
        this.context = context;
    }

    public static synchronized BluetoothService getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothService(context);
        }
        return instance;
    }

    public boolean connect(String device_name) {
        deviceName = device_name;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Permissions are not granted
                Log.e(TAG, "Permissions not granted");
                return false;
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device");
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Please enable Bluetooth");
            return false;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device_name.equals(device.getName())) {
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    connected = true;
                    Log.d(TAG, "Connected to " + device_name);
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "Error connecting to Bluetooth device", e);
                    return false;
                }
            }
        }

        Log.e(TAG, "Bluetooth device not found");
        return false;
    }

    public void send(String data) {
        if (!connected) {
            Log.e(TAG, "Bluetooth not connected");
            return;
        }

        try {
            outputStream.write(data.getBytes());
            Log.d(TAG, "Data sent: " + data);
        } catch (IOException e) {
            Log.e(TAG, "Error sending data", e);
        }
    }

    public String receive() {
        if (!connected) {
            Log.e(TAG, "Bluetooth not connected");
            return null;
        }

        try {
            byte[] buffer = new byte[1024];
            int bytes = inputStream.read(buffer);
            String receivedData = new String(buffer, 0, bytes);
            Log.d(TAG, "Data received: " + receivedData);
            return receivedData;
        } catch (IOException e) {
            Log.e(TAG, "Error receiving data", e);
            return null;
        }
    }

    public void disconnect() {
        if (!connected) {
            Log.e(TAG, "Bluetooth not connected");
            return;
        }

        try {
            bluetoothSocket.close();
            connected = false;
            Log.d(TAG, "Bluetooth disconnected");
        } catch (IOException e) {
            Log.e(TAG, "Error closing Bluetooth socket", e);
        }
    }

    public boolean isConnected() {

        if (!connected) {
            return false;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled.");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permissions not granted");
                return false;
            }
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(deviceName)) {
                int connectionState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
                if (connectionState == BluetoothAdapter.STATE_CONNECTED) {
                    Log.d(TAG, "Device is connected: " + deviceName);
                    return true;
                }
            }
        }
        Log.d(TAG, "Device is not connected: " + deviceName);
        return false;
    }
}
