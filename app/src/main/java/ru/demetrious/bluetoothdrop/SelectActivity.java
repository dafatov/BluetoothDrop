package ru.demetrious.bluetoothdrop;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SelectActivity extends AppCompatActivity {
    final static String REGEX = "ru.demetrious.bluetoothdrop.regex";
    final static int SELECT = 1, UNSELECT = 2, INVERT = 3;

    private Intent intent;
    private Button select, unselect, invert;
    private EditText regex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        declarations();
        listeners();
    }

    private void listeners() {
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra(REGEX, regex.getText().toString());
                setResult(SELECT, intent);
                finish();
            }
        });

        unselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra(REGEX, regex.getText().toString());
                setResult(UNSELECT, intent);
                finish();
            }
        });

        invert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra(REGEX, regex.getText().toString());
                setResult(INVERT, intent);
                finish();
            }
        });
    }

    private void declarations() {
        intent = new Intent();

        select = findViewById(R.id.select_select);
        unselect = findViewById(R.id.select_unselect);
        invert = findViewById(R.id.select_invert);

        regex = findViewById(R.id.select_regex);
    }
}
