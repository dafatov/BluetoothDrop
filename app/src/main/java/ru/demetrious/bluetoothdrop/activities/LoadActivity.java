package ru.demetrious.bluetoothdrop.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import ru.demetrious.bluetoothdrop.R;

public class LoadActivity extends AppCompatActivity {
    public final static int HANDLER_PROGRESS_INC = 1;
    public final static int HANDLER_STATUS_SET = 2;
    public final static int HANDLER_PROGRESS_ALL_CHG = 3;
    public final static int HANDLER_PROGRESS_FILE_CHG = 4;
    public final static int HANDLER_ACTIVITY_FINISH = 5;
    final static String EXTRA_IS_SERVER = "ru.demetrious.bluetoothdrop.is_server";
    private Handler handler;

    private ProgressBar progressBarAll, progressBarFile;
    private TextView countFile, countAll, status;
    private Button buttonCancel;
    private boolean isServer;

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
            setTitle(getString(R.string.send_title_receive));
            MainActivity.getHandler().obtainMessage(MainActivity.HANDLER_RECEIVE_HANDLER, handler).sendToTarget();
        } else {
            setTitle(getString(R.string.send_title_send));
            MainActivity.getHandler().obtainMessage(MainActivity.HANDLER_SEND_START, handler).sendToTarget();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFinishOnTouchOutside(false);
    }

    @Override
    protected void onStop() {
        MainActivity.getHandler().obtainMessage(MainActivity.HANDLER_LOAD_ACTIVITY_FINISH).sendToTarget();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
    }

    private void listeners() {
        buttonCancel.setOnClickListener(v -> MainActivity.getHandler().obtainMessage(MainActivity.HANDLER_STOP_TRANSFER).sendToTarget());
    }

    private void declarations() {
        progressBarAll = findViewById(R.id.progress_all);
        progressBarFile = findViewById(R.id.progress_file);

        countFile = findViewById(R.id.count_file);
        countAll = findViewById(R.id.count_all);
        status = findViewById(R.id.load_status);

        buttonCancel = findViewById(R.id.path_button_cancel);

        isServer = getIntent().getBooleanExtra(EXTRA_IS_SERVER, false);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case LoadActivity.HANDLER_STATUS_SET:
                        status.setText((CharSequence) msg.obj);
                        break;
                    case LoadActivity.HANDLER_PROGRESS_FILE_CHG:
                        progressBarFile.setProgress(0);
                        progressBarFile.setMax(msg.arg1);
                        countFile.setText("0%");
                        break;
                    case LoadActivity.HANDLER_PROGRESS_ALL_CHG:
                        progressBarAll.setProgress(0);
                        progressBarAll.setMax(msg.arg1);
                        countAll.setText("0%");
                        break;
                    case LoadActivity.HANDLER_PROGRESS_INC:
                        progressBarFile.setProgress(progressBarFile.getProgress() + 1);
                        progressBarAll.setProgress(progressBarAll.getProgress() + 1);

                        String tmp = 100 * progressBarFile.getProgress() / progressBarFile.getMax() + "%";
                        if (!tmp.equals(countFile.getText().toString())) countFile.setText(tmp);
                        tmp = 100 * progressBarAll.getProgress() / progressBarAll.getMax() + "%";
                        if (!tmp.equals(countAll.getText().toString()))
                            countAll.setText(tmp);
                        break;
                    case LoadActivity.HANDLER_ACTIVITY_FINISH:
                        finish();
                        break;
                }
            }
        };
    }
}
