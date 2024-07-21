package com.example.myapplication;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class add_election extends AppCompatActivity {

    private Button btnSelectDate, btnSelectTime,addsecb, addchairb, submit , btnendingtime;
    private Calendar calendar;
    private HashMap<String , String> secretaryhash , chairmanhash;
    private TableLayout secta , chairta;
    private Vibrator vibrator;
    private TextView errormm;
    private BluetoothService bluetoothService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_election);

        btnSelectDate = findViewById(R.id.selectdate);
        btnSelectTime = findViewById(R.id.selecttime);
        btnendingtime = findViewById(R.id.selecttime_end);
        errormm = findViewById(R.id.errormm);
        addsecb = findViewById(R.id.addsecretary);
        addchairb = findViewById(R.id.addchairman);
        submit = findViewById(R.id.submit);
        secta = findViewById(R.id.sectable);
        chairta = findViewById(R.id.chairtable);
        calendar = Calendar.getInstance();

        secretaryhash = new HashMap<>();
        chairmanhash = new HashMap<>();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        bluetoothService = BluetoothService.getInstance(this);

        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        btnSelectTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
                updateTimeLabel(btnSelectTime);
            }
        });

        btnendingtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
                updateTimeLabel(btnendingtime);
            }
        });

        addsecb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {showAddTextDialog("secretary",secretaryhash,secta);}
        });
        addchairb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {showAddTextDialog("chairman",chairmanhash,chairta);}
        });
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitdata();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void vibrate(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final VibrationEffect vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
            vibrator.cancel();
            vibrator.vibrate(vibrationEffect);
        }
    }

    public void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }
        };

        new DatePickerDialog(this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    public void showTimePickerDialog() {
        TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
            }
        };

        new TimePickerDialog(this, timeSetListener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false)
                .show();
    }

    private void updateDateLabel() {
        String dateFormat = "dd-MM-yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
        btnSelectDate.setText(sdf.format(calendar.getTime()));
    }

    private void updateTimeLabel(Button button) {
        DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        button.setText(timeFormat.format(calendar.getTime()));
    }

    private void showAddTextDialog(final String whichc, final HashMap<String, String> data, final TableLayout table) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.add_dailog, null);
        builder.setView(dialogView);

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetter(source.charAt(i)) && source.charAt(i) != ' ') {
                        return "";
                    }
                }
                return null;
            }
        };

        final TextView tl = dialogView.findViewById(R.id.tl);
        final TextInputLayout tl1 = dialogView.findViewById(R.id.tl1);
        final TextInputLayout tl2 = dialogView.findViewById(R.id.tl2);
        final TextInputEditText code = dialogView.findViewById(R.id.editText);
        final TextInputEditText name = dialogView.findViewById(R.id.editText2);
        final TextView errorm = dialogView.findViewById(R.id.error_m);

        name.setFilters(filters);

        tl.setText("Add " + whichc);
        tl1.setHint(whichc + " number");
        tl2.setHint(whichc + " name");

        builder.setPositiveButton("Add", null);

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F6F6F6")));

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button addButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String enteredCode = code.getText().toString().trim();
                        String enteredName = name.getText().toString().trim();

                        if (enteredCode.length() == 1) {
                            enteredCode = "0" + enteredCode;
                        }

                        if (data.containsKey(enteredCode)) {
                            vibrate();
                            errorm.setVisibility(View.VISIBLE);
                            errorm.setText("The candidate number already exists.\nThen press Add.");
                        } else if (enteredCode.length() == 0) {
                            vibrate();
                            errorm.setVisibility(View.VISIBLE);
                            errorm.setText("Please enter a candidate number.\nThen press Add.");
                        } else if (enteredName.length() == 0) {
                            vibrate();
                            errorm.setVisibility(View.VISIBLE);
                            errorm.setText("Please enter a candidate name.\nThen press Add.");
                        }else {
                            errorm.setVisibility(View.GONE);
                            data.put(enteredCode, enteredName);
                            TextView namet = CTV(enteredName, 19, 250, false);
                            TextView codet = CTV(enteredCode, 19, 60, true);
                            TableRow tr = new TableRow(add_election.this);
                            tr.addView(codet);
                            tr.addView(namet);
                            table.addView(tr);
                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        dialog.show();
    }

    private TextView CTV(String text, int textS, int width, boolean pad) {
        TextView textView = new TextView(this);
        textView.setText(text);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                width,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        if(pad){
            params.setMargins(0, 10, 9, 0);
        }else{
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

    public static boolean isTimeValid(String t1, String t2) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getDefault()); // Use system's default time zone

        try {

            Date time1 = sdf.parse(t1);
            Date time2 = sdf.parse(t2);
            Date currentTime = new Date();

            String currentTimeString = sdf.format(currentTime);
            Date currentTimeParsed = sdf.parse(currentTimeString);

            return !(time1.compareTo(currentTimeParsed) <= 0 || time1.compareTo(time2) >= 0);

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void submitdata(){

        /*
        Json format:
        {
            "type":"add election",

            "election.json":{
              "times":"election start time",
              "timest":"election end time",
            },

            "secretary.json":{
              "code":"name"
            },

            "chairman.json":{
              "code":"name"
            },

            "result.json":{
              "secretary":{
                "code":"votes"
              },
              "chairman":{
                "code":"votes"
              }
            }
          }
         */

        String date = btnSelectDate.getText().toString();
        String starting = btnSelectTime.getText().toString();
        String ending = btnendingtime.getText().toString();

        if(date == "Select date"){

            vibrate();
            errormm.setText("Please choose date");

        }else if(starting == "Select starting time"){

            vibrate();
            errormm.setText("Please choose election starting time");

        }
        else if(ending == "Select ending time"){

            vibrate();
            errormm.setText("Please choose election ending time");

        }else if(isTimeValid(starting,ending)){

            vibrate();
            errormm.setText("starting time cannot be grater than ending");

        }else if(secretaryhash.size() == 0){

            vibrate();
            errormm.setText("Please add any secretary");

        }else if(chairmanhash.size() == 0){

            vibrate();
            errormm.setText("Please add chairman");

        }else{

            try {

                String times = date+"-"+starting.replace(":","-");
                String timest = date+"-"+ending.replace(":","-");

                JSONObject headjson = new JSONObject();
                JSONObject electionfile = new JSONObject();

                JSONObject secretaryfile = new JSONObject();
                JSONObject chairmanfile = new JSONObject();

                JSONObject resultfile = new JSONObject();
                JSONObject resultsecretary = new JSONObject();
                JSONObject resultchairman = new JSONObject();

                electionfile.put("times",times);
                electionfile.put("timest",timest);

                for (Map.Entry<String, String> entry : secretaryhash.entrySet()) {
                    secretaryfile.put(entry.getKey(),entry.getValue());
                    resultsecretary.put(entry.getKey(),0);
                }
                for (Map.Entry<String, String> entry : chairmanhash.entrySet()) {
                    chairmanfile.put(entry.getKey(),entry.getValue());
                    resultchairman.put(entry.getKey(),0);
                }

                resultfile.put("secretary",resultsecretary);
                resultfile.put("chairman",resultchairman);

                headjson.put("type", "add election");
                headjson.put("election.json", electionfile);
                headjson.put("secretary.json", secretaryfile);
                headjson.put("chairman.json", chairmanfile);
                headjson.put("result.json", resultfile);

                String sending_data = headjson.toString();

                /*if(bluetoothService.isConnected()){

                    bluetoothService.send(sending_data);

                }else{
                    Intent main = new Intent(add_election.this, MainActivity.class);
                    startActivity(main);
                }*/

                bluetoothService.send(sending_data);

            } catch (JSONException e) {

                e.printStackTrace();
            }

        }
    }

}
