package ru.demetrious.bluetoothdrop.activities;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import ru.demetrious.bluetoothdrop.R;

public class AboutActivity extends AppCompatActivity {
    private TextView version, author, info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        declarations();
        init();
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        try {
            version.setText(java.text.MessageFormat.format("{0}: {1}", getString(R.string.about_version), getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        author.setText(java.text.MessageFormat.format("{0}: {1}", getString(R.string.about_author), "Афатов Дмитрий Владимирович, группа ИВБО-04-15"));
        info.setText(java.text.MessageFormat.format("{0}: {1}", getString(R.string.about_info), "Данная программа создана в качестве программной части при выполенении дипломной работы в 2019 году для МТУ МИРЭА"));
    }

    private void declarations() {
        version = findViewById(R.id.about_version);
        author = findViewById(R.id.about_author);
        info = findViewById(R.id.about_info);
    }
}
