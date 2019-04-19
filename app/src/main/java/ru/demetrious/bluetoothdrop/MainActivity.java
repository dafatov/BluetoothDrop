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
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    final static String[] sizeUnits = new String[5];
    final static int ACTIVITY_SELECT = 1;
    final static int ACTIVITY_SORT = 2;
    final static int TURNING_ON_BLUETOOTH = 3;
    final static int TURNING_ON_DISCOVERABLE = 4;

    String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    private IntentFilter intentFilter;
    String lastProcess = "null";

    ArrayList<ExplorerElement> explorerElements = new ArrayList<>();
    ArrayList<String> selectedFiles = new ArrayList<>();
    ArrayList<FriendsElement> friendsElements = new ArrayList<>();
    ExplorerElementAdapter explorerElementsAdapter;
    ArrayAdapter<String> selectedFilesAdapter;
    FriendsElementAdapter friendsElementAdapter;

    ListView listMain;
    Spinner listSpinner;
    TextView textPath, textAmount;
    BottomNavigationView navigation;
    ImageButton imageButtonUp, imageButtonRefresh, imageButtonHome;

    Explorer explorer;
    Friends friends;
    Settings settings;

    String homePath;//into settings

    Handler handlerTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        explorer = new Explorer(this);
        friends = new Friends(this);
        settings = new Settings(this);

        permissions();
        declarations();
        listeners();

        homePath = explorer.getGlobalFileDir().getAbsolutePath();
        explorer.explorer();


        handlerTimer = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //super.handleMessage(msg);
                textPath.setText(MessageFormat.format("Time remaining: {0}", secondsToTime(msg.what)));
                if (msg.what > 0) handlerTimer.sendEmptyMessageDelayed(--msg.what, 1000);
                else textPath.setText("");
            }
        };
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
                    Toast.makeText(getApplicationContext(), "Bluetooth was enable", Toast.LENGTH_SHORT).show();
                    reLaunchProcesses();
                } else {
                    Toast.makeText(getApplicationContext(), "To use this function, bluetooth must be enabled", Toast.LENGTH_SHORT).show();
                }
                break;
            case TURNING_ON_DISCOVERABLE:
                handlerTimer.sendEmptyMessage(120);
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
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.navigation_explorer:
                        explorer.explorer();
                        return true;
                    case R.id.navigation_friends:
                        friends.friends();
                        return true;
                    case R.id.navigation_settings:
                        return true;
                }
                return false;
            }
        });

        navigation.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem menuItem) {
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
            }
        });

        listMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listMain.getAdapter().equals(explorerElementsAdapter)) {
                    ExplorerElement explorerElement = explorerElements.get(position);

                    if (explorerElement.isFolder())
                        explorer.showDirectory(new File(explorer.currentDirectory + "/" + explorerElement.getName()));
                } else if (listMain.getAdapter().equals(friendsElementAdapter)) {
                    FriendsElement friendsElement = friendsElements.get(position);
                    friendsElement.setOnline(true);
                    friendsElementAdapter.notifyDataSetChanged();
                }
            }
        });

        imageButtonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageButtonUp();
            }
        });

        imageButtonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                explorer.showDirectory(new File(explorer.currentDirectory));
            }
        });

        imageButtonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                explorer.showDirectory(new File(homePath));
            }
        });
    }

    private boolean imageButtonUp() {
        if (!explorer.currentDirectory.equals(explorer.getGlobalFileDir().getAbsolutePath())) {
            String[] folders = String.valueOf(explorer.currentDirectory).split("/");
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < folders.length - 1; i++) {
                sb.append(folders[i]);
                if (i != folders.length - 1) sb.append("/");
            }
            explorer.showDirectory(new File(sb.toString()));
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
        selectedFilesAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, selectedFiles);
        listSpinner.setAdapter(selectedFilesAdapter);

        textPath = findViewById(R.id.text_path);
        textAmount = findViewById(R.id.text_amount);

        imageButtonHome = findViewById(R.id.imageButton_home);
        imageButtonRefresh = findViewById(R.id.imageButton_refresh);
        imageButtonUp = findViewById(R.id.imageButton_up);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(friends.discoveryFinishReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(friends.discoveryFinishReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(friends.discoveryFinishReceiver, intentFilter);
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
            case "showDiscoveringDevices":
                friends.showDiscoveringDevices();
                break;
            case "showBoundedDevices":
                friends.showBoundedDevices();
                break;
            default:
                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                System.exit(3464);
        }
    }

    private String secondsToTime(int seconds) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] times = {"seconds", "minutes", "hours"};
        int[] time = new int[times.length];
        int index = 0;

        while (seconds > 0) {
            time[index++] = seconds%60;
            seconds /= 60;
        }

        for (int i = index-1; i >= 0; i--) {
            stringBuilder.append(time[i]).append(" ").append(times[i]).append(" ");
        }
        return stringBuilder.toString();
    }
}


