package ru.demetrious.bluetoothdrop.friends;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.activities.MainActivity;
import ru.demetrious.bluetoothdrop.settings.Settings;

public class Friends {
    private BluetoothAdapter bluetoothAdapter;
    private boolean isDiscoverable = false;
    private MainActivity mainActivity;

    public Friends(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void friends() {
        mainActivity.getListMain().setAdapter(mainActivity.getFriendsElementAdapter());

        mainActivity.getImageButtonUp().setVisibility(View.INVISIBLE);
        mainActivity.getTextAmount().setVisibility(View.INVISIBLE);
        mainActivity.getTextPath().setVisibility(View.VISIBLE);
        mainActivity.getImageButtonHome().setVisibility(View.VISIBLE);
        mainActivity.getListSpinner().setVisibility(View.VISIBLE);
        mainActivity.getImageButtonRefresh().setVisibility(View.VISIBLE);

        mainActivity.getImageButtonHome().setImageResource(R.drawable.ic_action_bluetooth_discoverable_off);
        mainActivity.getTextPath().setText("");

        showBoundedDevices();
        startDiscovery();
    }

    public void enableDiscoveryMode() {
        if (getBluetoothAdapter() != null && !isDiscoverable) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, (int) Settings.getSetting(Settings.APP_SETTING_DISCOVERABLE_TIME, 30, Integer.class));
            mainActivity.startActivityForResult(discoverableIntent, MainActivity.TURNING_ON_DISCOVERABLE);
        }
    }

    public void showBoundedDevices() {
        mainActivity.getFriendsElements().clear();
        for (BluetoothDevice bondedDevice : getBluetoothAdapter().getBondedDevices()) {
            mainActivity.getFriendsElements().add(new FriendsElement(bondedDevice, false));
        }
        mainActivity.getFriendsElementAdapter().notifyDataSetChanged();
    }

    public void startDiscovery() {
        Log.e("Friends", "showDiscoveringDevices");

        stopDiscovery();
        getBluetoothAdapter().startDiscovery();
    }

    private void stopDiscovery() {
        if (getBluetoothAdapter().isDiscovering()) getBluetoothAdapter().cancelDiscovery();
    }

    public boolean checkBluetooth(String process) {
        if (getBluetoothAdapter() != null) {
            if (getBluetoothAdapter().isEnabled()) {
                return true;
            } else {
                Intent turningOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mainActivity.setLastProcess(process);
                mainActivity.startActivityForResult(turningOn, MainActivity.TURNING_ON_BLUETOOTH);
                return false;
            }
        } else {
            Toast.makeText(mainActivity.getApplicationContext(), mainActivity.getString(R.string.bluetooth_not_available), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public void setDiscoverable(boolean discoverable) {
        isDiscoverable = discoverable;
    }


}
