package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ViewResult extends AppCompatActivity implements BluetoothService.OnDataReceivedListener {

    private BluetoothService bluetoothService;
    private static final String TAG = "ViewResult";
    private TableLayout secta, chairta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_result);

        bluetoothService = BluetoothService.getInstance(this);
        secta = findViewById(R.id.sectable);
        chairta = findViewById(R.id.chairtable);

        bluetoothService.setOnDataReceivedListener(this);

        bluetoothService.send("ss");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onDataReceived(String data) {
        if(!data.trim().isEmpty()) {
            Log.d(TAG, "Received data: " + data);
            runOnUiThread(() -> showResult(data));
            }
        }


    private void showResult(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            JSONObject secretary = jsonObject.getJSONObject("secretary");
            JSONObject chairman = jsonObject.getJSONObject("chairman");
            JSONObject secretaryJson = jsonObject.getJSONObject("secretary_json");
            JSONObject chairmanJson = jsonObject.getJSONObject("chairman_json");

            HashMap<String, Integer> secretaryMap = new HashMap<>();
            HashMap<String, Integer> chairmanMap = new HashMap<>();

            Iterator<String> secretaryKeys = secretary.keys();
            while (secretaryKeys.hasNext()) {
                String key = secretaryKeys.next();
                int code = secretary.getInt(key);
                String name = secretaryJson.getString(key);
                secretaryMap.put(name, code);
            }

            Iterator<String> chairmanKeys = chairman.keys();
            while (chairmanKeys.hasNext()) {
                String key = chairmanKeys.next();
                int code = chairman.getInt(key);
                String name = chairmanJson.getString(key);
                chairmanMap.put(name, code);
            }

            List<Map.Entry<String, Integer>> sortedSecretaryList = new ArrayList<>(secretaryMap.entrySet());
            List<Map.Entry<String, Integer>> sortedChairmanList = new ArrayList<>(chairmanMap.entrySet());

            Collections.sort(sortedSecretaryList, (e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
            Collections.sort(sortedChairmanList, (e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

            for (Map.Entry<String, Integer> entry : sortedSecretaryList) {
                Updatetable(secta, entry.getKey(), entry.getValue().toString());
            }

            Log.d(TAG, "Sorted Chairman Map (Descending):");
            for (Map.Entry<String, Integer> entry : sortedChairmanList) {
                Updatetable(chairta, entry.getKey(), entry.getValue().toString());
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
    }

    private TextView CTV(String text, int textS, int width, boolean pad) {
        TextView textView = new TextView(this);
        textView.setText(text);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                width,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        if (pad) {
            params.setMargins(0, 10, 9, 0);
        } else {
            params.setMargins(0, 10, 0, 0);
        }
        textView.setLayoutParams(params);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(textS);
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(Color.parseColor("#BF32BC32"));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);

        return textView;
    }

    private void Updatetable(TableLayout table, String name, String vote) {
        TextView namet = CTV(name, 19, 250, true);
        TextView votet = CTV(vote, 19, 60, false);
        TableRow tr = new TableRow(this);
        tr.addView(namet);
        tr.addView(votet);
        table.addView(tr);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
