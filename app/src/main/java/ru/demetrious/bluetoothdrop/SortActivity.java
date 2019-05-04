package ru.demetrious.bluetoothdrop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

public class SortActivity extends AppCompatActivity {
    final static String SORT_BY = "ru.demetrious.bluetoothdrop.sort_by",
            SORT = "ru.demetrious.bluetoothdrop.sort",
            SORT_FOLDERS = "ru.demetrious.bluetoothdrop.sort_folders",
            SORT_IGNORE_CASE = "ru.demetrious.bluetoothdrop.sort_ignoreCase";

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

        /*folders.setVisibility(getIntent().getBooleanExtra("bottomNavigationMode", true)? View.VISIBLE:View.INVISIBLE);
        findViewById(R.id.sort_date).setVisibility(getIntent().getBooleanExtra("bottomNavigationMode", true)? View.VISIBLE:View.INVISIBLE);
        findViewById(R.id.sort_size).setVisibility(getIntent().getBooleanExtra("bottomNavigationMode", true)? View.VISIBLE:View.INVISIBLE);*/
    }

    @Override
    protected void onResume() {
        sortBy.check(Settings.getPreferences().getInt(Settings.APP_PREFERENCES_SORT_BY, R.id.sort_name));
        sort.check(Settings.getPreferences().getInt(Settings.APP_PREFERENCES_SORT, R.id.sort_asc));
        folders.setChecked(Settings.getPreferences().getBoolean(Settings.APP_PREFERENCES_FOLDERS, true));
        ignoreCase.setChecked(Settings.getPreferences().getBoolean(Settings.APP_PREFERENCES_IGNORE_CASE, true));
        super.onResume();
    }

    @Override
    protected void onPause() {
        Settings.getPreferences().edit().putInt(Settings.APP_PREFERENCES_SORT_BY, sortBy.getCheckedRadioButtonId()).apply();
        Settings.getPreferences().edit().putInt(Settings.APP_PREFERENCES_SORT, sort.getCheckedRadioButtonId()).apply();
        Settings.getPreferences().edit().putBoolean(Settings.APP_PREFERENCES_FOLDERS, folders.isChecked()).apply();
        Settings.getPreferences().edit().putBoolean(Settings.APP_PREFERENCES_IGNORE_CASE, ignoreCase.isChecked()).apply();
        super.onPause();
    }

    private void listeners() {
        ok.setOnClickListener(v -> {
            intent.putExtra(SORT_BY, sortBy.getCheckedRadioButtonId());
            intent.putExtra(SORT, sort.getCheckedRadioButtonId());
            intent.putExtra(SORT_FOLDERS, folders.isChecked());
            intent.putExtra(SORT_IGNORE_CASE, ignoreCase.isChecked());
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
