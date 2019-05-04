package ru.demetrious.bluetoothdrop;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class SelectActivity extends AppCompatActivity {
    final static String REGEX = "ru.demetrious.bluetoothdrop.regex";
    final static int SELECT = 1, UNSELECT = 2, INVERT = 3;

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
        regex.setText(Settings.getPreferences().getString(Settings.APP_PREFERENCES_REGEX, getString(R.string.select_regex)));
        super.onResume();
    }

    @Override
    protected void onPause() {
        Settings.getPreferences().edit().putString(Settings.APP_PREFERENCES_REGEX, regex.getText().toString()).apply();
        super.onPause();
    }

    private void listeners() {
        select.setOnClickListener(v -> {
            intent.putExtra(REGEX, regex.getText().toString());
            setResult(SELECT, intent);
            finish();
        });

        unselect.setOnClickListener(v -> {
            intent.putExtra(REGEX, regex.getText().toString());
            setResult(UNSELECT, intent);
            finish();
        });

        invert.setOnClickListener(v -> {
            intent.putExtra(REGEX, regex.getText().toString());
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
