package ru.demetrious.bluetoothdrop.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.settings.Settings;

public class SortActivity extends AppCompatActivity {
    final static String EXTRA_SORT_BY = "ru.demetrious.bluetoothdrop.sort_by",
            EXTRA_SORT = "ru.demetrious.bluetoothdrop.sort",
            EXTRA_SORT_FOLDERS = "ru.demetrious.bluetoothdrop.sort_folders",
            EXTRA_SORT_IGNORE_CASE = "ru.demetrious.bluetoothdrop.sort_ignoreCase";

    private Intent intent;
    private RadioGroup sortBy, sort;
    private Button ok;
    private CheckBox folders, ignoreCase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sort);

        declarations();
        listeners();
    }

    @Override
    protected void onResume() {
        sortBy.check((Integer) Settings.getPreference(Settings.APP_PREFERENCES_SORT_BY, R.id.sort_name, Integer.class));
        sort.check((Integer) Settings.getPreference(Settings.APP_PREFERENCES_SORT, R.id.sort_asc, Integer.class));
        folders.setChecked((Boolean) Settings.getPreference(Settings.APP_PREFERENCES_FOLDERS, true, Boolean.class));
        ignoreCase.setChecked((Boolean) Settings.getPreference(Settings.APP_PREFERENCES_IGNORE_CASE, true, Boolean.class));
        super.onResume();
    }

    @Override
    protected void onPause() {
        Settings.getPreferencesEditor().putInt(Settings.APP_PREFERENCES_SORT_BY, sortBy.getCheckedRadioButtonId()).apply();
        Settings.getPreferencesEditor().putInt(Settings.APP_PREFERENCES_SORT, sort.getCheckedRadioButtonId()).apply();
        Settings.getPreferencesEditor().putBoolean(Settings.APP_PREFERENCES_FOLDERS, folders.isChecked()).apply();
        Settings.getPreferencesEditor().putBoolean(Settings.APP_PREFERENCES_IGNORE_CASE, ignoreCase.isChecked()).apply();
        super.onPause();
    }

    private void listeners() {
        ok.setOnClickListener(v -> {
            intent.putExtra(EXTRA_SORT_BY, sortBy.getCheckedRadioButtonId());
            intent.putExtra(EXTRA_SORT, sort.getCheckedRadioButtonId());
            intent.putExtra(EXTRA_SORT_FOLDERS, folders.isChecked());
            intent.putExtra(EXTRA_SORT_IGNORE_CASE, ignoreCase.isChecked());
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }

    private void declarations() {
        intent = new Intent();

        sort = findViewById(R.id.group_sort);
        sortBy = findViewById(R.id.group_sort_by);

        folders = findViewById(R.id.sort_folders);
        ignoreCase = findViewById(R.id.sort_ignore_case);

        ok = findViewById(R.id.sort_ok);
    }
}
