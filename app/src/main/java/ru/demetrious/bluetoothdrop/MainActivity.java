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
    final static int ACTIVITY_PATH = 5;

    final static int HANDLER_TIMER = 1;
    final static int HANDLER_CONNECTED = 4;
    final static int HANDLER_DISCONNECTED = 5;
    final static int HANDLER_SEND_START = 12;
    final static int HANDLER_RECEIVE_START = 13;
    final static int HANDLER_OUT_OF_MEMORY = 14;
    final static int HANDLER_STOP_TRANSFER_CLIENT = 15;
    final static int HANDLER_STOP_TRANSFER_SERVER = 16;
    final static int HANDLER_RECEIVE_HANDLER = 17;
    final static int HANDLER_LOAD_ACTIVITY_FINISH = 18;
    final static int HANDLER_EMPTY_SEND = 19;
    final static int HANDLER_ERROR_CREATE_DIRECTORY = 20;
    final static int HANDLER_ERROR_CREATE_FILE = 21;

    String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    String lastProcess = "null";

    final ArrayList<ExplorerElement> explorerElements = new ArrayList<>();
    ArrayList<String> selectedFiles = new ArrayList<>();
    final ArrayList<FriendsElement> friendsElements = new ArrayList<>();
    ExplorerElementAdapter explorerElementsAdapter;
    ArrayAdapter<String> selectedFilesAdapter;
    FriendsElementAdapter friendsElementAdapter;
    final ArrayList<SettingsElement> settingsElements = new ArrayList<>();
    SettingsElementAdapter settingsElementAdapter;

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
        new Received(this, "Received").start();

        permissions();
        declarations();
        listeners();

        explorer.currentDirectory = (String) Settings.getPreference(Settings.APP_PREFERENCES_CURRENT_DIRECTORY, Settings.DEFAULT_HOME_PATH, String.class);
        explorer.explorer();

        bluetooth.startServer();
    }

    @Override
    protected void onResume() {
        //settings.loadPreferences();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Settings.getPreferencesEditor().putString(Settings.APP_PREFERENCES_CURRENT_DIRECTORY, explorer.currentDirectory).apply();
        super.onPause();
    }

    @Override
    protected void onStop() {
        /*if (bluetooth.transfer != null && bluetooth.transfer.isAlive())
            if (send.send != null && send.send.isAlive())
                bluetooth.transferDate.cancelClient();
            else
                bluetooth.transferDate.cancelServer();*/
        if (bluetooth.isTransferring)
            bluetooth.transferDate.cancel();
        super.onStop();
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
                    explorer.setSort(data.getIntExtra(SortActivity.EXTRA_SORT_BY, R.id.sort_name), data.getIntExtra(SortActivity.EXTRA_SORT, R.id.sort_asc), data.getBooleanExtra(SortActivity.EXTRA_SORT_IGNORE_CASE, true), data.getBooleanExtra(SortActivity.EXTRA_SORT_FOLDERS, true));
                }
                break;
            case ACTIVITY_PATH:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    String id = data.getStringExtra(PathActivity.EXTRA_PARENT_SETTING);
                    String current = data.getStringExtra(PathActivity.EXTRA_CURRENT_DIR);
                    settingsElements.get(Integer.parseInt(id.replaceAll("ru.demetrious.bluetoothdrop.setting", ""))).setDescription(current);
                    Settings.getPreferencesEditor().putString(id, current).apply();
                    settingsElementAdapter.notifyDataSetChanged();
                }
                break;
            default:
                Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_LONG).show();
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
                    friends.startDiscovery();
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
                FriendsElement element = friendsElements.get(position);
                if (bluetooth.device != null) {
                    bluetooth.transferDate.stop();
                } else {
                    bluetooth.connect(element.getBluetoothDevice());
                }
            }
        });

        buttonSend.setOnClickListener(v -> {
            if (!explorer.selectedFiles.isEmpty() && bluetooth.device != null) {
                Intent intentLoad = new Intent(MainActivity.this, LoadActivity.class);
                intentLoad.putExtra(LoadActivity.EXTRA_IS_SERVER, false);
                startActivity(intentLoad);
            }
        });

        imageButtonUp.setOnClickListener(v -> imageButtonUp());

        imageButtonRefresh.setOnClickListener(v -> {
            switch (navigation.getSelectedItemId()) {
                case R.id.navigation_explorer:
                    explorer.showDirectory(new File(explorer.currentDirectory));
                    break;
                case R.id.navigation_friends:
                    friends.showBoundedDevices();
                    friends.startDiscovery();
                    break;
            }
        });

        imageButtonHome.setOnClickListener(v -> {
            switch (navigation.getSelectedItemId()) {
                case R.id.navigation_explorer:
                    explorer.showDirectory(new File((String) Settings.getPreference(Settings.APP_SETTING_HOME_PATH, Settings.DEFAULT_HOME_PATH, String.class)));
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
        settingsElementAdapter = new SettingsElementAdapter(this, R.layout.advanced_list_settings, settingsElements);

        listSpinner = findViewById(R.id.list_spinner);
        selectedFilesAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, selectedFiles) {
            @Override
            public void notifyDataSetChanged() {
                buttonSend.setVisibility((!explorer.selectedFiles.isEmpty() && bluetooth.device != null) ? View.VISIBLE : View.GONE);
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
            @Override
            public void handleMessage(Message msg) {
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
                        boolean exist = false;
                        for (int i = 0; i < friendsElements.size(); i++) {
                            if (friendsElements.get(i).getBluetoothDevice().getAddress().equals(bluetooth.device.getAddress())) {
                                friendsElements.get(i).setOnline();
                                exist = true;
                                break;
                            }
                        }
                        if (!exist)
                            friendsElements.add(0, new FriendsElement(bluetooth.device, true));
                        friendsElementAdapter.notifyDataSetChanged();
                        break;
                    case HANDLER_DISCONNECTED:
                        if (!explorer.selectedFiles.isEmpty()) buttonSend.setVisibility(View.GONE);
                        friendsElementAdapter.notifyDataSetChanged();
                        if (friends.bluetoothAdapter.isEnabled())
                            bluetooth.startServer();
                        break;
                    case HANDLER_SEND_START:
                        bluetooth.handlerLoadActivity = (Handler) msg.obj;
                        bluetooth.isTransferring = true;
                        bluetooth.isServer = false;
                        send.send();
                        break;
                    case HANDLER_RECEIVE_START:
                        bluetooth.isTransferring = true;
                        bluetooth.isServer = true;
                        Intent intentLoad = new Intent(MainActivity.this, LoadActivity.class);
                        intentLoad.putExtra(LoadActivity.EXTRA_IS_SERVER, true);
                        startActivity(intentLoad);
                        break;
                    case HANDLER_RECEIVE_HANDLER:
                        bluetooth.handlerLoadActivity = (Handler) msg.obj;
                        break;
                    case HANDLER_OUT_OF_MEMORY:
                        Toast.makeText(getApplicationContext(), getString(R.string.error_out_of_memory), Toast.LENGTH_LONG).show();
                        break;
                    case HANDLER_EMPTY_SEND:
                        Toast.makeText(getApplicationContext(), getString(R.string.error_empty_send), Toast.LENGTH_LONG).show();
                        break;
                    case HANDLER_ERROR_CREATE_DIRECTORY:
                        Toast.makeText(getApplicationContext(), getString(R.string.error_create_directory), Toast.LENGTH_LONG).show();
                        break;
                    case HANDLER_ERROR_CREATE_FILE:
                        Toast.makeText(getApplicationContext(), getString(R.string.error_create_file), Toast.LENGTH_LONG).show();
                        break;
                    case HANDLER_STOP_TRANSFER_CLIENT:
                        /*bluetooth.transferDate.cancelClient();
                        break;*/
                    case HANDLER_STOP_TRANSFER_SERVER:
                        //bluetooth.transferDate.cancelServer();
                        bluetooth.transferDate.cancel();
                        break;
                    case HANDLER_LOAD_ACTIVITY_FINISH:
                        bluetooth.handlerLoadActivity = null;
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
                Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_LONG).show();
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


