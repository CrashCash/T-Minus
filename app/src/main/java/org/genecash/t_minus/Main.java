package org.genecash.t_minus;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Main extends Activity {
    static final String KEY_SETTING = "setting";
    SharedPreferences prefs;
    Context ctx;
    CountDownTimer timer = null;
    Calendar calendar;
    SimpleDateFormat formatDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
    SimpleDateFormat formatTime = new SimpleDateFormat("hh:mm:ss a", Locale.US);
    TextView textCountdown;
    Button btnSetDate, btnSetTime;
    EditText seconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ctx = this;

        // retrieve current countdown setting
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long setting = prefs.getLong(KEY_SETTING, System.currentTimeMillis());
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(setting);

        // hook up date setting button
        btnSetDate = (Button) findViewById(R.id.set_date);
        btnSetDate.setText(formatDate.format(calendar.getTime()));
        btnSetDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog picker = new DatePickerDialog(ctx, new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        btnSetDate.setText(formatDate.format(calendar.getTime()));
                        save();
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                picker.show();
            }
        });

        // set date when clicked
        Button btnSetToday = (Button) findViewById(R.id.set_today);
        btnSetToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                calendar.set(Calendar.YEAR, cal.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, cal.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH));
                btnSetDate.setText(formatDate.format(cal.getTime()));
                save();
            }
        });

        // hook up time setting button
        btnSetTime = (Button) findViewById(R.id.set_time);
        btnSetTime.setText(formatTime.format(calendar.getTime()));
        btnSetTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog picker = new TimePickerDialog(ctx, new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        btnSetTime.setText(formatTime.format(calendar.getTime()));
                        save();
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
                picker.show();
            }
        });

        // set time when clicked
        Button btnSetNow = (Button) findViewById(R.id.set_now);
        btnSetNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                int h = cal.get(Calendar.HOUR_OF_DAY);
                int m = cal.get(Calendar.MINUTE);
                calendar.set(Calendar.HOUR_OF_DAY, h);
                calendar.set(Calendar.MINUTE, m);
                btnSetTime.setText(formatTime.format(calendar.getTime()));
                save();
            }
        });

        // hook up seconds field
        seconds = (EditText) findViewById(R.id.seconds);
        seconds.setText("" + calendar.get(Calendar.SECOND));
        seconds.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // set when enter is pressed
                    int val = Integer.parseInt(seconds.getText().toString());
                    if (val < 0 || val > 59) {
                        Toast.makeText(ctx, "Invalid seconds value", Toast.LENGTH_LONG).show();
                        seconds.setText("" + calendar.get(Calendar.SECOND));
                        return false;
                    }
                    calendar.set(Calendar.SECOND, val);
                    btnSetTime.setText(formatTime.format(calendar.getTime()));
                    save();
                }
                return false;
            }
        });

        // start countdown
        textCountdown = (TextView) findViewById(R.id.countdown);
        startCount();
    }

    // save new setting & reset countdown
    void save() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_SETTING, calendar.getTimeInMillis());
        editor.apply();
        startCount();
    }

    // start countdown timer
    void startCount() {
        if (timer != null) {
            // stop an already running timer
            timer.cancel();
        }

        // clean up calendar
        calendar.set(Calendar.MILLISECOND, 0);

        // if it's expired, then punt
        long count = calendar.getTimeInMillis() - System.currentTimeMillis();
        if (count < 0) {
            textCountdown.setText("Expired");
            return;
        }

        // start timer
        timer = new CountDownTimer(count, 500) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;
                seconds %= 60;
                minutes %= 60;
                hours %= 24;

                String time = ((days == 0) ? "" : String.format("%d:", days)) +
                              ((days == 0 && hours == 0) ? "" : String.format("%02d:", hours)) +
                              ((days == 0 && hours == 0 && minutes == 0) ? "" : String.format("%02d:", minutes)) +
                              String.format("%02d", seconds);

                textCountdown.setText(time);
            }

            public void onFinish() {
                textCountdown.setText("Expired");
                Toast.makeText(ctx, "Done!", Toast.LENGTH_LONG).show();
            }
        };
        timer.start();
    }
}
