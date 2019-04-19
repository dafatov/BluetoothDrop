package ru.demetrious.bluetoothdrop;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

class Friends {
    private MainActivity mainActivity;
    BluetoothAdapter bluetoothAdapter;

    final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Toast.makeText(mainActivity.getApplicationContext(), "Start discovery...", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (!device.getName().equals("")) {
                        mainActivity.friendsElements.add(new FriendsElement(device, true));
                        mainActivity.friendsElementAdapter.notifyDataSetChanged();
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Toast.makeText(mainActivity.getApplicationContext(), "Stop discovery...", Toast.LENGTH_SHORT).show();
                    if (mainActivity.friendsElements.size() == 0) {
                        Toast.makeText(mainActivity.getApplicationContext(), "Noup found", Toast.LENGTH_LONG).show();
                    }
                    break;
                default:
            }
        }
    };

    Friends(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    void friends() {
        mainActivity.listMain.setAdapter(mainActivity.friendsElementAdapter);

        mainActivity.listSpinner.setVisibility(View.INVISIBLE);
        mainActivity.imageButtonUp.setVisibility(View.INVISIBLE);
        mainActivity.textAmount.setVisibility(View.INVISIBLE);
        mainActivity.textAmount.setText("");


        enableDiscoveryMode();
        showBoundedDevices();
    }

    void enableDiscoveryMode() {
        if (bluetoothAdapter != null) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //discoverableIntent.putExtra("BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION", 300);
            mainActivity.startActivityForResult(discoverableIntent, MainActivity.TURNING_ON_DISCOVERABLE);
        }
    }

    void showBoundedDevices() {
        mainActivity.friendsElements.clear();
        if (checkBluetooth("showBoundedDevices")) {
            for (BluetoothDevice bondedDevice : bluetoothAdapter.getBondedDevices()) {
                mainActivity.friendsElements.add(new FriendsElement(bondedDevice, false));
            }
        }
        mainActivity.friendsElementAdapter.notifyDataSetChanged();
    }

    void showDiscoveringDevices() {
        mainActivity.friendsElements.clear();

        if (checkBluetooth("showDiscoveringDevices")) {
            stopDiscovery();
            bluetoothAdapter.startDiscovery();
        }
    }

    private void stopDiscovery() {
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
    }

    /*void showDiscoveringDevices() {
        mainActivity.friendsElements.clear();
        bluetoothAdapter.startDiscovery();
        Toast.makeText(mainActivity.getApplicationContext() , "Starting discovery...", Toast.LENGTH_SHORT).show();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        mainActivity.friendsElements.add(new FriendsElement(device, true));
                        mainActivity.friendsElementAdapter.notifyDataSetChanged();
                        break;
                }
            }
        };
        mainActivity.registerReceiver(receiver, intentFilter);
    }*/

    private boolean checkBluetooth(String process) {
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
            Toast.makeText(mainActivity.getApplicationContext(), "Bluetooth module is not available on this device", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
