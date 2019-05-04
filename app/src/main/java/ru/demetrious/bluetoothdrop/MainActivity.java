package ru.demetrious.bluetoothdrop;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    final static String[] sizeUnits = new String[5];
    final static int ACTIVITY_SELECT = 1;
    final static int ACTIVITY_SORT = 2;
    final static int TURNING_ON_BLUETOOTH = 3;
    final static int TURNING_ON_DISCOVERABLE = 4;

    final static int HANDLER_TIMER = 1;
    final static int HANDLER_CONNECTED = 4;
    final static int HANDLER_DISCONNECTED = 5;
    final static int HANDLER_RECEIVED_SIZE = 9;
    final static int HANDLER_RECEIVED_PART = 10;
    final static int HANDLER_RECEIVED_NAME = 11;
    final static int HANDLER_SEND_START = 12;
    final static int HANDLER_RECEIVED_START = 13;

    String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    String lastProcess = "null";

    ArrayList<ExplorerElement> explorerElements = new ArrayList<>();
    ArrayList<String> selectedFiles = new ArrayList<>();
    final ArrayList<FriendsElement> friendsElements = new ArrayList<>();
    ExplorerElementAdapter explorerElementsAdapter;
    ArrayAdapter<String> selectedFilesAdapter;
    FriendsElementAdapter friendsElementAdapter;

    ListView listMain;
    Spinner listSpinner;
    TextView textPath, textAmount;
    BottomNavigationView navigation;
    ImageButton imageButtonUp, imageButtonRefresh, imageButtonHome, buttonSend;

    Explorer explorer;
    Friends friends;
    Settings settings;
    Send send;
    Bluetooth bluetooth;

    String homePath;//into settings

    static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        explorer = new Explorer(this);
        friends = new Friends(this);
        settings = new Settings(this);
        send = new Send(this);
        bluetooth = new Bluetooth(this);
        //
        new Test("Test").start();
        //

        permissions();
        declarations();
        listeners();

        homePath = explorer.getGlobalFileDir().getAbsolutePath();
        explorer.currentDirectory = Settings.getPreferences().getString(Settings.APP_PREFERENCES_CURRENT_DIRECTORY, homePath);
        explorer.explorer();

        bluetooth.startServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        Settings.getPreferences().edit().putString(Settings.APP_PREFERENCES_CURRENT_DIRECTORY, explorer.currentDirectory).apply();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(friends.discoveryFinishReceiver);
        if (friends.bluetoothAdapter.isDiscovering()) {
            friends.bluetoothAdapter.cancelDiscovery();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bar, menu);
        menu.findItem(R.id.bar_select).setVisible(navigation.getSelectedItemId() == R.id.navigation_explorer);
        menu.findItem(R.id.bar_sort).setVisible(navigation.getSelectedItemId() == R.id.navigation_explorer);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bar_about:
                Intent intentAbout = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intentAbout);
                return true;
            case R.id.bar_select:
                Intent intentSelect = new Intent(MainActivity.this, SelectActivity.class);
                startActivityForResult(intentSelect, ACTIVITY_SELECT);
                return true;
            case R.id.bar_sort:
                Intent intentSort = new Intent(MainActivity.this, SortActivity.class);
                startActivityForResult(intentSort, ACTIVITY_SORT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (!imageButtonUp()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case TURNING_ON_BLUETOOTH:
                if (resultCode == RESULT_OK) {
                    reLaunchProcesses();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.bluetooth_off), Toast.LENGTH_SHORT).show();
                }
                break;
            case TURNING_ON_DISCOVERABLE:
                if (resultCode != RESULT_CANCELED) {
                    handler.obtainMessage(HANDLER_TIMER, resultCode, -1).sendToTarget();
                    imageButtonHome.setImageResource(R.drawable.ic_action_bluetooth_discoverable_on);
                    friends.isDiscoverable = true;
                }
                break;
            case ACTIVITY_SELECT:
                explorer.select(resultCode, data);
                break;
            case ACTIVITY_SORT:
                if (resultCode == Activity.RESULT_OK) {
                    assert data != null;
                    explorer.setSort(data.getIntExtra(SortActivity.SORT_BY, R.id.sort_name), data.getIntExtra(SortActivity.SORT, R.id.sort_asc), data.getBooleanExtra(SortActivity.SORT_IGNORE_CASE, true), data.getBooleanExtra(SortActivity.SORT_FOLDERS, true));
                }
                break;
            default:
                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                System.exit(32342);
        }
    }

    private void listeners() {
        navigation.setOnNavigationItemSelectedListener(menuItem -> {
            invalidateOptionsMenu();
            switch (menuItem.getItemId()) {
                case R.id.navigation_explorer:
                    explorer.explorer();
                    return true;
                case R.id.navigation_friends:
                    if (friends.checkBluetooth("onNavigationItemSelected")) {
                        Log.e("Checked", "Checked");
                        friends.friends();
                        return true;
                    }
                    return false;
                case R.id.navigation_settings:
                    settings.settings();
                    return true;
            }
            return false;
        });

        navigation.setOnNavigationItemReselectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.navigation_explorer:
                    explorer.showDirectory(new File(explorer.currentDirectory));
                    break;
                case R.id.navigation_friends:
                    friends.showDiscoveringDevices();
                    break;
                case R.id.navigation_settings:
                    break;
            }
        });

        listMain.setOnItemClickListener((parent, view, position, id) -> {
            if (listMain.getAdapter().equals(explorerElementsAdapter)) {
                ExplorerElement explorerElement = explorerElements.get(position);

                if (explorerElement.isFolder())
                    explorer.showDirectory(new File(explorer.currentDirectory + "/" + explorerElement.getName()));
            } else if (listMain.getAdapter().equals(friendsElementAdapter)) {
                bluetooth.connect(position);
            }
        });

        //TODO test
        buttonSend.setOnClickListener(v -> {
            if (!explorer.selectedFiles.isEmpty() && bluetooth.clientSocket != null && bluetooth.clientSocket.isConnected()) {
                Intent intentLoad = new Intent(MainActivity.this, LoadActivity.class);
                intentLoad.putExtra(LoadActivity.EXTRA_IS_SERVER, false);
                startActivity(intentLoad);
            }
        });
        //

        imageButtonUp.setOnClickListener(v -> imageButtonUp());

        imageButtonRefresh.setOnClickListener(v -> {
            switch (navigation.getSelectedItemId()) {
                case R.id.navigation_explorer:
                    explorer.showDirectory(new File(explorer.currentDirectory));
                    break;
                case R.id.navigation_friends:
                    friends.showDiscoveringDevices();
                    break;
            }
        });

        imageButtonHome.setOnClickListener(v -> {
            switch (navigation.getSelectedItemId()) {
                case R.id.navigation_explorer:
                    explorer.showDirectory(new File(homePath));
                    break;
                case R.id.navigation_friends:
                    friends.enableDiscoveryMode();
                    break;
            }
        });
    }

    private boolean imageButtonUp() {
        if (!explorer.currentDirectory.equals(explorer.getGlobalFileDir().getAbsolutePath())) {
            explorer.showDirectory(new File(explorer.currentDirectory).getParentFile());
            return true;
        }
        return false;
    }

    private void declarations() {
        sizeUnits[0] = getString(R.string.sizeUnits_byte);
        sizeUnits[1] = getString(R.string.sizeUnits_kilobyte);
        sizeUnits[2] = getString(R.string.sizeUnits_megabyte);
        sizeUnits[3] = getString(R.string.sizeUnits_gigabyte);
        sizeUnits[4] = getString(R.string.sizeUnits_terabyte);

        navigation = findViewById(R.id.navigation);

        listMain = findViewById(R.id.list_main);
        explorerElementsAdapter = new ExplorerElementAdapter(this, R.layout.advanced_list_explorer, explorerElements);
        listMain.setAdapter(explorerElementsAdapter);

        friendsElementAdapter = new FriendsElementAdapter(this, R.layout.advanced_list_friends, friendsElements);

        listSpinner = findViewById(R.id.list_spinner);
        selectedFilesAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, selectedFiles) {
            @Override
            public void notifyDataSetChanged() {
                buttonSend.setVisibility((!explorer.selectedFiles.isEmpty() && bluetooth.connected) ? View.VISIBLE : View.GONE);//TODO Event allows the sending data
                super.notifyDataSetChanged();
            }
        };
        listSpinner.setAdapter(selectedFilesAdapter);

        textPath = findViewById(R.id.text_path);
        textAmount = findViewById(R.id.text_amount);

        imageButtonHome = findViewById(R.id.imageButton_home);
        imageButtonRefresh = findViewById(R.id.imageButton_refresh);
        imageButtonUp = findViewById(R.id.imageButton_up);

        buttonSend = findViewById(R.id.floatingActionButton_send);

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(friends.discoveryFinishReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(friends.discoveryFinishReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(friends.discoveryFinishReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(friends.discoveryFinishReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(friends.discoveryFinishReceiver, intentFilter);

        handler = new Handler() {
            String currentFile = "";
            PartType partType = PartType.receivedAllSize;
            int allSize, fileSize, receivedFileSize = 0;
            byte[] file;

            @Override
            public void handleMessage(Message msg) {
                //super.handleMessage(msg);
                switch (msg.what) {
                    case HANDLER_TIMER:
                        Message message = new Message();
                        message.what = HANDLER_TIMER;
                        message.arg1 = msg.arg1 - 1;
                        if (msg.arg1 > 0) {
                            if (navigation.getSelectedItemId() == R.id.navigation_friends)
                                textPath.setText(MessageFormat.format("{0} {1}", getString(R.string.bluetooth_discoverable_timer), secondsToTime(msg.arg1)));
                            handler.sendMessageDelayed(message, 1000);
                        } else {
                            textPath.setText("");
                            imageButtonHome.setImageResource(R.drawable.ic_action_bluetooth_discoverable_off);
                            friends.isDiscoverable = false;
                        }
                        break;
                    case HANDLER_CONNECTED:
                        if (!explorer.selectedFiles.isEmpty())
                            buttonSend.setVisibility(View.VISIBLE);
                        break;
                    case HANDLER_DISCONNECTED:
                        if (!explorer.selectedFiles.isEmpty()) buttonSend.setVisibility(View.GONE);
                        break;
                    case HANDLER_SEND_START:
                        send.send();
                        break;
                    case HANDLER_RECEIVED_START:
                        bluetooth.transferDate.wait = false;
                        //
                        Intent intentLoad = new Intent(MainActivity.this, LoadActivity.class);
                        intentLoad.putExtra(LoadActivity.EXTRA_IS_SERVER, true);
                        startActivity(intentLoad);
                        //
                        break;
                    case HANDLER_RECEIVED_NAME:
                        currentFile = (String) msg.obj;
                        LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_STATUS_SET, currentFile).sendToTarget();
                        LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_PROGRESS_FILE_CHG, msg.arg1, -1).sendToTarget();
                        break;
                }
            }
        };
    }

    private void permissions(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
        }
    }

    private void permissions() {
        for (String perm : permissions) {
            permissions(perm);
        }
    }

    private void reLaunchProcesses() {
        switch (lastProcess) {
            case "":
                break;
            case "onNavigationItemSelected":
                navigation.setSelectedItemId(R.id.navigation_friends);
                break;
            default:
                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                System.exit(3464);
        }
    }

    private String secondsToTime(int seconds) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] times = {getString(R.string.timeUnits_seconds), getString(R.string.timeUnits_minutes), getString(R.string.timeUnits_hours)};
        int[] time = new int[times.length];
        int index = 0;

        while (seconds > 0) {
            time[index++] = seconds % 60;
            seconds /= 60;
        }

        for (int i = index - 1; i >= 0; i--) {
            stringBuilder.append(time[i]).append(" ").append(times[i]).append(" ");
        }
        return stringBuilder.toString();
    }
}


