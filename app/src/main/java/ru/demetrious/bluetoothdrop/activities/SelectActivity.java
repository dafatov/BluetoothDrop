package ru.demetrious.bluetoothdrop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.settings.Settings;

public class SelectActivity extends AppCompatActivity {
    public final static String EXTRA_REGEX = "ru.demetrious.bluetoothdrop.regex";
    public final static int SELECT = 1;
    public final static int UNSELECT = 2;
    public final static int INVERT = 3;

    private Intent intent;
    private Button select, unselect, invert;
    private ImageButton clear;
    private EditText regex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        declarations();
        listeners();
    }

    @Override
    protected void onResume() {
        this.regex.setText((String) Settings.getPreference(Settings.APP_PREFERENCES_REGEX, getString(R.string.select_regex), String.class));
        super.onResume();
    }

    @Override
    protected void onPause() {
        Settings.getPreferencesEditor().putString(Settings.APP_PREFERENCES_REGEX, regex.getText().toString()).apply();
        super.onPause();
    }

    private void listeners() {
        select.setOnClickListener(v -> {
            intent.putExtra(EXTRA_REGEX, regex.getText().toString());
            setResult(SELECT, intent);
            finish();
        });

        unselect.setOnClickListener(v -> {
            intent.putExtra(EXTRA_REGEX, regex.getText().toString());
            setResult(UNSELECT, intent);
            finish();
        });

        invert.setOnClickListener(v -> {
            intent.putExtra(EXTRA_REGEX, regex.getText().toString());
            setResult(INVERT, intent);
            finish();
        });

        clear.setOnClickListener(v -> regex.setText(""));
    }

    private void declarations() {
        intent = new Intent();

        select = findViewById(R.id.select_select);
        unselect = findViewById(R.id.select_unselect);
        invert = findViewById(R.id.select_invert);
        clear = findViewById(R.id.select_clear);

        regex = findViewById(R.id.select_regex);
    }
}
