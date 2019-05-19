package ru.demetrious.bluetoothdrop.activities;

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

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.bluetooth.Bluetooth;
import ru.demetrious.bluetoothdrop.bluetooth.Received;
import ru.demetrious.bluetoothdrop.bluetooth.Send;
import ru.demetrious.bluetoothdrop.explorer.Explorer;
import ru.demetrious.bluetoothdrop.explorer.ExplorerElement;
import ru.demetrious.bluetoothdrop.explorer.ExplorerElementAdapter;
import ru.demetrious.bluetoothdrop.friends.Friends;
import ru.demetrious.bluetoothdrop.friends.FriendsElement;
import ru.demetrious.bluetoothdrop.friends.FriendsElementAdapter;
import ru.demetrious.bluetoothdrop.settings.Settings;
import ru.demetrious.bluetoothdrop.settings.SettingsElement;
import ru.demetrious.bluetoothdrop.settings.SettingsElementAdapter;

public class MainActivity extends AppCompatActivity {
    public final static int TURNING_ON_BLUETOOTH = 3;
    public final static int TURNING_ON_DISCOVERABLE = 4;
    public final static int ACTIVITY_PATH = 5;
    public final static int HANDLER_CONNECTED = 4;
    public final static int HANDLER_DISCONNECTED = 5;
    public final static int HANDLER_RECEIVE_START = 13;
    public final static int HANDLER_OUT_OF_MEMORY = 14;
    public final static int HANDLER_EMPTY_SEND = 19;
    public final static int HANDLER_ERROR_CREATE_DIRECTORY = 20;
    public final static int HANDLER_ERROR_CREATE_FILE = 21;
    final static int HANDLER_SEND_START = 12;
    final static int HANDLER_STOP_TRANSFER_CLIENT = 15;
    final static int HANDLER_STOP_TRANSFER_SERVER = 16;
    final static int HANDLER_RECEIVE_HANDLER = 17;
    final static int HANDLER_LOAD_ACTIVITY_FINISH = 18;
    private final static String[] sizeUnits = new String[5];
    private final static int ACTIVITY_SELECT = 1;
    private final static int ACTIVITY_SORT = 2;
    private final static int HANDLER_TIMER = 1;
    private static Handler handler;
    private final ArrayList<ExplorerElement> explorerElements = new ArrayList<>();
    private final ArrayList<FriendsElement> friendsElements = new ArrayList<>();
    private final ArrayList<SettingsElement> settingsElements = new ArrayList<>();
    private String lastProcess = "null";
    private ArrayList<String> selectedFiles = new ArrayList<>();
    private ExplorerElementAdapter explorerElementsAdapter;
    private ArrayAdapter<String> selectedFilesAdapter;
    private FriendsElementAdapter friendsElementAdapter;
    private SettingsElementAdapter settingsElementAdapter;

    private ListView listMain;
    private Spinner listSpinner;
    private TextView textPath;
    private TextView textAmount;
    private BottomNavigationView navigation;
    private ImageButton imageButtonUp;
    private ImageButton imageButtonRefresh;
    private ImageButton imageButtonHome;
    private Explorer explorer;
    private Friends friends;
    private Send send;
    private Bluetooth bluetooth;
    private String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION};
    private ImageButton buttonSend;
    private Settings settings;

    public static String[] getSizeUnits() {
        return sizeUnits;
    }

    public static Handler getHandler() {
        return handler;
    }

    public static void setHandler(Handler handler) {
        MainActivity.handler = handler;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setExplorer(new Explorer(this));
        setFriends(new Friends(this));
        settings = new Settings(this);
        setSend(new Send(this));
        setBluetooth(new Bluetooth(this));
        new Received(this, "Received").start();

        permissions();
        declarations();
        listeners();

        getExplorer().currentDirectory = (String) Settings.getPreference(Settings.APP_PREFERENCES_CURRENT_DIRECTORY, Settings.DEFAULT_HOME_PATH, String.class);
        getExplorer().explorer();

        getBluetooth().startServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        Settings.getPreferencesEditor().putString(Settings.APP_PREFERENCES_CURRENT_DIRECTORY, getExplorer().currentDirectory).apply();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (getBluetooth().isTransferring())
            getBluetooth().getTransferDate().cancel();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(getBluetooth().getDiscoveryFinishReceiver());
        if (getFriends().getBluetoothAdapter().isDiscovering()) {
            getFriends().getBluetoothAdapter().cancelDiscovery();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bar, menu);
        menu.findItem(R.id.bar_select).setVisible(getNavigation().getSelectedItemId() == R.id.navigation_explorer);
        menu.findItem(R.id.bar_sort).setVisible(getNavigation().getSelectedItemId() == R.id.navigation_explorer);
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
                    getHandler().obtainMessage(HANDLER_TIMER, resultCode, -1).sendToTarget();
                    getImageButtonHome().setImageResource(R.drawable.ic_action_bluetooth_discoverable_on);
                    getFriends().setDiscoverable(true);
                }
                break;
            case ACTIVITY_SELECT:
                getExplorer().select(resultCode, data);
                break;
            case ACTIVITY_SORT:
                if (resultCode == Activity.RESULT_OK) {
                    assert data != null;
                    getExplorer().setSort(data.getIntExtra(SortActivity.EXTRA_SORT_BY, R.id.sort_name), data.getIntExtra(SortActivity.EXTRA_SORT, R.id.sort_asc), data.getBooleanExtra(SortActivity.EXTRA_SORT_IGNORE_CASE, true), data.getBooleanExtra(SortActivity.EXTRA_SORT_FOLDERS, true));
                }
                break;
            case ACTIVITY_PATH:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    String id = data.getStringExtra(PathActivity.EXTRA_PARENT_SETTING);
                    String current = data.getStringExtra(PathActivity.EXTRA_CURRENT_DIR);
                    getSettingsElements().get(Integer.parseInt(id.replaceAll("ru.demetrious.bluetoothdrop.setting", ""))).setDescription(current);
                    Settings.getPreferencesEditor().putString(id, current).apply();
                    getSettingsElementAdapter().notifyDataSetChanged();
                }
                break;
            default:
                Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_LONG).show();
                System.exit(32342);
        }
    }

    private void listeners() {
        getNavigation().setOnNavigationItemSelectedListener(menuItem -> {
            invalidateOptionsMenu();
            switch (menuItem.getItemId()) {
                case R.id.navigation_explorer:
                    getExplorer().explorer();
                    return true;
                case R.id.navigation_friends:
                    if (getFriends().checkBluetooth("onNavigationItemSelected")) {
                        getFriends().friends();
                        return true;
                    }
                    return false;
                case R.id.navigation_settings:
                    settings.settings();
                    return true;
            }
            return false;
        });

        getNavigation().setOnNavigationItemReselectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.navigation_explorer:
                    getExplorer().showDirectory(new File(getExplorer().currentDirectory));
                    break;
                case R.id.navigation_friends:
                    getFriends().startDiscovery();
                    break;
                case R.id.navigation_settings:
                    break;
            }
        });

        getListMain().setOnItemClickListener((parent, view, position, id) -> {
            if (getListMain().getAdapter().equals(getExplorerElementsAdapter())) {
                ExplorerElement explorerElement = getExplorerElements().get(position);

                if (explorerElement.isFolder())
                    getExplorer().showDirectory(new File(getExplorer().currentDirectory + "/" + explorerElement.getName()));
            } else if (getListMain().getAdapter().equals(getFriendsElementAdapter())) {
                FriendsElement element = getFriendsElements().get(position);
                if (getBluetooth().getDevice() != null) {
                    getBluetooth().getTransferDate().stop();
                } else {
                    getBluetooth().connect(element.getBluetoothDevice());
                }
            }
        });

        buttonSend.setOnClickListener(v -> {
            if (!getExplorer().selectedFiles.isEmpty() && getBluetooth().getDevice() != null) {
                Intent intentLoad = new Intent(MainActivity.this, LoadActivity.class);
                intentLoad.putExtra(LoadActivity.EXTRA_IS_SERVER, false);
                startActivity(intentLoad);
            }
        });

        getImageButtonUp().setOnClickListener(v -> imageButtonUp());

        getImageButtonRefresh().setOnClickListener(v -> {
            switch (getNavigation().getSelectedItemId()) {
                case R.id.navigation_explorer:
                    getExplorer().showDirectory(new File(getExplorer().currentDirectory));
                    break;
                case R.id.navigation_friends:
                    getFriends().showBoundedDevices();
                    getFriends().startDiscovery();
                    break;
            }
        });

        getImageButtonHome().setOnClickListener(v -> {
            switch (getNavigation().getSelectedItemId()) {
                case R.id.navigation_explorer:
                    getExplorer().showDirectory(new File((String) Settings.getPreference(Settings.APP_SETTING_HOME_PATH, Settings.DEFAULT_HOME_PATH, String.class)));
                    break;
                case R.id.navigation_friends:
                    getFriends().enableDiscoveryMode();
                    break;
            }
        });
    }

    private boolean imageButtonUp() {
        if (!getExplorer().currentDirectory.equals(getExplorer().getGlobalFileDir().getAbsolutePath())) {
            getExplorer().showDirectory(new File(getExplorer().currentDirectory).getParentFile());
            return true;
        }
        return false;
    }

    private void declarations() {
        getSizeUnits()[0] = getString(R.string.sizeUnits_byte);
        getSizeUnits()[1] = getString(R.string.sizeUnits_kilobyte);
        getSizeUnits()[2] = getString(R.string.sizeUnits_megabyte);
        getSizeUnits()[3] = getString(R.string.sizeUnits_gigabyte);
        getSizeUnits()[4] = getString(R.string.sizeUnits_terabyte);

        setNavigation(findViewById(R.id.navigation));

        setListMain(findViewById(R.id.list_main));
        setExplorerElementsAdapter(new ExplorerElementAdapter(this, R.layout.advanced_list_explorer, getExplorerElements()));
        getListMain().setAdapter(getExplorerElementsAdapter());

        setFriendsElementAdapter(new FriendsElementAdapter(this, R.layout.advanced_list_friends, getFriendsElements()));
        setSettingsElementAdapter(new SettingsElementAdapter(this, R.layout.advanced_list_settings, getSettingsElements()));

        setListSpinner(findViewById(R.id.list_spinner));
        setSelectedFilesAdapter(new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, getSelectedFiles()) {
            @Override
            public void notifyDataSetChanged() {
                buttonSend.setVisibility((!getExplorer().selectedFiles.isEmpty() && getBluetooth().getDevice() != null) ? View.VISIBLE : View.GONE);
                super.notifyDataSetChanged();
            }
        });
        getListSpinner().setAdapter(getSelectedFilesAdapter());

        setTextPath(findViewById(R.id.text_path));
        setTextAmount(findViewById(R.id.text_amount));

        setImageButtonHome(findViewById(R.id.imageButton_home));
        setImageButtonRefresh(findViewById(R.id.imageButton_refresh));
        setImageButtonUp(findViewById(R.id.imageButton_up));

        buttonSend = findViewById(R.id.floatingActionButton_send);

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(getBluetooth().getDiscoveryFinishReceiver(), intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(getBluetooth().getDiscoveryFinishReceiver(), intentFilter);

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(getBluetooth().getDiscoveryFinishReceiver(), intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(getBluetooth().getDiscoveryFinishReceiver(), intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(getBluetooth().getDiscoveryFinishReceiver(), intentFilter);

        setHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case HANDLER_TIMER:
                        Message message = new Message();
                        message.what = HANDLER_TIMER;
                        message.arg1 = msg.arg1 - 1;
                        if (msg.arg1 > 0) {
                            if (getNavigation().getSelectedItemId() == R.id.navigation_friends)
                                getTextPath().setText(MessageFormat.format("{0} {1}", getString(R.string.bluetooth_discoverable_timer), secondsToTime(msg.arg1)));
                            getHandler().sendMessageDelayed(message, 1000);
                        } else {
                            getTextPath().setText("");
                            getImageButtonHome().setImageResource(R.drawable.ic_action_bluetooth_discoverable_off);
                            getFriends().setDiscoverable(false);
                        }
                        break;
                    case HANDLER_CONNECTED:
                        if (!getExplorer().selectedFiles.isEmpty())
                            buttonSend.setVisibility(View.VISIBLE);
                        boolean exist = false;
                        for (int i = 0; i < getFriendsElements().size(); i++) {
                            if (getFriendsElements().get(i).getBluetoothDevice().getAddress().equals(getBluetooth().getDevice().getAddress())) {
                                getFriendsElements().get(i).setOnline();
                                exist = true;
                                break;
                            }
                        }
                        if (!exist)
                            getFriendsElements().add(0, new FriendsElement(getBluetooth().getDevice(), true));
                        getFriendsElementAdapter().notifyDataSetChanged();
                        break;
                    case HANDLER_DISCONNECTED:
                        if (!getExplorer().selectedFiles.isEmpty())
                            buttonSend.setVisibility(View.GONE);
                        getFriendsElementAdapter().notifyDataSetChanged();
                        if (getFriends().getBluetoothAdapter().isEnabled())
                            getBluetooth().startServer();
                        break;
                    case HANDLER_SEND_START:
                        getBluetooth().setHandlerLoadActivity((Handler) msg.obj);
                        getBluetooth().setTransferring(true);
                        getBluetooth().setServer(false);
                        getSend().send();
                        break;
                    case HANDLER_RECEIVE_START:
                        getBluetooth().setTransferring(true);
                        getBluetooth().setServer(true);
                        Intent intentLoad = new Intent(MainActivity.this, LoadActivity.class);
                        intentLoad.putExtra(LoadActivity.EXTRA_IS_SERVER, true);
                        startActivity(intentLoad);
                        break;
                    case HANDLER_RECEIVE_HANDLER:
                        getBluetooth().setHandlerLoadActivity((Handler) msg.obj);
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
                        break;
                    case HANDLER_STOP_TRANSFER_SERVER:
                        getBluetooth().getTransferDate().cancel();
                        break;
                    case HANDLER_LOAD_ACTIVITY_FINISH:
                        getBluetooth().setHandlerLoadActivity(null);
                        break;
                }
            }
        });
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
        switch (getLastProcess()) {
            case "":
                break;
            case "onNavigationItemSelected":
                getNavigation().setSelectedItemId(R.id.navigation_friends);
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

    public ArrayList<ExplorerElement> getExplorerElements() {
        return explorerElements;
    }

    public ArrayList<FriendsElement> getFriendsElements() {
        return friendsElements;
    }

    public ArrayList<SettingsElement> getSettingsElements() {
        return settingsElements;
    }

    public String getLastProcess() {
        return lastProcess;
    }

    public void setLastProcess(String lastProcess) {
        this.lastProcess = lastProcess;
    }

    public ArrayList<String> getSelectedFiles() {
        return selectedFiles;
    }

    public ExplorerElementAdapter getExplorerElementsAdapter() {
        return explorerElementsAdapter;
    }

    public void setExplorerElementsAdapter(ExplorerElementAdapter explorerElementsAdapter) {
        this.explorerElementsAdapter = explorerElementsAdapter;
    }

    public ArrayAdapter<String> getSelectedFilesAdapter() {
        return selectedFilesAdapter;
    }

    public void setSelectedFilesAdapter(ArrayAdapter<String> selectedFilesAdapter) {
        this.selectedFilesAdapter = selectedFilesAdapter;
    }

    public FriendsElementAdapter getFriendsElementAdapter() {
        return friendsElementAdapter;
    }

    public void setFriendsElementAdapter(FriendsElementAdapter friendsElementAdapter) {
        this.friendsElementAdapter = friendsElementAdapter;
    }

    public SettingsElementAdapter getSettingsElementAdapter() {
        return settingsElementAdapter;
    }

    public void setSettingsElementAdapter(SettingsElementAdapter settingsElementAdapter) {
        this.settingsElementAdapter = settingsElementAdapter;
    }

    public ListView getListMain() {
        return listMain;
    }

    public void setListMain(ListView listMain) {
        this.listMain = listMain;
    }

    public Spinner getListSpinner() {
        return listSpinner;
    }

    public void setListSpinner(Spinner listSpinner) {
        this.listSpinner = listSpinner;
    }

    public TextView getTextPath() {
        return textPath;
    }

    public void setTextPath(TextView textPath) {
        this.textPath = textPath;
    }

    public TextView getTextAmount() {
        return textAmount;
    }

    public void setTextAmount(TextView textAmount) {
        this.textAmount = textAmount;
    }

    public BottomNavigationView getNavigation() {
        return navigation;
    }

    public void setNavigation(BottomNavigationView navigation) {
        this.navigation = navigation;
    }

    public ImageButton getImageButtonUp() {
        return imageButtonUp;
    }

    public void setImageButtonUp(ImageButton imageButtonUp) {
        this.imageButtonUp = imageButtonUp;
    }

    public ImageButton getImageButtonRefresh() {
        return imageButtonRefresh;
    }

    public void setImageButtonRefresh(ImageButton imageButtonRefresh) {
        this.imageButtonRefresh = imageButtonRefresh;
    }

    public ImageButton getImageButtonHome() {
        return imageButtonHome;
    }

    public void setImageButtonHome(ImageButton imageButtonHome) {
        this.imageButtonHome = imageButtonHome;
    }

    public Explorer getExplorer() {
        return explorer;
    }

    public void setExplorer(Explorer explorer) {
        this.explorer = explorer;
    }

    public Friends getFriends() {
        return friends;
    }

    public void setFriends(Friends friends) {
        this.friends = friends;
    }

    public Send getSend() {
        return send;
    }

    public void setSend(Send send) {
        this.send = send;
    }

    public Bluetooth getBluetooth() {
        return bluetooth;
    }

    public void setBluetooth(Bluetooth bluetooth) {
        this.bluetooth = bluetooth;
    }
}


