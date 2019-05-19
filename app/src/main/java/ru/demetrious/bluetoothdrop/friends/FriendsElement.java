package ru.demetrious.bluetoothdrop.friends;

import android.bluetooth.BluetoothDevice;

public class FriendsElement {
    private BluetoothDevice bluetoothDevice;
    private boolean isOnline;

    public FriendsElement(BluetoothDevice bluetoothDevice, boolean isOnline) {
        this.bluetoothDevice = bluetoothDevice;
        this.isOnline = isOnline;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    boolean isOnline() {
        return isOnline;
    }

    public void setOnline() {
        isOnline = true;
    }
}
