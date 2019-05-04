package ru.demetrious.bluetoothdrop;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadActivity extends AppCompatActivity {
    final static String EXTRA_IS_SERVER = "is_server";

    final static int HANDLER_PROGRESS_INC = 1;
    final static int HANDLER_STATUS_SET = 2;
    final static int HANDLER_PROGRESS_ALL_CHG = 3;
    final static int HANDLER_PROGRESS_FILE_CHG = 4;
    final static int HANDLER_ACTIVITY_FINISH = 5;

    private ProgressBar progressBarAll, progressBarFile;
    private TextView countFile, countAll, status;
    private Button buttonCancel;
    private boolean isServer;
    static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        declarations();
        listeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isServer) {
            //MainActivity.handler.obtainMessage(MainActivity.HANDLER_RECEIVED_START).sendToTarget();
            setTitle("Receiving");
        } else {
            MainActivity.handler.obtainMessage(MainActivity.HANDLER_SEND_START).sendToTarget();
            setTitle("Sending");
        }
    }

    @Override
    public void onBackPressed() {
    }

    private void listeners() {
        buttonCancel.setOnClickListener(v -> {
        });
    }

    private void declarations() {
        progressBarAll = findViewById(R.id.progress_all);
        progressBarFile = findViewById(R.id.progress_file);

        countFile = findViewById(R.id.count_file);
        countAll = findViewById(R.id.count_all);
        status = findViewById(R.id.load_status);

        buttonCancel = findViewById(R.id.button_cancel);

        //filesPaths = getIntent().getStringArrayExtra("SelectedFiles");
        //bluetooth = (Bluetooth) getIntent().getSerializableExtra("Bluetooth");
        isServer = getIntent().getBooleanExtra(EXTRA_IS_SERVER, false);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case LoadActivity.HANDLER_STATUS_SET:
                        status.setText((CharSequence) msg.obj);
                        break;
                    case LoadActivity.HANDLER_PROGRESS_INC:
                        progressBarAll.setProgress(progressBarAll.getProgress() + 1);
                        progressBarFile.setProgress(progressBarFile.getProgress() + 1);

                        String tmp = 100 * progressBarFile.getProgress() / progressBarFile.getMax() + "%";
                        if (!tmp.equals(countFile.getText().toString())) countFile.setText(tmp);
                        tmp = 100 * progressBarAll.getProgress() / progressBarAll.getMax() + "%";
                        if (!tmp.equals(countAll.getText().toString())) countAll.setText(tmp);
                        break;
                    case LoadActivity.HANDLER_PROGRESS_FILE_CHG:
                        progressBarFile.setMax(msg.arg1);
                        progressBarFile.setProgress(0);
                        countFile.setText("0%");
                        break;
                    case LoadActivity.HANDLER_PROGRESS_ALL_CHG:
                        progressBarAll.setMax(msg.arg1);
                        progressBarAll.setProgress(0);
                        countAll.setText("0%");
                        break;
                    case LoadActivity.HANDLER_ACTIVITY_FINISH:
                        finish();
                        break;
                }
            }
        };
    }
}
