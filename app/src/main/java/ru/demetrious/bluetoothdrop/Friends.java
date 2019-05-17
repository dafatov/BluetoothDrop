package ru.demetrious.bluetoothdrop;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

class Friends {
    private MainActivity mainActivity;
    BluetoothAdapter bluetoothAdapter;

    boolean isDiscoverable = false;

    final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    boolean isBonded = false;
                    for (FriendsElement friendsElement : mainActivity.friendsElements) {
                        if (friendsElement.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                            friendsElement.setOnline(true);
                            isBonded = true;
                            break;
                        }
                    }
                    if (!isBonded) {
                        mainActivity.friendsElements.add(new FriendsElement(device, true));
                    }
                    mainActivity.friendsElementAdapter.notifyDataSetChanged();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON)) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                            //Log.e("BluetoothState", "STATE_TURNING_ON");
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            //Log.e("BluetoothState", "STATE_TURNING_OFF");
                            if (mainActivity.bluetooth.isTransferring)
                                mainActivity.bluetooth.transferDate.cancel();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            //Log.e("BluetoothState", "STATE_OFF");
                            mainActivity.bluetooth.server.stop();
                            if (mainActivity.navigation.getSelectedItemId() == R.id.navigation_friends) {
                                mainActivity.navigation.setSelectedItemId(R.id.navigation_explorer);
                                mainActivity.explorer.explorer();
                            }
                            break;
                        case BluetoothAdapter.STATE_ON:
                            //Log.e("BluetoothState", "STATE_ON");
                            mainActivity.bluetooth.startServer();
                            break;
                    }
                    break;
            }
        }
    };

    Friends(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    void friends() {
        mainActivity.listMain.setAdapter(mainActivity.friendsElementAdapter);

        mainActivity.imageButtonUp.setVisibility(View.INVISIBLE);
        mainActivity.textAmount.setVisibility(View.INVISIBLE);
        mainActivity.textPath.setVisibility(View.VISIBLE);
        mainActivity.imageButtonHome.setVisibility(View.VISIBLE);
        mainActivity.listSpinner.setVisibility(View.VISIBLE);
        mainActivity.imageButtonRefresh.setVisibility(View.VISIBLE);

        mainActivity.imageButtonHome.setImageResource(R.drawable.ic_action_bluetooth_discoverable_off);
        mainActivity.textPath.setText("");

        showBoundedDevices();
        startDiscovery();
    }

    void enableDiscoveryMode() {
        if (bluetoothAdapter != null && !isDiscoverable) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, (int) Settings.getSetting(Settings.APP_SETTING_DISCOVERABLE_TIME, 30, Integer.class));
            mainActivity.startActivityForResult(discoverableIntent, MainActivity.TURNING_ON_DISCOVERABLE);
        }
    }

    void showBoundedDevices() {
        mainActivity.friendsElements.clear();
        for (BluetoothDevice bondedDevice : bluetoothAdapter.getBondedDevices()) {
            mainActivity.friendsElements.add(new FriendsElement(bondedDevice, false));
        }
        mainActivity.friendsElementAdapter.notifyDataSetChanged();
    }

    void startDiscovery() {
        Log.e("Friends", "showDiscoveringDevices");

        stopDiscovery();
        bluetoothAdapter.startDiscovery();
    }

    private void stopDiscovery() {
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
    }

    boolean checkBluetooth(String process) {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                return true;
            } else {
                Intent turningOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mainActivity.lastProcess = process;
                mainActivity.startActivityForResult(turningOn, MainActivity.TURNING_ON_BLUETOOTH);
                return false;
            }
        } else {
            Toast.makeText(mainActivity.getApplicationContext(), mainActivity.getString(R.string.bluetooth_not_available), Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
