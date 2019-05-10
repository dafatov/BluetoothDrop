package ru.demetrious.bluetoothdrop;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class PathActivity extends AppCompatActivity {
    final static String EXTRA_PARENT_SETTING = "ru.demetrious.bluetoothdrop.parent_setting";
    final static String EXTRA_CURRENT_DIR = "ru.demetrious.bluetoothdrop.current_dir";
    final static String EXTRA_HOME = "ru.demetrious.bluetoothdrop.home";

    private ListView listView;
    private Button ok, cancel;
    private TextView path;
    private ArrayList<String> arrayList;
    private ArrayAdapter arrayAdapter;
    private String current, parent, home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path);

        declarations();
        listeners();
        showDirs();
    }

    private void listeners() {
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (!current.equals(home) && position == 0) {
                current = new File(current).getParent();
            } else {
                current = current + "/" + arrayList.get(position);
            }
            showDirs();
        });

        ok.setOnClickListener(v -> {
            Settings.getPreferencesEditor().putString(Settings.APP_PREFERENCES_SAVE_PATH, current).apply();
            Intent intent = new Intent();
            intent.putExtra(EXTRA_PARENT_SETTING, parent);
            intent.putExtra(EXTRA_CURRENT_DIR, current);
            setResult(RESULT_OK, intent);
            finish();
        });

        cancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void declarations() {
        ok = findViewById(R.id.path_button_ok);
        cancel = findViewById(R.id.path_button_cancel);
        path = findViewById(R.id.path_path);
        listView = findViewById(R.id.path_list_view);
        arrayList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(arrayAdapter);

        parent = getIntent().getStringExtra(EXTRA_PARENT_SETTING);
        current = getIntent().getStringExtra(EXTRA_CURRENT_DIR);
        home = getIntent().getStringExtra(EXTRA_HOME);
    }

    private void showDirs() {
        arrayList.clear();
        File current = new File(this.current);
        path.setText(this.current);
        for (File file : current.listFiles()) {
            if (file.isDirectory()) {
                arrayList.add(file.getName());
            }
        }
        if (!this.current.equals(home))
            arrayList.add(0, "...");
        arrayAdapter.notifyDataSetChanged();
    }
}
