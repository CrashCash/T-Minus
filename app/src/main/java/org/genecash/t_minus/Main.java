package org.genecash.t_minus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main extends Activity {
    static final String KEY_SETTING = "setting";
    SharedPreferences prefs;
    Context ctx;
    Calendar calendar;
    SimpleDateFormat formatDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
    SimpleDateFormat formatTime = new SimpleDateFormat("h:mm:ss a", Locale.US);
    TextView textCountdown;
    TextView textSeconds;
    Button btnSetDate, btnSetTime;
    CheckBox cbTalk;
    long launchTime;
    long secondsOld;

    Map<Integer, Map<Integer, List<Integer>>> mapSounds;
    MediaPlayer mPlayer;

    // runs without a timer by reposting this handler at the end of the runnable
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

                // talk to me, goose!
                if (cbTalk.isChecked() && hours == 0 && millisUntilLaunch >= 0) {
                    Map<Integer, List<Integer>> map = mapSounds.get(Integer.valueOf((int) minutes));
                    if (map != null) {
                        List<Integer> sounds = map.get(Integer.valueOf((int) seconds));
                        if (sounds != null) {
                            new Handler().postDelayed(() -> speak(sounds), 0);
                        }
                    }
                }
            }
            timerHandler.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ctx = this;

        createSoundMap();

        // retrieve current countdown setting
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long setting = prefs.getLong(KEY_SETTING, System.currentTimeMillis());
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(setting);
        calendar.set(Calendar.MILLISECOND, 0);

        // set date to today when clicked
        Button btnSetToday = findViewById(R.id.set_today);
        btnSetToday.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH));
            calendar.set(Calendar.MONTH, c.get(Calendar.MONTH));
            calendar.set(Calendar.YEAR, c.get(Calendar.YEAR));
            btnSetDate.setText(formatDate.format(calendar.getTime()));
            save();
        });

        // date button
        btnSetDate = findViewById(R.id.set_date);
        btnSetDate.setText(formatDate.format(calendar.getTime()));
        btnSetDate.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(ctx, (view, year, monthOfYear, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnSetDate.setText(formatDate.format(calendar.getTime()));
                save();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            picker.show();
        });

        // time button
        btnSetTime = findViewById(R.id.set_time);
        btnSetTime.setText(formatTime.format(calendar.getTime()));
        btnSetTime.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(ctx, (view, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                btnSetTime.setText(formatTime.format(calendar.getTime()));
                save();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
            picker.show();
        });
        btnSetTime.setOnLongClickListener(v -> {
            AlertDialog.Builder dialogSeconds = new AlertDialog.Builder(this);
            dialogSeconds.setTitle("Seconds");
            LinearLayout layout = new LinearLayout(this);
            layout.setGravity(Gravity.CENTER);

            NumberPicker picker1 = new NumberPicker(this);
            picker1.setMaxValue(5);
            picker1.setMinValue(0);
            picker1.setValue(calendar.get(Calendar.SECOND) / 10);
            layout.addView(picker1, 0);

            NumberPicker picker2 = new NumberPicker(this);
            picker2.setMaxValue(9);
            picker2.setMinValue(0);
            picker2.setValue(calendar.get(Calendar.SECOND) % 10);
            layout.addView(picker2, 1);

            dialogSeconds.setView(layout);
            dialogSeconds.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            dialogSeconds.setPositiveButton("OK", (dialog, which) -> {
                calendar.set(Calendar.SECOND, picker1.getValue() * 10 + picker2.getValue());
                btnSetTime.setText(formatTime.format(calendar.getTime()));
                save();
                dialog.dismiss();
            });
            dialogSeconds.show();
            return true;
        });

        cbTalk = findViewById(R.id.cb_talk);

        // start countdown
        textCountdown = findViewById(R.id.countdown);
        textSeconds = findViewById(R.id.tplus);
        launchTime = calendar.getTimeInMillis();
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

    @Override
    protected void onResume() {
        super.onResume();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    void createSoundMap() {
        Map<Integer, List<Integer>> map;
        List<Integer> sounds;
        int id1, id2;
        String pkg = getPackageName();

        mapSounds = new HashMap<>();

        // sounds for last minute
        map = new HashMap<>();
        id1 = getResources().getIdentifier("digit60", "raw", pkg);
        id2 = getResources().getIdentifier("seconds", "raw", pkg);
        sounds = new ArrayList<>();
        sounds.add(id1);
        sounds.add(id2);
        map.put(0, sounds);
        mapSounds.put(1, map);

        // seconds
        map = new HashMap<>();
        for (int i = 50; i > 10; i -= 10) {
            id1 = getResources().getIdentifier("digit" + i, "raw", pkg);
            sounds = new ArrayList<>();
            sounds.add(id1);
            sounds.add(id2);
            map.put(i, sounds);
        }
        mapSounds.put(0, map);

        // last 10 seconds
        for (int i = 0; i < 11; i++) {
            id1 = getResources().getIdentifier("digit" + i, "raw", pkg);
            sounds = new ArrayList<>();
            sounds.add(id1);
            map.put(i, sounds);
        }
        mapSounds.put(0, map);

        // sounds for last 15 minutes
        id2 = getResources().getIdentifier("minutes", "raw", pkg);
        for (int i = 2; i < 16; i++) {
            map = new HashMap<>();
            id1 = getResources().getIdentifier("digit" + i, "raw", pkg);
            sounds = new ArrayList<>();
            sounds.add(id1);
            sounds.add(id2);
            map.put(0, sounds);
            mapSounds.put(i, map);
        }
    }

    void speak(List<Integer> ids) {
        for (int id : ids) {
            while (mPlayer != null && mPlayer.isPlaying()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }

            if (mPlayer != null) {
                mPlayer.reset();
                mPlayer.release();
            }

            mPlayer = MediaPlayer.create(this, id);
            mPlayer.start();
        }
    }

    void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
