package org.genecash.t_minus;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Main extends Activity {
    static final String KEY_SETTING = "setting";
    SharedPreferences prefs;
    Context ctx;
    Calendar calendar;
    SimpleDateFormat formatDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
    SimpleDateFormat formatTime = new SimpleDateFormat("hh:mm:ss a", Locale.US);
    TextView textCountdown;
    TextView textSeconds;
    Button btnSetDate;
    NumberPicker pickHours, pickMinutes, pickSeconds;
    RadioButton radioAM, radioPM;
    long launchTime;
    long secondsOld;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();

    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millisUntilLaunch = launchTime - System.currentTimeMillis();
            long seconds = Math.abs(millisUntilLaunch / DateUtils.SECOND_IN_MILLIS);
            if (seconds != secondsOld) {
                secondsOld = seconds;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                if (millisUntilLaunch < 0 && seconds < 9999) {
                    textSeconds.setText("" + seconds);
                    textSeconds.setVisibility(View.VISIBLE);
                } else {
                    textSeconds.setVisibility(View.GONE);
                }

                seconds %= 60;
                minutes %= 60;
                hours %= 24;

                String time = "";
                if (days != 0) {
                    time = String.format("%d:", days);
                }
                if (time.equals("")) {
                    if (hours != 0) {
                        time = String.format("%d:", hours);
                    }
                } else {
                    time += String.format("%02d:", hours);
                }
                if (time.equals("")) {
                    if (minutes != 0) {
                        time = String.format("%d:", minutes);
                    }
                } else {
                    time += String.format("%02d:", minutes);
                }
                if (time.equals("")) {
                    time += String.format("%d", seconds);
                } else {
                    time += String.format("%02d", seconds);
                }

                textCountdown.setText(time);
            }
            timerHandler.postDelayed(this, 200);
        }
    };

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
        btnSetDate = findViewById(R.id.set_date);
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
        Button btnSetToday = findViewById(R.id.set_today);
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

        // hook up time settings
        pickHours = findViewById(R.id.hours);
        pickHours.setMinValue(1);
        pickHours.setMaxValue(12);
        pickHours.setValue(calendar.get(Calendar.HOUR));
        pickHours.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                calendar.set(Calendar.HOUR, newVal);
                save();
            }
        });

        pickMinutes = findViewById(R.id.minutes);
        pickMinutes.setMinValue(0);
        pickMinutes.setMaxValue(59);
        pickMinutes.setValue(calendar.get(Calendar.MINUTE));
        pickMinutes.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                calendar.set(Calendar.MINUTE, newVal);
                save();
            }
        });
        // add leading zero
        pickMinutes.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });

        pickSeconds = findViewById(R.id.seconds);
        pickSeconds.setMinValue(0);
        pickSeconds.setMaxValue(59);
        pickSeconds.setValue(calendar.get(Calendar.SECOND));
        pickSeconds.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                calendar.set(Calendar.SECOND, newVal);
                save();
            }
        });
        // add leading zero
        pickSeconds.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });

        radioAM = findViewById(R.id.am);
        radioPM = findViewById(R.id.pm);
        setAmPm(calendar.get(Calendar.AM_PM));
        calendar.set(Calendar.MILLISECOND, 0);

        // start countdown
        textCountdown = findViewById(R.id.countdown);
        textSeconds = findViewById(R.id.tplus);
        launchTime = calendar.getTimeInMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    // AM radio button clicked
    public void clickedAM(View view) {
        calendar.set(Calendar.AM_PM, Calendar.AM);
        save();
    }

    // PM radio button clicked
    public void clickedPM(View view) {
        calendar.set(Calendar.AM_PM, Calendar.PM);
        save();
    }

    // set radio buttons
    void setAmPm(int ampm) {
        if (ampm == Calendar.AM) {
            radioAM.setChecked(true);
        }
        if (ampm == Calendar.PM) {
            radioPM.setChecked(true);
        }
    }

    // save new setting & reset countdown
    void save() {
        // Log.e("T-Minus", "Setting: " + calendar);
        // NumberPicker.OnValueChangeListener fires a LOT
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_SETTING, calendar.getTimeInMillis());
        editor.apply();
        launchTime = calendar.getTimeInMillis();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
