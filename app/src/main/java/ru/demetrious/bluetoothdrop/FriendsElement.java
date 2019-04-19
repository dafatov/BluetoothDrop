package ru.demetrious.bluetoothdrop;

import android.bluetooth.BluetoothDevice;
import android.os.Parcelable;

public class FriendsElement {
    private BluetoothDevice bluetoothDevice;
    private boolean isOnline;

    FriendsElement(BluetoothDevice bluetoothDevice, boolean isOnline) {
        this.bluetoothDevice = bluetoothDevice;
        this.isOnline = isOnline;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
